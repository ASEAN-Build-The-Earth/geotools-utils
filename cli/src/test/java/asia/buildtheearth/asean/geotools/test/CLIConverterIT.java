package asia.buildtheearth.asean.geotools.test;

import io.hosuaby.inject.resources.junit.jupiter.GivenBinaryResource;
import io.hosuaby.inject.resources.junit.jupiter.TestWithResources;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Integration tests for converter CLI
 */
@TestWithResources
@DisplayName("CLI Integration Test from KML")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class CLIConverterIT extends AbstractConverterIT {
    @GivenBinaryResource("test1.kml") public static byte[] test1;
    @GivenBinaryResource("test2.kml") public static byte[] test2;
    @GivenBinaryResource("test3.kml") public static byte[] test3;

    @Test
    public void startProcess() throws InterruptedException, IOException {
        Path cli = directory.resolve("cli");
        Path target = Path.of("target", "geotools-utils-cli-1.0.0.jar");
        Path script = cli.resolve("converter.jar");

        Assertions.assertDoesNotThrow(() -> Files.createDirectories(Files.createDirectories(script)));
        Assertions.assertDoesNotThrow(() -> Files.copy(target, script, StandardCopyOption.REPLACE_EXISTING));

        Assertions.assertTrue(script.toFile().exists());
        Assertions.assertTrue(script.toFile().setExecutable(true));
        Assertions.assertTrue(script.toFile().canExecute());

        // Assertions.assertDoesNotThrow(() -> copyDirectoryRecursively(target, cli));

        ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xlog:class+load=info", "-jar",
                "cli/converter.jar",
                "geojson",
                "kml",
                "-f", source.toAbsolutePath().toString(),
                "-o", output.toAbsolutePath().toString())
            .inheritIO().directory(directory.toFile());

        // Redirect output if debugging is needed
        // processBuilder.redirectErrorStream(true);
        // processBuilder.redirectOutput(new File("/test-logs-1.txt"));


        Process process = Assertions.assertDoesNotThrow(processBuilder::start);

        int exitCode = process.waitFor();

        Assertions.assertEquals(0, exitCode);

        System.out.print(Files.readString(output));

        // Test BlueMap Marker
        ProcessBuilder processBuilder2 = new ProcessBuilder("java", "-Xlog:class+load=info", "-jar",
                "cli/converter.jar",
                "bluemap",
                "geojson",
                "-f", output.toAbsolutePath().toString(),
                "-o", output.toAbsolutePath().toString())
                .inheritIO().directory(directory.toFile());

        // Redirect output if debugging is needed
        // processBuilder2.redirectErrorStream(true);
        // processBuilder2.redirectOutput(new File("/test-logs-2.txt"));

        Process process2 = Assertions.assertDoesNotThrow(processBuilder2::start);

        int exitCode2 = process2.waitFor();

        Assertions.assertEquals(0, exitCode2);

        System.out.print(Files.readString(output));
    }
}
