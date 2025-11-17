package asia.buildtheearth.asean.geotools.cli;

import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Available Conversion file formats
 *
 * @see Format
 */
class Conversion implements CommandLine.ITypeConverter<Conversion.Format>, Iterable<String> {
    private static final Map<String, Format> extMap;
    private static final Stream<String> extItr;
    private static final RuntimeException extErr;
    private static final String separator = "\n  - ";

    static {
        extMap = new HashMap<>();
        extItr = Stream.of(Format.values()).map(Format::name);
        extErr = new CommandLine.TypeConversionException(
            "Unknown format provided.\n" +
            "Available formats:" + separator +
            extItr.collect(Collectors.joining(separator))
        );
    }

    @Override
    public @NotNull Format convert(String value) throws RuntimeException {
        Format supported = extMap.get(value.toLowerCase(Locale.ENGLISH));

        if (supported == null) throw extErr;

        return supported;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return extItr.iterator();
    }

    /**
     * Available format options.
     */
    public enum Format {
        geojson("geojson", "json", "gjson"),
        kml("kml"),
        bluemap("json");

        Format(String @NotNull ... extensions) {
            for (String ext : extensions) extMap.put(ext, this);
        }
    }
}
