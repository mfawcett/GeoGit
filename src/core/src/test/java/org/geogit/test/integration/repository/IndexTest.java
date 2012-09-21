/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration.repository;

import static org.geogit.api.NodeRef.appendChild;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.StagingDatabase;
import org.geogit.test.integration.PrintVisitor;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.util.NullProgressListener;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.base.Optional;

public class IndexTest extends RepositoryTestCase {

    private StagingArea index;

    @Override
    protected void setUpInternal() throws Exception {
        index = repo.getIndex();
    }

    // two features with the same content and different fid should point to the same object
    @Test
    public void testInsertIdenticalObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        Feature equalContentFeature = feature(pointsType, "DifferentId", ((SimpleFeature) points1)
                .getAttributes().toArray());

        ObjectId oId2 = insertAndAdd(equalContentFeature);

        // BLOBS.print(repo.getRawObject(insertedId1), System.err);
        // BLOBS.print(repo.getRawObject(insertedId2), System.err);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertEquals(oId1, oId2);
    }

    // two features with different content should point to different objects
    @Test
    public void testInsertNonEqualObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);

        ObjectId oId2 = insertAndAdd(points2);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertFalse(oId1.equals(oId2));
    }

    @Test
    public void testWriteTree() throws Exception {

        insertAndAdd(points1);
        insertAndAdd(lines1);

        // this new root tree must exist on the repo db, but is not set as the current head. In
        // fact, it is headless, as there's no commit pointing to it. CommitOp does that.
        ObjectId newRootTreeId = index.writeTree(repo.getHead().get());

        assertNotNull(newRootTreeId);
        assertFalse(repo.getRootTreeId().equals(newRootTreeId));
        // but the index staged root shall be pointing to it
        // assertEquals(newRootTreeId, index.getStaged().getId());

        RevTree tree = repo.getTree(newRootTreeId);
        assertEquals(2, tree.size().intValue());

        ObjectDatabase odb = repo.getObjectDatabase();

        assertNotNull(odb.getTreeChild(tree,
                appendChild(pointsName, points1.getIdentifier().getID())));

        assertNotNull(odb
                .getTreeChild(tree, appendChild(linesName, lines1.getIdentifier().getID())));

        // simulate a commit so the repo head points to this new tree
        ObjectInserter objectInserter = repo.newObjectInserter();
        RevCommit commit = new RevCommit(ObjectId.NULL);
        commit.setTreeId(newRootTreeId);
        ObjectId commitId = objectInserter.insert(getRepository().newCommitWriter(commit));
        Optional<Ref> newHead = geogit.command(UpdateRef.class).setName("refs/heads/master")
                .setNewValue(commitId).call();
        assertTrue(newHead.isPresent());

        index.deleted(appendChild(linesName, lines1.getIdentifier().getID()));
        index.stage(new NullProgressListener(), null);

        newRootTreeId = index.writeTree(newRootTreeId, new NullProgressListener());

        assertNotNull(newRootTreeId);
        assertFalse(repo.getRootTreeId().equals(newRootTreeId));

        tree = repo.getTree(newRootTreeId);
        assertNotNull(odb.getTreeChild(tree,
                appendChild(pointsName, points1.getIdentifier().getID())));

        Optional<NodeRef> treeChild = odb.getTreeChild(tree,
                appendChild(linesName, lines1.getIdentifier().getID()));

        // assertNull(String.valueOf(treeChild), treeChild);
        assertNotNull(treeChild);
        assertFalse(treeChild.isPresent());
    }

    @Test
    public void testMultipleStaging() throws Exception {

        final StagingDatabase indexDb = index.getDatabase();

        // insert and commit feature1_1
        final ObjectId oId1_1 = insertAndAdd(points1);

        System.err.println("++++++++++++ stage 1:  ++++++++++++++++++++");
        // staged1.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // check feature1_1 is there
        assertEquals(oId1_1, indexDb.findStaged(appendChild(pointsName, idP1)).get().getObjectId());

        // insert and commit feature1_2, feature1_2 and feature2_1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        System.err.println("++++++++++++ stage 2: ++++++++++++++++++++");
        // staged2.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // check feature1_2, feature1_3 and feature2_1
        Optional<NodeRef> treeChild;

        assertNotNull(treeChild = indexDb.findStaged(appendChild(pointsName, idP2)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId1_2, treeChild.get().getObjectId());

        assertNotNull(treeChild = indexDb.findStaged(appendChild(pointsName, idP3)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId1_3, treeChild.get().getObjectId());

        assertNotNull(treeChild = indexDb.findStaged(appendChild(linesName, idL1)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId2_1, treeChild.get().getObjectId());

        // as well as feature1_1 from the previous commit
        assertNotNull(treeChild = indexDb.findStaged(appendChild(pointsName, idP1)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId1_1, treeChild.get().getObjectId());

        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        System.err.println("++++++++++++ stage 3: ++++++++++++++++++++");
        // staged3.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // and check only points2 and lines2 remain (i.e. its oids are set to NULL)
        assertEquals(ObjectId.NULL, indexDb.findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(ObjectId.NULL, indexDb.findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
        assertEquals(ObjectId.NULL, indexDb.findStaged(appendChild(linesName, idL1)).get()
                .getObjectId());

        assertEquals(oId1_2, indexDb.findStaged(appendChild(pointsName, idP2)).get().getObjectId());
        assertEquals(oId2_2, indexDb.findStaged(appendChild(linesName, idL2)).get().getObjectId());

    }

    @Test
    public void testWriteTree2() throws Exception {

        final ObjectDatabase repoDb = repo.getObjectDatabase();

        // insert and commit feature1_1
        final ObjectId oId1_1 = insertAndAdd(points1);

        final ObjectId newRepoTreeId1;
        {
            newRepoTreeId1 = index.writeTree(repo.getHead().get());

            // assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId1);

            RevTree newRepoTree = repo.getTree(newRepoTreeId1);

            System.err.println("++++++++++ new repo tree 1: " + newRepoTreeId1 + " ++++++++++++");
            newRepoTree.accept(new PrintVisitor(repo, new PrintWriter(System.err)));
            // check feature1_1 is there
            assertEquals(oId1_1, repoDb.getTreeChild(newRepoTree, appendChild(pointsName, idP1))
                    .get().getObjectId());

        }

        // insert and add (stage) points2, points3, and lines1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        {// simulate a commit so the repo head points to this new tree
            ObjectInserter objectInserter = repo.newObjectInserter();
            RevCommit commit = new RevCommit(ObjectId.NULL);
            commit.setTreeId(newRepoTreeId1);
            ObjectId commitId = objectInserter.insert(getRepository().newCommitWriter(commit));
            Optional<Ref> newHead = geogit.command(UpdateRef.class).setName("refs/heads/master")
                    .setNewValue(commitId).call();
        }

        final ObjectId newRepoTreeId2;
        {
            // write comparing the the previously generated tree instead of the repository HEAD, as
            // it was not updated (no commit op was performed)
            newRepoTreeId2 = index.writeTree(newRepoTreeId1, new NullProgressListener());

            // assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId2);

            System.err.println("++++++++ new root 2:" + newRepoTreeId2 + " ++++++++++");
            RevTree newRepoTree = repo.getTree(newRepoTreeId2);

            newRepoTree.accept(new PrintVisitor(repo, new PrintWriter(System.err)));

            // check feature1_2, feature1_2 and feature2_1
            Optional<NodeRef> treeChild;
            assertNotNull(treeChild = repoDb.getTreeChild(newRepoTree,
                    appendChild(pointsName, idP2)));
            assertEquals(oId1_2, treeChild.get().getObjectId());

            assertNotNull(treeChild = repoDb.getTreeChild(newRepoTree,
                    appendChild(pointsName, idP3)));
            assertEquals(oId1_3, treeChild.get().getObjectId());

            assertNotNull(treeChild = repoDb
                    .getTreeChild(newRepoTree, appendChild(linesName, idL1)));
            assertEquals(oId2_1, treeChild.get().getObjectId());

            // as well as feature1_1 from the previous commit
            assertNotNull(treeChild = repoDb.getTreeChild(newRepoTree,
                    appendChild(pointsName, idP1)));
            assertEquals(oId1_1, treeChild.get().getObjectId());
        }

        {// simulate a commit so the repo head points to this new tree
            ObjectInserter objectInserter = repo.newObjectInserter();
            RevCommit commit = new RevCommit(ObjectId.NULL);
            commit.setTreeId(newRepoTreeId2);
            ObjectId commitId = objectInserter.insert(getRepository().newCommitWriter(commit));
            Optional<Ref> newHead = geogit.command(UpdateRef.class).setName("refs/heads/master")
                    .setNewValue(commitId).call();
        }

        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        final ObjectId newRepoTreeId3;
        {
            // write comparing the the previously generated tree instead of the repository HEAD, as
            // it was not updated (no commit op was performed)
            newRepoTreeId3 = index.writeTree(newRepoTreeId2, new NullProgressListener());

            // assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId3);

            System.err.println("++++++++ new root 3:" + newRepoTreeId3 + " ++++++++++");
            RevTree newRepoTree = repo.getTree(newRepoTreeId3);

            newRepoTree.accept(new PrintVisitor(repo, new PrintWriter(System.err)));

            // and check only feature1_2 and feature2_2 remain
            assertFalse(repoDb.getTreeChild(newRepoTree, appendChild(pointsName, idP1)).isPresent());
            assertFalse(repoDb.getTreeChild(newRepoTree, appendChild(pointsName, idP3)).isPresent());
            assertFalse(repoDb.getTreeChild(newRepoTree, appendChild(linesName, idL3)).isPresent());

            assertEquals(oId1_2, repoDb.getTreeChild(newRepoTree, appendChild(pointsName, idP2))
                    .get().getObjectId());
            assertEquals(oId2_2, repoDb.getTreeChild(newRepoTree, appendChild(linesName, idL2))
                    .get().getObjectId());
        }
    }

}
