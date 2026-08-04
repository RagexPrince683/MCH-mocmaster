package mcheli.hud.layout;

import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_HudLayoutProfile {
    public int schemaVersion = 2;
    public String profileId = "";
    public String source = "";
    public Map<String, MCH_HudLayoutElement> elements = new LinkedHashMap<String, MCH_HudLayoutElement>();
    private transient Map<String, State> originals;
    private static final class State { final int x,y,scale; State(MCH_HudLayoutElement e){x=e.offsetX;y=e.offsetY;scale=Float.floatToIntBits(MCH_HudLayoutElement.normalizeScale(e.scale));} }

    public void captureOriginalOffsets() {
        originals = new LinkedHashMap<String, State>();
        for(Map.Entry<String, MCH_HudLayoutElement> entry : elements.entrySet()) originals.put(entry.getKey(), new State(entry.getValue()));
    }
    public boolean isDirty() {
        if(originals == null || originals.size() != elements.size()) return true;
        for(Map.Entry<String, MCH_HudLayoutElement> entry : elements.entrySet()) { State s=originals.get(entry.getKey()); MCH_HudLayoutElement e=entry.getValue(); if(s==null||s.x!=e.offsetX||s.y!=e.offsetY||s.scale!=Float.floatToIntBits(MCH_HudLayoutElement.normalizeScale(e.scale))) return true; }
        return false;
    }

    public MCH_HudLayoutProfile copy() {
        MCH_HudLayoutProfile p = new MCH_HudLayoutProfile();
        p.schemaVersion = schemaVersion; p.profileId = profileId; p.source = source;
        for(Map.Entry<String, MCH_HudLayoutElement> e : elements.entrySet()) p.elements.put(e.getKey(), e.getValue().copy());
        return p;
    }
}
