package asia.buildtheearth.asean.geotools;

import asia.buildtheearth.asean.geotools.kml.store.KMLFeatureReader;
import asia.buildtheearth.asean.geotools.projection.MinecraftCRS;
import asia.buildtheearth.asean.geotools.projection.MinecraftProjection;
import asia.buildtheearth.asean.geotools.projection.TerraProjection;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.referencing.operation.projection.MapProjection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import javax.xml.namespace.QName;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.*;

/**
 * Converts KML geometry into a schematic-compatible raster format using brute-force pixel containment logic.
 *
 * <p><strong>Deprecated since 1.0.0:</strong> This implementation is deprecated due to severe performance limitations
 * and lack of integration with modern schematic editing libraries.</p>
 *
 * <p>The responsibility for schematic generation has been moved to a dedicated extension using
 * the <a href="https://enginehub.org/worldedit/">WorldEdit (FAWE)</a> API for efficient and native manipulation
 * of Minecraft `.schem` files.
 *
 * @deprecated Since 1.0.0 – superseded by FAWE-based schematic writer with native raster pipeline.
 */
@Deprecated(since = "1.0.0")
public class KMLToSchematic {

    interface ProjectionSupplier {
        MapProjection get() throws FactoryException;
    }

    private ProjectionSupplier projection = MinecraftProjection::getBTE;
    private final File kmlFile;
    private QName parsingElement = org.geotools.kml.KML.Placemark;

    /**
     * Constructs a new converter using the given input file and output path.
     *
     * @param kmlFile source file to be converted
     */
    public KMLToSchematic(File kmlFile) {
        this.kmlFile = kmlFile;
    }

    /**
     * Set a custom projection for this converter.
     *
     * @param projection New projection to set
     * @return This instance for chaining
     */
    public KMLToSchematic setProjection(@NotNull MapProjection projection) {
        this.projection = () -> projection;
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
    public KMLToSchematic setParsingElement(@NotNull QName elementName) {
        this.parsingElement = elementName;
        return this;
    }

    /**
     * Rasterizes each geometry individually using brute-force containment checks on every pixel.
     *
     * @deprecated  This method performs extremely poorly on large datasets due to its
     * pixel-wise spatial checks using {@link Geometry#intersects}, which scale linearly with geometry count
     * and pixel resolution.
     *
     * @return A list of individual {@link RasterResult}s, one per geometry.
     * @throws IOException if reading KML data or performing coordinate transformation fails.
     */
    @Deprecated(since = "1.0.0")
    public List<RasterResult> rasterizedGeometries() throws IOException, FactoryException {

        // Read all KML features
        try(KMLFeatureReader reader = new KMLFeatureReader(this.kmlFile, this.parsingElement)) {

            List<RasterResult> result = new ArrayList<>();

            // Write each feature into geojson file
            while(reader.hasNext()) {
                SimpleFeature feature = reader.next();

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null) continue;

                MathTransform transform = this.projection.get();

                Geometry projected = JTS.transform(geometry, transform);

                result.add(this.rasterizeToBlockData(projected, PreparedGeometry::intersects));
            }

            return result;
        }
        catch (IOException ex) { throw new IOException("Failed to read KML data to process", ex); }
        catch (TransformException ex) {throw new IOException(ex);}
    }

    /**
     * Brute-force rasterization of all geometries into a single {@link GridCoverage2D}.
     *
     * @deprecated  This method performs very poorly with many features or high-resolution grids.
     * All features are transformed, stored, and rasterized in memory without spatial optimization.
     *
     * @param listener Optional progress listener.
     * @return The combined raster coverage as {@link GridCoverage2D}.
     * @throws IOException if reading or transforming geometry fails.
     */
    @Deprecated(since = "1.0.0")
    public GridCoverage2D rasterizedGrid2D(@Nullable ProgressListener listener) throws IOException {

        // Read all KML features
        try(KMLFeatureReader reader = new KMLFeatureReader(this.kmlFile, this.parsingElement)) {

            List<SimpleFeature> result = new ArrayList<>();

            DefaultFeatureCollection features = new DefaultFeatureCollection(UUID.randomUUID().toString());

            // Write each feature into geojson file
            while(reader.hasNext()) result.add(reader.next());

            features.addAll(result);

            return rasterizeToGrid2D(features, listener);
        }
        catch (IOException ex) { throw new IOException("Failed to read KML data to process", ex); }
        catch (TransformException ex) {throw new IOException(ex);}
    }

