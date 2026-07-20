package mcheli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MCH_ResourceHelper {

    private static File sourceJar = null;
    private static List<File> devClasspathDirs = null;

    public static void setSourceJar(File jar) {
        sourceJar = jar;
    }

    /**
     * Discovers filesystem directories on the classpath that contain assets.
     * Called from PreInit when sourceJar is null (dev mode).
     */
    public static void discoverDevClasspath() {
        devClasspathDirs = new ArrayList<>();

        // Try URLClassLoader.getURLs() first (works in most dev setups)
        ClassLoader cl = MCH_ResourceHelper.class.getClassLoader();
        if (cl instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) cl).getURLs()) {
                if ("file".equals(url.getProtocol())) {
                    File dir = new File(url.getPath());
                    if (dir.isDirectory()) {
                        devClasspathDirs.add(dir);
                    }
                }
            }
        }

        // Also walk java.class.path as a fallback
        String cp = System.getProperty("java.class.path", "");
        for (String entry : cp.split(File.pathSeparator)) {
            File f = new File(entry);
            if (f.isDirectory() && !devClasspathDirs.contains(f)) {
                devClasspathDirs.add(f);
            }
        }

        // Check which dirs actually contain our assets
        List<File> validDirs = new ArrayList<>();
        for (File dir : devClasspathDirs) {
            File assetsDir = new File(dir, "assets/mcheli");
            if (assetsDir.isDirectory()) {
                validDirs.add(dir);
            }
        }

        if (!validDirs.isEmpty()) {
            devClasspathDirs = validDirs;
            MCH_Lib.DbgLog(false, "MCH_ResourceHelper: Found %d dev classpath dirs with assets", validDirs.size());
            for (File dir : validDirs) {
                MCH_Lib.DbgLog(false, "  -> %s", dir.getAbsolutePath());
            }
        } else {
            MCH_Lib.DbgLog(true, "MCH_ResourceHelper: WARNING - no dev classpath dirs found containing assets/mcheli");
        }
    }

    /**
     * Lists all resource entry names under the given classpath directory prefix.
     * Returns paths like "assets/mcheli/helicopters/ah-64.txt".
     */
    public static List<String> listResources(String dirPrefix, String suffix) {
        List<String> result = new ArrayList<>();

        if (!dirPrefix.endsWith("/")) dirPrefix = dirPrefix + "/";

        final String normalizedPrefix = dirPrefix;
        final String jarPrefix = dirPrefix.startsWith("/") ? dirPrefix.substring(1) : dirPrefix;

        if (sourceJar != null && sourceJar.exists() && sourceJar.isFile()) {
            // JAR mode — enumerate JAR entries
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
                MCH_Lib.DbgLog(true, "MCH_ResourceHelper: Failed to enumerate JAR: %s", e.getMessage());
            }
        } else {
            // Dev mode — walk classpath directories on the filesystem
            if (devClasspathDirs != null) {
                for (File cpDir : devClasspathDirs) {
                    File targetDir = new File(cpDir, jarPrefix);
                    if (targetDir.isDirectory()) {
                        try {
                            Path dirPath = targetDir.toPath();
                            Files.walk(dirPath)
                                .filter(p -> p.toString().endsWith(suffix) && !Files.isDirectory(p))
                                .forEach(p -> {
                                    String rel = dirPath.relativize(p).toString().replace('\\', '/');
                                    result.add(jarPrefix + rel);
                                });
                        } catch (Exception e) {
                            MCH_Lib.DbgLog(true, "MCH_ResourceHelper: Failed to walk %s: %s", targetDir, e.getMessage());
                        }
                    }
                }
            }

            // Fallback: try getResourceAsStream (works for individual files)
            if (result.isEmpty()) {
                MCH_Lib.DbgLog(false, "MCH_ResourceHelper: devClasspath walk found nothing for %s, trying getResourceAsStream fallback", jarPrefix);
            }
        }

        return result;
    }

    public static boolean resourceExists(String resourcePath) {
        if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;

        if (sourceJar != null && sourceJar.exists() && sourceJar.isFile()) {
            try (JarFile jar = new JarFile(sourceJar)) {
                return jar.getEntry(resourcePath.substring(1)) != null;
            } catch (Exception e) {
                return false;
            }
        } else {
            // Check classpath dirs first
            String relPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            if (devClasspathDirs != null) {
                for (File cpDir : devClasspathDirs) {
                    if (new File(cpDir, relPath).isFile()) return true;
                }
            }
            // Fallback to getResourceAsStream
            return MCH_ResourceHelper.class.getResourceAsStream(resourcePath) != null;
        }
    }

    public static BufferedReader openResource(String resourcePath) {
        if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;

        InputStream is = MCH_ResourceHelper.class.getResourceAsStream(resourcePath);
        if (is == null) return null;
        return new BufferedReader(new InputStreamReader(is));
    }

    public static String getFileName(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        return lastSlash >= 0 ? resourcePath.substring(lastSlash + 1) : resourcePath;
    }

    public static String getEntryName(String resourcePath) {
        String fileName = getFileName(resourcePath);
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) fileName = fileName.substring(0, dot);
        return fileName.toLowerCase();
    }
}
