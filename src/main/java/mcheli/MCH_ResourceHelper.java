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
    private static List<File> addonAssetRoots = null;

    private static final String ASSET_PREFIX = "assets/mcheli/";

    public static void setSourceJar(File jar) {
        sourceJar = jar;
    }

    /**
     * Sets the addon directory and discovers addon asset roots.
     * Supports two layouts:
     *   Flat:      mcheli_addons/helicopters/ah-60.txt
     *   Nested:    mcheli_addons/atom4a/assets/mcheli/helicopters/ah-60.txt
     */
    public static void setAddonDir(File dir) {
        addonDir = dir;
        addonAssetRoots = new ArrayList<>();
        if (addonDir != null) {
            if (!addonDir.exists()) {
                addonDir.mkdirs();
                MCH_Lib.Log("MCH_ResourceHelper: Created addon directory: %s", addonDir.getAbsolutePath());
            }
            discoverAddonRoots();
        }
        MCH_Lib.Log("MCH_ResourceHelper: Addon directory: %s (%d asset roots)",
                addonDir != null ? addonDir.getAbsolutePath() : "null", addonAssetRoots.size());
    }

    public static File getAddonDir() {
        return addonDir;
    }

    public static List<File> getAddonAssetRoots() {
        return addonAssetRoots;
    }

    private static void discoverAddonRoots() {
        addonAssetRoots = new ArrayList<>();

        // Check if the addon root itself is an asset root (flat layout)
        if (new File(addonDir, ASSET_PREFIX).isDirectory()) {
            addonAssetRoots.add(addonDir);
            MCH_Lib.Log("  Flat addon root: %s", addonDir.getAbsolutePath());
        }

        // Scan subdirectories for nested addon packs (e.g. atom4a/assets/mcheli/)
        File[] children = addonDir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && new File(child, ASSET_PREFIX).isDirectory()) {
                    addonAssetRoots.add(child);
                    MCH_Lib.Log("  Nested addon root: %s", child.getName());
                }
            }
        }
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
        if (addonAssetRoots != null && jarPrefix.startsWith(ASSET_PREFIX)) {
            String relSubdir = jarPrefix.substring(ASSET_PREFIX.length());
            for (File root : addonAssetRoots) {
                File addonSubdir = new File(root, ASSET_PREFIX + relSubdir);
                if (addonSubdir.isDirectory()) {
                    try {
                        Path dirPath = addonSubdir.toPath();
                        Files.walk(dirPath)
                            .filter(p -> p.toString().endsWith(suffix) && !Files.isDirectory(p))
                            .forEach(p -> {
                                String rel = dirPath.relativize(p).toString().replace('\\', '/');
                                String resourcePath = ASSET_PREFIX + relSubdir + rel;
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

        // Check addon asset roots first (additive overlay)
        if (addonAssetRoots != null && !addonAssetRoots.isEmpty()) {
            File addonFile = findAddonResourceFile(resourcePath);
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

        // Check addon asset roots first (user overrides take priority)
        if (addonAssetRoots != null && !addonAssetRoots.isEmpty()) {
            File addonFile = findAddonResourceFile(resourcePath);
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
     * Opens a resource as an InputStream (for binary resources like .mqo, .obj models).
     * Checks addon dirs first, then classpath.
     * Caller is responsible for closing the stream.
     */
    public static InputStream openResourceStream(String resourcePath) {
        if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;

        // Check addon asset roots first
        if (addonAssetRoots != null && !addonAssetRoots.isEmpty()) {
            File addonFile = findAddonResourceFile(resourcePath);
            if (addonFile != null && addonFile.isFile()) {
                try {
                    return new FileInputStream(addonFile);
                } catch (FileNotFoundException e) {
                    // fall through
                }
            }
        }

        return MCH_ResourceHelper.class.getResourceAsStream(resourcePath);
    }

    /**
     * Searches all addon asset roots for a resource file.
     * Checks nested packs first (higher priority), then flat layout.
     * Returns the first match found.
     */
    private static File findAddonResourceFile(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        if (!path.startsWith(ASSET_PREFIX)) return null;
        String relPath = path.substring(ASSET_PREFIX.length());

        // Check nested addon packs first (they can override flat)
        for (File root : addonAssetRoots) {
            if (root != addonDir) {
                File candidate = new File(root, ASSET_PREFIX + relPath);
                if (candidate.isFile()) return candidate;
            }
        }
        // Check flat layout last
        if (addonDir != null && addonAssetRoots.contains(addonDir)) {
            File candidate = new File(addonDir, relPath);
            if (candidate.isFile()) return candidate;
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
