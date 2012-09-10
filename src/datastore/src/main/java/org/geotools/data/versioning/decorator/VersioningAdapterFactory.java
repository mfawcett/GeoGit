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

import org.geogit.api.GeoGIT;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.versioning.VersioningDataStore;
import org.opengis.feature.type.Name;

public class VersioningAdapterFactory {

    @SuppressWarnings({ "rawtypes" })
    public static FeatureSource create(final FeatureSource subject, GeoGIT versioningRepo) {

        final Name typeName = subject.getSchema().getName();

        if (subject.getDataStore() instanceof VersioningDataStore) {
            return subject;
        }

        // do not wrap if not versioned
        if (!FeatureSourceDecorator.isVersioned(typeName, versioningRepo)) {
            return subject;
        }

        if (subject instanceof SimpleFeatureLocking) {
            return new SimpleFeatureLockingDecorator((SimpleFeatureLocking) subject, versioningRepo);
        }
        if (subject instanceof SimpleFeatureStore) {
            return new SimpleFeatureStoreDecorator((SimpleFeatureStore) subject, versioningRepo);
        }
        if (subject instanceof SimpleFeatureSource) {
            return new SimpleFeatureSourceDecorator((SimpleFeatureSource) subject, versioningRepo);
        }

        if (subject instanceof FeatureLocking) {
            return new FeatureLockingDecorator((FeatureLocking) subject, versioningRepo);
        }
        if (subject instanceof FeatureStore) {
            return new FeatureStoreDecorator((FeatureStore) subject, versioningRepo);
        }

        return new FeatureSourceDecorator(subject, versioningRepo);
    }

    @SuppressWarnings("rawtypes")
    public static DataAccess create(final DataAccess subject, GeoGIT versioningRepo) {
        if (subject == null) {
            throw new NullPointerException("DataAccess subject is required");
        }

        // if (subject instanceof DataStore) {
        // return new VersioningDataStore((DataStore) subject, versioningRepo);
        // }

        return new DataAccessDecorator((DataStore) subject, versioningRepo);
    }
}
