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

import java.io.IOException;

import org.geogit.api.GeoGIT;
import org.geogit.api.RevTree;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.versioning.SimpleVersioningFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public class SimpleFeatureStoreDecorator extends
        FeatureStoreDecorator<SimpleFeatureType, SimpleFeature> implements
        SimpleVersioningFeatureStore {

    /**
     * @param unversioned
     * @param store
     */
    public SimpleFeatureStoreDecorator(SimpleFeatureStore unversioned, GeoGIT repo) {
        super(unversioned, repo);
    }

    /**
     * @see org.geotools.data.simple.SimpleFeatureStore#modifyFeatures(java.lang.String,
     *      java.lang.Object, org.opengis.filter.Filter)
     */
    @Override
    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException {

        Name attributeName = new NameImpl(name);
        super.modifyFeatures(attributeName, attributeValue, filter);
    }

    /**
     * @see org.geotools.data.simple.SimpleFeatureStore#modifyFeatures(java.lang.String[],
     *      java.lang.Object[], org.opengis.filter.Filter)
     */
    @Override
    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter)
            throws IOException {

        Name[] attributeNames = new Name[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            attributeNames[i] = new NameImpl(name);
        }
        super.modifyFeatures(attributeNames, attributeValues, filter);
    }

    /**
     * @see SimpleFeatureStore#getFeatures()
     */
    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return (SimpleFeatureCollection) super.getFeatures();
    }

    /**
     * @see org.geotools.data.versioning.decorator.FeatureSourceDecorator#getFeatures(org.opengis.filter.Filter)
     */
    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return (SimpleFeatureCollection) super.getFeatures(filter);
    }

    /**
     * @see org.geotools.data.versioning.decorator.FeatureSourceDecorator#getFeatures(org.geotools.data.Query)
     */
    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        return (SimpleFeatureCollection) super.getFeatures(query);
    }

    /**
     * @see org.geotools.data.versioning.decorator.FeatureSourceDecorator#createFeatureCollection(org.geotools.feature.FeatureCollection,
     *      org.geoserver.data.versioning.decorator.VersioningDataAccess, org.geogit.api.ObjectId)
     */
    @Override
    protected FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection(
            FeatureCollection<SimpleFeatureType, SimpleFeature> delegate, RevTree typeTree) {

        return new SimpleResourceIdAssigningFeatureCollection((SimpleFeatureCollection) delegate,
                this, typeTree);
    }
}
