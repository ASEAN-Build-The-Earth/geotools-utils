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
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Read a KML file directly.
 *
 * @author Niels Charlier, Scitus Development
 */
public class KMLFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

    SimpleFeatureType type = null;
    SimpleFeature f = null;
    // PullParser parser;
    org.geotools.xsd.StreamingParser parser;
    FileInputStream fis;

    /**
     * Construct a KMLFeatureReader on a specified element
     *
     * @param file The KML parse-able source file
     * @param name The {@link QName} of an element that will be the parsing root
     * @throws IOException If {@linkplain org.geotools.xsd.StreamingParser parser} failed to parse the source file
     */
    public KMLFeatureReader(File file, QName name) throws IOException {
        fis = new FileInputStream(file);
        try {
            parser = new org.geotools.xsd.StreamingParser(new org.geotools.kml.KMLConfiguration(), fis, name);
        } catch (Exception e) {
            throw new IOException("Error processing KML file", e);
        }
        forward();
        if (f != null) type = f.getType();
    }

    /**
     * Return the FeatureType this reader has been configured to create.
     *
     * @return the FeatureType of the Features this FeatureReader will create.
     */
    @Override
    public SimpleFeatureType getFeatureType() {
        return type;
    }

    /**
     * Grab the next feature from the property file.
     *
     * @return feature
     * @throws NoSuchElementException Check hasNext() to avoid reading off the end of the file
     */
    @Override
    public SimpleFeature next() throws IOException, NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        SimpleFeature next = f;
        forward();
        return next;
    }

    /**
     * Forward the streaming parser to a constructed member
     *
     * @throws IOException If the parser failed to parse
     */
    public void forward() throws IOException {
        try {
            f = (SimpleFeature) parser.parse();
        } catch (Exception e) {
            throw new IOException("Error processing KML file", e);
        }
    }

    /**
     * Query whether this FeatureReader has another Feature.
     *
     * @return True if there are more Features to be read. In other words, true if calls to next would return a feature
     *     rather than throwing an exception.
     * @throws IOException If an error occurs determining if there are more Features.
     */
    @Override
    public boolean hasNext() throws IOException {
        return f != null;
    }

    /**
     * Be sure to call close when you are finished with this reader; as it must close the file it
     * has open.
     */
    @Override
    public void close() throws IOException {
        fis.close();
    }
}
