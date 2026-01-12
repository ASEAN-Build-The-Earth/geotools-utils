package asia.buildtheearth.asean.geotools.projection;

import asia.buildtheearth.asean.geotools.projection.metadata.Citations;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.buildtheearth.terraminusminus.TerraConstants;
import net.buildtheearth.terraminusminus.config.GlobalParseRegistries;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.projection.dymaxion.DymaxionProjection;
import org.geotools.api.parameter.*;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.Point2D;

import static asia.buildtheearth.asean.geotools.projection.MinecraftProjection.EARTH_TO_MINECRAFT_SCALE;

public class DymaxionMapProjection extends MapProjection {
    private final DymaxionProjection projection;

    protected DymaxionMapProjection(ParameterValueGroup values) throws ParameterNotFoundException, FactoryException {
        super(values);

        ParameterValue<?> value = values.parameter(Provider.PROJECTION.getName().getCode());

        try {
            // Upstream library uses JSON based mapping for querying instance.
            // It'll look better on t++ v2.0 when we integrate with open-gis
            GeographicProjection projection = TerraConstants.JSON_MAPPER
                .readValue("{ \"" + value.stringValue() + "\": {} }", GeographicProjection.class);

            if(projection instanceof DymaxionProjection dymaxion) {
                this.projection = dymaxion;
            }
            else throw new FactoryException("Trying to use "
                + projection.getClass().getName()
                + " but this map projection only support dymaxion map projection."
            );
        }
        catch (IllegalArgumentException | JsonProcessingException ex) {
            throw new FactoryException(ex);
        }
    }

    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return DymaxionMapProjection.Provider.PARAMETERS;
    }

    @Override
    public ParameterValueGroup getParameterValues() {
        ParameterValueGroup values = super.getParameterValues();

        String projection = GlobalParseRegistries.PROJECTIONS.inverse().get(this.projection.getClass());

        values.parameter(Provider.PROJECTION.getName().getCode()).setValue(projection);

        return values;
    }

    @Override
    protected Point2D inverseTransformNormalized(double x, double y, Point2D ptDst) throws ProjectionException {
        try {
            double[] inverse = this.projection.toGeo(x, -y);
            double lambda = inverse[0];
            double phi = Math.PI / 2 - inverse[1];

            if (ptDst != null) {
                ptDst.setLocation(lambda, phi);
                return ptDst;
            }
            return new Point2D.Double(lambda, phi);

        } catch (OutOfProjectionBoundsException ex) {
            throw new ProjectionException("Out of bound exception in inverse transformation", ex);
        }
    }

    @Override
    protected Point2D transformNormalized(double lambda, double phi, Point2D ptDst) {
        // NOTE: this does not throw out-of-bound exception as the input here is normalized
        //       and is checked beforehand by MapProjection superclass
        double[] transform = this.projection.fromGeoNormalized(lambda, (Math.PI / 2) - phi);

        double x = transform[0];
        double z = -transform[1];

        if (ptDst != null) {
            ptDst.setLocation(x, z);
            return ptDst;
        }
        return new Point2D.Double(x, z);
    }

    public static class Provider extends AbstractProvider {
        /**
         * Projection codename as specified by upstream registries.
         * @see net.buildtheearth.terraminusminus.config.GlobalParseRegistries
         */
        public static final String CITATION_CODE = "bte_conformal_dymaxion";

        public static final ParameterDescriptor<String> PROJECTION;

        static final ParameterDescriptorGroup PARAMETERS;

        public Provider() {
            super(PARAMETERS);
        }

        @Override
        protected MathTransform createMathTransform(ParameterValueGroup parameters) throws ParameterNotFoundException, InvalidParameterValueException {

            Provider.assertParameter(parameters, SEMI_MINOR, 1.0,
                "This projection operates in a normalized unit circle."
            , false);

            Provider.assertParameter(parameters, SEMI_MAJOR, 1.0,
                "This projection operates in a normalized unit circle."
            , false);

            Provider.assertParameter(parameters, SCALE_FACTOR, EARTH_TO_MINECRAFT_SCALE,
            """
               
               This projection has a validated constant of Earth-to-Minecraft scale.
               Using a different value may produce untested behavior.
               """
            , true);

            try { return new DymaxionMapProjection(parameters); }
            catch (FactoryException ex) {
                String invalidProjection = PROJECTION.getName().getCode();
                ParameterValue<?> actual = parameters.parameter(invalidProjection);
                throw new InvalidParameterValueException(
                    "No mapping found for the specify projection.",
                    invalidProjection,
                    actual.stringValue()
                );
            }
        }

        static <T> void assertParameter(@NotNull ParameterValueGroup group,
                                        @NotNull ParameterDescriptor<T> descriptor,
                                        @NotNull T assertion,
                                        @NotNull String hint,
                                        boolean warning) throws InvalidParameterValueException {
            ParameterValue<?> parameter = group.parameter(descriptor.getName().getCode());
            T value = descriptor.getValueClass().cast(parameter.getValue());

            if(!assertion.equals(value)) {
                String message = (warning? "Unexpected value for parameter '" : "Invalid value for parameter '")
                    + descriptor.getName().getCode() + "'. "
                    + "Expected: " + assertion
                    + ", but got: " + value + ". " + hint;

                if(warning) LOGGER.warning(message);
                else throw new InvalidParameterValueException(message, descriptor.getName().getCode(), value);
            }
        }

        static {
            PROJECTION = DefaultParameterDescriptor.create("projection", "projection", String.class, CITATION_CODE, true);
            PARAMETERS = createDescriptorGroup(new NamedIdentifier[] {
                    new NamedIdentifier(org.geotools.metadata.iso.citation.Citations.GEOTOOLS, "Dymaxion"),
                    new NamedIdentifier(Citations.BTE, "dymaxion")
                },
                new ParameterDescriptor[] {
                    PROJECTION, // Identifier to find the projection instance for dymaxion
                    SEMI_MAJOR, // Must be 1.0 as we do calculations on unit circle
                    SEMI_MINOR, // Must be 1.0 as we do calculations on unit circle
                    SCALE_FACTOR, // Mandatory for Minecraft-To-Earth scale
                    FALSE_EASTING,  // We can use this for X offset, Default to 0.
                    FALSE_NORTHING, // We can use this for Y offset, Default to 0.
                }
            );
        }
    }
}
