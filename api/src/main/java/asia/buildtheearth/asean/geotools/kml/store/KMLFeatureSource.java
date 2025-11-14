/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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

/*
 * This file is part of GeoTools (https://geotools.org)
 * Originally from the gt-kml module, licensed under LGPL v2.1
 *
 * Modified by Tintinkung <tintinkung.lemonade@gmail.com> on 2025-06-16
 * - Adapted for standalone use without full GeoTools module dependencies
 */
package asia.buildtheearth.asean.geotools.kml.store;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * Feature source for KML
 *
 * @author Niels Charlier, Scitus Development
 */
public class KMLFeatureSource extends ContentFeatureSource {

    /**
     * Creates the new feature source from a query.
     *
     * <p>The {@code query} is taken into account for any operations done against the feature source. For example, when
     * getReader(Query) is called the query specified is "joined" to the query specified in the constructor.</p>
     *
     * @param entry The entry for the feature source.
     * @param query Specify that the feature source represents the entire set of features, may be {@code null}.
     */
    public KMLFeatureSource(ContentEntry entry, Query query) {
        super(entry, query);
    }

    @Override
    protected QueryCapabilities buildQueryCapabilities() {
        return new QueryCapabilities() {
            public boolean isUseProvidedFIDSupported() {
                return true;
            }
        };
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        ReferencedEnvelope bounds =
                new ReferencedEnvelope(getSchema().getCoordinateReferenceSystem());

        FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReaderInternal(query);
        try {
            while (featureReader.hasNext()) {
                SimpleFeature feature = featureReader.next();
                bounds.include(feature.getBounds());
            }
        } finally {
            featureReader.close();
        }
        return bounds;
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        int count = 0;
        try (FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReaderInternal(query)) {
            while (featureReader.hasNext()) {
                featureReader.next();
                count++;
            }
        }
        return count;
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        String typeName = getEntry().getTypeName();
        String namespace = getEntry().getName().getNamespaceURI();

        SimpleFeatureType type;
        try (FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReaderInternal(query)) {
            type = featureReader.getFeatureType();
        }

        // rename
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        if (type != null) {
            b.init(type);
        }
        b.setName(typeName);
        b.setNamespaceURI(namespace);
        return b.buildFeatureType();
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        KMLDataStore dataStore = (KMLDataStore) getEntry().getDataStore();
        return new KMLFeatureReader(
                dataStore.file,
                new QName(getEntry().getName().getNamespaceURI(), getEntry().getTypeName()));
    }
}
