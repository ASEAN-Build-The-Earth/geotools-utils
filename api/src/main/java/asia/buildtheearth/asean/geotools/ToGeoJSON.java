package asia.buildtheearth.asean.geotools;

import asia.buildtheearth.asean.geotools.kml.store.KMLFeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.geojson.store.GeoJSONDataStore;
import org.geotools.data.geojson.store.GeoJSONFeatureReader;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *  Converts geospatial file to GeoJSON format.
 *
 * @see #identity(File)
 * @see #fromKML(File)
 */
public abstract sealed class ToGeoJSON extends AbstractGeoToolsConverter {
    private ToGeoJSON(File source) { super(source); }

    /** {@inheritDoc} */
    @Override
    public abstract void convert(Path output) throws IOException;

    /**
     * Identity converter that reads a GeoJSON file and writes it back with optional modifications
     * such as projection transformation or geometry adjustment.
     *
     * @param geojsonFile the input GeoJSON file
     * @return a converter that performs an identity transformation on the GeoJSON input
     */
    public static @NotNull FromGeoJSON identity(File geojsonFile) {
        return new FromGeoJSON(geojsonFile);
    };

    /**
     * Creates a new GeoJSON converter that reads a KML file as input.
     *
     * <p>Equivalent to {@link ToGeoJSON.FromKML#FromKML(File)}.</p>
     *
     * @param kmlFile the input KML file to convert
     * @return a converter that transforms the KML file to GeoJSON
     */
    @Contract("_ -> new")
    public static @NotNull ToGeoJSON fromKML(File kmlFile) {
        return new FromKML(kmlFile);
    }

    /**
     * Converts KML file to GeoJSON format.
     *
     * <p>Only elements matching the configured {@code parsingElement} are parsed.
     * Geometry and supported feature data are retained in the GeoJSON output.</p>
     *
     * @see #setParsingElement(QName)
     * @see #convert(Path)
     */
    public static final class FromKML extends ToGeoJSON {

        /**
         * Constructs a new converter using the given input file and output path.
         *
         * @param kmlFile source file to be converted
         */
        public FromKML(File kmlFile) {
            super(kmlFile);
        }

        /**
         * Converts the input KML file to GeoJSON format and writes the result to the output path.
         *
         * @param output the destination GeoJSON file path for the converted result
         * @throws IOException if the input file cannot be read or the output file cannot be written
         */
        @Override
        public void convert(Path output) throws IOException {
            // Create a geojson writer
            try(GeoJSONWriter geojsonWriter = new GeoJSONWriter(Files.newOutputStream(output))) {
                if(precision != null) geojsonWriter.setMaxDecimals(precision);
                geojsonWriter.setPrettyPrinting(prettyPrint);

                // Read all KML features
                try(KMLFeatureReader reader = new KMLFeatureReader(this.sourceFile, this.parsingElement)) {

                    // Write each feature into geojson file
                    while(reader.hasNext()) {
                        SimpleFeature feature = reader.next();

                        // Modify coordinates if detected change
                        if(this.hasCoordinatesModifier())
                            this.modifyAllCoordinates(feature);

                        geojsonWriter.write(feature);
                    }
                }
                catch (IOException ex) { throw new IOException("Failed to write KML data to GeoJSONWriter", ex); }
            }
            catch (IOException ex) { throw new IOException("Failed to construct GeoJSONWriter", ex); }
        }
    }

    /**
     * Identity converter from GeoJSON to GeoJSON
     */
    public static final class FromGeoJSON extends ToGeoJSON {
        /**
         * Constructs a new converter using the given input file and output path.
         *
         * @param geojsonFile source file to be converted
         */
        public FromGeoJSON(File geojsonFile) {
            super(geojsonFile);
        }

        @Override
        public void convert(Path output) throws IOException {
            // Create a geojson writer
            try(GeoJSONWriter geojsonWriter = new GeoJSONWriter(Files.newOutputStream(output))) {
                if(precision != null) geojsonWriter.setMaxDecimals(precision);
                geojsonWriter.setPrettyPrinting(prettyPrint);

                GeoJSONDataStore store = new GeoJSONDataStore(this.sourceFile);
                ContentState content = new ContentState(new ContentEntry(store, store.getTypeName()));

                // Read all GeoJSON features
                try(GeoJSONFeatureReader reader = new GeoJSONFeatureReader(content, Query.ALL)) {

                    // Write each feature into kml file
                    while(reader.hasNext()) {
                        SimpleFeature feature = reader.next();

                        // Modify coordinates if detected change
                        if(this.hasCoordinatesModifier())
                            this.modifyAllCoordinates(feature);

                        geojsonWriter.write(feature);
                    }
                }
                catch (IOException ex) { throw new IOException("Failed to write KML data to GeoJSONWriter", ex); }
            }
            catch (IOException ex) { throw new IOException("Failed to construct GeoJSONWriter", ex); }
        }
    }

    protected void modifyAllCoordinates(@NotNull SimpleFeature feature) {
        for(Property props : feature.getValue()) {
            if(props == null) continue;

            if(props.getValue() instanceof Geometry geometry) {
                props.setValue(this.applyAllCoordinates(geometry));
            }
        }
    }
}
