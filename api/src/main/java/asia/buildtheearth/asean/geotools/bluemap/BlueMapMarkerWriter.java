package asia.buildtheearth.asean.geotools.bluemap;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.*;
import de.bluecolored.bluemap.api.math.Line;
import de.bluecolored.bluemap.api.math.Shape;
import org.apache.commons.text.CaseUtils;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BlueMapMarkerWriter {

    /**
     * JSON writer instance for writing bluemap marker file.
     *
     * <p>Modified from {@link MarkerGson} to pretty printing on result.</p>
     */
    public static final Gson PRETTY_WRITER;
    public static final Gson COMPACT_WRITER;

    static {
        PRETTY_WRITER = MarkerGson.addAdapters(new GsonBuilder())
                .setPrettyPrinting()
                .setLenient()
                .create();
        COMPACT_WRITER = MarkerGson.INSTANCE;
    }

    @FunctionalInterface
    public interface MarkerWriter {

        void write(@NotNull String key, @NotNull Marker marker);
    }

    protected Supplier<@NotNull Gson> writer = () -> PRETTY_WRITER;

    protected ElevationMode elevation = ElevationMode.AUTO;

    protected Long normalizeZ, extrudePolygon = null;

    protected boolean normalizeNaming, disableClipping = false;

    public BlueMapMarkerWriter useCompactWriter() {
        this.writer = () -> COMPACT_WRITER;
        return this;
    }

    public BlueMapMarkerWriter averageAllElevation() {
        this.elevation = ElevationMode.AVERAGE;
        return this;
    }

    public BlueMapMarkerWriter normalizeAllElevation(long elevation) {
        this.elevation = ElevationMode.NORMALIZED;
        this.normalizeZ = elevation;
        return this;
    }

    public BlueMapMarkerWriter extrudeIfPolygon(long length) {
        this.extrudePolygon = length;
        return this;
    }

    public BlueMapMarkerWriter normalizeNaming() {
        this.normalizeNaming = true;
        return this;
    }

    public BlueMapMarkerWriter normalizeNaming(boolean normalize) {
        this.normalizeNaming = normalize;
        return this;
    }

    /**
     * Export marker set to final JSON file
     *
     * @param marker
     * @param output
     * @throws IOException
     */
    public void export(@NotNull MarkerSet marker, @NotNull Path output) throws IOException {
        Map<String, MarkerSet> exports = Map.of(this.getKeyName(output.getFileName().toString()), marker);

        // Finally, write the marker to JSON output file
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(output))) {
            this.writer.get().toJson(exports, writer);
        } catch (IOException ex) {
            throw new IOException("Failed to export Geometry data to final BlueMap marker file", ex);
        }
    }

    /**
     * Normalizes a base string into lower-hyphen (kebab-case) to be conventional with BlueMap marker naming.
     * <p>If {@code preserveNaming} is true, the original string is returned unmodified.</p>
     *
     * @see <a href="https://stackoverflow.com/a/70956516">Normalization Process</a>
     *
     * @param base The input base name (may include extension, Unicode, or CamelCase)
     * @return A normalized, lower-hyphen-case string
     */
    protected String toLowerHyphen(@NotNull String base) {
        // Strip Extension
        String name = base.replaceAll("\\.[^.]+$", "");

        // Normalize latin/unicode characters
        name = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("_", "-");

        // Split obvious non-word characters
        String[] words = name.split("\\W+");

        // Cannot determine words, returning as non-white-space
        if(words.length == 0) return name.replaceAll("\\s+", "");

        StringBuilder lowerHyphen = new StringBuilder();

        for (String word : words) {
            CaseUtils.toCamelCase(word, true, ' ');

            String hyphen = word.replaceAll("[a-z]+[0-9]*|[A-Z][a-z]+[0-9]*", "-$0-")
                    .replaceFirst("^-+", "")
                    .replaceFirst("-+$", "")
                    .replaceAll("--+", "-")
                    .toLowerCase();

            if(!lowerHyphen.isEmpty()) lowerHyphen.append("-");

            lowerHyphen.append(hyphen);
        }

        return lowerHyphen.toString();
    }

    /**
     * Write N geometry to a writer consumer.
     *
     * @param count The geometry count of the current geometry to write,
     *              only use as a fallback name for when geometry identifier is not found.
     * @param geometry The geometry data to be written
     * @param feature The parent feature containing the geometry attribute, used to define the marker name.
     * @param writer Writer consumer to handle how the resulting {@link Marker} will be written.
     */
    public void writeGeometry(int count,
                              @NotNull Geometry geometry,
                              @NotNull SimpleFeature feature,
                              @NotNull MarkerWriter writer) {


        // Determine marker name
        String markerName = getFeatureName(feature);
        String key = getKeyName(markerName, String.valueOf(count));

        switch (geometry) {
            case Point point: {
                POIMarker marker = new POIMarker.Builder()
                        .label(markerName)
                        .position(point.getX(), point.getY(),
                            this.elevation == ElevationMode.NORMALIZED?
                            this.normalizeZ : 0)
                        .build();

                writer.write(key, marker);
                break;
            }
            case LinearRing loop: {
                ShapeMarker.Builder marker = new ShapeMarker.Builder()
                        .label(markerName)
                        .depthTestEnabled(disableClipping);

                this.collectRing(loop, marker::shape);

                writer.write(key, marker.build());
                break;
            }
            case LineString line: {
                LineMarker marker = new LineMarker.Builder()
                        .label(markerName)
                        .line(this.collectLine(line))
                        .depthTestEnabled(disableClipping)
                        .build();

                writer.write(key, marker);
                break;
            }
            case Polygon polygon: {
                ObjectMarker.Builder<?, ?> marker = collectPolygon(markerName, polygon.getExteriorRing());

                // Add (if any) hole exists in the polygon
                if(polygon.getNumInteriorRing() > 0) {
                    Shape[] holes = new Shape[polygon.getNumInteriorRing()];

                    for (int i = 0; i < polygon.getNumInteriorRing(); i++)
                        holes[i] = this.collectRing(polygon.getInteriorRingN(i));

                    if(marker instanceof ShapeMarker.Builder builder) builder.holes(holes);
                    else if(marker instanceof ExtrudeMarker.Builder builder) builder.holes(holes);
                }

                writer.write(key, marker.build());
                break;
            }
            case GeometryCollection collection: {
                for (int i = 0; i < collection.getNumGeometries(); i++) {
                    // Display name will be modified as name-[i]
                    Function<String, String> index = this.indexedKey(i);
                    this.writeGeometry(count, collection.getGeometryN(i), feature,
                        (keyN, marker) -> writer.write(index.apply(keyN), marker)
                    );
                }
                break;
            }
            default: // Silent Fail
        }
    }

    protected @Nullable String getFeatureName(@NotNull SimpleFeature feature) {
        Property nameProp = feature.getProperty("name");
        if(nameProp != null && nameProp.getValue() instanceof String name) return name;
        else if(feature.getAttribute("name") instanceof String name) return name;
        else if(feature.getIdentifier() != null) return feature.getID();
        else return null;
    }

    protected ObjectMarker.Builder<?, ?> collectPolygon(String markerName, LinearRing loop) {

        // Returns extrude marker if the polygon should be extruded
        if(extrudePolygon != null) {
            ExtrudeMarker.Builder marker = new ExtrudeMarker.Builder()
                .label(markerName)
                .depthTestEnabled(disableClipping);

            this.collectRing(loop, (ring, elevation) ->
                marker.shape(ring, elevation, elevation + this.extrudePolygon)
            );

            return marker;
        }

        // Collect polygon as default shape marker
        ShapeMarker.Builder marker = new ShapeMarker.Builder()
            .label(markerName)
            .depthTestEnabled(disableClipping);

        this.collectRing(loop, marker::shape);

        return marker;
    }

    protected void collectRing(@NotNull LinearRing ring,
                               @NotNull BiConsumer<@NotNull Shape, @NotNull Long> receiver) {

        if(this.elevation == ElevationMode.NORMALIZED) {
            Shape shape = this.collectRing(ring);
            receiver.accept(shape, this.normalizeZ);
            return;
        }

        Vector2d[] shape = new Vector2d[ring.getNumPoints()];

        double sumZ = 0;
        int countZ = 0;

        for (int i = 0; i < ring.getNumPoints(); i++) {
            Coordinate point = ring.getCoordinateN(i);
            shape[i] = new Vector2d(point.x, point.y);

            if (Double.isNaN(point.getZ())) continue;

            sumZ += point.getZ();
            countZ++;
        }

        long averageZ = countZ > 0 ? Math.round(sumZ / countZ) : 0;

        receiver.accept(new Shape(shape), averageZ);
    }

    protected Shape collectRing(@NotNull LinearRing ring) {
        Vector2d[] shape = new Vector2d[ring.getNumPoints()];

        for (int i = 0; i < ring.getNumPoints(); i++) {
            Coordinate point = ring.getCoordinateN(i);
            shape[i] = new Vector2d(point.x, point.y);
        }

        return new Shape(shape);
    }

    protected Line collectLine(@NotNull LineString line) {
        if(this.elevation == ElevationMode.AVERAGE) {
            double sumZ = 0;
            int countZ = 0;
            ArrayList<Vector2d> shape = new ArrayList<>(line.getNumPoints());

            for (Coordinate point : line.getCoordinates()) {
                shape.add(new Vector2d(point.x, point.y));

                if (Double.isNaN(point.getZ())) continue;

                sumZ += point.getZ();
                countZ++;
            }

            long averageZ = countZ > 0 ? Math.round(sumZ / countZ) : 0;

            return new Line(shape
                .stream()
                .map(vector -> new Vector3d(vector, averageZ))
                .toArray(Vector3d[]::new)
            );
        }

        Vector3d[] shape = new Vector3d[line.getNumPoints()];

        for (int i = 0; i < line.getNumPoints(); i++) {
            Coordinate point = line.getCoordinateN(i);
            shape[i] = new Vector3d(point.x, point.y, point.z);
        }

        return new Line(shape);
    }

    private @NotNull String getKeyName(@Nullable String name, @NotNull String fallback) {
        if(name == null) return fallback;

        if(!normalizeNaming) return name;

        return toLowerHyphen(name);
    }

    private @NotNull String getKeyName(@NotNull String name) {
        if(!normalizeNaming) return name;

        return toLowerHyphen(name);
    }

    @Contract(pure = true)
    private @NotNull Function<String, String> indexedKey(int index) {
        return key -> key + '-' + index;
    }
}
