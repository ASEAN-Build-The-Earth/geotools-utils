package asia.buildtheearth.asean.geotools;

import asia.buildtheearth.asean.geotools.bluemap.BlueMapMarkerWriter;
import asia.buildtheearth.asean.geotools.projection.MinecraftProjection;
import asia.buildtheearth.asean.geotools.projection.TerraProjection;
import de.bluecolored.bluemap.api.markers.*;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.store.GeoJSONDataStore;
import org.geotools.data.geojson.store.GeoJSONFeatureReader;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentState;
import org.geotools.geometry.jts.JTS;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.*;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Supplier;

public abstract sealed class ToBlueMapMarker extends AbstractGeoToolsConverter {
    private ToBlueMapMarker(File source) { super(source); }

    protected Supplier<@NotNull TerraProjection> projection = () -> MinecraftProjection.DEFAULT_BTE_PROJECTION;
    protected BlueMapMarkerWriter writer = new BlueMapMarkerWriter();

    protected String makerLabel = null;

    protected Integer sortingPriority = null;

    protected Boolean toggleable, defaultHidden;

    /** {@inheritDoc} */
    @Override
    public abstract void convert(Path output) throws IOException;

    @Override
    public ToBlueMapMarker disablePrettyPrint() {
        this.writer.useCompactWriter();
        return (ToBlueMapMarker) super.disablePrettyPrint();
    }

    @Contract("_ -> new")
    public static @NotNull ToBlueMapMarker fromGeoJSON(File geojsonFile) {
        return new ToBlueMapMarker.FromGeoJSON(geojsonFile);
    }

    /**
     * Set a custom projection for this converter.
     *
     * @param projection New projection to set
     * @return This instance for chaining
     */
    public ToBlueMapMarker setProjection(@NotNull TerraProjection projection) {
        this.projection = () -> projection;
        return this;
    }

    public ToBlueMapMarker setMakerLabel(String makerLabel) {
        this.makerLabel = makerLabel;
        return this;
    }

    public ToBlueMapMarker normalizeNaming() {
        this.writer.normalizeNaming();
        return this;
    }

    public ToBlueMapMarker normalizeNaming(boolean normalize) {
        this.writer.normalizeNaming(normalize);
        return this;
    }

    public ToBlueMapMarker averageAllElevation() {
        this.writer.averageAllElevation();
        return this;
    }

    public ToBlueMapMarker normalizeAllElevation(long elevation) {
        this.writer.normalizeAllElevation(elevation);
        return this;
    }

    public ToBlueMapMarker extrudeIfPolygon(long length) {
        this.writer.extrudeIfPolygon(length);
        return this;
    }

    public ToBlueMapMarker setDefaultHidden(Boolean defaultHidden) {
        this.defaultHidden = defaultHidden;
        return this;
    }

    public ToBlueMapMarker setSortingPriority(Integer sortingPriority) {
        this.sortingPriority = sortingPriority;
        return this;
    }

    public ToBlueMapMarker setToggleable(Boolean toggleable) {
        this.toggleable = toggleable;
        return this;
    }

    public static final class FromGeoJSON extends ToBlueMapMarker {

        public FromGeoJSON(File source) {
            super(source);
        }

        @Override
        public void convert(Path output) throws IOException {
            // Create a marker writer
            MarkerSet.Builder markerSet = MarkerSet.builder();

            if (makerLabel != null) markerSet.label(makerLabel);
            else markerSet.label(this.stripExt(output.getFileName()));

            if (toggleable != null) markerSet.toggleable(toggleable);
            if (defaultHidden != null) markerSet.defaultHidden(defaultHidden);
            if (sortingPriority != null) markerSet.sorting(sortingPriority);

            MarkerSet marker = markerSet.build();
            GeoJSONDataStore store = new GeoJSONDataStore(this.sourceFile);
            ContentState content = new ContentState(new ContentEntry(store, store.getTypeName()));

            // Read all GeoJSON features
            try (GeoJSONFeatureReader reader = new GeoJSONFeatureReader(content, Query.ALL)) {

                // Write each feature into kml file
                int count = 0;
                while(reader.hasNext()) {
                    SimpleFeature feature = reader.next();

                    Object geometryAttribute = feature.getAttribute(GeoJSONReader.GEOMETRY_NAME);
                    if (geometryAttribute instanceof Geometry geometry) {
                        if (this.hasCoordinatesModifier()) {
                            this.applyAllCoordinates(geometry.getCoordinates());
                            geometry.geometryChanged();
                        }

                        MathTransform transform = this.projection.get();

                        Geometry projected = JTS.transform(geometry, transform);

                        this.writer.writeGeometry(count++, projected, feature, marker::put);
                    }
                }

            } catch (IOException ex) {
                throw new IOException("Failed to write Geometry data to KMLWriter", ex);
            } catch (TransformException ex) {
                throw new IOException("Failed to transform the geometry into minecraft projection", ex);
            }

            // Finally, write the marker to JSON output file
            this.writer.export(marker, output);
        }

        private @NotNull String stripExt(@NotNull Path base) {
            return this.stripExt(base.toString());
        }

        @Contract(pure = true)
        private @NotNull String stripExt(@NotNull String base) {
            return base.replaceAll("\\.[^.]+$", "");
        }
    }
}
