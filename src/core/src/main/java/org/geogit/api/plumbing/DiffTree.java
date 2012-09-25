/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffTreeWalk;
import org.geogit.storage.ObjectDatabase;

import com.google.inject.Inject;

/**
 * Compares the content and metadata links of blobs found via two tree objects
 */
public class DiffTree extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private ObjectId oldTreeId;

    private ObjectId newTreeId;

    private ObjectDatabase objectDb;

    private String path;

    @Inject
    public DiffTree(ObjectDatabase objectDb) {
        this.objectDb = objectDb;
    }

    /**
     * @param oldTreeId
     * @return
     */
    public DiffTree setOldTree(ObjectId oldTreeId) {
        this.oldTreeId = oldTreeId;
        return this;
    }

    /**
     * @param newTreeId
     * @return
     */
    public DiffTree setNewTree(ObjectId newTreeId) {
        this.newTreeId = newTreeId;
        return this;
    }

    public DiffTree setFilterPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public Iterator<DiffEntry> call() throws Exception {
        checkNotNull(oldTreeId);
        checkNotNull(newTreeId);

        final RevTree oldTree;
        final RevTree newTree;
        if (oldTreeId.isNull()) {
            oldTree = objectDb.newTree();
        } else {
            oldTree = (RevTree) command(RevObjectParse.class).setObjectId(oldTreeId).call();
        }
        if (newTreeId.isNull()) {
            newTree = objectDb.newTree();
        } else {
            newTree = (RevTree) command(RevObjectParse.class).setObjectId(newTreeId).call();
        }
        DiffTreeWalk treeWalk = new DiffTreeWalk(objectDb, oldTree, newTree);
        treeWalk.setFilter(this.path);
        return treeWalk.get();
    }
}
