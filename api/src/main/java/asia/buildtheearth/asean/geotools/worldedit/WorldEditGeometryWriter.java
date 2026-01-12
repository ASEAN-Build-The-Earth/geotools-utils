package asia.buildtheearth.asean.geotools.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.block.*;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.operation.projection.MapProjection;
import org.locationtech.jts.geom.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WorldEditGeometryWriter extends AbstractGeometryWriter {
    public static final Pattern DEFAULT_PLACING_DIAMOND_BLOCK;
    public static final Pattern DEFAULT_AIR_BLOCK;

    protected final EditSession editSession;
    protected final GeometryCoordinateSequenceTransformer transformer;

    protected double writingSize = 0.0f;
    protected boolean fillStroke = false;
    protected boolean fillGeometry = false;

    static {
        DEFAULT_PLACING_DIAMOND_BLOCK = new DefaultPattern(() -> BlockTypes.DIAMOND_BLOCK);
        DEFAULT_AIR_BLOCK = new DefaultPattern(() -> BlockTypes.AIR);
    }

    public WorldEditGeometryWriter(@NotNull EditSession editSession,
                                   @NotNull MapProjection projection,
                                   EnumMap<Geometries, ? extends Pattern> patternMap,
                                   @NotNull Pattern fallback) {
        super(patternMap, fallback);
        this.editSession = editSession;
        this.transformer = new GeometryCoordinateSequenceTransformer();
        this.transformer.setMathTransform(projection);
    }

    /**
     * Create a writer writing to default placing block.
     * @param editSession Edit session to write blocks into
     * @param projection Minecraft coordinate projection
     * @deprecated You wouldn't be {@linkplain #DEFAULT_PLACING_DIAMOND_BLOCK}
     */
    @Deprecated
    public WorldEditGeometryWriter(@NotNull EditSession editSession,
                                   @NotNull MapProjection projection) {
        this(editSession, projection, null, DEFAULT_PLACING_DIAMOND_BLOCK);
    }

    public WorldEditGeometryWriter(@NotNull EditSession editSession,
                                   @NotNull MapProjection projection,
                                   EnumMap<Geometries, ? extends Pattern> patternMap) {
        this(editSession, projection, patternMap, DEFAULT_PLACING_DIAMOND_BLOCK);
    }

    public WorldEditGeometryWriter(@NotNull EditSession editSession,
                                   @NotNull MapProjection projection,
                                   @NotNull Pattern pattern) {
        this(editSession, projection, null, pattern);
    }

    public WorldEditGeometryWriter fillStroke() {
        this.fillStroke = true;
        return this;
    }

    public WorldEditGeometryWriter fillGeometry() {
        this.fillGeometry = true;
        return this;
    }

    public WorldEditGeometryWriter setWritingSize(float writingSize) {
        this.writingSize = writingSize;
        return this;
    }

    public int writeGeometry(@NotNull Geometry geometry) throws TransformException, MaxChangedBlocksException {
        Geometry transformed = (this.transformer != null)? this.transformer.transform(geometry) : geometry;

        return this.writeGeometryInternal(transformed, null);
    }

    protected int writePoint(@NotNull Point point, Pattern pattern) throws MaxChangedBlocksException {

        BlockVector3 position = BlockVector3.at(
                point.getCoordinate().getX(),
                point.getCoordinate().getZ(),
                point.getCoordinate().getY()
        );

        // Expand a block to sphere if there's writing radius
        if(Math.signum(this.writingSize) == 0)
            return this.editSession.makeSphere(position, pattern, this.writingSize, this.fillStroke);

        boolean edit = this.editSession.setBlock(position, pattern);

        return edit? 1 : 0;
    }

    protected int writeLine(@NotNull LineString line, Pattern pattern)  throws MaxChangedBlocksException {
        List<BlockVector3> list = new ArrayList<>(line.getNumPoints());

        for(Coordinate point : line.getCoordinates()) list.add(
                BlockVector3.at(point.getX(), point.getZ(), point.getY())
        );


        return this.editSession.drawLine(pattern, list, this.writingSize, this.fillStroke);
    }

    protected int writePolygon(@NotNull Polygon polygon, Pattern pattern) throws MaxChangedBlocksException {
        LinearRing shell = polygon.getExteriorRing();
        int holes = polygon.getNumInteriorRing();
        int edits = 0;
        int fill = this.fillGeometry? this.editSession.setBlocks(create2DRegion(shell), pattern) : 0;

        edits += this.writeLine(shell, pattern);
        edits += fill;

        for (int i = 0; i < holes; i++) {
            LinearRing hole = polygon.getInteriorRingN(i);

            if(fill > 0)
                edits -= this.editSession.setBlocks(create2DRegion(hole), DEFAULT_AIR_BLOCK);

            edits += this.writeLine(hole, pattern);
        }

        return edits;
    }

    @Contract("_ -> new")
    private static @NotNull Polygonal2DRegion create2DRegion(@NotNull LinearRing ring) {
        List<BlockVector2> fill = new ArrayList<>(ring.getNumPoints());

        int minY = -1, maxY = -1;

        for(Coordinate point : ring.getCoordinates()) {
            int newY = (int) Math.floor(point.getZ());
            if(minY == -1 || maxY == -1) {
                minY = newY;
                maxY = newY;
            }
            else {
                if (newY < minY) minY = newY;
                if (newY > maxY) maxY = newY;
            }

            fill.add(BlockVector2.at(point.getX(), point.getY()));
        }

        return new Polygonal2DRegion(NullWorld.getInstance(), fill, minY, maxY);
    }
}
