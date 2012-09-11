/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.geogit.api.DefaultPlatform;
import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitModule;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.PorcelainCommands;
import org.geogit.api.RevCommit;
import org.geogit.command.plumbing.PlumbingCommands;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.repository.Triplet;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.memory.HeapObjectDatabse;
import org.geogit.storage.memory.HeapRefDatabase;
import org.geogit.storage.memory.HeapStagingDatabase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.CRS;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.vividsolutions.jts.io.ParseException;

public abstract class RepositoryTestCase {

    protected static final String idL1 = "Lines.1";

    protected static final String idL2 = "Lines.2";

    protected static final String idL3 = "Lines.3";

    protected static final String idP1 = "Points.1";

    protected static final String idP2 = "Points.2";

    protected static final String idP3 = "Points.3";

    protected static final String pointsNs = "http://geogit.points";

    protected static final String pointsName = "Points";

    protected static final String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected static final Name pointsTypeName = new NameImpl(pointsNs, pointsName);

    protected SimpleFeatureType pointsType;

    protected Feature points1;

    protected Feature points2;

    protected Feature points3;

    protected static final String linesNs = "http://geogit.lines";

    protected static final String linesName = "Lines";

    protected static final String linesTypeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";

    protected static final Name linesTypeName = new NameImpl(linesNs, linesName);

    protected SimpleFeatureType linesType;

    protected Feature lines1;

    protected Feature lines2;

    protected Feature lines3;

    protected GeoGIT geogit;

    protected Repository repo;

    // prevent recursion
    private boolean setup = false;

    private static File envHome;

    Injector injector;

    static class TestPlatform extends DefaultPlatform {
        @Override
        public File pwd() {
            return envHome;
        }
    }

