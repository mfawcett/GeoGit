/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Pairing of a name and the {@link ObjectId} it currently has.
 * <p>
 * A ref in Git is (more or less) a variable that holds a single object identifier. The object
 * identifier can be any valid Git object (blob, tree, commit, annotated tag, ...).
 * <p>
 * The ref name has the attributes of the ref that was asked for as well as the ref it was resolved
 * to for symbolic refs plus the object id it points to and (for tags) the peeled target object id,
 * i.e. the tag resolved recursively until a non-tag object is referenced.
 */
public class Ref implements Comparable<Ref> {

    /**
     * By convention, name of the main branch
     */
    public static final String MASTER = "refs/heads/master";

    /**
     * Pointer to the latest commit in the current branch
     */
    public static final String HEAD = "HEAD";

    /**
     * Pointer to the current tree in the staging index
     */
    public static final String STAGE_HEAD = "STAGE_HEAD";

    /**
     * Pointer to the current tree in the working directory
     */
    public static final String WORK_HEAD = "WORK_HEAD";

    public static final String REFS_PREFIX = "refs/";

    public static final String REMOTES_PREFIX = "remotes/";

    public static final String TAGS_PREFIX = REFS_PREFIX + "tags/";

    public static final String HEADS_PREFIX = REFS_PREFIX + "heads/";

    public static final String ORIGIN = "refs/remotes/origin";

    private String name;

    private RevObject.TYPE type;

    private ObjectId objectId;

    public Ref(final String name, final ObjectId oid, @Nullable final RevObject.TYPE type) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(oid);
        Preconditions.checkArgument(oid.isNull() || type != null);
        this.name = name;
        this.objectId = oid;
        this.type = type;
    }

    /**
     * @see org.geogit.api.Ref#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @see org.geogit.api.Ref#getObjectId()
     */
    public ObjectId getObjectId() {
        return objectId;
    }

    public RevObject.TYPE getType() {
        return type;
    }

    /**
     * @see org.geogit.api.Ref#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ref)) {
            return false;
        }
        Ref r = (Ref) o;
        return name.equals(r.getName()) && type.equals(r.getType())
                && objectId.equals(r.getObjectId());
    }

    /**
     * @see org.geogit.api.Ref#hashCode()
     */
    @Override
    public int hashCode() {
        return name.hashCode() * objectId.hashCode();
    }

    /**
     * @see org.geogit.api.Ref#compareTo(org.geogit.api.Ref)
     */
    @Override
    public int compareTo(Ref o) {
        return name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return new StringBuilder("Ref").append('[').append(name).append(" -> ").append(objectId)
                .append(']').toString();
    }
}