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

    public static void discoverDevClasspath() {
        devClasspathDirs = new ArrayList<>();

        // Method 1: Check java.class.path entries for directories containing assets/mcheli
        String cp = System.getProperty("java.class.path", "");
        for (String entry : cp.split(File.pathSeparator)) {
            File f = new File(entry);
            if (f.isDirectory() && new File(f, "assets/mcheli").isDirectory()) {
                devClasspathDirs.add(f);
            }
        }

        // Method 2: Search classloader hierarchy for URLClassLoaders
        if (devClasspathDirs.isEmpty()) {
            ClassLoader cl = MCH_ResourceHelper.class.getClassLoader();
            while (cl != null) {
                if (cl instanceof URLClassLoader) {
                    try {
                        for (URL url : ((URLClassLoader) cl).getURLs()) {
                            if ("file".equals(url.getProtocol())) {
                                File dir = new File(url.getPath());
                                if (dir.isDirectory() && new File(dir, "assets/mcheli").isDirectory()) {
                                    devClasspathDirs.add(dir);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                cl = cl.getParent();
            }
        }

        // Method 3: Walk upward from working directory to find build/resources/main
        if (devClasspathDirs.isEmpty()) {
            File cwd = new File(System.getProperty("user.dir"));
            File candidate = new File(cwd, "build/resources/main");
            if (candidate.isDirectory() && new File(candidate, "assets/mcheli").isDirectory()) {
                devClasspathDirs.add(candidate);
            }
        }

        // Method 4: Also check src/main/resources as last resort
        if (devClasspathDirs.isEmpty()) {
            File cwd = new File(System.getProperty("user.dir"));
            File candidate = new File(cwd, "src/main/resources");
            if (candidate.isDirectory() && new File(candidate, "assets/mcheli").isDirectory()) {
                devClasspathDirs.add(candidate);
            }
        }

        MCH_Lib.Log("MCH_ResourceHelper: dev classpath search found %d dirs", devClasspathDirs.size());
        for (File dir : devClasspathDirs) {
            MCH_Lib.Log("  -> %s", dir.getAbsolutePath());
        }

        if (devClasspathDirs.isEmpty()) {
            MCH_Lib.Log("WARNING: java.class.path = %s", cp);
        }
    }

    public static List<String> listResources(String dirPrefix, String suffix) {
        List<String> result = new ArrayList<>();

        if (!dirPrefix.endsWith("/")) dirPrefix = dirPrefix + "/";

        final String normalizedPrefix = dirPrefix;
        final String jarPrefix = dirPrefix.startsWith("/") ? dirPrefix.substring(1) : dirPrefix;

        if (sourceJar != null && sourceJar.exists() && sourceJar.isFile()) {
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
                MCH_Lib.Log("MCH_ResourceHelper: Failed to enumerate JAR: %s", e.getMessage());
            }
        } else {
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
                            MCH_Lib.Log("MCH_ResourceHelper: Failed to walk %s: %s", targetDir, e.getMessage());
                        }
                    }
                }
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
            String relPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            if (devClasspathDirs != null) {
                for (File cpDir : devClasspathDirs) {
                    if (new File(cpDir, relPath).isFile()) return true;
                }
            }
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
