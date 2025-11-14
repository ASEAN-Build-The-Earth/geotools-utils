package asia.buildtheearth.asean.geotools.kml.writer;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract base class for exporting geometries as KML documents using different KML versions.
 *
 * @see KMLWriterV22
 */
public abstract sealed class KMLWriter permits KMLWriterV21, KMLWriterV22 {

    /** The GeoTools encoder used to serialize the KML document. */
    protected final Encoder encoder;

    /** The feature collection to be serialized. */
    protected final List<SimpleFeature> collection;

    /** Optional identifier for the exported feature collection. */
    protected String outputID = null;

    private KMLWriter(Configuration configuration) {
        this.encoder = new Encoder(configuration);
        this.collection = new ArrayList<>();
    }

    /**
     * Creates a new writer using GeoTools KML 2.1 configuration.
     *
     * @param configuration Configuration class, specifically {@link org.geotools.kml.KMLConfiguration}
     */
    protected KMLWriter(org.geotools.kml.KMLConfiguration configuration) {
        this((Configuration) configuration);
    }

    /**
     * Creates a new writer using GeoTools KML 2.2 configuration.
     *
     * @param configuration Configuration class, specifically {@link org.geotools.kml.v22.KMLConfiguration}
     */
    protected KMLWriter(org.geotools.kml.v22.KMLConfiguration configuration) {
        this((Configuration) configuration);
    }

    /**
     * Returns the QName of the container element, conventionally a {@code Placemark}.
     *
     * @return Q-name definition of the container element
     */
    abstract protected QName getContainer();

    /**
     * Returns the QName of the KML root element, typically {@code kml}.
     *
     * @return Q-name definition of the root element
     */
    abstract protected QName getKML();

    /**
     * Enables pretty-printing with default indentation of {@code 2}.
     */
    public void enablePrettyPrinting() {
        this.encoder.setIndenting(true);
    }

    /**
     * Enables pretty-printing with a custom indent size.
     *
     * @param indentSize number of spaces for indentation
     */
    public void enablePrettyPrinting(int indentSize) {
        this.encoder.setIndenting(true);
        this.encoder.setIndentSize(indentSize);
    }

    /**
     * Disables XML namespaces in the generated KML output.
     *
     * <p>This can be useful for simpler KML documents or tools that are not namespace-aware.</p>
     */
    public void dropNamespace() {
        this.encoder.setNamespaceAware(false);
    }

    /**
     * Sets an identifier for the exported feature collection.
     *
     * @param identifier the ID to associate with the collection
     */
    public void setOutputID(String identifier) {
        this.outputID = identifier;
    }

    /**
     * Exports the KML content to the specified output stream.
     *
     * @param output the stream to write to
     * @throws IOException if writing fails
     */
    public void export(OutputStream output) throws IOException {
        DefaultFeatureCollection feature = new DefaultFeatureCollection(this.outputID);
        feature.addAll(this.collection);
        this.encoder.encode(feature, getKML(), output);
    }

    /**
     * Adds a geometry as a {@link SimpleFeature} to the collection to be exported.
     *
     * @param geometry the geometry to add
     * @param identifier optional feature ID; if {@code null}, an auto-generated ID is used
     */
    public void writeGeometry(@NotNull Geometry geometry, @Nullable FeatureId identifier) {
        FeatureId id = identifier == null? new FeatureIdImpl(String.valueOf(this.collection.size())) : identifier;
        this.collection.add(new SimpleFeatureImpl(Collections.singletonList(geometry), this.buildContainer(), id));
    }

    /**
     * Builds the container {@link SimpleFeatureType} for this writer's KML structure.
     *
     * @return a new feature type with a geometry field
     */
    protected @NotNull SimpleFeatureType buildContainer() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(getContainer().getLocalPart());
        typeBuilder.setNamespaceURI(getContainer().getNamespaceURI());
        typeBuilder.add("geometry", Geometry.class);

        return typeBuilder.buildFeatureType();
    }
}
