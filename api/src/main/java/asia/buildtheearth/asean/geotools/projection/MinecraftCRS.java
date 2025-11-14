package asia.buildtheearth.asean.geotools.projection;

import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;

/**
 * Provides pre-defined {@link ProjectedCRS} instances for use within the Build The Earth project.
 * <p>
 * These CRS definitions are based on popular projection configurations used throughout the project,
 * particularly modified Dymaxion-based projections optimized for Minecraft scale and structure alignment.
 * <p>
 * The coordinate transformations are provided
 * via the {@link MinecraftProjection} class, which exposes statically available or offset-adjustable
 * {@link TerraProjection} implementations.
 *
 * @see MinecraftProjection
 * @see TerraProjection
 * @see net.buildtheearth.terraminusminus.projection.dymaxion.BTEDymaxionProjection
 */
public abstract class MinecraftCRS {
    private MinecraftCRS() {}

    /**
     * CRS identifier for the default Build The Earth global projection using a Dymaxion-based layout.
     *
     * @see #BTE
     */
    public static final String AIROCEAN_BTE = "airocean:bte";

    /**
     * CRS identifier for the ASEAN Build The Earth regional projection, adjusted with local offsets.
     *
     * @see #ASEAN_BTE
     */
    public static final String AIROCEAN_ASEAN_BTE = "airocean:aseanbte";

    /**
     * {@value #AIROCEAN_BTE} A projected coordinate reference system for the default global
     * {@linkplain net.buildtheearth.terraminusminus.projection.dymaxion.BTEDymaxionProjection Dymaxion-based}
     * projection used in the Build The Earth project.
     * <p>
     * This CRS maps geographic WGS84 coordinates into a flat Minecraft-oriented coordinate system
     * using the {@link TerraProjection} transform logic.
     *
     * @see MinecraftProjection#DEFAULT_BTE_PROJECTION
     */
    public static final ProjectedCRS BTE;

    /**
     * {@value #AIROCEAN_ASEAN_BTE} A projected coordinate reference system for the ASEAN region, used by
     * the Build The Earth ASEAN team to better align local builds to Minecraft’s block grid.
     * <p>
     * This CRS uses an offset variant of the global projection, accessible via
     * {@link TerraProjection}.
     *
     * @see MinecraftProjection#ASEAN_BTE_PROJECTION
     */
    public static final ProjectedCRS ASEAN_BTE;

    static {
        BTE = new DefaultProjectedCRS(
            AIROCEAN_BTE,
            DefaultGeographicCRS.WGS84,
            MinecraftProjection.DEFAULT_BTE_PROJECTION,
            DefaultCartesianCS.GENERIC_2D
        );

        ASEAN_BTE = new DefaultProjectedCRS(
            AIROCEAN_ASEAN_BTE,
            DefaultGeographicCRS.WGS84,
            MinecraftProjection.ASEAN_BTE_PROJECTION,
            DefaultCartesianCS.GENERIC_2D
        );
    }
}
