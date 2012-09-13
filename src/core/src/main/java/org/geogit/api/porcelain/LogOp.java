/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.DiffEntry;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RevParse;
import org.geogit.repository.Repository;
import org.geotools.util.Range;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;

/**
 * Operation to query the commits logs.
 * <p>
 * The list of commits to return can be filtered by setting the following properties:
 * <ul>
 * <li> {@link #setLimit(int) limit}: Limits the number of commits to return.
 * <li> {@link #setTimeRange(Range) timeRange}: return commits that fall in to the given time range.
 * <li> {@link #setSince(ObjectId) since}...{@link #setUntil(ObjectId) until}: Show only commits
 * between the named two commits.
 * <li> {@link #addPath(List) addPath}: Show only commits that affect any of the specified paths.
 * </ul>
 * </p>
 * 
 * @author groldan
 * 
 */
public class LogOp extends AbstractGeoGitOp<Iterator<RevCommit>> {

    private static final Range<Long> ALWAYS = new Range<Long>(Long.class, 0L, true, Long.MAX_VALUE,
            true);

    private Range<Long> timeRange;

    private Integer limit;

    private ObjectId since;

    private ObjectId until;

    private Set<List<String>> paths;

    private Repository repository;

    @Inject
    public LogOp(final Repository repository) {
        this.repository = repository;
        timeRange = ALWAYS;
    }

    public LogOp setLimit(int limit) {
        Preconditions.checkArgument(limit > 0, "limit shall be > 0: " + limit);
        this.limit = Integer.valueOf(limit);
        return this;
    }

    /**
     * Indicates to return only commits newer than the given one ({@code since} is exclusive)
     * 
     * @param the initial (oldest and exclusive) commit id, ({@code null} sets the default)
     * @return
     * @see #setUntil(ObjectId)
     */
    public LogOp setSince(final ObjectId since) {
        this.since = since;
        return this;
    }

    /**
     * Indicates to return commits up to the provided one, inclusive.
     * 
     * @param the final (newest and inclusive) commit id, ({@code null} sets the default)
     * @return
     * @see #setSince(ObjectId)
     */
    public LogOp setUntil(ObjectId until) {
        this.until = until;
        return this;
    }

    /**
     * @see #addPath(List)
     */
    public LogOp addPath(final String... path) {
        Preconditions.checkNotNull(path);
        return addPath(Arrays.asList(path));
    }

    /**
     * Show only commits that affect any of the specified paths.
     * 
     * @param path
     * @return
     */
    public LogOp addPath(final List<String> path) {
        Preconditions.checkNotNull(path);

        if (this.paths == null) {
            this.paths = new HashSet<List<String>>();
        }
        this.paths.add(ImmutableList.copyOf(path));
        return this;
    }

    public LogOp setTimeRange(final Range<Date> commitRange) {
        if (commitRange == null) {
            this.timeRange = ALWAYS;
        } else {
            this.timeRange = new Range<Long>(Long.class, commitRange.getMinValue().getTime(),
                    commitRange.isMinIncluded(), commitRange.getMaxValue().getTime(),
                    commitRange.isMaxIncluded());
        }
        return this;
    }

    /**
     * @return the list of commits that satisfy the query criteria, most recent first.
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    @Override
    public Iterator<RevCommit> call() throws Exception {

        ObjectId newestCommitId;
        ObjectId oldestCommitId;
        {
            if (this.until == null) {
                newestCommitId = command(RevParse.class).setRefSpec(Ref.HEAD).call();
            } else {
                if (!repository.commitExists(this.until)) {
                    throw new IllegalStateException("Provided 'until' commit id does not exist: "
                            + until.toString());
                }
                newestCommitId = this.until;
            }
            if (this.since == null) {
                oldestCommitId = ObjectId.NULL;
            } else {
                if (!ObjectId.NULL.equals(this.since) && !repository.commitExists(this.since)) {
                    throw new IllegalStateException("Provided 'since' commit id does not exist: "
                            + since.toString());
                }
                oldestCommitId = this.since;
            }
        }

        Iterator<RevCommit> linearHistory = new LinearHistoryIterator(newestCommitId, repository);
        LogFilter filter = new LogFilter(repository, oldestCommitId, timeRange, paths);
        Iterator<RevCommit> filteredCommits = Iterators.filter(linearHistory, filter);
        if (limit != null) {
            filteredCommits = Iterators.limit(filteredCommits, limit.intValue());
        }
        return filteredCommits;
    }

    /**
     * Iterator that traverses the commit history backwards starting from the provided commmit
     * 
     * @author groldan
     * 
     */
    private static class LinearHistoryIterator extends AbstractIterator<RevCommit> {

        private ObjectId nextCommitId;

        private final Repository repo;

        public LinearHistoryIterator(final ObjectId tip, final Repository repo) {
            this.nextCommitId = tip;
            this.repo = repo;
        }

        @Override
        protected RevCommit computeNext() {
            if (nextCommitId.isNull()) {
                return endOfData();
            }
            final RevCommit commit = repo.getCommit(nextCommitId);
            List<ObjectId> parentIds = commit.getParentIds();
            Preconditions.checkNotNull(parentIds);
            Preconditions.checkState(parentIds.size() > 0);

            nextCommitId = commit.getParentIds().get(0);

            return commit;
        }

    }

    /**
     * Checks whether the given commit satisfies all the filter criteria set to this op.
     * 
     * @return {@code true} if the commit satisfies the filter criteria set to this op
     */
    private static class LogFilter implements Predicate<RevCommit> {

        private boolean toReached;

        private final ObjectId oldestCommitId;

        private final Range<Long> timeRange;

        private final Set<List<String>> paths;

        private final Repository repo;

        /**
         * @param repo the repository where to get the commits from
         * @param oldestCommitId the oldest commit, exclusive. Indicates when to stop evaluating.
         * @param timeRange extra time range filter besides oldest commit
         * @param paths extra filter on content, indicates to return only commits that affected any
         *        of the provided paths
         */
        public LogFilter(final Repository repo, final ObjectId oldestCommitId,
                final Range<Long> timeRange, final Set<List<String>> paths) {
            Preconditions.checkNotNull(repo);
            Preconditions.checkNotNull(oldestCommitId);
            Preconditions.checkNotNull(timeRange);

            this.repo = repo;
            this.oldestCommitId = oldestCommitId;
            this.timeRange = timeRange;
            this.paths = paths;
        }

        /**
         * @return {@code true} if the commit satisfies the filter criteria set to this op
         * @see com.google.common.base.Predicate#apply(java.lang.Object)
         */
        @Override
        public boolean apply(final RevCommit commit) {
            if (toReached) {
                return false;
            }
            if (oldestCommitId.equals(commit.getId())) {
                toReached = true;
                return false;
            }
            boolean applies = timeRange.contains(Long.valueOf(commit.getTimestamp()));
            if (!applies) {
                return false;
            }
            if (paths != null && paths.size() > 0) {
                // did this commit touch any of the paths?
                for (List<String> path : paths) {
                    DiffOp diff = new DiffOp(repo);
                    ObjectId parentId = commit.getParentIds().get(0);
                    Iterator<DiffEntry> diffResult;
                    try {
                        diff.setOldVersion(parentId).setNewVersion(commit.getId()).setFilter(path);
                        diffResult = diff.call();
                        applies = applies && diffResult.hasNext();
                        if (applies) {
                            break;
                        }
                    } catch (Exception e) {
                        Throwables.propagate(e);
                    }
                }
            }

            return applies;
        }
    }
}
