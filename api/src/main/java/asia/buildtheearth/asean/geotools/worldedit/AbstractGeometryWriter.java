package asia.buildtheearth.asean.geotools.worldedit;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.pattern.Pattern;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.Geometries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.*;

import java.util.EnumMap;

import static org.geotools.geometry.jts.Geometries.MULTIPOINT;
import static org.geotools.geometry.jts.Geometries.MULTIPOLYGON;
import static org.geotools.geometry.jts.Geometries.MULTILINESTRING;

public abstract class AbstractGeometryWriter {
    protected final @NotNull  Pattern defaultPattern;
    protected final @Nullable EnumMap<Geometries, ? extends Pattern> patternMap;

    public AbstractGeometryWriter(@Nullable EnumMap<Geometries, ? extends Pattern> patternMap,
                                  @NotNull Pattern fallback) {
        this.patternMap = patternMap;
        this.defaultPattern = fallback;
    }

    public int writeGeometryInternal(@NotNull Geometry geometry,
                                     @Nullable GeometryCollection parentCollection) throws MaxChangedBlocksException {

        // Prioritize parent pattern (if any)
        Pattern parent = getPattern(parentCollection);
        Pattern pattern = (parent != null)? parent : getPatternOrDefault(geometry);

        return switch (geometry) {
            case Point point -> writePoint(point, pattern);
            case LineString line -> writeLine(line, pattern);
            case Polygon polygon -> writePolygon(polygon, pattern);
            case GeometryCollection collection -> writeCollection(collection);
            default -> 0;
        };
    }

    public abstract int writeGeometry(@NotNull Geometry geometry) throws TransformException, MaxChangedBlocksException;

    protected abstract int writePoint(@NotNull Point point, Pattern pattern) throws MaxChangedBlocksException;

    protected abstract int writeLine(@NotNull LineString line, Pattern pattern) throws MaxChangedBlocksException;

    protected abstract int writePolygon(@NotNull Polygon polygon, Pattern pattern) throws MaxChangedBlocksException;

    protected interface GeometryWriter { int write(Geometry geometry) throws MaxChangedBlocksException; }

    protected int writeCollection(GeometryCollection collection) throws MaxChangedBlocksException {
        Geometries geometries = Geometries.get(collection);

        if(geometries == null) return 0;

        GeometryWriter writer = switch (geometries) {
            case MULTIPOINT:
                yield geometry -> writePoint((Point) geometry, getPatternOrDefault(MULTIPOINT));
            case MULTILINESTRING:
                yield geometry -> writeLine((LineString) geometry, getPatternOrDefault(MULTILINESTRING));
            case MULTIPOLYGON:
                yield geometry -> writePolygon((Polygon) geometry, getPatternOrDefault(MULTIPOLYGON));
            default:
                yield geometry -> writeGeometryInternal(geometry, collection); // Unknown collection of type
        };

        return writeCollection(collection, writer);
    }

    protected final int writeCollection(@NotNull GeometryCollection geometries,
                                        @NotNull GeometryWriter writer) throws MaxChangedBlocksException {
        int count = geometries.getNumGeometries();
        int edits = 0;
        for (int i = 0; i < count; i++) edits += writer.write(geometries.getGeometryN(i));
        return edits;
    }

    private @Nullable Pattern getPattern(@Nullable Geometries geometry) {
        if(this.patternMap == null)
            return null;

        if(this.patternMap.containsKey(Geometries.GEOMETRY))
            return this.patternMap.get(Geometries.GEOMETRY);

        return this.patternMap.get(geometry);
    }

    private @Nullable Pattern getPattern(@Nullable Geometry geometry) {
        return getPattern(Geometries.get(geometry));
    }

    private @NotNull Pattern getPatternOrDefault(@Nullable Geometry geometry) {
        Pattern pattern = getPattern(geometry);
        return (pattern != null)? pattern : this.defaultPattern;
    }

    private @NotNull Pattern getPatternOrDefault(@Nullable Geometries geometry) {
        Pattern pattern = getPattern(geometry);
        return (pattern != null)? pattern : this.defaultPattern;
    }
}
