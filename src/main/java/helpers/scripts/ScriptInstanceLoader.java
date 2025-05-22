package helpers.scripts;

import helpers.ScriptCategory;
import helpers.ScriptMetadata;
import helpers.annotations.ScriptManifest;
import helpers.scripts.utils.Script;
import helpers.scripts.utils.MetadataDTO;
import utils.SystemUtils;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

        cachedScriptsDTO = new ArrayList<>();

        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                try {
                    ScriptMetadata metadata = extractMetadataFromJar(jarFile);
                    if (metadata != null) {
                        MetadataDTO scriptDTO = new MetadataDTO(metadata, jarFile.getAbsolutePath(), "local");
                        cachedScriptsDTO.add(scriptDTO);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load metadata from: " + jarFile.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private ScriptMetadata extractMetadataFromJar(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            URL[] urls = { new URL("jar:file:" + jarFile.getAbsolutePath() + "!/") };
            try (URLClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader())) {
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);

                        try {
                            Class<?> clazz = cl.loadClass(className);
                            if (clazz.isAnnotationPresent(ScriptManifest.class)) {
                                ScriptManifest manifest = clazz.getAnnotation(ScriptManifest.class);
                                return new ScriptMetadata(
                                        manifest.name(),
                                        manifest.description(),
                                        manifest.version(),
                                        null,
                                        Arrays.asList(manifest.categories())
                                );
                            }
                        } catch (Throwable ignored) {
                            // Some classes might fail to load if they have static blocks or dependencies
                        }
                    }
                }
            }
        }
        return null;
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