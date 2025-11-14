package asia.buildtheearth.asean.geotools.test;

import org.geotools.api.filter.identity.FeatureId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import org.locationtech.jts.geom.Geometry;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static asia.buildtheearth.asean.geotools.test.GeoToolsConverterIT.KML;

@ParameterizedClass(name = "{arguments}")
@EnumSource(GeoToolsConverterIT.KML.Test.class)
abstract sealed class AbstractConverterIT permits CLIConverterIT {

    protected static final java.util.Random random = new java.util.Random(System.nanoTime());
    protected static final String TEST_FILE_NAME = "output.";
    protected static final String SOURCE_FILE_NAME = "source.";

    @Parameter
    protected KML.Test test;

    protected static Path directory;
    protected static Path source;
    protected static Path output;
    protected static Map<String, Geometry> original = new HashMap<>();
    protected static Map<String, Geometry> finalGeo = new HashMap<>();
    protected static Double randomOffset = null;

    @BeforeParameterizedClassInvocation
    static void beforeInvocation(@NotNull KML.Test test, @TempDir @NotNull Path tempDir) {
        AbstractConverterIT.source = tempDir.resolve(SOURCE_FILE_NAME + "kml");
        AbstractConverterIT.output = tempDir.resolve(TEST_FILE_NAME + "geojson");
        AbstractConverterIT.directory = tempDir;

        if(test.type == KML.TestType.Z) randomOffset = random.nextDouble();

        try(FileOutputStream file = new FileOutputStream(source.toFile())) {
            file.write(((GeoToolsConverterIT.TestResource) test).get());
        }
        catch (IOException ex) {
            Assertions.fail("Failed to write test resource to temp directory", ex);
        }
    }

    @AfterParameterizedClassInvocation
    static void afterInvocation() {
        AbstractConverterIT.original.clear();
        AbstractConverterIT.finalGeo.clear();
        AbstractConverterIT.randomOffset = null;
    }

    protected void validateArray(@NotNull JSONArray array, int dimension) {
        Assertions.assertEquals(dimension, array.size(),
                "Expected GeoJSON coordinates array in %,dD dimension of length %,d, ".formatted(dimension, dimension) +
                        "but received data of size " + array.size()
        );
    }

    protected void validate2DArray(@NotNull JSONArray array) {
        this.validateArray(array, 2);
    }

    protected void validate3DArray(@NotNull JSONArray array) {
        this.validateArray(array, 3);
    }

    protected void putTestGeometry(@NotNull Geometry geometry, @Nullable FeatureId identifier) {
        Assertions.assertNotNull(identifier, "Test geometry is expected to have an identifier");
        Assertions.assertNull(AbstractConverterIT.original.put(TEST_FILE_NAME + identifier.getID(), geometry));
    }
}
