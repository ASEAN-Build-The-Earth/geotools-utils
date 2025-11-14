package asia.buildtheearth.asean.geotools.test;

import asia.buildtheearth.asean.geotools.*;
import asia.buildtheearth.asean.geotools.kml.store.KMLFeatureReader;
import asia.buildtheearth.asean.geotools.projection.MinecraftProjection;
import asia.buildtheearth.asean.geotools.test.utils.CoordinatesTraverser;
import io.hosuaby.inject.resources.junit.jupiter.GivenBinaryResource;
import io.hosuaby.inject.resources.junit.jupiter.TestWithResources;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.kml.KML;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test KML file for GeoJSON conversion.
 */
@TestWithResources
@DisplayName("Test from KML")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class TestFromKML extends AbstractTestFromKML {
    @GivenBinaryResource("test1.kml") public static byte[] test1;
    @GivenBinaryResource("test2.kml") public static byte[] test2;
    @GivenBinaryResource("test3.kml") public static byte[] test3;

    @Test @Order(1)
    @DisplayName("Convert to GeoJSON")
    public void toGeoJSON() {

        GeoToolsConverter converter = switch (test.type) {
            case XYZ, Z -> ToGeoJSON.fromKML(source.toFile());
            case XY -> ToGeoJSON.fromKML(source.toFile()).dropZ();
        };

        CoordinatesTraverser traverser = switch (test.type) {
            case XYZ, Z -> new CoordinatesTraverser(this::validate3DArray, this::putTestGeometry);
            case XY -> new CoordinatesTraverser(this::validate2DArray, this::putTestGeometry);
        };

        Assertions.assertDoesNotThrow(() -> converter.convert(output));

        String json =  Assertions.assertDoesNotThrow(() -> Files.readString(output));

        traverser.traverse(json);
    }

    @Test @Order(2)
    @DisplayName("All geometry coordinates are valid")
    public void coordinatesIsValid() {
        AbstractTestFromKML.original.values().forEach(geometry -> {
            for (Coordinate coordinate : geometry.getCoordinates()) {
                Assertions.assertFalse(Double.isNaN(coordinate.getX()));
                Assertions.assertFalse(Double.isNaN(coordinate.getY()));
                switch (test.type) {
                    case XYZ, Z -> Assertions.assertFalse(Double.isNaN(coordinate.getZ()));
                    case XY -> Assertions.assertTrue(Double.isNaN(coordinate.getZ()));
                }
            }
        });
    }


    @Test @Order(3)
    @DisplayName("Convert to BlueMap Marker(s)")
    public void toBlueMapMarker() {
        Path markers = directory.resolve("markerSet.json");

        ToBlueMapMarker converter = new ToBlueMapMarker.FromGeoJSON(output.toFile());
        
        Assertions.assertDoesNotThrow(() -> {
            converter.convert(markers);

            System.out.print(Files.readString(markers));
        });

    }

    @Test @Order(4)
    @DisplayName("Convert back to KML")
    public void toKML() {

        Path converted = directory.resolve("converted.kml");
        GeoToolsConverter converter = switch (test.type) {
            case XYZ, XY -> ToKML.fromGeoJSON(output.toFile());
            case Z -> ToKML.fromGeoJSON(output.toFile()).setOffsetZ(AbstractTestFromKML.randomOffset);
        };

        Assertions.assertDoesNotThrow(() -> converter.convert(converted));

        try(KMLFeatureReader reader = new KMLFeatureReader(converted.toFile(), KML.Placemark)) {
            // Write each feature into geojson file
            while(reader.hasNext()) {
                SimpleFeature feature = reader.next();

                for(Property props : feature.getValue()) {
                    if(props == null) continue;
                    if(props.getValue() instanceof Geometry geo) {
                        Assertions.assertNotNull(feature.getIdentifier());
                        Assertions.assertTrue(feature.getIdentifier().getID().startsWith(TEST_FILE_NAME));
                        Assertions.assertNull(AbstractTestFromKML.finalGeo.put(feature.getIdentifier().getID(), geo));
                    }
                }
            }
        }
        catch (IOException ex) { Assertions.fail("Failed to write KML data to GeoJSONWriter", ex); }
    }

    @Test @Order(5)
    @DisplayName("Converted geometry match the original")
    public void matchTestGeometry() {
        // Topology equality check
        Assertions.assertEquals(AbstractTestFromKML.original, AbstractTestFromKML.finalGeo);

        // Coordinates check: expecting all converted coordinates in the geometries to be equal
        AbstractTestFromKML.finalGeo.forEach((key, actualGeo) -> {

            Geometry expectedGeo = AbstractTestFromKML.original.get(key);

            for (int i = 0; i < actualGeo.getCoordinates().length; i++) {
                Coordinate expected = expectedGeo.getCoordinates()[i];
                Coordinate actual = actualGeo.getCoordinates()[i];

                switch (test.type) {
                    case XYZ -> {
                        Assertions.assertTrue(expected.equals3D(actual));
                    }
                    case XY -> {
                        Assertions.assertTrue(expected.equals3D(actual));
                        Assertions.assertTrue(Double.isNaN(expected.z));
                        Assertions.assertTrue(Double.isNaN(actual.z));
                    }
                    case Z -> {
                        Assertions.assertFalse(expected.equals3D(actual));
                        Assertions.assertEquals(0, expected.getZ());
                        Assertions.assertEquals(AbstractTestFromKML.randomOffset, actual.getZ());
                    }
                }
            }
        });
    }
}
