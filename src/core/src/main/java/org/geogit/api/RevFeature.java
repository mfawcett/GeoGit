/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * A binary representation of the state of a Feature.
 * 
 * @author groldan
 * 
 */
public class RevFeature extends AbstractRevObject {

    private final Object parsed;

    public RevFeature(ObjectId id, Object parsed) {
        super(id, TYPE.BLOB);
        this.parsed = parsed;
    }

    public Object getParsed() {
        return parsed;
    }
}
