package asia.buildtheearth.asean.geotools;

import asia.buildtheearth.asean.geotools.kml.store.KMLFeatureReader;
import asia.buildtheearth.asean.geotools.kml.writer.KMLWriter;
import asia.buildtheearth.asean.geotools.kml.writer.KMLWriterV21;
import asia.buildtheearth.asean.geotools.kml.writer.KMLWriterV22;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.store.GeoJSONDataStore;
import org.geotools.data.geojson.store.GeoJSONFeatureReader;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Converts geospatial file to KML format.
 *
 * @see #identity(File)
 * @see #fromGeoJSON(File)
 */
public abstract sealed class ToKML extends AbstractGeoToolsConverter {
    private ToKML(File source) {
        super(source);
    }

    /** Writing indentation size */
    protected int indentSize = 2;

    /** Writer of KML v2.2 configuration */
    protected Supplier<@NotNull KMLWriter> writer = KMLWriterV22::new;


    /** {@inheritDoc} */
    @Override
    public abstract void convert(Path output) throws IOException;

    /**
     * Creates an identity converter that reads a KML file and writes it back with optional modifications,
     * such as applying offsets, reprojection, or rewriting geometry structures.
     *
     * @param kmlFile the input KML file to be transformed and written as KML again
     * @return a converter that performs an identity transformation on the KML input
     */
    public static @NotNull FromKML identity(File kmlFile) {
        return new FromKML(kmlFile);
    };

    /**
     * Creates a new KML converter that reads a GeoJSON file and writes its contents as KML.
     *
     * <p>Equivalent to {@link ToKML.FromKML#FromKML(File)}.</p>
     *
     * @param geojsonFile the input GeoJSON file to convert to KML
     * @return a converter that transforms the GeoJSON file into KML format
     */
    @Contract("_ -> new")
    public static @NotNull ToKML fromGeoJSON(File geojsonFile) {
        return new FromGeoJSON(geojsonFile);
    }

    /**
     * Forces the converter to use the deprecated KML 2.1 writer.
     *
     * @apiNote Use with caution. This is primarily intended for compatibility with older tools.
     * @return this instance for chaining
     */
    @SuppressWarnings("deprecation")
    @Contract(value = " -> this", mutates = "this")
    public ToKML forceWriterV21() {
        this.writer = KMLWriterV21::new;
        return this;
    }

    /**
     * Sets the indentation size for pretty-printed XML output.
     *
     * <p>
     * By default, the indent size is {@code 2}.
     * This setting only takes effect if pretty printing is not disabled.
     * </p>
     *
     * @param indentSize the number of spaces used for each indent level
     * @return this instance for chaining
     */
    @Contract(value = "_ -> this", mutates = "this")
    public ToKML setIndentSize(int indentSize) {
        this.indentSize = indentSize;
        return this;
    }

    /**
     * Disables pretty-print formatting for the XML output.
     *
     * <p>By default, pretty printing is enabled. Disabling it will produce a compact, single-line XML.</p>
     *
     * @return this instance for chaining
     */
    @Override
    @Contract(" -> this")
    public ToKML disablePrettyPrint() {
        this.prettyPrint = false;
        return this;
    }

    /**
     * Unsupported operation.
     *
     * <p>The KML writer uses full precision by default and does not support limiting coordinate precision.</p>
     *
     * @throws UnsupportedOperationException always thrown since this operation is not supported
     */
    @Override
    @Contract("_ -> fail")
    public ToKML setPrecision(int ignored) {
        throw new UnsupportedOperationException("Fixed precision is not supported by KML writer");
    }

    /**
     * Converts GeoJSON file to KML format.
     *
     * <p>Only geometry data is converted. Feature properties and metadata may not be preserved.</p>
     */
    public static final class FromGeoJSON extends ToKML {
        /**
         * Constructs a new converter using the given input file and output path.
         *
         * @param geojsonFile source file to be converted
         */
        public FromGeoJSON(File geojsonFile) {
            super(geojsonFile);
        }

        /**
         * Converts a GeoJSON file to a KML file, exporting only geometry data.
         * <p>
         * This writes geometry from the input GeoJSON file to a KML format.
         * Feature properties and custom metadata will be discarded.
         * </p>
         *
         * @implNote Only coordinate data and geometry structure will be retained in the output KML.
         *
         * @param kmlOutput the destination kml file path for the converted result
         * @throws IOException if the input file cannot be read or the output file cannot be written
         */
        @Override
        public void convert(Path kmlOutput) throws IOException {
            // Create a kml writer
            try(OutputStream writer = Files.newOutputStream(kmlOutput)) {
                KMLWriter kmlWriter = this.writer.get();

                if(prettyPrint) kmlWriter.enablePrettyPrinting(this.indentSize);

                GeoJSONDataStore store = new GeoJSONDataStore(this.sourceFile);
                ContentState content = new ContentState(new ContentEntry(store, store.getTypeName()));
                // Read all GeoJSON features
                try(GeoJSONFeatureReader reader = new GeoJSONFeatureReader(content, Query.ALL)) {

                    // Write each feature into kml file
                    while(reader.hasNext()) {
                        SimpleFeature feature = reader.next();
                        Object geometryAttribute = feature.getAttribute(GeoJSONReader.GEOMETRY_NAME);
                        if(geometryAttribute instanceof Geometry raw) {
                            Geometry geometry = this.hasCoordinatesModifier()? this.applyAllCoordinates(raw) : raw;
                            kmlWriter.writeGeometry(geometry, feature.getIdentifier());
                        }
                    }

                    kmlWriter.export(writer);
                }
                catch (IOException ex) { throw new IOException("Failed to write Geometry data to KMLWriter", ex); }
            }
            catch (IOException ex) { throw new IOException("Failed to construct KMLWriter", ex); }
        }
    }

    public static final class FromKML extends ToKML {
        /**
         * Constructs a new converter using the given input file and output path.
         *
         * @param kmlFile source file to be converted
         */
        public FromKML(File kmlFile) {
            super(kmlFile);
        }

        /** {@inheritDoc} */
        @Override
        public void convert(Path kmlOutput) throws IOException {
            // Create a kml writer
            try(OutputStream writer = Files.newOutputStream(kmlOutput)) {
                KMLWriter kmlWriter = this.writer.get();

                if(prettyPrint) kmlWriter.enablePrettyPrinting(this.indentSize);

                // Read all KML features
                try(KMLFeatureReader reader = new KMLFeatureReader(this.sourceFile, this.parsingElement)) {

                    // Write each feature into geojson file
                    while(reader.hasNext()) {
                        SimpleFeature feature = reader.next();

                        for(Property props : feature.getValue()) {
                            if(props == null) continue;

                            if(props.getValue() instanceof Geometry raw) {
                                Geometry geometry = this.hasCoordinatesModifier()? this.applyAllCoordinates(raw) : raw;
                                kmlWriter.writeGeometry(geometry, feature.getIdentifier());
                            }
                        }
                    }
                }
                catch (IOException ex) { throw new IOException("Failed to write KML data to KML file", ex); }

                kmlWriter.export(writer);
            }
            catch (IOException ex) { throw new IOException("Failed to construct KMLWriter", ex); }
        }
    }
}
