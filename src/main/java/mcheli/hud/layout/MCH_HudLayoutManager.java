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
        final MCH_HudLayoutElement element; final int x, y; final double scale, pivotX, pivotY;
        Scope(MCH_HudLayoutElement element) { this.element = element; this.x = element.offsetX; this.y = element.offsetY; this.scale=MCH_HudLayoutElement.normalizeScale(element.scale); this.pivotX=element.hasFramePivot?element.framePivotX:element.geometry==null?0.0D:element.geometry.centerX(); this.pivotY=element.hasFramePivot?element.framePivotY:element.geometry==null?0.0D:element.geometry.centerY(); }
        Scope(MCH_HudLayoutElement element,double pivotX,double pivotY) { this.element=element; this.x=element.offsetX; this.y=element.offsetY; this.scale=MCH_HudLayoutElement.normalizeScale(element.scale); this.pivotX=pivotX; this.pivotY=pivotY; }
        double tx(double value){return x+pivotX+(value-pivotX)*scale;}
        double ty(double value){return y+pivotY+(value-pivotY)*scale;}
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
        e.id=id; e.profileId=profileId; e.displayName=name; e.movable=movable; e.scale=MCH_HudLayoutElement.normalizeScale(e.scale);
        renderScope(e, draw);
    }
    public static void renderBuiltin(String context, String id, Runnable draw) {
        renderBuiltin(context, id, displayName(id), Double.NaN, Double.NaN, draw);
    }
    public static void renderBuiltin(String context, String id, String name, double pivotX, double pivotY, Runnable draw) {
        String profileId="builtin:"+safeId(context); MCH_HudLayoutProfile p=renderProfile(profileId,"Java-rendered HUD groups");
        MCH_HudLayoutElement e=p.elements.get(id);
        if(e==null) { e=new MCH_HudLayoutElement(); e.id=id; e.fingerprint="builtin:"+id; e.groupId=id; p.elements.put(id,e); }
        e.id=id; e.profileId=profileId; e.displayName=name; e.movable=true; e.scale=MCH_HudLayoutElement.normalizeScale(e.scale);
        renderScope(e, draw, pivotX, pivotY);
    }
    private static void renderScope(MCH_HudLayoutElement e, Runnable draw) {
        renderScope(e, draw, Double.NaN, Double.NaN);
    }
    private static void renderScope(MCH_HudLayoutElement e, Runnable draw, double pivotX, double pivotY) {
        boolean capture=editorFrame && e.movable;
        if(capture && !FRAME.contains(e)) { e.hasFramePivot=e.geometry!=null; if(e.hasFramePivot){e.framePivotX=e.geometry.centerX();e.framePivotY=e.geometry.centerY();} e.bounds=null; e.geometry=null; }
        Scope scope=new Scope(e); if(!Double.isNaN(pivotX)&&!Double.isNaN(pivotY)){scope=new Scope(e,pivotX,pivotY);} if(capture) { SCOPES.push(scope); }
        GL11.glPushMatrix(); try { GL11.glTranslated(scope.x+scope.pivotX,scope.y+scope.pivotY,0); if(scope.scale!=1.0D) GL11.glScaled(scope.scale,scope.scale,1); GL11.glTranslated(-scope.pivotX,-scope.pivotY,0); draw.run(); }
        finally { GL11.glPopMatrix(); if(capture) { SCOPES.pop(); if(e.bounds!=null && !FRAME.contains(e)) FRAME.add(e); } }
    }
    public static void capture(double left,double top,double right,double bottom) {
        if(!editorFrame || SCOPES.isEmpty()) return;
        double[] xs={left,right,right,left}, ys={top,top,bottom,bottom};
        int level=0; for(Scope owner:SCOPES) {
            // At this point the coordinates are in owner's local space: retain that geometry for its next stable pivot.
            MCH_HudLayoutBounds local=bounds(xs,ys); if(owner.element.geometry==null) owner.element.geometry=local; else owner.element.geometry.include(local);
            int i=0; for(Scope transform:SCOPES) { if(i++<level) continue; for(int c=0;c<4;c++){xs[c]=transform.tx(xs[c]);ys[c]=transform.ty(ys[c]);} }
            MCH_HudLayoutBounds screen=bounds(xs,ys); if(owner.element.bounds==null) owner.element.bounds=screen; else owner.element.bounds.include(screen);
            // Rebuild raw corners for the next owner, transformed only by scopes below it.
            xs=new double[]{left,right,right,left}; ys=new double[]{top,top,bottom,bottom}; level++;
            int applied=0; for(Scope transform:SCOPES){if(applied++>=level-1)break;for(int c=0;c<4;c++){xs[c]=transform.tx(xs[c]);ys[c]=transform.ty(ys[c]);}}
        }
    }
    private static MCH_HudLayoutBounds bounds(double[] x,double[] y){double l=x[0],r=x[0],t=y[0],b=y[0];for(int i=1;i<4;i++){l=Math.min(l,x[i]);r=Math.max(r,x[i]);t=Math.min(t,y[i]);b=Math.max(b,y[i]);}return new MCH_HudLayoutBounds(l,t,r,b);}
    public static void captureLine(double[] line) { if(line==null||line.length<2)return; double l=line[0],r=l,t=line[1],b=t; for(int i=2;i+1<line.length;i+=2){l=Math.min(l,line[i]);r=Math.max(r,line[i]);t=Math.min(t,line[i+1]);b=Math.max(b,line[i+1]);} capture(l,t,r,b); }
    private static String displayName(String id) { int dot=id.lastIndexOf('.'); String s=dot>=0?id.substring(dot+1):id; String[] words=s.replace('_',' ').split(" "); StringBuilder b=new StringBuilder(); for(String w:words) if(w.length()>0)b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' '); return b.toString().trim(); }

    public static synchronized MCH_HudLayoutProfile profile(String id,String source){MCH_HudLayoutProfile p=PROFILES.get(id);if(p==null){p=new MCH_HudLayoutProfile();p.profileId=id;p.source=source;PROFILES.put(id,p);}return p;}
    public static synchronized List<MCH_HudLayoutProfile> profiles(){return Collections.unmodifiableList(new ArrayList<MCH_HudLayoutProfile>(PROFILES.values()));}
    public static synchronized boolean save(MCH_HudLayoutProfile working){if(working==null)return false;File dir=new File(baseDirectory(),working.profileId.startsWith("hud:")?"hud":"builtin");if(!dir.isDirectory()&&!dir.mkdirs())return false;File file=new File(dir,safeId(working.profileId)+".json"),temp=new File(dir,file.getName()+".tmp");try{BufferedWriter out=new BufferedWriter(new FileWriter(temp));try{GSON.toJson(working,out);}finally{out.close();}try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException ex){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}PROFILES.put(working.profileId,working.copy());return true;}catch(Exception e){MCH_Lib.Log("Failed to write HUD layout %s: %s",file,e.toString());if(temp.exists())temp.delete();return false;}}
    private static void loadDirectory(File dir){File[] files=dir.listFiles();if(files==null)return;for(File file:files)if(file.isFile()&&file.getName().endsWith(".json"))try{BufferedReader in=new BufferedReader(new FileReader(file));MCH_HudLayoutProfile p;try{p=GSON.fromJson(in,MCH_HudLayoutProfile.class);}finally{in.close();}if(p==null||p.schemaVersion<1||p.schemaVersion>2||p.profileId==null)throw new IOException("invalid layout schema");if(p.elements==null)p.elements=new LinkedHashMap<String,MCH_HudLayoutElement>();for(Map.Entry<String,MCH_HudLayoutElement> entry:p.elements.entrySet()){MCH_HudLayoutElement e=entry.getValue();if(e==null){e=new MCH_HudLayoutElement();entry.setValue(e);}e.id=entry.getKey();e.profileId=p.profileId;e.scale=MCH_HudLayoutElement.normalizeScale(e.scale);}p.schemaVersion=2;PROFILES.put(p.profileId,p);}catch(Exception e){File broken=new File(file.getParentFile(),file.getName()+".broken-"+System.currentTimeMillis());if(!file.renameTo(broken))MCH_Lib.Log("Could not preserve malformed HUD layout %s",file);MCH_Lib.Log("Failed to read HUD layout %s: %s",file,e.toString());}}
    private static File baseDirectory(){return new File(new File(Minecraft.getMinecraft().mcDataDir,"config/mcheli"),"hud_layouts");}
    public static String safeId(String value){String s=value==null?"unknown":value.toLowerCase().replaceAll("[^a-z0-9._-]+","_");return s.length()==0?"unknown":s;}
}
