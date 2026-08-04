package mcheli.hud.layout;

public final class MCH_HudLayoutBounds {
    public double left, top, right, bottom;

    public MCH_HudLayoutBounds(double left, double top, double right, double bottom) {
        this.left = Math.min(left, right);
        this.top = Math.min(top, bottom);
        this.right = Math.max(left, right);
        this.bottom = Math.max(top, bottom);
    }

    public boolean contains(int x, int y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    public void include(MCH_HudLayoutBounds other) {
        if(other == null) return;
        left = Math.min(left, other.left); top = Math.min(top, other.top);
        right = Math.max(right, other.right); bottom = Math.max(bottom, other.bottom);
    }

    public double centerX() { return (left + right) * 0.5D; }
    public double centerY() { return (top + bottom) * 0.5D; }
}
