/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.versioning.decorator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.geogit.api.DiffEntry;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.repository.Repository;
import org.geogit.storage.StagingDatabase;
import org.geotools.data.Query;
import org.geotools.util.DateRange;
import org.geotools.util.Range;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.ResourceId;
import org.opengis.filter.identity.Version;
import org.opengis.filter.identity.Version.Action;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

public class VersionQuery {
    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geoserver.data.geogit.decorator");

    private final Name typeName;

    private final GeoGIT ggit;

    public VersionQuery(final GeoGIT ggit, final Name typeName) {
        this.ggit = ggit;
        this.typeName = typeName;
    }

    public Iterator<NodeRef> getByQuery(Query query) {
        VersionDetail vDetail = VersionDetail.extractVersionDetails(query);
        if (vDetail == null) {
            return Iterators.emptyIterator();
        }
        VersionDetail.VersionType vType = vDetail.getType();

        switch (vType) {
        case Action:
            return fetchByAction(vDetail.getAction()).iterator();
        case Date:
            return fetchByDate(vDetail.getDate()).iterator();
        case DateRange:
            return fetchByRange(vDetail.getRange()).iterator();
        case Index:
            return fetchByIndex(vDetail.getIndex()).iterator();
        }
        return Iterators.emptyIterator();
    }

    public Iterator<NodeRef> filterByQueryVersion(Iterator<NodeRef> refs, Query query) {
        VersionDetail vDetail = VersionDetail.extractVersionDetails(query);
        if (vDetail == null) {
            return refs;
        }
        VersionDetail.VersionType vType = vDetail.getType();

        switch (vType) {
        case Action:
            return filterByAction(refs, vDetail.getAction()).iterator();
        case Date:
            return filterByDate(refs, vDetail.getDate()).iterator();
        case DateRange:
            return filterByRange(refs, vDetail.getRange()).iterator();
        case Index:
            return filterByIndex(refs, vDetail.getIndex()).iterator();
        }
        return Iterators.emptyIterator();
    }

    private List<NodeRef> fetchByDate(final Date date) {
        LogOp logOp = ggit.log().addPath(typeNamePath());
        try {
            Iterator<RevCommit> featureCommits = logOp.call();
            RevCommit commit = findClosest(date, featureCommits);
            return getRefsByCommit(commit);
        } catch (Exception ex) {
            /*
             * Need some logging.
             */
            return Collections.emptyList();
        }
    }

    private List<NodeRef> filterByDate(Iterator<NodeRef> refs, final Date date) {
        List<NodeRef> dateRefs = fetchByDate(date);
        return filterIteratorByList(dateRefs, refs);
    }

    private List<NodeRef> fetchByRange(DateRange range) {
        return null;
    }

    private List<NodeRef> filterByRange(Iterator<NodeRef> refs, DateRange range) {
        List<NodeRef> rangeRefs = fetchByRange(range);
        return filterIteratorByList(rangeRefs, refs);
    }

    private List<NodeRef> fetchByAction(Version.Action action) {
        List<NodeRef> featureRefs = new ArrayList<NodeRef>();
        if (Version.Action.ALL.equals(action)) {
            LogOp logOp = ggit.log().addPath(typeNamePath());
            try {
                Iterator<RevCommit> featureCommits = logOp.call();
                while (featureCommits.hasNext()) {
                    featureRefs.addAll(getRefsByCommit(featureCommits.next()));
                }
            } catch (Exception ex) {
                return Collections.emptyList();
            }

        } else if (Version.Action.NEXT.equals(action) || Version.Action.PREVIOUS.equals(action)) {
            /*
             * There is no reference point for NEXT/PREVIOUS, so we're going to leave them with
             * nothing.
             */
            return featureRefs;
        } else if (Version.Action.FIRST.equals(action)) {
            featureRefs = fetchByIndex(1);
        } else if (Version.Action.LAST.equals(action)) {
            LogOp logOp = ggit.log().addPath(typeNamePath()).setLimit(1);
            try {
                Iterator<RevCommit> featureCommits = logOp.call();
                if (featureCommits.hasNext()) {
                    RevCommit commit = featureCommits.next();
                    return getRefsByCommit(commit);
                }
            } catch (Exception ex) {
                return Collections.emptyList();
            }

        }
        return featureRefs;
    }

    private List<NodeRef> filterByAction(Iterator<NodeRef> refs, Version.Action action) {
        if (Version.Action.ALL.equals(action)) {
            List<NodeRef> newRefs = new ArrayList<NodeRef>();
            while (refs.hasNext()) {
                NodeRef ref = refs.next();
                newRefs.add(ref);
            }
            return newRefs;
        } else if (Version.Action.NEXT.equals(action)) {
            // iterate through refs and retrieve the previous version
            return Collections.emptyList();
        } else if (Version.Action.PREVIOUS.equals(action)) {
            // iterate through refs and retrieve the next version
            return Collections.emptyList();
        }
        List<NodeRef> actionRefs = fetchByAction(action);
        return filterIteratorByList(actionRefs, refs);
    }

