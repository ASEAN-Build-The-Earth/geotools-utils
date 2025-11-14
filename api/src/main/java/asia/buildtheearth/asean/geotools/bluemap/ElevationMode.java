package asia.buildtheearth.asean.geotools.bluemap;


import de.bluecolored.bluemap.api.markers.LineMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

/**
 * Supported elevation strategies for BlueMap markers.
 */
public enum ElevationMode {

    /**
     * Automatically chooses the most suitable elevation strategy depending on the geometry type.
     *
     * <p>For geometries that support per-vertex elevation (e.g., {@linkplain LineString} as {@linkplain LineMarker}), the Z values are preserved.</p>
     *
     * <p>For geometries that only allow a single elevation (e.g., {@linkplain Polygon} as {@linkplain ShapeMarker}), the elevation will
     * default to {@link #AVERAGE}.</p>
     */
    AUTO,

    /**
     * Applies the average elevation of all coordinates in a geometry as its single Z value.
     */
    AVERAGE,

    /**
     * Ignores actual elevation data and uses a fixed normalized value instead.
     */
    NORMALIZED
}