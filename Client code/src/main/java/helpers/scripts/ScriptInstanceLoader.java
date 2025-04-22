package helpers.scripts;

import helpers.ScriptCategory;
import helpers.ScriptMetadata;
import helpers.scripts.utils.Script;
import helpers.scripts.utils.MetadataDTO;
import utils.SystemUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ScriptInstanceLoader {
    private static List<MetadataDTO> cachedScriptsDTO;


    public synchronized List<MetadataDTO> loadScripts() {
        if (cachedScriptsDTO == null) {
            loadLocalScripts();
        }
        return cachedScriptsDTO;
    }

    private void loadLocalScripts() {
        File localScriptsDir = new File(SystemUtils.getLocalScriptFolderPath());
        File[] jarFiles = localScriptsDir.listFiles((dir, name) -> name.endsWith(".jar"));

        // Initialize the list before use
        cachedScriptsDTO = new ArrayList<>();

        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                ScriptMetadata scriptMetadata = new ScriptMetadata(
                        jarFile.getName(),
                        "local",
                        "local",
                        "",
                        Collections.singletonList(ScriptCategory.Local)
                );
                MetadataDTO scriptDTO = new MetadataDTO(scriptMetadata, jarFile.getAbsolutePath(), "local");

                cachedScriptsDTO.add(scriptDTO);
            }
        }
    }

    public Script createScriptObjectFromFile(String jarFilePath) {
        try (InputStream fileStream = new FileInputStream(jarFilePath);
             JarInputStream jarStream = new JarInputStream(fileStream)) {

            Script script = new Script();
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                if (!entry.isDirectory()) {
                    // Read the bytes of each entry
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = jarStream.read(buffer, 0, 1024)) != -1) {
                            baos.write(buffer, 0, count);
                        }
                        script.addFile(entry.getName(), baos.toByteArray());
                    }
                }
            }
            return script;
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
            return null;
        }
    }

    public void invalidateCache() {
        // We can use this method in case we get some edge case where we need to re-cache scripts
        cachedScriptsDTO = null;
    }
}