package asia.buildtheearth.asean.geotools.kml.writer;

import javax.xml.namespace.QName;

/**
 * This class provides the same functionality as {@link KMLWriterV22}
 * but relies on the older KML 2.1 schema. May be less compatible with modern KML tools.
 *
 * @see KMLWriterV22
 */
public final class KMLWriterV21 extends KMLWriter {

    /**
     * Constructs a new writer using the GeoTools KML 2.1 configuration.
     *
     * @deprecated Use {@link KMLWriterV22} instead.
     * This class provides the same functionality but relies on the older KML 2.1 schema,
     * which is less compatible with modern KML tools.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public KMLWriterV21() {
        super(new org.geotools.kml.KMLConfiguration());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.geotools.kml.KML#Placemark}
     */
    @Override
    protected QName getContainer() {
        return org.geotools.kml.KML.Placemark;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.geotools.kml.KML#kml}
     */
    @Override
    protected QName getKML() {
        return org.geotools.kml.KML.kml;
    }
}
