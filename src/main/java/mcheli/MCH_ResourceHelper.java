package mcheli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Resolves MCHeli resources with a stable overlay order. */
public class MCH_ResourceHelper {

    private static final String ASSET_PREFIX = "assets/mcheli/";
    private static File sourceJar;
    private static File addonDir;
    private static List<File> addonAssetRoots = Collections.emptyList();
    private static List<File> devSourceDirs = Collections.emptyList();
    private static List<File> classpathDirs = Collections.emptyList();
    private static List<File> jarSources = Collections.emptyList();

    public static synchronized void setSourceJar(File jar) {
        sourceJar = jar;
    }

    public static synchronized void setAddonDir(File dir) {
        addonDir = dir;
        if (dir != null && !dir.exists()) dir.mkdirs();
        discoverAddonRoots();
        MCH_Lib.Log("MCH_ResourceHelper: Addon directory: %s (%d asset roots)",
                dir != null ? dir.getAbsolutePath() : "null", addonAssetRoots.size());
    }

    public static File getAddonDir() { return addonDir; }
    public static List<File> getAddonAssetRoots() { return new ArrayList<File>(addonAssetRoots); }
    public static List<File> getDevelopmentSourceDirs() { return new ArrayList<File>(devSourceDirs); }

    /** Re-scans both the classpath/project layout and addon packs for /mcheli reload. */
    public static synchronized void refreshResourceSources() {
        discoverDevClasspath();
        discoverAddonRoots();
    }

    private static void discoverAddonRoots() {
        ArrayList<File> roots = new ArrayList<File>();
        if (addonDir != null) {
            File[] children = addonDir.listFiles();
            if (children != null) {
                List<File> sorted = new ArrayList<File>();
                Collections.addAll(sorted, children);
                Collections.sort(sorted);
                for (File child : sorted) {
                    if (child.isDirectory() && new File(child, ASSET_PREFIX).isDirectory()) roots.add(canonical(child));
                }
            }
            if (new File(addonDir, ASSET_PREFIX).isDirectory() || isFlatAddon(addonDir)) roots.add(canonical(addonDir));
        }
        addonAssetRoots = roots;
    }

    private static boolean isFlatAddon(File root) {
        String[] assetDirs = {"helicopters", "planes", "ships", "tanks", "vehicles", "weapons", "items", "throwable", "hud", "models", "textures", "sounds"};
        for (String dir : assetDirs) if (new File(root, dir).isDirectory()) return true;
        return false;
    }

