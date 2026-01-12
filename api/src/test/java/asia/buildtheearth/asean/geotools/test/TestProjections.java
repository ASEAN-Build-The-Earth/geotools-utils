package asia.buildtheearth.asean.geotools.test;

import asia.buildtheearth.asean.geotools.projection.MinecraftProjection;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.api.referencing.operation.TransformException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;


public class TestProjections {

    /** Sets of geographic coordinates to project. */
    private static final double[] GEOGRAPHIC = {
        2.350987d, 48.856667d,
        -74.005974d, 40.714268d,
        -0.166670d, 51.500000d,
        116.397230d, 39.907500d,
        -122.332070, 47.606210d,
        151.208666d, -33.875113d,
        2.295026d, 48.87378100000001d,
        2.236214, 48.8926507,
        2.349270d, 48.853474d,
        2.348969d, 48.853065d
    };

    /** Set of projected coordinates. */
    private static final double[] PROJECTED = {
        2851660.278582057, -5049718.243628887,
        -8526456.75523275, -6021812.714103152,
        2774758.1546624764, -5411708.236500686,
        11571988.173618957, -6472387.375809908,
        -12410431.110669583, -6894851.702710003,
        20001061.636216827, -2223355.8371363534,
        2848192.3338641203, -5053053.018157968,
        2844585.5271490104, -5056657.959395678,
        2851410.680220599, -5049403.7778784195,
        2851372.726732094, -5049365.549214174
    };


    @Test
    void testBTEDymaxion() throws TransformException {
        MapProjection map = Assertions.assertDoesNotThrow(MinecraftProjection::getBTE);

        System.out.println(map.toWKT());

        for (int i = 0; i < GEOGRAPHIC.length / 2; i++) {
            double lon = GEOGRAPHIC[i * 2];
            double lat = GEOGRAPHIC[i*2 + 1];
            double x = PROJECTED[i * 2];
            double z = PROJECTED[i * 2 + 1];
            Point2D lola = map.inverse().transform(new Point2D.Double(x, z), null);
            Point2D xz = map.transform(new Point2D.Double(lon, lat), null);

            Assertions.assertEquals(lon, lola.getX(), .1d);
            Assertions.assertEquals(lat, lola.getY(), .1d);
            Assertions.assertEquals(x, xz.getX(), .1d);
            Assertions.assertEquals(z, xz.getY(), .1d);
        }
    }
}
