package mcheli.hud.layout;

import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_HudLayoutProfile {
    public int schemaVersion = 1;
    public String profileId = "";
    public String source = "";
    public Map<String, MCH_HudLayoutElement> elements = new LinkedHashMap<String, MCH_HudLayoutElement>();

    public MCH_HudLayoutProfile copy() {
        MCH_HudLayoutProfile p = new MCH_HudLayoutProfile();
        p.schemaVersion = schemaVersion; p.profileId = profileId; p.source = source;
        for(Map.Entry<String, MCH_HudLayoutElement> e : elements.entrySet()) p.elements.put(e.getKey(), e.getValue().copy());
        return p;
    }
}
