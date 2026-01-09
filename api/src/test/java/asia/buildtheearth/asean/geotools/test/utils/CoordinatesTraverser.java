package asia.buildtheearth.asean.geotools.test.utils;

import com.bedatadriven.jackson.datatype.jts.GeoJson;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Assertions;
import org.locationtech.jts.geom.Geometry;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility class for recursively traversing all coordinate arrays in a GeoJSON-compatible
 * {@link org.json.simple.JSONObject} structure.
 * <p>
 * Specifically targets values under keys named {@code "coordinates"}, regardless of nesting depth,
 * and invokes a callback on each innermost coordinate array (typically [x, y] or [x, y, z]).
 * </p>
 *
 * <p>Used for validating all coordinates data in GeoJSON geometries like
 * Points, LineStrings, Polygons, and their multi- variants.</p>
 *
 * <p>Example usage:</p>
 * <blockquote>{@snippet :
 * CoordinatesTraverser traverser = new CoordinatesTraverser(arr -> {
 *     if (arr.size() != 2) {
 *         System.out.println("Invalid coordinate: " + arr);
 *     }
 * });
 * traverser.traverse(geoJsonObject);
 * }</blockquote>
 * 
 * @see #traverse(String) 
 */
public class CoordinatesTraverser {

    private final Consumer<JSONArray> onEachArray;
    private final BiConsumer<Geometry, FeatureId> onEachGeometry;

    /**
     * Constructs a {@code CoordinatesTraverser} with the given consumer.
     *
     * @param onEachArray A callback function that will be invoked for each innermost
     *                    coordinate array with {@link JSONArray}.
     *                    The array typically contains two or three numbers: [x, y] or [x, y, z].
     * @param onEachGeometry A callback function that will be invoked on each {@link SimpleFeature}
     *                       providing the feature's {@linkplain  FeatureId identifier} and its {@link Geometry}
     */
    public CoordinatesTraverser(@NotNull Consumer<JSONArray> onEachArray,
                                @NotNull BiConsumer<Geometry, FeatureId> onEachGeometry) {
        this.onEachArray = onEachArray;
        this.onEachGeometry = onEachGeometry;
    }

    /**
     * Traverse the raw JSON string to find geometry feature(s),
     * invoking the callback for each feature found.
     *
     * <p>This split to 2 check:</p>
     * <ol>
     *     <li>Parse a raw JSON string as {@link JSONObject} and traverse for all raw JSON coordinates array
     *     to invoke {@link #onEachArray}.</li>
     *     <li>Parse a raw JSON string as {@link org.geotools.data.simple.SimpleFeatureCollection}
     *     and iterate through all features invoking {@link #onEachGeometry} if geometry data is found.</li>
     * </ol>
     *
     * @param rawJSON the raw GeoJSON-parsable string
     */
    public void traverse(String rawJSON) {
        Object jsonObj = Assertions.assertDoesNotThrow(() -> new JSONParser().parse(rawJSON),
            "Expected valid JSON string to traverse its features."
        );

        Assertions.assertInstanceOf(JSONObject.class, jsonObj);

        this.traverse(rawJSON, null);

        try(SimpleFeatureIterator features = GeoJSONReader.parseFeatureCollection(rawJSON).features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();

                for(Property props : feature.getValue()) {
                    if(props == null) continue;
                    if(props.getValue() instanceof Geometry geometry)
                        this.onEachGeometry.accept(geometry, feature.getIdentifier());
                }
            }
        }
    }

    private void traverse(Object node, String parentKey) {
        if(node instanceof JSONObject obj) for (Object keyObj : obj.keySet()) {
            String key = (String) keyObj;
            Object value = obj.get(key);
            traverse(value, key);
        }
        else if(node instanceof JSONArray array)
            if(GeoJson.COORDINATES.equals(parentKey)) checkCoordinatesArray(array);
            else for(Object item : array) traverse(item, null);
    }

    private void checkCoordinatesArray(Object node) {
        if (node instanceof JSONArray array) {
            if (isFlatArrayOfNumbers(array)) this.onEachArray.accept(array);
            else for (Object item : array) checkCoordinatesArray(item);
        }
    }

    @Contract(pure = true)
    private boolean isFlatArrayOfNumbers(@NotNull JSONArray arr) {
        for (Object item : arr) if (!(item instanceof Number)) return false;
        return !arr.isEmpty(); // only count non-empty arrays
    }

    static {
        // Static sanity check: GeoJson.COORDINATES must match the literal string "coordinates".
        // Will never fail unless the underlying library changes the expected value.
        Assertions.assertEquals(
                GeoJson.COORDINATES,
                "coordinates",
                "The value of GeoJson.COORDINATES differs from the expected 'coordinates' string. "
                        + "This may indicate a breaking change in the GeoJson library."
        );
    }
}
