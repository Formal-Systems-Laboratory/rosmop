package rosmop.util;

import java.io.File;
import java.io.IOException;

/**
 * Utilities Shared Across Test Files.
 * Each Test File is expected to create an Instance of ResourceUtil,
 */
public class ResourceUtils {

    ClassLoader classLoader = getClass().getClassLoader();

    public File processFileNameFromResources(String specFileName) throws IOException {
        File file = new File(classLoader.getResource(specFileName).getFile());
        if(file == null) {
            throw new IOException("File " + specFileName + " absent or invoker doesn't have adequate privileges to get the resource");
        }
        return file;
    }
}
