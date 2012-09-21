/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

class MutableRevSHA1Tree extends RevSHA1Tree implements MutableTree {

    private BigInteger mutableSize;

    /**
     * Copy constructor
     */
    public MutableRevSHA1Tree(final RevSHA1Tree copy) {
        super(copy.getId(), copy.db, copy.depth);
        this.mutableSize = copy.size();
        super.myEntries.putAll(copy.myEntries);
        super.mySubTrees.putAll(copy.mySubTrees);
    }

    MutableRevSHA1Tree(ObjectDatabase db, int childOrder) {
        super(db, childOrder);
    }

    @Override
    public MutableTree mutable() {
        return this;
    }

    /**
     * @return the number of elements in the tree, forces {@link #normalize()} if the tree has been
     *         modified since retrieved from the db
     */
    @SuppressWarnings("unchecked")
    @Override
    public BigInteger size() {
        if (mutableSize == null) {
            if (!isNormalized()) {
                normalize();
            } else {
                try {
                    this.mutableSize = computeSize(BigInteger.ZERO, Collections.EMPTY_SET);
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        }
        return mutableSize;
    }

    /**
     * Adds or replaces an element in the tree with the given key.
     * <p>
     * <!-- Implementation detail: If the number of cached entries (entries held directly by this
     * tree) reaches {@link #SPLIT_FACTOR}, this tree will {@link #normalize()} itself.
     * 
     * -->
     * 
     * @param key non null
     * @param value non null
     */
    @Override
    public void put(final NodeRef ref) {
        Preconditions.checkNotNull(ref, "ref can't be null");

        NodeRef oldCachedValue = myEntries.put(ref.getPath(), ref);
        if (myEntries.size() >= SPLIT_FACTOR) {
            // hit the split factor modification tolerance, lets normalize
            normalize();
        } else {
            // still can handle more modifications before aut-splitting
            if (mySubTrees.isEmpty()) {
                // I'm not yet split into subtrees, can handle size safely
                if (this.mutableSize == null) {
                    this.mutableSize = BigInteger.ONE;
                } else if (oldCachedValue == null) {
                    // only increment size if it wasn't a replacement operation
                    this.mutableSize = this.mutableSize.add(BigInteger.ONE);
                }
            } else {
                // I'm not sure what my size is anymore, lets handle it lazily
                this.mutableSize = null;
            }
        }
    }

    @Override
    public Optional<NodeRef> remove(final String key) {
        Preconditions.checkNotNull(key, "key can't be null");
        final Integer bucket = computeBucket(key);
        if (null == mySubTrees.get(bucket)) {
            // we don't even have a subtree for this key's bucket, it's sure this tree doesn't
            // already hold a value for it
            NodeRef removed = myEntries.remove(key);
            if (removed != null) {
                this.mutableSize = size().subtract(BigInteger.ONE);
            }
            return Optional.fromNullable(removed);
        } else {
            Optional<NodeRef> ref = this.get(key);
            if (ref.isPresent()) {
                // use null value signaling the removal
                // of the entry. normalize() is gonna take care of removing it from the subtree
                // subsequently
                myEntries.put(key, null);
                this.mutableSize = size().subtract(BigInteger.ONE);
                // mutableSize = null; // I'm not sure what my size is anymore
                if (myEntries.size() >= SPLIT_FACTOR) {
                    normalize();
                }
            }
            return ref;
        }
    }

    /**
     * Splits the cached entries into subtrees and saves them, making sure the tree contains either
     * only entries or subtrees
     */
    @Override
    public void normalize() {
        if (isNormalized()) {
            return;
        }
        // System.err.println("spliting tree with order " + this.depth + " having "
        // + this.myEntries.size() + " entries....");
        if (myEntries.size() <= NORMALIZED_SIZE_LIMIT && mySubTrees.size() == 0) {
            mutableSize = BigInteger.valueOf(myEntries.size());
            return;
        }
        final int childOrder = this.depth + 1;
        try {
            // sort entries by the bucket they fall on
            Map<Integer, Set<String>> entriesByBucket = new TreeMap<Integer, Set<String>>();
            for (Object key : myEntries.keySet()) {
                Integer bucket = computeBucket((String) key);
                if (!entriesByBucket.containsKey(bucket)) {
                    entriesByBucket.put(bucket, new HashSet<String>());
                }
                entriesByBucket.get(bucket).add((String) key);
            }

            BigInteger size = BigInteger.ZERO;

            // ignore this subtrees for computing the size later as by that time their size has been
            // already added
            Set<NodeRef> ignoreForSizeComputation = new HashSet<NodeRef>();

            // for each bucket retrieve/create the bucket's subtree and set its entries
            Iterator<Map.Entry<Integer, Set<String>>> it = entriesByBucket.entrySet().iterator();

            NodeRef subtreeRef;
            ObjectId subtreeId;
            MutableTree subtree;

            ObjectSerialisingFactory serialFactory = db.getSerialFactory();
            while (it.hasNext()) {
                Entry<Integer, Set<String>> e = it.next();
                Integer bucket = e.getKey();
                Set<String> keys = e.getValue();
                it.remove();
                subtreeRef = mySubTrees.get(bucket);
                if (subtreeRef == null) {
                    subtree = new MutableRevSHA1Tree(db, childOrder);
                } else {
                    subtreeId = subtreeRef.getObjectId();
                    ObjectReader<RevTree> reader = serialFactory
                            .createRevTreeReader(db, childOrder);
                    subtree = db.get(subtreeId, reader).mutable();
                    // subtree = db.get(subtreeId, new BxmlRevTreeReader(db, childOrder)).mutable();
                }
                for (String key : keys) {
                    NodeRef value = myEntries.remove(key);
                    if (value == null) {
                        subtree.remove(key);
                    } else {
                        subtree.put(value);
                    }
                }
                size = size.add(subtree.size());
                subtreeId = this.db.put(serialFactory.createRevTreeWriter(subtree));
                // subtreeId = this.db.put(new BxmlRevTreeWriter(subtree));
                subtreeRef = new NodeRef("", subtreeId, ObjectId.NULL, TYPE.TREE);
                ignoreForSizeComputation.add(subtreeRef);
                mySubTrees.put(bucket, subtreeRef);
            }

            // compute the overall size
            this.mutableSize = computeSize(size, ignoreForSizeComputation);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // System.err.println("spliting complete.");
    }

    private BigInteger computeSize(final BigInteger initialSize,
            final Set<NodeRef> ignoreForSizeComputation) throws IOException {
        final int childOrder = this.depth + 1;
        ObjectId subtreeId;
        BigInteger size = initialSize;
        for (NodeRef ref : mySubTrees.values()) {
            if (ignoreForSizeComputation.contains(ref)) {
                continue;
            }
            subtreeId = ref.getObjectId();
            ObjectSerialisingFactory serialFactory = db.getSerialFactory();
            ObjectReader<RevTree> reader = serialFactory.createRevTreeReader(db, childOrder);
            RevTree cached = db.get(subtreeId, reader);
            size = size.add(cached.size());
            // size = size.add(db.getCached(subtreeId, new BxmlRevTreeReader(db,
            // childOrder)).size());
        }
        return size;
    }
}
