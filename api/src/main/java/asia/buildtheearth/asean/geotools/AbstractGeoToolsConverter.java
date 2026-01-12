package asia.buildtheearth.asean.geotools;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import org.geotools.api.referencing.FactoryException;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.referencing.operation.projection.MapProjection;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryEditor;

import javax.xml.namespace.QName;
import java.io.*;

/**
 * A utility class to convert between KML and GeoJSON files with optional customization.
 * <p>
 * Supports KML → GeoJSON and GeoJSON → KML transformations, with options for precision,
 * Z-coordinate normalization, and pretty-printing.
 * </p>
 *
 */
abstract sealed class AbstractGeoToolsConverter
        implements GeoToolsConverter
        permits SchematicExport, ToBlueMapMarker, ToGeoJSON, ToKML {

    public interface ProjectionSupplier {
        MapProjection get() throws FactoryException;
    }

    /** Whether to pretty-print the output XML or JSON. */
    protected boolean prettyPrint = true;

    /** Maximum number of decimal places for coordinate values; {@code null} means default precision. */
    protected Integer precision = null;

    /** Z value to normalize all coordinates to; {@code null} means no normalization. */
    protected Double normalizedZ = null;

    /** Offset to apply to existing Z values; {@code null} means no offset. */
    protected Double offsetZ = null;

    /** Root element used when parsing KML input. */
    protected QName parsingElement = org.geotools.kml.KML.Placemark;

    /** The input file to be converted. */
    protected final File sourceFile;

    /**
     * Constructs a new converter using the given input file and output path.
     *
     * @param sourceFile source file to be converted
     */
    public AbstractGeoToolsConverter(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * Normalizes all Z (elevation) values to a fixed value.
     * <p>
     * This forces all coordinates in the output to have the same Z value, regardless of their original value.
     * </p>
     *
     * @param value the fixed Z value to apply to all coordinates
     * @return this instance for chaining
     */
    @Override
    public AbstractGeoToolsConverter normalizeZ(double value) {
        this.normalizedZ = value;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractGeoToolsConverter setOffsetZ(double value) {
        this.offsetZ = value;
        return this;
    }

    /**
     * Specifies the single KML element type to be parsed when reading from a KML file.
     * <p>
     * Only elements matching this name will be streamed and parsed as features.
     * By default, this is set to {@linkplain org.geotools.kml.KML#Placemark}, which represents
     * most geometries in KML documents.
     * </p>
     *
     * @param elementName the qualified name of the KML element to parse (e.g. {@linkplain org.geotools.kml.KML#Polygon Polygon})
     * @return this instance for chaining
     *
     * @see org.geotools.xsd.StreamingParser
     * @see org.geotools.kml.KML
     */
    @Override
    public AbstractGeoToolsConverter setParsingElement(@NotNull QName elementName) {
        this.parsingElement = elementName;
        return this;
    }

    /**
     * Drops all Z (elevation) values from the geometry, producing 2D coordinates only.
     * <p>
     * This sets the Z component of each coordinate to
     * {@link org.locationtech.jts.geom.Coordinate#NULL_ORDINATE}, ensuring output is strictly [x, y].
     * </p>
     *
     * <p>If you prefer to keep Z values but normalize them to a constant,
     * use {@link #normalizeZ(double)} instead.</p>
     *
     * @return this instance for chaining
     */
    @Override
    public AbstractGeoToolsConverter dropZ() {
        this.normalizedZ = Coordinate.NULL_ORDINATE;
        return this;
    }

    /**
     * Disables pretty-printing (human-readable formatting) for the output file.
     * <p>
     * Pretty-printing is enabled by default.
     * </p>
     **
     * @return this instance for chaining
     */
    @Override
    public AbstractGeoToolsConverter disablePrettyPrint() {
        this.prettyPrint = false;
        return this;
    }

    /**
     * Sets the maximum number of decimal places for coordinate values in the output.
     * <p>
     * This controls the rounding precision when writing coordinate data:
     * </p>
     * <ul>
     *   <li><strong>GeoJSON:</strong> Precision is applied via {@link GeoJSONWriter}, with a default of {@linkplain JtsModule#DEFAULT_MAX_DECIMALS 6} decimal places.</li>
     *   <li><strong>KML:</strong> No precision limit is applied by default; full {@code double} precision is retained unless this is explicitly set.</li>
     * </ul>
     *
     * @param precision the number of decimal places to keep (e.g. 6 for micro-degree accuracy)
     * @return this instance for chaining
     */
    @Override
    public AbstractGeoToolsConverter setPrecision(int precision) {
        this.precision = precision;
        return this;
    }

    /**
     * Checks whether any Z-coordinate transformation (normalization or offset) has been requested.
     *
     * @return {@code true} if either {@link #normalizedZ} or {@link #offsetZ} is set; {@code false} otherwise
     */
    protected final boolean hasCoordinatesModifier() {
        return normalizedZ != null || offsetZ != null;
    }

    /**
     * Applies Z-coordinate modifications to the given coordinate array.
     *
     * @param toEdit the geometry to be edited.
     * @return edited geometry.
     */
    protected final Geometry applyAllCoordinates(Geometry toEdit) {
        GeometryEditor editor = new GeometryEditor();

        return editor.edit(toEdit, new DefaultCoordinateOperation());
    }

    /**
     * Default operation that apply Z coordinate modifier.
     * <p>
     * TODO: The class {@link CoordinateArrays} has many utilities method to clean up coordinates of any is invalid
     */
    class DefaultCoordinateOperation extends GeometryEditor.CoordinateOperation {
        /**
         * Operate all coordinates.
         * @param coordinates the coordinate array to operate on
         * @param geometry the geometry containing the coordinate list
         * @return Modified coordinate with Z value edited
         */
        @Override
        public Coordinate[] edit(Coordinate[] coordinates, Geometry geometry) {
            Coordinate[] edits = CoordinateArrays.copyDeep(coordinates);

            if (normalizedZ != null) {
                // Check for normalized Z which takes 1st priority
                for (Coordinate coordinate : edits) coordinate.setZ(normalizedZ);
            }
            else if (offsetZ != null) {
                // Else, if offsetZ is set then apply it to all coordinates
                for (Coordinate coordinate : edits) {
                    double originalZ = coordinate.getZ();

                    // Retain original Z if some coordinate value is NaN
                    coordinate.setZ(Double.isNaN(originalZ) ? originalZ : originalZ + offsetZ);
                }
            }

            return edits;
        }
    }
}
