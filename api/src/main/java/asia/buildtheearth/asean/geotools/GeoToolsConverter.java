package asia.buildtheearth.asean.geotools;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import org.geotools.api.referencing.FactoryException;
import org.geotools.data.geojson.GeoJSONWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Common interface for GeoTools-based format converters.
 *
 * <p>Implementations convert between geospatial file formats such as KML and GeoJSON,
 * supporting optional features like coordinate precision control, Z-value adjustment,
 * and pretty printing.</p>
 *
 * <p>The main entry point is {@link #convert(Path)}, which writes the converted output
 * to the specified file.</p>
 *
 * @see ToKML
 * @see ToGeoJSON
 */
public interface GeoToolsConverter {

    /**
     * Normalizes all Z (elevation) values to a fixed value.
     * <p>
     * This forces all coordinates in the output to have the same Z value, regardless of their original value.
     * </p>
     *
     * @param value the fixed Z value to apply to all coordinates
     * @return this instance for chaining
     */
    GeoToolsConverter normalizeZ(double value);

    /**
     * Offsets all Z (elevation) values in the geometry by a fixed amount.
     *
     * <p>a coordinate {@code (x, y, z)} becomes {@code (x, y, z + offset)}.</p>
     *
     * <p>Has no effect if the geometry has been {@linkplain #normalizeZ(double) normalized}
     * or the Z dimension is {@linkplain #dropZ() dropped}.</p>
     *
     * @param value the amount to add to each Z value
     * @return this instance for chaining
     */
    GeoToolsConverter setOffsetZ(double value);

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
    GeoToolsConverter setParsingElement(@NotNull QName elementName);

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
    GeoToolsConverter dropZ();

    /**
     * Disables pretty-printing (human-readable formatting) for the output file.
     * <p>
     * Pretty-printing is enabled by default.
     * </p>
     *
     * @apiNote Only applies when converting from KML to GeoJSON.
     *
     * @return this instance for chaining
     */
    GeoToolsConverter disablePrettyPrint();

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
    GeoToolsConverter setPrecision(int precision);

    /**
     * Performs the format conversion and writes the result to the specified output path.
     *
     * @param output the target file path to write the converted result to
     * @throws IOException if an I/O error occurs during reading or writing
     */
    void convert(Path output) throws FactoryException, IOException;
}