    /** Discovers all source types; finding a jar never terminates directory discovery. */
    public static synchronized void discoverDevClasspath() {
        LinkedHashSet<File> sources = new LinkedHashSet<File>();
        LinkedHashSet<File> dirs = new LinkedHashSet<File>();
        LinkedHashSet<File> jars = new LinkedHashSet<File>();
        if (sourceJar != null && sourceJar.isFile()) jars.add(canonical(sourceJar));

        File root = findProjectRoot(new File(System.getProperty("user.dir", ".")));
        if (root != null) addIfAssetRoot(sources, new File(root, "src/main/resources"));

        String cp = System.getProperty("java.class.path", "");
        for (String entry : cp.split(java.util.regex.Pattern.quote(File.pathSeparator))) classify(new File(entry), sources, dirs, jars);
        ClassLoader loader = MCH_ResourceHelper.class.getClassLoader();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader)loader).getURLs()) if ("file".equals(url.getProtocol())) {
                    try { classify(new File(url.toURI()), sources, dirs, jars); } catch (Exception ignored) {}
                }
            }
            loader = loader.getParent();
        }
        devSourceDirs = new ArrayList<File>(sources);
        classpathDirs = new ArrayList<File>(dirs);
        jarSources = new ArrayList<File>(jars);
        MCH_Lib.DbgLog(false, "Resource roots refreshed: editable=%s classpath=%s jars=%s",
                devSourceDirs, classpathDirs, jarSources);
    }

    private static void classify(File file, Set<File> sources, Set<File> dirs, Set<File> jars) {
        if (file.isDirectory() && new File(file, ASSET_PREFIX).isDirectory()) {
            File c = canonical(file);
            String p = c.getPath().replace('\\', '/');
            if (p.endsWith("/src/main/resources")) sources.add(c); else dirs.add(c);
        } else if (file.isFile() && file.getName().toLowerCase().endsWith(".jar") && containsAssets(file)) {
            jars.add(canonical(file));
        }
    }

    private static boolean containsAssets(File file) {
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) if (e.nextElement().getName().startsWith(ASSET_PREFIX)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private static File findProjectRoot(File from) {
        for (File f = canonical(from); f != null; f = f.getParentFile()) {
            if ((new File(f, "build.gradle.kts").isFile() || new File(f, "build.gradle").isFile())
                    && new File(f, "src/main/resources/" + ASSET_PREFIX).isDirectory()) return f;
        }
        return null;
    }

    private static void addIfAssetRoot(Set<File> result, File root) {
        if (new File(root, ASSET_PREFIX).isDirectory()) result.add(canonical(root));
    }

    public static synchronized List<String> listResources(String dirPrefix, String suffix) {
        String prefix = normalizeAssetPath(dirPrefix);
        if (!prefix.endsWith("/")) prefix += "/";
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        // Highest-precedence files are enumerated first, matching openResource.
        addDirectoryResources(result, addonAssetRoots, prefix, suffix);
        addDirectoryResources(result, devSourceDirs, prefix, suffix);
        // An editable tree is authoritative: absent source files are deletions, not jar fallbacks.
        if (devSourceDirs.isEmpty()) {
            addDirectoryResources(result, classpathDirs, prefix, suffix);
            addJarResources(result, jarSources, prefix, suffix);
        }
        return new ArrayList<String>(result);
    }

    private static void addDirectoryResources(Set<String> result, List<File> roots, String prefix, String suffix) {
        for (File root : roots) {
            File target = resourceFile(root, prefix);
            if (!target.isDirectory()) continue;
            ArrayList<String> found = new ArrayList<String>();
            try (Stream<Path> paths = Files.walk(target.toPath())) {
                for (java.util.Iterator<Path> it = paths.iterator(); it.hasNext();) {
                    Path p = it.next();
                    if (!Files.isDirectory(p) && p.toString().endsWith(suffix))
                        found.add(prefix + target.toPath().relativize(p).toString().replace('\\', '/'));
                }
            } catch (Exception e) { MCH_Lib.Log("Failed to walk resource root %s: %s", target, e.getMessage()); }
            Collections.sort(found);
            result.addAll(found);
        }
    }

    private static void addJarResources(Set<String> result, List<File> jars, String prefix, String suffix) {
        for (File file : jars) try (JarFile jar = new JarFile(file)) {
            ArrayList<String> found = new ArrayList<String>();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (!e.isDirectory() && e.getName().startsWith(prefix) && e.getName().endsWith(suffix)) found.add(e.getName());
            }
            Collections.sort(found); result.addAll(found);
        } catch (Exception e) { MCH_Lib.Log("Failed to enumerate resource jar %s: %s", file, e.getMessage()); }
    }

    public static synchronized boolean resourceExists(String path) {
        ResolvedResource r = resolve(path);
        if (r.file != null) return true;
        if (r.jar != null) { try (JarFile j = new JarFile(r.jar)) { return j.getEntry(r.path) != null; } catch (Exception ignored) {} }
        return r.classpath && MCH_ResourceHelper.class.getResource("/" + r.path) != null;
    }

    public static BufferedReader openResource(String path) {
        InputStream in = openResourceStream(path);
        return in == null ? null : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public static synchronized InputStream openResourceStream(String path) {
        ResolvedResource r = resolve(path);
        try {
            if (r.file != null) { debugSource(r.path, r.file); return new FileInputStream(r.file); }
            if (r.jar != null) {
                // JarFile must remain open until its entry stream is closed.
                final JarFile jar = new JarFile(r.jar);
                JarEntry entry = jar.getJarEntry(r.path);
                if (entry != null) { debugSource(r.path, r.jar); return new java.io.FilterInputStream(jar.getInputStream(entry)) {
                    public void close() throws java.io.IOException { try { super.close(); } finally { jar.close(); } }
                }; }
                jar.close();
            }
        } catch (Exception e) { MCH_Lib.Log("Failed to open resource %s: %s", r.path, e.getMessage()); return null; }
        if (r.classpath) {
            URL url = MCH_ResourceHelper.class.getResource("/" + r.path);
            if (url != null) MCH_Lib.DbgLog(false, "Resource %s <- %s", r.path, url);
            return MCH_ResourceHelper.class.getResourceAsStream("/" + r.path);
        }
        return null;
    }

    private static ResolvedResource resolve(String raw) {
        String path = normalizeAssetPath(raw);
        File f = findFile(addonAssetRoots, path);
        if (f != null) return new ResolvedResource(path, f, null, false);
        f = findFile(devSourceDirs, path);
        if (f != null) return new ResolvedResource(path, f, null, false);
        if (!devSourceDirs.isEmpty()) return new ResolvedResource(path, null, null, false);
        f = findFile(classpathDirs, path);
        if (f != null) return new ResolvedResource(path, f, null, false);
        for (File jar : jarSources) if (jarHas(jar, path)) return new ResolvedResource(path, null, jar, false);
        return new ResolvedResource(path, null, null, true);
    }

    private static File findFile(List<File> roots, String path) {
        for (File root : roots) { File f = resourceFile(root, path); if (f.isFile()) return f; }
        return null;
    }
    private static File resourceFile(File root, String path) {
        if (root.equals(addonDir) && !new File(root, ASSET_PREFIX).isDirectory() && path.startsWith(ASSET_PREFIX))
            return new File(root, path.substring(ASSET_PREFIX.length()));
        return new File(root, path);
    }
    private static boolean jarHas(File file, String path) {
        try (JarFile jar = new JarFile(file)) { return jar.getJarEntry(path) != null; } catch (Exception ignored) { return false; }
    }
    private static void debugSource(String path, File source) { MCH_Lib.DbgLog(false, "Resource %s <- %s", path, source); }
    private static File canonical(File f) { try { return f.getCanonicalFile(); } catch (Exception e) { return f.getAbsoluteFile(); } }

    public static String normalizeAssetPath(String path) {
        path = path == null ? "" : path.replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        while (path.contains("//")) path = path.replace("//", "/");
        return path;
    }
    public static String getFileName(String path) { int i = path.lastIndexOf('/'); return i < 0 ? path : path.substring(i + 1); }
    public static String getEntryName(String path) { String n = getFileName(path); int i = n.lastIndexOf('.'); return (i > 0 ? n.substring(0, i) : n).toLowerCase(); }

    private static final class ResolvedResource {
        final String path; final File file; final File jar; final boolean classpath;
        ResolvedResource(String p, File f, File j, boolean c) { path = p; file = f; jar = j; classpath = c; }
    }
}
