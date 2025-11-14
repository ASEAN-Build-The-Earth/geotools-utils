package asia.buildtheearth.asean.geotools.kml.writer;

import org.geotools.api.filter.identity.FeatureId;
import org.locationtech.jts.geom.Geometry;

import javax.xml.namespace.QName;
import java.io.OutputStream;

/**
 * KML 2.2 writer for exporting a collection of {@link org.locationtech.jts.geom.Geometry} objects as a KML document.
 *
 * <p>This writer generates a KML output conforming to the KML 2.2 specification using GeoTools' {@link org.geotools.kml.v22.KMLConfiguration}.
 * The resulting document always uses a {@code <Document>} element as the container and wraps each geometry inside a {@code <Placemark>} element.</p>
 *
 * <p><strong>Limitations:</strong></p>
 * <ul>
 *   <li>Only supports writing {@linkplain org.locationtech.jts.geom.Geometry Geometry} data.</li>
 *   <li>Additional properties or metadata (e.g., name, description, styles) are not supported and will not be written.</li>
 *   <li>Each geometry will be represented inside its own {@code <Placemark>} tag within the {@code <Document>}.</li>
 *   <li>
 *     Identifiers can be set per geometry using {@link #writeGeometry(Geometry, FeatureId)}, and for the whole collection using
 *     {@link #setOutputID(String)}.
 *     <br/>
 *     These identifiers are internal feature IDs only and are <strong>not</strong> mapped to the KML {@code <name>} element or used as display labels.
 *   </li>
 * </ul>
 *
 * @see #writeGeometry(Geometry, FeatureId)
 * @see #setOutputID(String)
 * @see #export(OutputStream)
 */
public final class KMLWriterV22 extends KMLWriter {

    /**
     * Constructs a new writer using the GeoTools KML 2.2 configuration.
     *
     * <p>Use {@link #writeGeometry(Geometry, FeatureId)} to populate the document,
     * and call {@link #export(OutputStream)} to write the resulting KML output.</p>
     */
    public KMLWriterV22() {
        super(new org.geotools.kml.v22.KMLConfiguration());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.geotools.kml.v22.KML#Placemark}
     */
    @Override
    protected QName getContainer() {
        return org.geotools.kml.v22.KML.Placemark;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.geotools.kml.KML#kml}
     */
    @Override
    protected QName getKML() {
        return org.geotools.kml.v22.KML.kml;
    }
}
