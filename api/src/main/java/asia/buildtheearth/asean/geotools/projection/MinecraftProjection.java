package asia.buildtheearth.asean.geotools.projection;

import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.dymaxion.BTEDymaxionProjection;
import net.buildtheearth.terraminusminus.projection.transform.FlipVerticalProjectionTransform;
import net.buildtheearth.terraminusminus.projection.transform.OffsetProjectionTransform;
import net.buildtheearth.terraminusminus.projection.transform.ScaleProjectionTransform;
import org.geotools.api.parameter.ParameterDescriptor;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.MathTransformFactory;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

import static asia.buildtheearth.asean.geotools.projection.DymaxionMapProjection.Provider.*;

/**
 * Central registry for all BuildTheEarth-compatible projections.
 *
 * <h2>Projection Constants</h2>
 * <ul>
 *   <li>{@link #getBTE()} – Default BuildTheEarth projection centered globally.</li>
 *   <li>{@link #getASEAN()} – Regionally offset projection centered on Southeast Asia.</li>
 *   <li>{@link #getWithOffset(double, double)} – Factory for offset-adjusted Terra projections.</li>
 * </ul>
 *
 * <p>
 * All projections operate at a scale of {@value #EARTH_TO_MINECRAFT_SCALE}, which converts real-world meters to Minecraft blocks.
 * </p>
 */
public abstract class MinecraftProjection {

    private MinecraftProjection() {}

    /**
     * The global conversion factor from real-world meters to Minecraft blocks.
     * <p>
     * This scale factor represents the ratio used by BuildTheEarth to map Earth-sized
     * coordinates onto Minecraft’s grid system, where 1 unit = 1 block = 1 meter.
     * </p>
     */
    public static final double EARTH_TO_MINECRAFT_SCALE = 7318261.522857145;

    public static DymaxionMapProjection getBTE() throws FactoryException {
        return get("bte_conformal_dymaxion", 0, 0);
    }

    public static DymaxionMapProjection getASEAN() throws FactoryException {
        return get("bte_conformal_dymaxion", -13379008d, 2727648d);
    }

    public static DymaxionMapProjection getWithOffset(double offsetX, double offsetY) throws FactoryException {
        return get("bte_conformal_dymaxion", offsetX, offsetY);
    }

    public static DymaxionMapProjection get(String projection, double offsetX, double offsetY) throws FactoryException {
        MathTransformFactory mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = mtFactory.getDefaultParameters("dymaxion");
        set(parameters, PROJECTION, projection);
        set(parameters, SEMI_MAJOR, 1.0d);
        set(parameters, SEMI_MINOR, 1.0d);
        set(parameters, FALSE_EASTING, offsetX);
        set(parameters, FALSE_NORTHING, offsetY);
        set(parameters, SCALE_FACTOR, EARTH_TO_MINECRAFT_SCALE);
        return (DymaxionMapProjection) mtFactory.createParameterizedTransform(parameters);
    }

    private static <T> void set(ParameterValueGroup group, ParameterDescriptor<T> descriptor, T value) {
        group.parameter(descriptor.getName().getCode()).setValue(value);
    }

    /**
     * The default projection used by BuildTheEarth for global rendering.
     */
    public static final TerraProjection DEFAULT_BTE_PROJECTION = new MinecraftTerraProjection();

    /**
     * A regional projection customized for Southeast Asia (ASEAN).
     * <p>
     * This projection instance applies an offset to better align the
     * Dynmaxion projection center over the ASEAN region.
     * </p>
     */
    public static final TerraProjection ASEAN_BTE_PROJECTION = new MinecraftTerraProjection(-13379008, 2727648d);

    /**
     * A factory for creating new {@link TerraProjection} instances with custom offsets.
     */
    public static final OffsetTerraProjection CUSTOM_BTE_PROJECTION = MinecraftTerraProjection::new;

    /**
     * A factory for creating raw, un-configured {@link TerraProjection} instances.
     */
    public static final CustomProjection CUSTOM_PROJECTION = TerraProjection::new;

    /**
     * Create a custom terra projection with a defined {@linkplain GeographicProjection}
     */
    @FunctionalInterface
    public interface CustomProjection extends Function<GeographicProjection, TerraProjection> {
        /**
         * Construct a new {@link TerraProjection} with a custom projection axis.
         *
         * @param projection Custom defined projection.
         * @return New terra projection with the defined projector.
         */
        @Override
        TerraProjection apply(@NotNull GeographicProjection projection);
    }

    /**
     * Provide a default BuildTheEarth terra projection with a custom offset axis.
     */
    @FunctionalInterface
    public interface OffsetTerraProjection extends BiFunction<Double, Double, TerraProjection> {
        /**
         * Construct a new {@link TerraProjection} with a custom offset axis.
         *
         * @param offsetX Linear offset to be applied on the X axis.
         * @param offsetY Linear offset to be applied on the Y axis.
         * @return New Minecraft-Terra projection with the applied offset.
         */
        @Override
        TerraProjection apply(@NotNull Double offsetX, @NotNull Double offsetY);
    }

    private static class MinecraftTerraProjection extends TerraProjection {

        private static GeographicProjection projection = new BTEDymaxionProjection();

        static {
            // Default delegation: Flip the projection and scale to minecraft block unit
            projection = new FlipVerticalProjectionTransform(projection);
            projection = new ScaleProjectionTransform(projection, EARTH_TO_MINECRAFT_SCALE, EARTH_TO_MINECRAFT_SCALE);
        }

        /**
         * Construct a new projection from BuildTheEarth project defined projection.
         */
        private MinecraftTerraProjection() {
            super(projection);
        }


        /**
         * Construct a new projection from BuildTheEarth project defined projection
         * with a linear offset on plane axis.
         *
         * <p>Implementation: </p>
         * <blockquote>{@snippet :
         * class OffsetProjectionTransform implements GeographicProjection {
         *     double dx, dy;
         *
         *     double[] toGeo(double x, double y) {
         *         return new double[]{ x - this.dx, y - this.dy };
         *     }
         *
         *     double[] fromGeo(double longitude, double latitude) {
         *         double[] pos = new double[]{ longitude, latitude };
         *         pos[0] += this.dx;
         *         pos[1] += this.dy;
         *         return pos;
         *     }
         * }}</blockquote>
         *
         * @param offsetX Linear offset to be applied on the X axis.
         * @param offsetY Linear offset to be applied on the Y axis.
         */
        private MinecraftTerraProjection(double offsetX, double offsetY) {
            super(projection = new OffsetProjectionTransform(projection, offsetX, offsetY));
        }
    }
}
