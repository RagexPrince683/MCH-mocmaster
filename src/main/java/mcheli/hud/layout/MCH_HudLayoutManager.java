package mcheli.hud.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import mcheli.MCH_Lib;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Cached client-only persistence, live editor overrides, and render bounds. */
public final class MCH_HudLayoutManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, MCH_HudLayoutProfile> PROFILES = new LinkedHashMap<String, MCH_HudLayoutProfile>();
    private static final Map<String, MCH_HudLayoutProfile> WORKING = new LinkedHashMap<String, MCH_HudLayoutProfile>();
    private static final List<MCH_HudLayoutElement> FRAME = new ArrayList<MCH_HudLayoutElement>();
    private static final Deque<Scope> SCOPES = new ArrayDeque<Scope>();
    private static final ThreadLocal<Deque<String>> CALL_PATH = new ThreadLocal<Deque<String>>() {
        protected Deque<String> initialValue() { return new ArrayDeque<String>(); }
    };
    private static String rootHud = "none";
    private static boolean editing;
    private static boolean editorFrame;

    private static final class Scope {
        final MCH_HudLayoutElement element; final int x, y;
        Scope(MCH_HudLayoutElement element) { this.element = element; this.x = element.offsetX; this.y = element.offsetY; }
    }
    private MCH_HudLayoutManager() {}

    public static synchronized void reload() { PROFILES.clear(); loadDirectory(new File(baseDirectory(), "hud")); loadDirectory(new File(baseDirectory(), "builtin")); }
    public static void beginHud(String name) { rootHud = safeId(name); CALL_PATH.get().clear(); }
    public static String currentParsedProfileId() { return "hud:" + rootHud; }
    public static void endHud() { CALL_PATH.get().clear(); }
    public static void pushCall(String call) { CALL_PATH.get().addLast(safeId(call)); }
    public static void popCall() { if(!CALL_PATH.get().isEmpty()) CALL_PATH.get().removeLast(); }

    public static String parsedId(String sourceHud, int line, String directive, int ordinal, String groupId) {
        StringBuilder b = new StringBuilder(rootHud).append('/');
        if(CALL_PATH.get().isEmpty()) b.append("root/"); else for(String call : CALL_PATH.get()) b.append(call).append('/');
        b.append(safeId(sourceHud)).append('/');
        if(groupId != null && groupId.length() > 0) b.append("group-").append(safeId(groupId));
        else b.append("line-").append(line).append('/').append(safeId(directive)).append('/').append(ordinal);
        return b.toString();
    }

    public static synchronized void beginEditSession() { editing = true; editorFrame = false; WORKING.clear(); FRAME.clear(); SCOPES.clear(); }
    public static synchronized void beginEditorFrame() { if(editing) { editorFrame = true; FRAME.clear(); SCOPES.clear(); } }
    public static synchronized void endEditorFrame() { editorFrame = false; SCOPES.clear(); }
    public static synchronized boolean isEditing() { return editing; }
    public static synchronized List<MCH_HudLayoutElement> frameElements() { return Collections.unmodifiableList(FRAME); }
    public static synchronized Collection<MCH_HudLayoutProfile> workingProfiles() { return Collections.unmodifiableCollection(WORKING.values()); }
    public static synchronized void cancelEditSession() { editing = editorFrame = false; WORKING.clear(); FRAME.clear(); SCOPES.clear(); }
    public static synchronized boolean commitEditSession() {
        for(MCH_HudLayoutProfile p : WORKING.values()) if(p.isDirty() && !save(p)) return false;
        cancelEditSession(); return true;
    }

    private static synchronized MCH_HudLayoutProfile renderProfile(String id, String source) {
        MCH_HudLayoutProfile saved = profile(id, source);
        if(!editing) return saved;
        MCH_HudLayoutProfile p = WORKING.get(id);
        if(p == null) { p = saved.copy(); p.captureOriginalOffsets(); WORKING.put(id, p); }
        return p;
    }
    private static MCH_HudLayoutElement find(MCH_HudLayoutProfile p, String id, String sourceHud, String fingerprint) {
        MCH_HudLayoutElement exact = p.elements.get(id); if(exact != null) return exact;
        MCH_HudLayoutElement candidate = null;
        for(MCH_HudLayoutElement e : p.elements.values()) if(e != null && fingerprint != null && fingerprint.equals(e.fingerprint) && (e.sourceHud.length()==0 || e.sourceHud.equals(sourceHud))) {
            if(candidate != null) return null; candidate=e;
        }
        return candidate;
    }

    public static void renderParsed(String profileId, String id, String sourceHud, String fingerprint, int line, String name, boolean movable, Runnable draw) {
        MCH_HudLayoutProfile p = renderProfile(profileId, "assets/mcheli/hud/" + sourceHud + ".txt");
        MCH_HudLayoutElement e = find(p,id,sourceHud,fingerprint);
        if(e == null) { e=new MCH_HudLayoutElement(); e.id=id; e.sourceHud=sourceHud; e.sourceLine=line; e.fingerprint=fingerprint==null?"":fingerprint; p.elements.put(id,e); }
        e.id=id; e.displayName=name; e.movable=movable;
        renderScope(e, draw);
    }
    public static void renderBuiltin(String context, String id, Runnable draw) {
        String profileId="builtin:"+safeId(context); MCH_HudLayoutProfile p=renderProfile(profileId,"Java-rendered HUD groups");
        MCH_HudLayoutElement e=p.elements.get(id);
        if(e==null) { e=new MCH_HudLayoutElement(); e.id=id; e.fingerprint="builtin:"+id; e.groupId=id; p.elements.put(id,e); }
        e.id=id; e.displayName=displayName(id); e.movable=true; renderScope(e,draw);
    }
    private static void renderScope(MCH_HudLayoutElement e, Runnable draw) {
        boolean capture=editorFrame && e.movable; Scope scope=new Scope(e); if(capture) { if(!FRAME.contains(e)) e.bounds=null; SCOPES.push(scope); }
        GL11.glPushMatrix(); try { GL11.glTranslated(e.offsetX,e.offsetY,0); draw.run(); }
        finally { GL11.glPopMatrix(); if(capture) { SCOPES.pop(); if(e.bounds!=null && !FRAME.contains(e)) FRAME.add(e); } }
    }
    public static void capture(double left,double top,double right,double bottom) {
        if(!editorFrame || SCOPES.isEmpty()) return;
        int ox=0,oy=0; for(Scope s:SCOPES){ox+=s.x;oy+=s.y;}
        MCH_HudLayoutBounds b=new MCH_HudLayoutBounds(left+ox,top+oy,right+ox,bottom+oy);
        for(Scope s:SCOPES) { if(s.element.bounds==null) s.element.bounds=new MCH_HudLayoutBounds(b.left,b.top,b.right,b.bottom); else s.element.bounds.include(b); }
    }
    public static void captureLine(double[] line) { if(line==null||line.length<2)return; double l=line[0],r=l,t=line[1],b=t; for(int i=2;i+1<line.length;i+=2){l=Math.min(l,line[i]);r=Math.max(r,line[i]);t=Math.min(t,line[i+1]);b=Math.max(b,line[i+1]);} capture(l,t,r,b); }
    private static String displayName(String id) { int dot=id.lastIndexOf('.'); String s=dot>=0?id.substring(dot+1):id; String[] words=s.replace('_',' ').split(" "); StringBuilder b=new StringBuilder(); for(String w:words) if(w.length()>0)b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' '); return b.toString().trim(); }

    public static synchronized MCH_HudLayoutProfile profile(String id,String source){MCH_HudLayoutProfile p=PROFILES.get(id);if(p==null){p=new MCH_HudLayoutProfile();p.profileId=id;p.source=source;PROFILES.put(id,p);}return p;}
    public static synchronized List<MCH_HudLayoutProfile> profiles(){return Collections.unmodifiableList(new ArrayList<MCH_HudLayoutProfile>(PROFILES.values()));}
    public static synchronized boolean save(MCH_HudLayoutProfile working){if(working==null)return false;File dir=new File(baseDirectory(),working.profileId.startsWith("hud:")?"hud":"builtin");if(!dir.isDirectory()&&!dir.mkdirs())return false;File file=new File(dir,safeId(working.profileId)+".json"),temp=new File(dir,file.getName()+".tmp");try{BufferedWriter out=new BufferedWriter(new FileWriter(temp));try{GSON.toJson(working,out);}finally{out.close();}try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException ex){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}PROFILES.put(working.profileId,working.copy());return true;}catch(Exception e){MCH_Lib.Log("Failed to write HUD layout %s: %s",file,e.toString());if(temp.exists())temp.delete();return false;}}
    private static void loadDirectory(File dir){File[] files=dir.listFiles();if(files==null)return;for(File file:files)if(file.isFile()&&file.getName().endsWith(".json"))try{BufferedReader in=new BufferedReader(new FileReader(file));MCH_HudLayoutProfile p;try{p=GSON.fromJson(in,MCH_HudLayoutProfile.class);}finally{in.close();}if(p==null||p.schemaVersion<1||p.profileId==null)throw new IOException("invalid layout schema");if(p.elements==null)p.elements=new LinkedHashMap<String,MCH_HudLayoutElement>();PROFILES.put(p.profileId,p);}catch(Exception e){File broken=new File(file.getParentFile(),file.getName()+".broken-"+System.currentTimeMillis());if(!file.renameTo(broken))MCH_Lib.Log("Could not preserve malformed HUD layout %s",file);MCH_Lib.Log("Failed to read HUD layout %s: %s",file,e.toString());}}
    private static File baseDirectory(){return new File(new File(Minecraft.getMinecraft().mcDataDir,"config/mcheli"),"hud_layouts");}
    public static String safeId(String value){String s=value==null?"unknown":value.toLowerCase().replaceAll("[^a-z0-9._-]+","_");return s.length()==0?"unknown":s;}
}
