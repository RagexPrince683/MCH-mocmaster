package mcheli.hud.layout;

import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_HudLayoutProfile {
    public int schemaVersion = 1;
    public String profileId = "";
    public String source = "";
    public Map<String, MCH_HudLayoutElement> elements = new LinkedHashMap<String, MCH_HudLayoutElement>();
    private transient Map<String, Long> originalOffsets;

    public void captureOriginalOffsets() {
        originalOffsets = new LinkedHashMap<String, Long>();
        for(Map.Entry<String, MCH_HudLayoutElement> entry : elements.entrySet()) originalOffsets.put(entry.getKey(), pack(entry.getValue()));
    }
    public boolean isDirty() {
        if(originalOffsets == null || originalOffsets.size() != elements.size()) return true;
        for(Map.Entry<String, MCH_HudLayoutElement> entry : elements.entrySet()) if(!Long.valueOf(pack(entry.getValue())).equals(originalOffsets.get(entry.getKey()))) return true;
        return false;
    }
    private static long pack(MCH_HudLayoutElement e) { return ((long)e.offsetX << 32) ^ (e.offsetY & 0xffffffffL); }

    public MCH_HudLayoutProfile copy() {
        MCH_HudLayoutProfile p = new MCH_HudLayoutProfile();
        p.schemaVersion = schemaVersion; p.profileId = profileId; p.source = source;
        for(Map.Entry<String, MCH_HudLayoutElement> e : elements.entrySet()) p.elements.put(e.getKey(), e.getValue().copy());
        return p;
    }
}
