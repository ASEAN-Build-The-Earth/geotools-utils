package asia.buildtheearth.asean.geotools.cli;

import picocli.CommandLine;

@CommandLine.Command(name = "convert", version = "1.0.0",
        subcommands = { ToKMLConverter.class, ToGeoJSONConverter.class, CommandLine.HelpCommand.class },
        description = "Convert geospatial data from one format to other format")
@SuppressWarnings("unused")
public class GeoToolsConvertor  {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String... args) {
        int exitCode = new CommandLine(new GeoToolsConvertor()).execute(args);

        System.exit(exitCode);
    }
}