    private List<NodeRef> fetchByIndex(int index) {
        LogOp logOp = ggit.log().addPath(typeNamePath());
        try {
            Iterator<RevCommit> featureCommits = logOp.call();
            RevCommit[] commitTrail = new RevCommit[index];
            int ind = 0;
            int count = 0;
            RevCommit latest = null;
            RevCommit target = null;
            while (featureCommits.hasNext()) {
                RevCommit commit = featureCommits.next();
                if (latest == null) {
                    latest = commit;
                }
                commitTrail[ind] = commit;
                ind = (ind + 1) % index;
                count++;
            }
            if (count < index)
                target = latest;
            else
                target = commitTrail[(ind + 1) % index];
            return getRefsByCommit(target);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<NodeRef> filterByIndex(Iterator<NodeRef> refs, int index) {
        List<NodeRef> indexRefs = fetchByIndex(index);
        return filterIteratorByList(indexRefs, refs);
    }

    private List<NodeRef> getRefsByCommit(RevCommit commit) {
        List<NodeRef> treeRefs = new ArrayList<NodeRef>();
        if (commit != null) {
            ObjectId commitTreeId = commit.getTreeId();
            RevTree commitTree = ggit.getRepository().getTree(commitTreeId);
            NodeRef typeRef = commitTree.get(typeName.getLocalPart());
            RevTree typeTree = ggit.getRepository().getTree(typeRef.getObjectId());
            Iterator<NodeRef> it = typeTree.iterator(null);

            while (it.hasNext()) {
                NodeRef nextRef = it.next();
                treeRefs.add(nextRef);
            }
        }
        return treeRefs;
    }

    private List<NodeRef> filterIteratorByList(List<NodeRef> refList, Iterator<NodeRef> refs) {
        Preconditions.checkNotNull(refs);
        Preconditions.checkNotNull(refList);
        List<NodeRef> newRefs = new ArrayList<NodeRef>();
        while (refs.hasNext()) {
            NodeRef ref = refs.next();
            if (refList.contains(ref)) {
                newRefs.add(ref);
            }
        }
        return newRefs;
    }

    /**
     * @param id
     * @return an iterator for all the requested versions of a given feature, or the empty iterator
     *         if no such feature is found.
     * @throws Exception
     */
    public Iterator<NodeRef> get(final ResourceId id) throws Exception {
        final String featureId = id.getID();
        final String featureVersion = id.getFeatureVersion();

        final Version version = id.getVersion();
        final boolean isDateRangeQuery = id.getStartTime() != null || id.getEndTime() != null;
        final boolean isVesionQuery = !version.isEmpty();

        final NodeRef requestedVersionRef = extractRequestedVersion(ggit, featureId, featureVersion);
        {
            final boolean explicitVersionQuery = !isDateRangeQuery && !isVesionQuery;
            if (explicitVersionQuery) {
                if (requestedVersionRef == null) {
                    return Iterators.emptyIterator();
                } else {
                    // easy, no extra constraints specified
                    return Iterators.singletonIterator(requestedVersionRef);
                }
            }
        }

        // at this point is either a version query or a date range query...

        List<NodeRef> result = new ArrayList<NodeRef>(5);

        // filter commits that affect the requested feature
        final String path = path(featureId);
        LogOp logOp = ggit.log().addPath(path);

        if (isDateRangeQuery) {
            // time range query, limit commits by time range, if speficied
            Date startTime = id.getStartTime() == null ? new Date(0L) : id.getStartTime();
            Date endTime = id.getEndTime() == null ? new Date(Long.MAX_VALUE) : id.getEndTime();
            boolean isMinIncluded = true;
            boolean isMaxIncluded = true;
            Range<Date> timeRange = new Range<Date>(Date.class, startTime, isMinIncluded, endTime,
                    isMaxIncluded);
            logOp.setTimeRange(timeRange);
        }

        // all commits whose tree contains the requested feature
        Iterator<RevCommit> featureCommits = logOp.call();

        if (isDateRangeQuery) {
            List<NodeRef> allInAscendingOrder = getAllInAscendingOrder(ggit, featureCommits,
                    featureId);
            result.addAll(allInAscendingOrder);
        } else if (isVesionQuery) {
            if (version.isDateTime()) {
                final Date validAsOf = version.getDateTime();
                RevCommit closest = findClosest(validAsOf, featureCommits);
                if (closest != null) {
                    featureCommits = Iterators.singletonIterator(closest);
                    result.addAll(getAllInAscendingOrder(ggit, featureCommits, featureId));
                }
            } else if (version.isIndex()) {
                final int requestIndex = version.getIndex().intValue();
                final int listIndex = requestIndex - 1;// version indexing
                                                       // starts at 1
                List<NodeRef> allVersions = getAllInAscendingOrder(ggit, featureCommits, featureId);
                if (allVersions.size() > 0) {
                    if (allVersions.size() >= requestIndex) {
                        result.add(allVersions.get(listIndex));
                    } else {
                        result.add(allVersions.get(allVersions.size() - 1));
                    }
                }
            } else if (version.isVersionAction()) {
                final Action versionAction = version.getVersionAction();
                List<NodeRef> allInAscendingOrder = getAllInAscendingOrder(ggit, featureCommits,
                        featureId);
                switch (versionAction) {
                case ALL:
                    result.addAll(allInAscendingOrder);
                    break;
                case FIRST:
                    if (allInAscendingOrder.size() > 0) {
                        result.add(allInAscendingOrder.get(0));
                    }
                    break;
                case LAST:
                    if (allInAscendingOrder.size() > 0) {
                        result.add(allInAscendingOrder.get(allInAscendingOrder.size() - 1));
                    }
                    break;
                case NEXT:
                    NodeRef next = next(requestedVersionRef, allInAscendingOrder);
                    if (next != null) {
                        result.add(next);
                    }
                    break;
                case PREVIOUS:
                    NodeRef previous = previous(requestedVersionRef, allInAscendingOrder);
                    if (previous != null) {
                        result.add(previous);
                    }
                    break;
                default:
                    break;
                }
            }
        }
        return result.iterator();
    }

    private RevCommit findClosest(final Date date, Iterator<RevCommit> commitsInDescendingOrder) {
        final long requestedTime = date.getTime();
        RevCommit closest = null;
        while (commitsInDescendingOrder.hasNext()) {
            RevCommit current = commitsInDescendingOrder.next();
            if (closest == null) {
                closest = current;
            } else {
                long delta = Math.abs(current.getTimestamp() - requestedTime);
                long prevDelta = Math.abs(closest.getTimestamp() - requestedTime);
                if (delta < prevDelta) {
                    closest = current;
                }
            }
        }
        return closest;
    }

    private long toSecondsPrecision(final long timeStampMillis) {
        return timeStampMillis / 1000;
    }

    private NodeRef previous(NodeRef requestedVersionRef, List<NodeRef> allVersions) {
        int idx = locate(requestedVersionRef, allVersions);
        if (idx > 0) {
            return allVersions.get(idx - 1);
        }
        return null;
    }

    private NodeRef next(NodeRef requestedVersionRef, List<NodeRef> allVersions) {
        int idx = locate(requestedVersionRef, allVersions);
        if (idx > -1 && idx < allVersions.size() - 1) {
            return allVersions.get(idx + 1);
        }
        return null;
    }

    private int locate(final NodeRef requestedVersionRef, List<NodeRef> allVersions) {
        if (requestedVersionRef == null) {
            return -1;
        }
        for (int i = 0; i < allVersions.size(); i++) {
            NodeRef ref = allVersions.get(i);
            if (requestedVersionRef.equals(ref)) {
                return i;
            }
        }
        return -1;
    }

    private List<NodeRef> getAllInAscendingOrder(final GeoGIT ggit,
            final Iterator<RevCommit> commits, final String featureId) throws Exception {

        LinkedList<NodeRef> featureRefs = new LinkedList<NodeRef>();

        final String path = path(featureId);
        // find all commits where this feature is touched
        while (commits.hasNext()) {
            RevCommit commit = commits.next();
            ObjectId commitId = commit.getId();
            ObjectId parentCommitId = commit.getParentIds().get(0);
            DiffOp diffOp = ggit.diff().setOldVersion(parentCommitId).setNewVersion(commitId)
                    .setFilter(path);
            Iterator<DiffEntry> diffs = diffOp.call();
            Preconditions.checkState(diffs.hasNext());
            DiffEntry diff = diffs.next();
            Preconditions.checkState(!diffs.hasNext());
            switch (diff.changeType()) {
            case ADDED:
            case MODIFIED:
                featureRefs.addFirst(diff.getNewObject());
                break;
            case REMOVED:
                break;
            }
        }
        return featureRefs;
    }

    /**
     * Extracts the feature version from the given {@code rid} if supplied, or finds out the current
     * feature version from the feature id otherwise.
     * 
     * @return the version identifier of the feature given by {@code version}, or at the current
     *         geogit HEAD if {@code version == null}, or {@code null} if such a feature does not
     *         exist.
     */
    private NodeRef extractRequestedVersion(final GeoGIT ggit, final String featureId,
            final String version) {
        final Repository repository = ggit.getRepository();
        if (version != null) {
            ObjectId versionedId = ObjectId.valueOf(version);
            // verify the object exists
            StagingDatabase stagingDatabase = repository.getIndex().getDatabase();
            boolean exists = stagingDatabase.exists(versionedId);
            // Ref rootTreeChild = repository.getRootTreeChild(path(featureId));
            if (exists) {
                return new NodeRef(featureId, versionedId, TYPE.FEATURE);
            }
            return null;
        }
        // no version specified, find out the latest
        String path = path(featureId);
        NodeRef currFeatureRef = repository.getRootTreeChild(path);
        if (currFeatureRef == null) {
            // feature does not exist at the current repository state
            return null;
        }
        return currFeatureRef;
    }

    private String typeNamePath() {
        return typeName.getLocalPart();
    }

    private String path(final String featureId) {
        return NodeRef.appendChild(typeNamePath(), featureId);
    }

}
