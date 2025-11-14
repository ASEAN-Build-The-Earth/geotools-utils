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

import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.feature.type.Name;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * KMLDataStore implementation
 *
 * @author Niels Charlier, Scitus Development
 */
public class KMLDataStore extends ContentDataStore {
    /**
     *  The source file to retrieve information
     */
    protected File file;

    /**
     * Create a new KMLDataStore
     *
     * @param file The KML source file
     */
    public KMLDataStore(File file) {
        this(file, null);
    }

    // constructor start

    /**
     * Create a new KMLDataStore with specified namespace
     *
     * @param file The KML source file
     * @param namespaceURI The namespace URI
     */
    public KMLDataStore(File file, String namespaceURI) {
        if (file.isDirectory()) {
            throw new IllegalArgumentException(file + " must be a KML file");
        }
        if (namespaceURI == null) {
            if (file.getParent() != null) {
                namespaceURI = file.getParentFile().getName();
            }
        }
        this.file = file;
        setNamespaceURI(namespaceURI);
        //
        // factories
        setFilterFactory(CommonFactoryFinder.getFilterFactory(null));
        setGeometryFactory(new GeometryFactory());
        setFeatureTypeFactory(new FeatureTypeFactoryImpl());
        setFeatureFactory(CommonFactoryFinder.getFeatureFactory(null));
    }
    // constructor end

    // info start
    public ServiceInfo getInfo() {
        DefaultServiceInfo info = new DefaultServiceInfo();
        info.setDescription("Features from " + file);
        info.setSchema(FeatureTypes.DEFAULT_NAMESPACE);
        info.setSource(file.toURI());
        try {
            info.setPublisher(new URI(System.getProperty("user.name")));
        } catch (URISyntaxException e) {
        }
        return info;
    }

    // info end

    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    protected List<Name> createTypeNames() throws IOException {
        String name = file.getName();
        String typeName = name.substring(0, name.lastIndexOf('.'));
        List<Name> typeNames = new ArrayList<Name>();
        typeNames.add(new NameImpl(namespaceURI, typeName));
        return typeNames;
    }

    public List<Name> getNames() throws IOException {
        String[] typeNames = getTypeNames();
        List<Name> names = new ArrayList<Name>(typeNames.length);
        for (String typeName : typeNames) {
            names.add(new NameImpl(namespaceURI, typeName));
        }
        return names;
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        if (file.canWrite()) {
            return new KMLFeatureSource(entry, Query.ALL);
        } else {
            return new KMLFeatureSource(entry, Query.ALL);
        }
    }
}