/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.PlatformResolver;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.repository.CommitBuilder;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectInserter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Commits the staged changed in the index to the repository, creating a new commit pointing to the
 * new root tree resulting from moving the staged changes to the repository, and updating the HEAD
 * ref to the new commit object.
 * <p>
 * Like {@code git commit -a}, If the {@link #setAll(boolean) all} flag is set, first stages all the
 * changed objects in the index, but does not state newly created (unstaged) objects that are not
 * already staged.
 * </p>
 * 
 * @author groldan
 * 
 */
public class CommitOp extends AbstractGeoGitOp<RevCommit> {

    private String author;

    private String committer;

    private String message;

    private Long timeStamp;

    /**
     * This commit's parents. Will be the current HEAD, but when we support merges it should include
     * the equivalent to git's .git/MERGE_HEAD
     */
    private List<ObjectId> parents = new LinkedList<ObjectId>();

    // like the -a option in git commit
    private boolean all;

    private CommitStateResolver stateResolver;

    private Repository repository;

    @Inject
    public CommitOp(final Repository repository) {
        this(repository, new PlatformResolver());
    }

    public CommitOp(final Repository repository, final CommitStateResolver authResolver) {
        this.repository = repository;
        this.stateResolver = authResolver;
    }

    public CommitOp setStateResolver(final CommitStateResolver auth) {
        this.stateResolver = auth == null ? new PlatformResolver() : auth;
        return this;
    }

    public CommitOp setAuthor(final String author) {
        this.author = author;
        return this;
    }

    public CommitOp setCommitter(final String committer) {
        this.committer = committer;
        return this;
    }

    /**
     * Sets the {@link RevCommit#getMessage() commit message}.
     * 
     * @param message description of the changes to record the commit with.
     * @return {@code this}, to ease command chaining
     */
    public CommitOp setMessage(final String message) {
        this.message = message;
        return this;
    }

    /**
     * Sets the {@link RevCommit#getTimestamp() timestamp} the commit will be marked to, or if not
     * set defaults to the current system time at the time {@link #call()} is called.
     * 
     * @param timestamp commit timestamp, in milliseconds, as in {@link Date#getTime()}
     * @return {@code this}, to ease command chaining
     */
    public CommitOp setTimestamp(final Long timestamp) {
        this.timeStamp = timestamp;
        return this;
    }

    /**
     * If {@code true}, tells {@link #call()} to stage all the unstaged changes that are not new
     * object before performing the commit.
     * 
     * @param all {@code true} to {@link AddOp#setUpdateOnly(boolean) stage changes) before commit,
     *        {@code false} to not do that. Defaults to {@code false}.
     * @return {@code this}, to ease command chaining
     */
    public CommitOp setAll(boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @return the commit just applied, or {@code null} iif
     *         {@code getProgressListener().isCanceled()}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     * @throws NothingToCommitException if there are no staged changes by comparing the index
     *         staging tree and the repository HEAD tree.
     */
    public RevCommit call() throws Exception {
        // TODO: check repository is in a state that allows committing

        getProgressListener().started();
        if (all) {
            command(AddOp.class).addPattern(".").setUpdateOnly(true)
                    .setProgressListener(subProgress(49f)).call();
        }
        if (getProgressListener().isCanceled()) {
            return null;
        }

        final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions.checkState(currHead.isPresent(), "Repository has no HEAD, can't commit");

        final ObjectId currHeadCommitId = command(RevParse.class).setRefSpec(Ref.HEAD).call();
        parents.add(currHeadCommitId);

        final StagingArea index = repository.getIndex();

        final ObjectId newTreeId = index.writeTree(currHead.get(), subProgress(49f));
        if (getProgressListener().isCanceled()) {
            return null;
        }

        final ObjectId currentRootTreeId = repository.getRootTreeId();
        if (currentRootTreeId.equals(newTreeId)) {
            throw new NothingToCommitException("Nothing to commit after " + currHeadCommitId);
        }

        final ObjectId commitId;
        {
            CommitBuilder cb = new CommitBuilder();
            cb.setAuthor(getAuthor());
            cb.setCommitter(getCommitter());
            cb.setMessage(getMessage());
            cb.setParentIds(parents);
            cb.setTreeId(newTreeId);
            cb.setTimestamp(getTimeStamp());
            // cb.setBounds(bounds);

            if (getProgressListener().isCanceled()) {
                return null;
            }
            ObjectInserter objectInserter = repository.newObjectInserter();
            commitId = objectInserter.insert(repository.newCommitWriter(cb.build(ObjectId.NULL)));
        }
        final RevCommit commit = repository.getCommit(commitId);
        // set the HEAD pointing to the new commit
        final String branch = "refs/heads/master";
        final Optional<Ref> branchHead = command(UpdateRef.class).setName(branch)
                .setNewValue(commitId).call();
        Preconditions.checkState(commitId.equals(branchHead.get().getObjectId()));
        LOGGER.fine("New head: " + branchHead);

        final Optional<SymRef> newHead = command(UpdateSymRef.class).setName(Ref.HEAD)
                .setNewValue(branch).call();
        Preconditions.checkState(branch.equals(newHead.get().getTarget()));

        ObjectId treeId = repository.getCommit(branchHead.get().getObjectId()).getTreeId();
        Preconditions.checkState(newTreeId.equals(treeId));

        getProgressListener().complete();

        return commit;
    }

    private long getTimeStamp() {
        return timeStamp == null ? stateResolver.getCommitTimeMillis() : timeStamp.longValue();
    }

    private String getMessage() {
        return message == null ? stateResolver.getCommitMessage() : message;
    }

    private String getCommitter() {
        return committer == null ? stateResolver.getCommitter() : committer;
    }

    private String getAuthor() {
        return author == null ? stateResolver.getAuthor() : author;
    }
}