    /**
     * Simple minecraft schematic result.
     *
     * @deprecated Use world-edit api to write NBT data instead
     *
     * @param blockData The byte array of all block data in the region
     * @param width Region width
     * @param length Region Length
     * @param offsetX Geographic offset X
     * @param offsetZ Geographic offset Y
     */
    @Deprecated(since = "1.0.0")
    public record RasterResult(byte[] blockData, int width, int length, int offsetX, int offsetZ) {
        @Override
        @Contract(pure = true)
        public @NotNull String toString() {
            return "RasterResult{blockData=[" + String.join(", ",  String.valueOf(blockData[0]),  String.valueOf(blockData[1]),  String.valueOf(blockData[2])) +
                    ", ...], width=" + width + ", length=" + length +
                    ", offsetX=" + offsetX + ", offsetZ=" + offsetZ + '}';
        }
    }

    /**
     * Use {@link VectorToRasterProcess} to rasterize a collection of features into {@linkplain GridCoverage2D}.
     *
     * @param collection The feature collections to rasterize
     * @param listener Progress listener, can be {@code null}
     * @return The rasterized grid coverage
     * @throws TransformException If the process failed to transform coordinates data
     */
    protected GridCoverage2D rasterizeToGrid2D(@NotNull SimpleFeatureCollection collection, @Nullable ProgressListener listener) throws TransformException {
        ReferencedEnvelope boundary = collection.getBounds();

        return VectorToRasterProcess.process(
                collection,
                "geometry",
                new Dimension((int) boundary.getWidth(), (int) boundary.getHeight()),
                boundary.toBounds(MinecraftCRS.BTE),
                "Rasterize Process",
                listener
        );
    }

    /**
     * Rasterize a {@linkplain Geometry} into byte array data.
     *
     * <p>This bruteforce a predicate {@code method} to check for all hit byte in a grid.</p>
     *
     * @param geometry The geometry to rasterize
     * @param method The predicate in which will be used to define if a point is hit or miss
     * @return A raster result containing the block data as a byte array and its boundary data
     */
    @NotNull
    protected RasterResult rasterizeToBlockData(@NotNull Geometry geometry, @NotNull BiPredicate<PreparedGeometry, Geometry> method) {
        Envelope env = geometry.getEnvelopeInternal();

        int minX = (int) Math.floor(env.getMinX());
        int minZ = (int) Math.floor(env.getMinY());
        int width = (int) Math.ceil(env.getMaxX()) - minX;
        int length = (int) Math.ceil(env.getMaxY()) - minZ;

        byte[] data = new byte[width * length]; // 1 layer height, row-major

        // For containment test, we need a prepared geometry
        PreparedGeometry prepared = PreparedGeometryFactory.prepare(geometry);

        // Loop through each meter-sized pixel (each block in X/Z)
        for (int dz = 0; dz < length; dz++) {
            for (int dx = 0; dx < width; dx++) {
                double worldX = dx + minX + 0.5;
                double worldZ = dz + minZ + 0.5;

                if (method.test(prepared, new GeometryFactory().createPoint(new Coordinate(worldX, worldZ))))
                    data[dz * width + dx] = 1; // hit
                else data[dz * width + dx] = 0; // miss
            }
        }

        return new RasterResult(data, width, length, minX, minZ);
    }

}
