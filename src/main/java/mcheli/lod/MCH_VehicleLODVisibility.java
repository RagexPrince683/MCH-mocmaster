package mcheli.lod;

/** Pure, deterministic visibility math shared by the snapshot renderer and tests. */
public final class MCH_VehicleLODVisibility {
    public static final double KOSCHMIEDER_CONSTANT = 3.912D;
    /** Design limit: one block is one metre and naked-eye visibility ends at 60 km. */
    public static final double MAX_LOD_DISTANCE = 60000.0D;
    public static final double MAX_LOD_DISTANCE_SQ = MAX_LOD_DISTANCE * MAX_LOD_DISTANCE;
    public static final double NORMAL_TRACKING_RANGE = 200.0D;
    public static final double NORMAL_TRACKING_RANGE_SQ = NORMAL_TRACKING_RANGE * NORMAL_TRACKING_RANGE;

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

    /** Preserves the vanilla horizontal chunk-watching split used by snapshot selection. */
    public static boolean shouldSendSnapshot(boolean watchedChunk, double distanceSq, double hardDistance) {
        double limit = hardDistance(hardDistance);
        return !watchedChunk && isFinite(distanceSq) && distanceSq >= 0.0D && distanceSq < limit * limit;
    }

    public static boolean insideHardRange(double dx, double dy, double dz, double hardDistance) {
        double limit = hardDistance(hardDistance);
        double distanceSq = distanceSq(dx, dy, dz);
        return isFinite(distanceSq) && distanceSq < limit * limit;
    }

    public static double distanceSq(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    public static double hardDistance(double configured) {
        return isFinite(configured) && configured > 0.0D
            ? Math.min(configured, MAX_LOD_DISTANCE) : MAX_LOD_DISTANCE;
    }

    /**
     * Extends Minecraft's tracked-entity render eligibility to the configured LOD
     * boundary. The distance supplied by Entity is already squared.
     */
    public static boolean isTrackedEntityRenderEligible(boolean lodEnabled, double distanceSq,
        double configuredHardDistance, boolean vanillaResult) {
        if (!lodEnabled) return vanillaResult;
        double hard = hardDistance(configuredHardDistance);
        return isFinite(distanceSq) && distanceSq >= 0.0D && distanceSq < hard * hard;
    }

    /** Never enlarges a distant model beyond a one-pixel apparent footprint. */
    public static double minimumFootprintScale(double projectedPixels, double configuredMinimumPixels) {
        if (!isFinite(projectedPixels) || projectedPixels <= 0.0D) return 1.0D;
        double minimum = clamp(configuredMinimumPixels, 0.0D, 1.0D);
        return minimum > projectedPixels ? minimum / projectedPixels : 1.0D;
    }

    public static double positive(double value, double fallback) {
        return isFinite(value) && value > 0.0D ? value : fallback;
    }

    public static double clamp(double value, double minimum, double maximum) {
        if (!isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
