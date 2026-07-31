package mcheli.lod;

/** Pure, deterministic visibility math shared by the snapshot renderer and tests. */
public final class MCH_VehicleLODVisibility {
    public static final double KOSCHMIEDER_CONSTANT = 3.912D;

    private MCH_VehicleLODVisibility() {
    }

    public static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    public static double projectionFarPlane(float element10, float element14, double fallback) {
        double denominator = (double)element10 + 1.0D;
        double far = Math.abs(denominator) > 1.0E-7D ? (double)element14 / denominator : Double.NaN;
        return isFinite(far) && far > 0.0D ? far : positive(fallback, 256.0D);
    }

    public static double safeProxyDepth(double farPlane) {
        return positive(farPlane, 256.0D) * 0.9D;
    }

    public static double depthScale(double realDistance, double safeDepth) {
        if (!isFinite(realDistance) || realDistance <= 0.0D || !isFinite(safeDepth) || safeDepth <= 0.0D
            || realDistance <= safeDepth) {
            return 1.0D;
        }
        return safeDepth / realDistance;
    }

    public static double transmission(double realDistance, double effectiveVisibilityDistance) {
        double distance = isFinite(realDistance) ? Math.max(0.0D, realDistance) : Double.MAX_VALUE;
        double visibility = positive(effectiveVisibilityDistance, 1.0D);
        double result = Math.exp(-KOSCHMIEDER_CONSTANT * distance / visibility);
        return clamp(result, 0.0D, 1.0D);
    }

    public static double thermalAlpha(double transmission, double exponent) {
        return clamp(Math.pow(clamp(transmission, 0.0D, 1.0D), clamp(exponent, 0.01D, 1.0D)), 0.0D, 1.0D);
    }

    public static double projectedPixels(double physicalSize, double realDistance, float projectionY, int viewportHeight) {
        if (!isFinite(physicalSize) || physicalSize <= 0.0D || !isFinite(realDistance) || realDistance <= 0.0D
            || Float.isNaN(projectionY) || Float.isInfinite(projectionY) || viewportHeight <= 0) {
            return 0.0D;
        }
        return physicalSize * Math.abs((double)projectionY) * (double)viewportHeight / (2.0D * realDistance);
    }

    public static double positive(double value, double fallback) {
        return isFinite(value) && value > 0.0D ? value : fallback;
    }

    public static double clamp(double value, double minimum, double maximum) {
        if (!isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
