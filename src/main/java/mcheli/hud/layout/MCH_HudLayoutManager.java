package mcheli.hud.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mcheli.MCH_Lib;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Cached, client-only persistence and render scopes for HUD layout offsets. */
public final class MCH_HudLayoutManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, MCH_HudLayoutProfile> PROFILES = new LinkedHashMap<String, MCH_HudLayoutProfile>();
    private static final ThreadLocal<Deque<String>> CALL_PATH = new ThreadLocal<Deque<String>>() {
        protected Deque<String> initialValue() { return new ArrayDeque<String>(); }
    };
    private static String rootHud = "none";

    private MCH_HudLayoutManager() {}

    public static synchronized void reload() {
        PROFILES.clear();
        loadDirectory(new File(baseDirectory(), "hud"));
        loadDirectory(new File(baseDirectory(), "builtin"));
    }

    public static void beginHud(String name) { rootHud = safeId(name); CALL_PATH.get().clear(); }
    public static String currentParsedProfileId() { return "hud:" + rootHud; }
    public static void endHud() { CALL_PATH.get().clear(); }
    public static void pushCall(String call) { CALL_PATH.get().addLast(safeId(call)); }
    public static void popCall() { if(!CALL_PATH.get().isEmpty()) CALL_PATH.get().removeLast(); }

    public static String parsedId(String sourceHud, int line, String directive, int ordinal, String groupId) {
        StringBuilder b = new StringBuilder(rootHud).append('/');
        if(CALL_PATH.get().isEmpty()) b.append("root");
        else for(String call : CALL_PATH.get()) b.append(call).append('/');
        b.append(safeId(sourceHud)).append("/line-").append(line).append('/').append(safeId(directive)).append('/').append(ordinal);
        if(groupId != null && groupId.length() > 0) b.append("/group-").append(safeId(groupId));
        return b.toString();
    }

    public static MCH_HudLayoutElement offsetFor(String profileId, String id, String sourceHud, String fingerprint, int line) {
        MCH_HudLayoutProfile p = profile(profileId, "");
        MCH_HudLayoutElement exact = p.elements.get(id);
        if(exact != null) return exact;
        MCH_HudLayoutElement candidate = null;
        for(MCH_HudLayoutElement e : p.elements.values()) {
            if(e != null && fingerprint != null && fingerprint.equals(e.fingerprint) && (e.sourceHud.length() == 0 || e.sourceHud.equals(sourceHud))) {
                if(candidate != null) return null;
                candidate = e;
            }
        }
        return candidate;
    }

    public static void renderParsed(String profileId, String id, String sourceHud, String fingerprint, int line, Runnable draw) {
        MCH_HudLayoutProfile p = profile(profileId, "assets/mcheli/hud/" + sourceHud + ".txt");
        MCH_HudLayoutElement e = offsetFor(profileId, id, sourceHud, fingerprint, line);
        if(e == null) {
            e = new MCH_HudLayoutElement(); e.id = id; e.sourceHud = sourceHud; e.sourceLine = line;
            e.fingerprint = fingerprint == null ? "" : fingerprint; e.displayName = id;
            p.elements.put(id, e);
        }
        GL11.glPushMatrix();
        try {
            if(e != null) GL11.glTranslated(e.offsetX, e.offsetY, 0.0D);
            draw.run();
        } finally { GL11.glPopMatrix(); }
    }

    public static void renderBuiltin(String context, String id, Runnable draw) {
        String profileId = "builtin:" + safeId(context);
        MCH_HudLayoutProfile p = profile(profileId, "Java-rendered HUD groups");
        MCH_HudLayoutElement e = p.elements.get(id);
        if(e == null) {
            e = new MCH_HudLayoutElement(); e.id = id; e.displayName = id; e.fingerprint = "builtin:" + id;
            e.groupId = id; p.elements.put(id, e);
        }
        GL11.glPushMatrix();
        try { GL11.glTranslated(e.offsetX, e.offsetY, 0.0D); draw.run(); }
        finally { GL11.glPopMatrix(); }
    }

    public static synchronized MCH_HudLayoutProfile profile(String id, String source) {
        MCH_HudLayoutProfile p = PROFILES.get(id);
        if(p == null) { p = new MCH_HudLayoutProfile(); p.profileId = id; p.source = source; PROFILES.put(id, p); }
        return p;
    }

    public static synchronized List<MCH_HudLayoutProfile> profiles() {
        return Collections.unmodifiableList(new ArrayList<MCH_HudLayoutProfile>(PROFILES.values()));
    }

    public static synchronized boolean save(MCH_HudLayoutProfile working) {
        if(working == null) return false;
        File dir = new File(baseDirectory(), working.profileId.startsWith("hud:") ? "hud" : "builtin");
        if(!dir.isDirectory() && !dir.mkdirs()) return false;
        File file = new File(dir, safeId(working.profileId) + ".json");
        File temp = new File(dir, file.getName() + ".tmp");
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            try { GSON.toJson(working, out); } finally { out.close(); }
            try { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch(IOException unsupported) { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            PROFILES.put(working.profileId, working.copy());
            return true;
        } catch(Exception e) {
            MCH_Lib.Log("Failed to write HUD layout %s: %s", file, e.toString());
            if(temp.exists()) temp.delete();
            return false;
        }
    }

    private static void loadDirectory(File dir) {
        File[] files = dir.listFiles();
        if(files == null) return;
        for(File file : files) if(file.isFile() && file.getName().endsWith(".json")) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(file));
                MCH_HudLayoutProfile p;
                try { p = GSON.fromJson(in, MCH_HudLayoutProfile.class); } finally { in.close(); }
                if(p == null || p.schemaVersion < 1 || p.profileId == null) throw new IOException("invalid layout schema");
                if(p.elements == null) p.elements = new LinkedHashMap<String, MCH_HudLayoutElement>();
                PROFILES.put(p.profileId, p);
            } catch(Exception e) {
                File broken = new File(file.getParentFile(), file.getName() + ".broken-" + System.currentTimeMillis());
                if(!file.renameTo(broken)) MCH_Lib.Log("Could not preserve malformed HUD layout %s", file);
                MCH_Lib.Log("Failed to read HUD layout %s: %s", file, e.toString());
            }
        }
    }

    private static File baseDirectory() { return new File(new File(Minecraft.getMinecraft().mcDataDir, "config/mcheli"), "hud_layouts"); }
    public static String safeId(String value) {
        String s = value == null ? "unknown" : value.toLowerCase().replaceAll("[^a-z0-9._-]+", "_");
        return s.length() == 0 ? "unknown" : s;
    }
}
