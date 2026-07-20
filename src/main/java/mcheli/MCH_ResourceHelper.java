package mcheli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private static File addonDir = null;

    public static void setSourceJar(File jar) {
        sourceJar = jar;
    }

    public static void setAddonDir(File dir) {
        addonDir = dir;
        if (addonDir != null && !addonDir.exists()) {
            addonDir.mkdirs();
            MCH_Lib.Log("MCH_ResourceHelper: Created addon directory: %s", addonDir.getAbsolutePath());
        }
        MCH_Lib.Log("MCH_ResourceHelper: Addon directory: %s", addonDir != null ? addonDir.getAbsolutePath() : "null");
    }

    public static File getAddonDir() {
        return addonDir;
    }

    public static void discoverDevClasspath() {
        devClasspathDirs = new ArrayList<>();

        String cp = System.getProperty("java.class.path", "");

        // Method 1: Find JARs on classpath that contain assets/mcheli/ (RFG dev mode)
        for (String entry : cp.split(File.pathSeparator)) {
            File f = new File(entry);
            if (f.isFile() && entry.endsWith(".jar")) {
                try (JarFile jar = new JarFile(f)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry e = entries.nextElement();
                        if (e.getName().startsWith("assets/mcheli/") && !e.isDirectory()) {
                            sourceJar = f;
                            MCH_Lib.Log("MCH_ResourceHelper: Found dev JAR with assets: %s", f.getName());
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Method 2: Find directories on classpath with assets/mcheli
        for (String entry : cp.split(File.pathSeparator)) {
            File f = new File(entry);
            if (f.isDirectory() && new File(f, "assets/mcheli").isDirectory()) {
                devClasspathDirs.add(f);
            }
        }

        // Method 3: Search classloader hierarchy for URLClassLoaders
        if (devClasspathDirs.isEmpty()) {
            ClassLoader cl = MCH_ResourceHelper.class.getClassLoader();
            while (cl != null) {
                if (cl instanceof java.net.URLClassLoader) {
                    try {
                        for (java.net.URL url : ((java.net.URLClassLoader) cl).getURLs()) {
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

        // Method 4: Walk upward from working directory to find build/resources/main or src/main/resources
        if (devClasspathDirs.isEmpty()) {
            File cwd = new File(System.getProperty("user.dir"));
            File projectRoot = cwd;
            // If user.dir is a run/ subdirectory, walk up to project root
            while (projectRoot != null && !new File(projectRoot, "src").isDirectory()) {
                projectRoot = projectRoot.getParentFile();
            }
            if (projectRoot != null) {
                for (String rel : new String[]{"build/resources/main", "src/main/resources"}) {
                    File candidate = new File(projectRoot, rel);
                    if (candidate.isDirectory() && new File(candidate, "assets/mcheli").isDirectory()) {
                        devClasspathDirs.add(candidate);
                    }
                }
            }
        }

        MCH_Lib.Log("MCH_ResourceHelper: dev classpath found %d dirs", devClasspathDirs.size());
        for (File dir : devClasspathDirs) {
            MCH_Lib.Log("  -> %s", dir.getAbsolutePath());
        }

        if (devClasspathDirs.isEmpty() && sourceJar == null) {
            MCH_Lib.Log("WARNING: No assets found on classpath. java.class.path entries: %d", cp.split(File.pathSeparator).length);
        }
    }

    public static List<String> listResources(String dirPrefix, String suffix) {
        List<String> result = new ArrayList<>();

        if (!dirPrefix.endsWith("/")) dirPrefix = dirPrefix + "/";

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
        }

        if (result.isEmpty() && devClasspathDirs != null) {
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

        // Scan addon directory for additional files (additive, never removes JAR entries)
        if (addonDir != null && addonDir.isDirectory()) {
            // jarPrefix is like "assets/mcheli/helicopters/"
            // Strip "assets/mcheli/" to get the relative subdir
            String assetPrefix = "assets/mcheli/";
            if (jarPrefix.startsWith(assetPrefix)) {
                String relSubdir = jarPrefix.substring(assetPrefix.length());
                File addonSubdir = new File(addonDir, relSubdir);
                if (addonSubdir.isDirectory()) {
                    try {
                        Path dirPath = addonSubdir.toPath();
                        Files.walk(dirPath)
                            .filter(p -> p.toString().endsWith(suffix) && !Files.isDirectory(p))
                            .forEach(p -> {
                                String rel = dirPath.relativize(p).toString().replace('\\', '/');
                                String resourcePath = assetPrefix + relSubdir + rel;
                                // Avoid duplicates from JAR
                                if (!result.contains(resourcePath)) {
                                    result.add(resourcePath);
                                }
                            });
                    } catch (Exception e) {
                        MCH_Lib.Log("MCH_ResourceHelper: Failed to walk addon dir %s: %s", addonSubdir, e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    public static boolean resourceExists(String resourcePath) {
        if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;

        // Check addon directory first (additive overlay)
        if (addonDir != null) {
            File addonFile = addonResourceFile(resourcePath);
            if (addonFile != null && addonFile.isFile()) return true;
        }

        if (sourceJar != null && sourceJar.exists() && sourceJar.isFile()) {
            try (JarFile jar = new JarFile(sourceJar)) {
                return jar.getEntry(resourcePath.substring(1)) != null;
            } catch (Exception e) {
                return false;
            }
        }

        String relPath = resourcePath.substring(1);
        if (devClasspathDirs != null) {
            for (File cpDir : devClasspathDirs) {
                if (new File(cpDir, relPath).isFile()) return true;
            }
        }
        return MCH_ResourceHelper.class.getResourceAsStream(resourcePath) != null;
    }

    public static BufferedReader openResource(String resourcePath) {
        if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;

        // Check addon directory first (user overrides take priority)
        if (addonDir != null) {
            File addonFile = addonResourceFile(resourcePath);
            if (addonFile != null && addonFile.isFile()) {
                try {
                    return new BufferedReader(new InputStreamReader(new FileInputStream(addonFile), StandardCharsets.UTF_8));
                } catch (FileNotFoundException e) {
                    // fall through to classpath
                }
            }
        }

        InputStream is = MCH_ResourceHelper.class.getResourceAsStream(resourcePath);
        if (is == null) return null;
        return new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Maps a classpath resource path like "/assets/mcheli/helicopters/ah-64.txt"
     * to a file under the addon directory, e.g. "mcheli_addons/helicopters/ah-64.txt".
     * Returns null if the path doesn't map to an addon resource.
     */
    private static File addonResourceFile(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        String prefix = "assets/mcheli/";
        if (path.startsWith(prefix)) {
            String relPath = path.substring(prefix.length());
            return new File(addonDir, relPath);
        }
        return null;
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
