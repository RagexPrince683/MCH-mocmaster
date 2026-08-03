package mcheli.lod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MCH_VehicleLODVisibilityTest {
    @Test
    public void extractsFarPlaneAndFallsBackForInvalidProjection() {
        float m22 = (float)(-(400.0D + 0.1D) / (400.0D - 0.1D));
        float m32 = (float)(-(2.0D * 400.0D * 0.1D) / (400.0D - 0.1D));
        assertEquals(400.0D, MCH_VehicleLODVisibility.projectionFarPlane(m22, m32, 256.0D), 0.1D);
        assertEquals(256.0D, MCH_VehicleLODVisibility.projectionFarPlane(Float.NaN, m32, 256.0D), 0.0D);
    }

    @Test
    public void compressionPreservesAngularSizeAndLeavesNearTargetsAlone() {
        assertEquals(1.0D, MCH_VehicleLODVisibility.depthScale(200.0D, 360.0D), 0.0D);
        double scale = MCH_VehicleLODVisibility.depthScale(800.0D, 360.0D);
        assertEquals(0.45D, scale, 0.0D);
        assertEquals(4.0D / 800.0D, (4.0D * scale) / (800.0D * scale), 1.0E-12D);
    }

    @Test
    public void atmosphereAndThermalFollowConfiguredContrastModel() {
        double visibility = MCH_VehicleLODVisibility.MAX_LOD_DISTANCE;
        assertEquals(1.0D, MCH_VehicleLODVisibility.transmission(0.0D, visibility), 0.0D);
        double clear = MCH_VehicleLODVisibility.transmission(visibility, visibility);
        double rain = MCH_VehicleLODVisibility.transmission(visibility, visibility * 0.70D);
        double thunder = MCH_VehicleLODVisibility.transmission(visibility, visibility * 0.45D);
        assertEquals(0.02D, clear, 0.0001D);
        assertTrue(rain < clear);
        assertTrue(thunder < rain);
        assertTrue(MCH_VehicleLODVisibility.thermalAlpha(clear, 0.35D) > clear);
    }

    @Test
    public void projectionMagnificationIncreasesPixelSize() {
        double normal = MCH_VehicleLODVisibility.projectedPixels(8.0D, 1000.0D, 1.0F, 1080);
        double magnified = MCH_VehicleLODVisibility.projectedPixels(8.0D, 1000.0D, 4.0F, 1080);
        assertEquals(normal * 4.0D, magnified, 0.0D);
    }

    @Test
    public void validationClampsInvalidValues() {
        assertEquals(60000.0D, MCH_VehicleLODVisibility.hardDistance(Double.NaN), 0.0D);
        assertEquals(60000.0D, MCH_VehicleLODVisibility.hardDistance(Double.POSITIVE_INFINITY), 0.0D);
        assertEquals(60000.0D, MCH_VehicleLODVisibility.hardDistance(-1.0D), 0.0D);
        assertEquals(60000.0D, MCH_VehicleLODVisibility.hardDistance(90000.0D), 0.0D);
        assertEquals(0.0D, MCH_VehicleLODVisibility.clamp(Double.NaN, 0.0D, 1.0D), 0.0D);
        assertEquals(1.0D, MCH_VehicleLODVisibility.clamp(2.0D, 0.0D, 1.0D), 0.0D);
    }

    @Test
    public void verticalSnapshotSelectionIgnoresHorizontalChunkWatching() {
        // Chunk watching is deliberately absent from this pure predicate: same X/Z at 400 blocks qualifies.
        assertTrue(MCH_VehicleLODVisibility.qualifiesForSnapshot(0.0D, 400.0D, 0.0D, 60000.0D));
        assertTrue(MCH_VehicleLODVisibility.qualifiesForSnapshot(0.0D, 1000.0D, 0.0D, 60000.0D));
        assertTrue(MCH_VehicleLODVisibility.qualifiesForSnapshot(0.0D, 10000.0D, 0.0D, 60000.0D));
        assertTrue(MCH_VehicleLODVisibility.qualifiesForSnapshot(0.0D, 59999.0D, 0.0D, 60000.0D));
    }

    @Test
    public void normalTrackingPreventsSnapshotDuplicate() {
        assertTrue(!MCH_VehicleLODVisibility.qualifiesForSnapshot(0.0D, 199.0D, 0.0D, 60000.0D));
        assertTrue(!MCH_VehicleLODVisibility.qualifiesForSnapshot(120.0D, 120.0D, 20.0D, 60000.0D));
    }

    @Test
    public void hardRangeIsExclusiveAndThreeDimensional() {
        assertTrue(MCH_VehicleLODVisibility.insideHardRange(59999.0D, 0.0D, 0.0D, 60000.0D));
        assertTrue(MCH_VehicleLODVisibility.qualifiesForSnapshot(30000.0D, 40000.0D, 0.0D, 60000.0D));
        assertTrue(!MCH_VehicleLODVisibility.insideHardRange(0.0D, 60000.0D, 0.0D, 60000.0D));
        assertTrue(!MCH_VehicleLODVisibility.insideHardRange(60001.0D, 0.0D, 0.0D, 60000.0D));
        assertTrue(!MCH_VehicleLODVisibility.insideHardRange(50000.0D, 40000.0D, 0.0D, 60000.0D));
    }

    @Test
    public void minimumFootprintPreservesTinyModelsWithoutExcessiveGrowth() {
        assertEquals(1.0D, MCH_VehicleLODVisibility.minimumFootprintScale(2.0D, 0.75D), 0.0D);
        assertEquals(7.5D, MCH_VehicleLODVisibility.minimumFootprintScale(0.1D, 0.75D), 0.0D);
        assertEquals(10.0D, MCH_VehicleLODVisibility.minimumFootprintScale(0.1D, 64.0D), 0.0D);
    }
}
