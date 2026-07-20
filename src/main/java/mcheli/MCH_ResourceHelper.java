package mcheli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility for enumerating and reading classpath resources.
 * Used by InfoManagers to load vehicle/weapon definitions from assets/mcheli/
 * without requiring the mod to be extracted to a filesystem folder.
 */
public class MCH_ResourceHelper {

    private static File sourceJar = null;

    /**
     * Sets the source JAR file for classpath resource enumeration.
     * Called from MCH_MOD.PreInit() with the mod's own JAR location.
     */
    public static void setSourceJar(File jar) {
        sourceJar = jar;
    }

    /**
     * Lists all resource entry names under the given classpath directory prefix.
     * Example: listResources("assets/mcheli/helicopters") returns
     * ["assets/mcheli/helicopters/ah-64.txt", "assets/mcheli/helicopters/ah-1.txt", ...]
     *
     * Only entries ending with the given suffix are returned.
     */
    public static List<String> listResources(String dirPrefix, String suffix) {
        List<String> result = new ArrayList<>();

        // Normalize prefix: ensure it ends with /
        if (!dirPrefix.endsWith("/")) dirPrefix = dirPrefix + "/";

        final String normalizedPrefix = dirPrefix;
        final String jarPrefix = dirPrefix.startsWith("/") ? dirPrefix.substring(1) : dirPrefix;

        if (sourceJar != null && sourceJar.exists() && sourceJar.isFile()) {
            // Running from a JAR file — enumerate JAR entries
            try (JarFile jar = new JarFile(sourceJar)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(jarPrefix) && name.endsWith(suffix) && !entry.isDirectory()) {
                        result.add(name);
                    }
                }
            } catch (Exception e) {
                MCH_Lib.DbgLog(true, "MCH_ResourceHelper: Failed to enumerate JAR entries: %s", e.getMessage());
            }
        } else {
            // Running in dev environment — enumerate filesystem
            try {
                URL dirUrl = MCH_ResourceHelper.class.getResource(normalizedPrefix);
                if (dirUrl != null) {
                    URI uri = dirUrl.toURI();
                    if ("file".equals(uri.getScheme())) {
                        // Filesystem directory
                        Path dirPath = new File(uri).toPath();
                        Files.walk(dirPath)
                            .filter(p -> p.toString().endsWith(suffix) && !Files.isDirectory(p))
                            .forEach(p -> result.add(jarPrefix + dirPath.relativize(p).toString().replace('\\', '/')));
                    } else if ("jar".equals(uri.getScheme())) {
                        // Inside a JAR (shouldn't happen if sourceJar is null, but handle it)
                        String jarPath = uri.toString().split("!")[0].replace("jar:file:", "");
                        try (JarFile jar = new JarFile(jarPath)) {
                            Enumeration<JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                if (name.startsWith(jarPrefix) && name.endsWith(suffix) && !entry.isDirectory()) {
                                    result.add(name);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                MCH_Lib.DbgLog(true, "MCH_ResourceHelper: Failed to enumerate filesystem resources: %s", e.getMessage());
            }
        }

        return result;
    }

    /**
     * Checks if a classpath resource exists.
     * Example: resourceExists("assets/mcheli/models/ah-64.mqo")
     */
    public static boolean resourceExists(String resourcePath) {
        if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;

        if (sourceJar != null && sourceJar.exists() && sourceJar.isFile()) {
            try (JarFile jar = new JarFile(sourceJar)) {
                return jar.getEntry(resourcePath.substring(1)) != null;
            } catch (Exception e) {
                return false;
            }
        } else {
            return MCH_ResourceHelper.class.getResource(resourcePath) != null;
        }
    }

    /**
     * Opens a classpath resource as a BufferedReader (UTF-8).
     * Returns null if the resource is not found.
     */
    public static BufferedReader openResource(String resourcePath) {
        if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;

        InputStream is = MCH_ResourceHelper.class.getResourceAsStream(resourcePath);
        if (is == null) return null;
        return new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Returns the entry name (just the filename, no path) from a full resource path.
     * Example: "assets/mcheli/helicopters/ah-64.txt" -> "ah-64.txt"
     */
    public static String getFileName(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        return lastSlash >= 0 ? resourcePath.substring(lastSlash + 1) : resourcePath;
    }

    /**
     * Returns the entry name without extension, lowercased.
     * Example: "assets/mcheli/helicopters/AH-64.txt" -> "ah-64"
     */
    public static String getEntryName(String resourcePath) {
        String fileName = getFileName(resourcePath);
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) fileName = fileName.substring(0, dot);
        return fileName.toLowerCase();
    }
}
