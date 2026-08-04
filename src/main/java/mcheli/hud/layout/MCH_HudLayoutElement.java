package mcheli.hud.layout;

public class MCH_HudLayoutElement {
    public enum Category { SCREEN_SPACE, VIEW_ALIGNED, WORLD_PROJECTED, FULLSCREEN_EFFECT }

    public int offsetX;
    public int offsetY;
    public float scale = 1.0F;
    public String fingerprint = "";
    public transient String id = "";
    public transient String profileId = "";
    public String sourceHud = "";
    public transient String displayName = "";
    public transient String groupId = "";
    public int sourceLine;
    public transient boolean movable = true;
    public transient Category category = Category.SCREEN_SPACE;
    public transient MCH_HudLayoutBounds bounds;
    /** Last untransformed geometry; deliberately transient because it depends on resolution. */
    public transient MCH_HudLayoutBounds geometry;
    public transient double framePivotX;
    public transient double framePivotY;
    public transient boolean hasFramePivot;

    public static float normalizeScale(float value) {
        if(Float.isNaN(value) || Float.isInfinite(value) || value <= 0.0F) return 1.0F;
        return Math.max(0.25F, Math.min(4.0F, value));
    }

    public MCH_HudLayoutElement copy() {
        MCH_HudLayoutElement e = new MCH_HudLayoutElement();
        e.offsetX = offsetX; e.offsetY = offsetY; e.scale = normalizeScale(scale); e.fingerprint = fingerprint;
        e.id = id; e.profileId = profileId; e.sourceHud = sourceHud; e.displayName = displayName; e.groupId = groupId;
        e.sourceLine = sourceLine; e.movable = movable; e.category = category; e.bounds = bounds; e.geometry = geometry;
        return e;
    }
}
