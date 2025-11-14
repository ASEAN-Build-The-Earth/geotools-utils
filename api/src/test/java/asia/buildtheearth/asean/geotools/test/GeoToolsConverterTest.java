package asia.buildtheearth.asean.geotools.test;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Resource manager for GeoTools conversion tests.
 */
public class GeoToolsConverterTest {

    /**
     * Resource loader for each test.
     */
    public interface TestResource {
        byte[] get();
    }

    /**
     * KML-based test resources and test variants.
     *
     * @see TestFromKML For raw resource declarations used in these tests.
     */
    public static class KML {
        /**
         * Types of test variations applied to a base KML resource.
         */
        public enum TestType {
            /** Initial test using original 3D geometry (X, Y, Z). */
            XYZ,
            /** Drops Z-coordinate (converts to X, Y, NaN). */
            XY,
            /** Offsets the Z-coordinate by a fixed value (X, Y, Z + offset). */
            Z
        }

        /**
         * Defines each KML test case and its suppose type variant.
         */
        public enum Test implements TestResource {
            XYZ_1(() -> TestFromKML.test1, 1), XY_1(XYZ_1), Z_1(XY_1),
            XYZ_2(() -> TestFromKML.test2, 2), XY_2(XYZ_2), Z_2(XY_2),
            XYZ_3(() -> TestFromKML.test3, 3), XY_3(XYZ_3), Z_3(XY_3);

            public final TestType type;
            private final Supplier<byte[]> source;
            private final int testID;

            @Contract(pure = true)
            Test(@NotNull Supplier<byte[]> source, int testID) {
                this.source = source;
                this.testID = testID;
                this.type = TestType.XYZ;
            }

            @Contract(pure = true)
            Test(@NotNull GeoToolsConverterTest.KML.Test source) {
                this.source = source.source;
                this.testID = source.testID;
                this.type = source.type == TestType.XYZ? TestType.XY : TestType.Z;
            }

            public byte[] get() {
                return this.source.get();
            }

            @Override
            @Contract(pure = true)
            public @NotNull String toString() {
                return this.testID + " → [" + this.type + "]";
            }
        }
    }
}