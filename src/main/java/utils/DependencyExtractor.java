package utils;

import java.io.*;
import java.util.zip.*;

public class DependencyExtractor {
    public static void extractDependencies() throws IOException {
        String destinationDir = SystemUtils.getSystemPath();

        try (InputStream resourceStream = DependencyExtractor.class.getResourceAsStream("/dependencies.zip")) {
            if (resourceStream == null) {
                throw new FileNotFoundException("Could not find dependencies.zip in resources.");
            }

            try (ZipInputStream zis = new ZipInputStream(resourceStream)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    String[] parts = entryName.split("/", 2);
                    if (parts.length < 2) continue;

                    String cleanName = parts[1]; // strip "dependencies/" prefix
                    File outFile = new File(destinationDir, cleanName);

                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        File parent = outFile.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }

                        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile))) {
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = zis.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                        }
                    }
                }
            }
        }
    }
}
