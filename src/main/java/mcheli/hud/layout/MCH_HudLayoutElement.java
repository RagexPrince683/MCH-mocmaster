package mcheli.hud.layout;

public class MCH_HudLayoutElement {
    public enum Category { SCREEN_SPACE, VIEW_ALIGNED, WORLD_PROJECTED, FULLSCREEN_EFFECT }

    public int offsetX;
    public int offsetY;
    public String fingerprint = "";
    public transient String id = "";
    public String sourceHud = "";
    public transient String displayName = "";
    public transient String groupId = "";
    public int sourceLine;
    public transient boolean movable = true;
    public transient Category category = Category.SCREEN_SPACE;
    public transient MCH_HudLayoutBounds bounds;

    public MCH_HudLayoutElement copy() {
        MCH_HudLayoutElement e = new MCH_HudLayoutElement();
        e.offsetX = offsetX; e.offsetY = offsetY; e.fingerprint = fingerprint;
        e.id = id; e.sourceHud = sourceHud; e.displayName = displayName; e.groupId = groupId;
        e.sourceLine = sourceLine; e.movable = movable; e.category = category; e.bounds = bounds;
        return e;
    }
}
