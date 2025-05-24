package utils;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.*;

public class PlatformToolsInstaller {
    private static final String PLATFORM_TOOLS_DIR = "platform-tools";
    private static final String WINDOWS_URL  =
            "https://dl.google.com/android/repository/platform-tools-latest-windows.zip";
    private static final String MAC_URL      =
            "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip";
    private static final String ZIP_NAME     = "platform-tools.zip";

    public static void install() throws IOException {
        String systemPath = SystemUtils.getSystemPath();
        boolean isWindows = Constants.IS_WINDOWS_USER;

        // prepare the output dir
        Path ptDir = Paths.get(systemPath, PLATFORM_TOOLS_DIR);
        if (!Files.exists(ptDir)) {
            Files.createDirectories(ptDir);
        }

        // download into ptDir/platform-tools.zip
        String downloadUrl = isWindows ? WINDOWS_URL : MAC_URL;
        Path zipPath = ptDir.resolve(ZIP_NAME);
        try (InputStream in = new URL(downloadUrl).openStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // unzip *stripping* the leading "platform-tools/" folder
        unzipStripTopFolder(zipPath, ptDir, !isWindows);

        // cleanup
        Files.deleteIfExists(zipPath);
    }

    /**
     * Unpacks zipFile → targetDir, but if an entry name starts with "platform-tools/",
     * strips that prefix off so you don't double-nest.
     * If markExecutable==true, sets +x on every extracted file.
     */
    private static void unzipStripTopFolder(Path zipFile,
                                            Path targetDir,
                                            boolean markExecutable)
            throws IOException {
        String prefix = PLATFORM_TOOLS_DIR + "/";

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                // strip the leading platform-tools/ folder if present
                if (entryName.startsWith(prefix)) {
                    entryName = entryName.substring(prefix.length());
                }
                // skip any root entry (e.g. the stripped-to-empty dir)
                if (entryName.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                Path outPath = targetDir.resolve(entryName).normalize();
                if (!outPath.startsWith(targetDir)) {
                    throw new IOException("Bad ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                    if (markExecutable) {
                        outPath.toFile().setExecutable(true, false);
                    }
                }
                zis.closeEntry();
            }
        }
    }
}