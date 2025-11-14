package asia.buildtheearth.asean.geotools.cli;

import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;

abstract class AbstractConverter implements Callable<Integer> {

    /**
     * The file format to convert to
     */
    @CommandLine.Parameters(index = "0",
            paramLabel = "FORMAT",
            description = "The file format to be converted to.",
            type = Conversion.Format.class,
            converter = Conversion.class,
            completionCandidates = Conversion.class)
    protected Conversion.Format format;

    /**
     * The input file to read and convert to.
     */
    @CommandLine.Option(names = {"-f", "--file"},
            description = "The input file.",
            paramLabel = "<path>",
            required = true,
            type = File.class)
    protected File input;

    /**
     * The output path to file be created or overwritten
     */
    @CommandLine.Option(names = {"-o", "--output"},
            description = "The output path.",
            paramLabel = "<path>",
            required = true,
            type = Path.class)
    protected Path output;


    @Override
    public Integer call() throws RuntimeException {
        if (!input.exists()) throw new CommandLine.ParameterException(new CommandLine(this),
            "Input file not found:\n\n\t" + input.getAbsolutePath() + '\n'
        );
        else if (!input.isFile()) throw new CommandLine.ParameterException(new CommandLine(this),
            "Input path is not a file (maybe a directory?):\n\n\n\t" + input.getAbsolutePath() + '\n'
        );

        System.out.println(
            "Converting from: " + CommandLine.Help.Ansi.AUTO.string(
            "@|bold,green,underline " + format.name() + "|@")
            + " To: " + CommandLine.Help.Ansi.AUTO.string(
            "@|bold,yellow,underline " + getFormat().name() + "|@")
        );

        return 0;
    }

    protected abstract Conversion.Format getFormat();

    /**
     * Elevation options for geo data.
     *
     * <p>Only applies for Z axis since modifying x and y axis doesn't make sense</p>
     */
    protected static class Elevation {
        @CommandLine.Option(
                names = {"-n", "--normalize"},
                paramLabel = "<double>",
                description = "Normalizes all Z (elevation) to a fixed value.",
                type = Double.class)
        Double normalizeZ;

        @CommandLine.Option(
                names = {"-z", "--z-offset"},
                paramLabel = "<double>",
                description = "Offsets all Z (elevation) by a fixed amount.",
                type = Double.class)
        Double offsetZ;

        @CommandLine.Option(
                names = {"-d", "--drop-z"},
                paramLabel = "<boolean>",
                description = {
                        "Drops all Z (elevation) values from the geometry",
                        "Producing 2D coordinates only."
                })
        boolean dropZ = false;
    }
}
