package utils;

import java.io.*;
import java.util.zip.*;

public class DependencyExtractor {
    private static final String ZIP_PREFIX = "dependencies/";
    private static final String PLATFORM_TOOLS_PREFIX = ZIP_PREFIX + "platform-tools/";

    public static void extractDependencies() throws IOException {
        String destinationDir = SystemUtils.getSystemPath();

        // 1) Unpack everything under dependencies/, except platform-tools
        try (InputStream res = DependencyExtractor.class.getResourceAsStream("/dependencies.zip")) {
            if (res == null) {
                throw new FileNotFoundException("Could not find dependencies.zip in resources.");
            }
            try (ZipInputStream zis = new ZipInputStream(res)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();

                    // ←— ADD THESE TWO GUARDS:
                    //   a) must start with "dependencies/"
                    //   b) skip the old platform-tools folder
                    if (!name.startsWith(ZIP_PREFIX)
                            || name.startsWith(PLATFORM_TOOLS_PREFIX)) {
                        zis.closeEntry();
                        continue;
                    }

                    // safe to strip the "dependencies/" prefix now
                    String relPath = name.substring(ZIP_PREFIX.length());
                    File outFile = new File(destinationDir, relPath);

                    if (entry.isDirectory()) {
                        if (!outFile.isDirectory() && !outFile.mkdirs()) {
                            throw new IOException("Failed to create dir: " + outFile);
                        }
                    } else {
                        File parent = outFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create dir: " + parent);
                        }
                        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile))) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = zis.read(buf)) != -1) {
                                os.write(buf, 0, len);
                            }
                        }
                    }

                    zis.closeEntry();
                }
            }
        }

        // 2) Now install platform-tools via network
        PlatformToolsInstaller.install();
    }
}
