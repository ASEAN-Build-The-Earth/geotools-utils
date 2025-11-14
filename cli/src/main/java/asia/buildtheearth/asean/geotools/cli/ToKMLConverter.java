package asia.buildtheearth.asean.geotools.cli;

import asia.buildtheearth.asean.geotools.ToKML;
import picocli.CommandLine;
import java.io.IOException;

@CommandLine.Command(name = "kml", version = "1.0.0",
        description = "Converts a geospatial file to KML format.",
        mixinStandardHelpOptions = true, sortOptions = false)
public class ToKMLConverter extends AbstractConverter {

    /**
     * The (optional) elevation modification options.
     */
    @CommandLine.ArgGroup
    private Elevation elevation;

    /**
     * The (optional) XML output style.
     */
    @CommandLine.ArgGroup
    private Style style;

    @Override
    protected Conversion.Format getFormat() {
        return Conversion.Format.kml;
    }

    @Override
    public Integer call() throws RuntimeException {
        super.call();

        // Figure out converter
        ToKML converter = switch (this.format) {
            case kml -> ToKML.identity(this.input);
            case geojson -> ToKML.fromGeoJSON(this.input);
            case null -> throw new RuntimeException(
                "Conversion format return null, this should not happen."
            );
        };

        // Style can either be indented or disabled completely
        if(style != null) {
            if(style.indenting != null) converter.setIndentSize(style.indenting);
            else converter.disablePrettyPrint();
        }

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

    static class Style {
        @CommandLine.Option(
                names = {"-i", "--indenting"},
                paramLabel = "<integer>",
                defaultValue = "2",
                description = "Sets the indentation size for XML output.",
                type = Integer.class)
        Integer indenting;

        @CommandLine.Option(
                names = {"-c", "--compact"},
                paramLabel = "<boolean>",
                description = "Disables pretty-printing for the output file.")
        boolean compact = false;
    }
}
