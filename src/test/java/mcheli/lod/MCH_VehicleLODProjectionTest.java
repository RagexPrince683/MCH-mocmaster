package mcheli.lod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MCH_VehicleLODProjectionTest {
    private static float[] perspective(double near, double far, float yScale) {
        float[] matrix = new float[16];
        matrix[0] = yScale;
        matrix[5] = yScale;
        matrix[10] = (float)(-(far + near) / (far - near));
        matrix[11] = -1.0F;
        matrix[14] = (float)(-(2.0D * far * near) / (far - near));
        return matrix;
    }

    @Test
    public void extractsValidProjectionAndCompressesToSafeDepth() {
        MCH_VehicleLODProjection.Context context = MCH_VehicleLODProjection.create(
            perspective(0.1D, 400.0D, 2.0F), true, 1080, 256.0D, 800.0D);
        assertTrue(context.validProjection);
        assertEquals(400.0D, context.farPlane, 0.1D);
        assertEquals(360.0D, context.safeProxyDepth, 0.1D);
        assertEquals(1080, context.viewportHeight);
        assertEquals(2.0F, context.projectionYScale, 0.0F);
        assertTrue(context.depthScale > 0.0D && context.depthScale < 1.0D);
        assertEquals(context.safeProxyDepth, 800.0D * context.depthScale, 1.0E-9D);
    }

    @Test
    public void invalidMatricesUseSanitizedFallbacks() {
        float[] invalid = perspective(0.1D, 400.0D, 1.0F);
        invalid[10] = Float.NaN;
        MCH_VehicleLODProjection.Context nan = MCH_VehicleLODProjection.create(
            invalid, true, 0, 512.0D, 1000.0D);
        assertFalse(nan.validProjection);
        assertEquals(512.0D, nan.farPlane, 0.0D);
        assertEquals(1, nan.viewportHeight);

        MCH_VehicleLODProjection.Context infinite = MCH_VehicleLODProjection.create(
            perspective(0.1D, Double.POSITIVE_INFINITY, 1.0F), true, 720, -1.0D, 1000.0D);
        assertFalse(infinite.validProjection);
        assertEquals(256.0D, infinite.farPlane, 0.0D);
    }

    @Test
    public void nearZeroAndInvalidDistancesAreNotCompressed() {
        float[] projection = perspective(0.1D, 400.0D, 1.0F);
        assertEquals(1.0D, MCH_VehicleLODProjection.create(projection, true, 720, 256.0D, 100.0D).depthScale, 0.0D);
        assertEquals(1.0D, MCH_VehicleLODProjection.create(projection, true, 720, 256.0D, 0.0D).depthScale, 0.0D);
        assertEquals(1.0D, MCH_VehicleLODProjection.create(projection, true, 720, 256.0D, Double.NaN).depthScale, 0.0D);
        assertEquals(1.0D, MCH_VehicleLODProjection.create(projection, true, 720, 256.0D, Double.POSITIVE_INFINITY).depthScale, 0.0D);
    }

    @Test
    public void coordinateAndModelCompressionPreserveDirectionAndAngularSize() {
        MCH_VehicleLODProjection.Context context = MCH_VehicleLODProjection.create(
            perspective(0.1D, 400.0D, 1.0F), true, 720, 256.0D, 1000.0D);
        double x = -300.0D, y = 400.0D, z = -Math.sqrt(750000.0D);
        double compressedDistance = Math.sqrt(x * x + y * y + z * z) * context.depthScale;
        assertEquals(context.safeProxyDepth, compressedDistance, 0.1D);
        assertTrue(x * context.depthScale < 0.0D);
        assertTrue(z * context.depthScale < 0.0D);
        assertEquals(8.0D / 1000.0D,
            (8.0D * context.depthScale) / compressedDistance, 1.0E-12D);
    }
}
