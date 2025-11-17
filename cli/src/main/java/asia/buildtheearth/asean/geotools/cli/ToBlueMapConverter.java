package asia.buildtheearth.asean.geotools.cli;

import asia.buildtheearth.asean.geotools.ToBlueMapMarker;
import asia.buildtheearth.asean.geotools.ToGeoJSON;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "bluemap", version = "1.0.0",
        description = "Converts a geospatial file to BlueMap Marker file.",
        mixinStandardHelpOptions = true, sortOptions = false)
public class ToBlueMapConverter extends AbstractConverter {

    /**
     * The (optional) elevation modification options.
     */
    @CommandLine.ArgGroup
    private Elevation elevation;

    /**
     * Make the output file compact
     */
    @CommandLine.Option(
            names = {"-c", "--compact"},
            paramLabel = "<boolean>",
            description = "Disables pretty-printing for the output file.")
    private boolean compact;

    @Override
    protected Conversion.Format getFormat() {
        return Conversion.Format.geojson;
    }

    @Override
    public Integer call() throws RuntimeException {
        super.call();

        // Figure out converter
        ToBlueMapMarker converter = switch (this.format) {
            case geojson -> ToBlueMapMarker.fromGeoJSON(this.input);
            case kml -> throw new RuntimeException("Not Supported yet.");
            case bluemap -> throw new RuntimeException("Not Supported yet.");
            case null -> throw new RuntimeException(
                    "Conversion format return null, this should not happen."
            );
        };

        if(compact) converter.disablePrettyPrint();

        // Elevation modification for Z axis
        if(elevation != null) {
            if(elevation.dropZ) converter.dropZ();
            if(elevation.normalizeZ != null) converter.normalizeZ(elevation.normalizeZ);
            if(elevation.offsetZ != null) converter.setOffsetZ(elevation.offsetZ);
        }

        // Convert for output
        try { converter.convert(this.output); }
        catch (IOException ex) {
            throw new RuntimeException("Exception occurred converting to output file!", ex);
        }

        return 0;
    }
}