    static class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Platform.class).to(TestPlatform.class).in(Scopes.SINGLETON);
            bind(ObjectDatabase.class).to(HeapObjectDatabse.class).in(Scopes.SINGLETON);
            bind(RefDatabase.class).to(HeapRefDatabase.class).in(Scopes.SINGLETON);
            bind(StagingDatabase.class).to(HeapStagingDatabase.class).in(Scopes.SINGLETON);
        }
    }

    @Before
    public final void setUp() throws Exception {
        if (setup) {
            throw new IllegalStateException("Are you calling super.setUp()!?");
        }

        setup = true;
        Logging.ALL.forceMonolineConsoleOutput();
        envHome = new File(new File("target"), "testrepository");

        FileUtils.deleteDirectory(envHome);
        assertFalse(envHome.exists());
        assertTrue(envHome.mkdirs());

        // ///////////////////////////
        // injector = Guice
        // .createInjector(Modules.override(new GeogitModule()).with(new TestModule()),
        // new PlumbingCommands());
        injector = Guice.createInjector(new GeogitModule(), new PlumbingCommands(),
                new PorcelainCommands());

        geogit = new GeoGIT(injector, envHome);
        repo = geogit.getOrCreateRepository();

        pointsType = DataUtilities.createType(pointsNs, pointsName, pointsTypeSpec);

        points1 = feature(pointsType, idP1, "StringProp1_1", new Integer(1000), "POINT(1 1)");
        points2 = feature(pointsType, idP2, "StringProp1_2", new Integer(2000), "POINT(2 2)");
        points3 = feature(pointsType, idP3, "StringProp1_3", new Integer(3000), "POINT(3 3)");

        linesType = DataUtilities.createType(linesNs, linesName, linesTypeSpec);

        lines1 = feature(linesType, idL1, "StringProp2_1", new Integer(1000),
                "LINESTRING (1 1, 2 2)");
        lines2 = feature(linesType, idL2, "StringProp2_2", new Integer(2000),
                "LINESTRING (3 3, 4 4)");
        lines3 = feature(linesType, idL3, "StringProp2_3", new Integer(3000),
                "LINESTRING (5 5, 6 6)");

        setUpInternal();
    }

    @After
    public final void tearDown() throws Exception {
        setup = false;
        tearDownInternal();
        if (repo != null) {
            repo.close();
        }
        repo = null;
        injector = null;
        System.gc();
        FileUtils.deleteDirectory(envHome);
        assertFalse(envHome.exists());
    }

    /**
     * Called as the last step in {@link #setUp()}
     */
    protected abstract void setUpInternal() throws Exception;

    /**
     * Called before {@link #tearDown()}, subclasses may override as appropriate
     */
    protected void tearDownInternal() throws Exception {
        //
    }

    public Repository getRepository() {
        return repo;
    }

    protected Feature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, Feature... features)
            throws Exception {
        return populate(oneCommitPerFeature, Arrays.asList(features));
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, List<Feature> features)
            throws Exception {

        List<RevCommit> commits = new ArrayList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            if (oneCommitPerFeature) {
                RevCommit commit = geogit.commit().call();
                commits.add(commit);
            }
        }

        if (!oneCommitPerFeature) {
            RevCommit commit = geogit.commit().call();
            commits.add(commit);
        }

        return commits;
    }

    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    protected ObjectId insertAndAdd(Feature f) throws Exception {
        ObjectId objectId = insert(f);

        geogit.add().call();
        return objectId;
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    protected ObjectId insert(Feature f) throws Exception {
        final StagingArea index = getRepository().getIndex();
        Name name = f.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = f.getIdentifier().getID();

        NodeRef ref = index.inserted(getRepository().newFeatureWriter(f), f.getBounds(),
                namespaceURI, localPart, id);
        ObjectId objectId = ref.getObjectId();
        return objectId;
    }

    protected void insertAndAdd(Feature... features) throws Exception {
        insert(features);
        geogit.add().call();
    }

    protected void insert(Feature... features) throws Exception {

        final StagingArea index = getRepository().getIndex();

        Iterator<Triplet<ObjectWriter<?>, BoundingBox, List<String>>> iterator;
        Function<Feature, Triplet<ObjectWriter<?>, BoundingBox, List<String>>> function = new Function<Feature, Triplet<ObjectWriter<?>, BoundingBox, List<String>>>() {

            @Override
            public Triplet<ObjectWriter<?>, BoundingBox, List<String>> apply(final Feature f) {
                Name name = f.getType().getName();
                String namespaceURI = name.getNamespaceURI();
                String localPart = name.getLocalPart();
                String id = f.getIdentifier().getID();

                Triplet<ObjectWriter<?>, BoundingBox, List<String>> tuple;
                ObjectWriter<?> writer = getRepository().newFeatureWriter(f);
                BoundingBox bounds = f.getBounds();
                List<String> path = Arrays.asList(namespaceURI, localPart, id);
                tuple = new Triplet<ObjectWriter<?>, BoundingBox, List<String>>(writer, bounds,
                        path);
                return tuple;
            }
        };

        iterator = Iterators.transform(Iterators.forArray(features), function);

        index.inserted(iterator, new NullProgressListener(), null, null);

    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    protected boolean deleteAndAdd(Feature f) throws Exception {
        boolean existed = delete(f);
        if (existed) {
            geogit.add().call();
        }

        return existed;
    }

    protected boolean delete(Feature f) throws Exception {
        final StagingArea index = getRepository().getIndex();
        Name name = f.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = f.getIdentifier().getID();
        boolean existed = index.deleted(namespaceURI, localPart, id);
        return existed;
    }

    protected <E> List<E> toList(Iterator<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterators.addAll(logged, logs);
        return logged;
    }

    protected <E> List<E> toList(Iterable<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterables.addAll(logged, logs);
        return logged;
    }

    /**
     * Computes the aggregated bounds of {@code features}, assuming all of them are in the same CRS
     */
    protected ReferencedEnvelope boundsOf(Feature... features) {
        ReferencedEnvelope bounds = null;
        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            if (bounds == null) {
                bounds = (ReferencedEnvelope) f.getBounds();
            } else {
                bounds.include(f.getBounds());
            }
        }
        return bounds;
    }

    /**
     * Computes the aggregated bounds of {@code features} in the {@code targetCrs}
     */
    protected ReferencedEnvelope boundsOf(CoordinateReferenceSystem targetCrs, Feature... features)
            throws Exception {
        ReferencedEnvelope bounds = new ReferencedEnvelope(targetCrs);

        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            BoundingBox fbounds = f.getBounds();
            if (!CRS.equalsIgnoreMetadata(targetCrs, fbounds)) {
                fbounds = fbounds.toBounds(targetCrs);
            }
            bounds.include(fbounds);
        }
        return bounds;
    }
}
