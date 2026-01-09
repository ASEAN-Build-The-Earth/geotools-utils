package asia.buildtheearth.asean.geotools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public final class SchematicExport extends AbstractGeoToolsConverter {


    /**
     * Constructs a new converter using the given input file and output path.
     *
     * @param sourceFile source file to be converted
     */
    public SchematicExport(File sourceFile) {
        super(sourceFile);
    }

    /**
     * TODO: Finish this implementation
     *
     * @param output the target file path to write the converted result to
     * @throws IOException If conversion went wrong
     */
    @Override
    public void convert(Path output) throws IOException {

    }
}
