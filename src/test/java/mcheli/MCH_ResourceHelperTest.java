package mcheli;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MCH_ResourceHelperTest {
    private String oldUserDir;
    private String oldClasspath;
    private File project;

    @Before public void setUp() throws Exception {
        oldUserDir = System.getProperty("user.dir");
        oldClasspath = System.getProperty("java.class.path");
        project = Files.createTempDirectory("mch-live-resources").toFile();
        new File(project, "build.gradle.kts").createNewFile();
        new File(project, "src/main/resources/assets/mcheli/test").mkdirs();
        System.setProperty("user.dir", new File(project, "run").getPath());
        new File(project, "run").mkdirs();
        MCH_ResourceHelper.setSourceJar(null);
        MCH_ResourceHelper.setAddonDir(new File(project, "run/mcheli_addons"));
    }

    @After public void tearDown() {
        System.setProperty("user.dir", oldUserDir);
        System.setProperty("java.class.path", oldClasspath);
        MCH_ResourceHelper.setAddonDir(null);
        MCH_ResourceHelper.discoverDevClasspath();
    }

    @Test public void liveSourceIsAuthoritativeAndAllApisAgree() throws Exception {
        File source = write("src/main/resources/assets/mcheli/test/shared.txt", "source");
        File generated = write("build/resources/main/assets/mcheli/test/shared.txt", "generated");
        write("build/resources/main/assets/mcheli/test/deleted.txt", "stale");
        File jar = new File(project, "dev.jar");
        makeJar(jar, "assets/mcheli/test/shared.txt", "jar");
        System.setProperty("java.class.path", new File(project, "build/resources/main").getPath()
                + File.pathSeparator + jar.getPath());

        MCH_ResourceHelper.refreshResourceSources();
        assertTrue(MCH_ResourceHelper.resourceExists("\\assets\\mcheli\\test\\shared.txt"));
        assertEquals("source", read(MCH_ResourceHelper.openResource("/assets/mcheli/test/shared.txt")));
        assertEquals("source", read(MCH_ResourceHelper.openResourceStream("//assets//mcheli//test//shared.txt")));
        assertFalse("a deleted source asset must not fall back to generated output",
                MCH_ResourceHelper.resourceExists("assets/mcheli/test/deleted.txt"));
        assertEquals(1, MCH_ResourceHelper.listResources("/assets/mcheli/test", ".txt").size());

        File added = write("src/main/resources/assets/mcheli/test/added.txt", "new");
        MCH_ResourceHelper.refreshResourceSources();
        assertTrue(MCH_ResourceHelper.listResources("assets/mcheli/test", ".txt").contains(
                "assets/mcheli/test/added.txt"));
        assertTrue(added.delete());
        assertTrue(source.isFile());
    }

    @Test public void addonOverridesSourceAndRefreshFindsNewPackWithoutDuplicates() throws Exception {
        write("src/main/resources/assets/mcheli/test/shared.txt", "source");
        MCH_ResourceHelper.refreshResourceSources();
        File addon = write("run/mcheli_addons/pack/assets/mcheli/test/shared.txt", "addon");
        MCH_ResourceHelper.refreshResourceSources();
        assertEquals("addon", read(MCH_ResourceHelper.openResourceStream("assets/mcheli/test/shared.txt")));
        for (int i = 0; i < 20; ++i) {
            MCH_ResourceHelper.refreshResourceSources();
            List<String> paths = MCH_ResourceHelper.listResources("assets/mcheli/test", ".txt");
            assertEquals(1, paths.size());
            assertEquals("addon", read(MCH_ResourceHelper.openResource(paths.get(0))));
        }
        assertTrue("streams must not keep addon files open", addon.delete());
    }

    @Test public void normalizesSeparatorsAndLeadingSlashes() {
        assertEquals("assets/mcheli/a.txt", MCH_ResourceHelper.normalizeAssetPath("//assets\\mcheli//a.txt"));
    }

    private File write(String relative, String value) throws Exception {
        File f = new File(project, relative); f.getParentFile().mkdirs();
        Files.write(f.toPath(), value.getBytes(StandardCharsets.UTF_8)); return f;
    }
    private static String read(BufferedReader reader) throws Exception {
        assertNotNull(reader); try { return reader.readLine(); } finally { reader.close(); }
    }
    private static String read(InputStream stream) throws Exception {
        assertNotNull(stream); try { byte[] b = new byte[64]; int n = stream.read(b); return new String(b, 0, n, StandardCharsets.UTF_8); }
        finally { stream.close(); }
    }
    private static void makeJar(File file, String path, String value) throws Exception {
        JarOutputStream out = new JarOutputStream(new FileOutputStream(file));
        try { out.putNextEntry(new JarEntry(path)); out.write(value.getBytes(StandardCharsets.UTF_8)); out.closeEntry(); }
        finally { out.close(); }
    }
}
