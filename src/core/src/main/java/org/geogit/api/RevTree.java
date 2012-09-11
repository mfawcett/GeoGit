/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.math.BigInteger;
import java.util.Iterator;

import com.google.common.base.Predicate;

public interface RevTree extends RevObject {

    public abstract NodeRef get(final String key);

    public abstract void accept(TreeVisitor visitor);

    public abstract BigInteger size();

    public abstract Iterator<NodeRef> iterator(Predicate<NodeRef> filter);

    public abstract boolean isNormalized();

    public abstract MutableTree mutable();
}