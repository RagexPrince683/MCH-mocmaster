package mcheli.hud.layout;

public final class MCH_HudLayoutContext {
    public final String profileId;
    public final String source;
    public final String seatMode;

    public MCH_HudLayoutContext(String profileId, String source, String seatMode) {
        this.profileId = profileId == null ? "builtin:common" : profileId;
        this.source = source == null ? "" : source;
        this.seatMode = seatMode == null ? "normal" : seatMode;
    }
}
