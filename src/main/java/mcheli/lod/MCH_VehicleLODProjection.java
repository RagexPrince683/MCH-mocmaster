package mcheli.lod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import mcheli.MCH_ActiveRenderInfoHolder;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Projection information shared by tracked and snapshot vehicle LOD rendering. */
@SideOnly(Side.CLIENT)
public final class MCH_VehicleLODProjection {
    private MCH_VehicleLODProjection() {
    }

    /** Client-only reusable state, initialized only when a render capture is requested. */
    private static final class CaptureBuffers {
        private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
        private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);

        private CaptureBuffers() {
        }
    }

    /** Captures the projection which is active at the world-render call site. */
    public static Context capture(Minecraft mc, double realDistance) {
        float[] projection = new float[16];
        boolean valid = false;
        try {
            CaptureBuffers.PROJECTION.clear();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, CaptureBuffers.PROJECTION);
            valid = copyProjection(CaptureBuffers.PROJECTION, projection);
        } catch (RuntimeException ignored) {
            // A headless/incomplete OpenGL implementation may reject glGet.
        }
        if (!valid) {
            valid = copyProjection(MCH_ActiveRenderInfoHolder.projection, projection);
        }

        int viewportHeight = 0;
        try {
            CaptureBuffers.VIEWPORT.clear();
            GL11.glGetInteger(GL11.GL_VIEWPORT, CaptureBuffers.VIEWPORT);
            viewportHeight = copyViewportHeight(CaptureBuffers.VIEWPORT);
        } catch (RuntimeException ignored) {
        }
        if (viewportHeight <= 0) viewportHeight = copyViewportHeight(MCH_ActiveRenderInfoHolder.viewport);
        if (viewportHeight <= 0) viewportHeight = Math.max(1, mc.displayHeight);
        double fallbackFar = Math.max(64.0D, (double)mc.gameSettings.renderDistanceChunks * 16.0D);
        return create(projection, valid, viewportHeight, fallbackFar, realDistance);
    }

    /** Pure factory kept independent of an OpenGL context for regression tests. */
    public static Context create(float[] projection, boolean validProjection, int viewportHeight,
        double fallbackFarPlane, double realDistance) {
        float[] matrix = new float[16];
        boolean valid = validProjection && copyProjection(projection, matrix);
        double fallback = MCH_VehicleLODVisibility.positive(fallbackFarPlane, 256.0D);
        double far = valid ? MCH_VehicleLODVisibility.projectionFarPlane(matrix[10], matrix[14], fallback) : fallback;
        double safe = MCH_VehicleLODVisibility.safeProxyDepth(far);
        return new Context(matrix, valid, Math.max(1, viewportHeight), far, safe,
            MCH_VehicleLODVisibility.depthScale(realDistance, safe));
    }

    static boolean copyProjection(FloatBuffer source, float[] destination) {
        if (source == null || destination == null || destination.length < 16 || source.capacity() < 16) return false;
        for (int i = 0; i < 16; ++i) destination[i] = source.get(i);
        return isValidProjection(destination);
    }

    static boolean copyProjection(float[] source, float[] destination) {
        if (source == null || destination == null || source.length < 16 || destination.length < 16) return false;
        System.arraycopy(source, 0, destination, 0, 16);
        return isValidProjection(destination);
    }

    private static boolean isValidProjection(float[] matrix) {
        for (int i = 0; i < 16; ++i) if (Float.isNaN(matrix[i]) || Float.isInfinite(matrix[i])) return false;
        return Math.abs(matrix[0]) > 1.0E-6F && Math.abs(matrix[5]) > 1.0E-6F
            && Math.abs(matrix[11]) > 1.0E-6F;
    }

    static int copyViewportHeight(IntBuffer viewport) {
        return viewport != null && viewport.capacity() >= 4 ? viewport.get(3) : 0;
    }

    public static final class Context {
        public final float[] projection;
        public final boolean validProjection;
        public final int viewportHeight;
        public final float projectionYScale;
        public final double farPlane;
        public final double safeProxyDepth;
        public final double depthScale;

        private Context(float[] projection, boolean validProjection, int viewportHeight, double farPlane,
            double safeProxyDepth, double depthScale) {
            this.projection = projection;
            this.validProjection = validProjection;
            this.viewportHeight = viewportHeight;
            this.projectionYScale = validProjection ? projection[5] : 1.0F;
            this.farPlane = farPlane;
            this.safeProxyDepth = safeProxyDepth;
            this.depthScale = depthScale;
        }
    }
}
