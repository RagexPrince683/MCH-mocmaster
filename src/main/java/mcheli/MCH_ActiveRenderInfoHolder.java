package mcheli;

import net.minecraft.client.renderer.ActiveRenderInfo;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class MCH_ActiveRenderInfoHolder {

    public static IntBuffer viewport;
    public static FloatBuffer modelview;
    public static FloatBuffer projection;
    public static FloatBuffer objectCoords;

    static {
        try {
            System.out.println("Hooking ActiveRenderInfo");

            Field viewportField = ActiveRenderInfo.class.getDeclaredField("field_74597_i");
            viewportField.setAccessible(true);
            viewport = (IntBuffer) viewportField.get(null);

            Field modelviewField = ActiveRenderInfo.class.getDeclaredField("field_74594_j");
            modelviewField.setAccessible(true);
            modelview = (FloatBuffer) modelviewField.get(null);

            Field projectionField = ActiveRenderInfo.class.getDeclaredField("field_74595_k");
            projectionField.setAccessible(true);
            projection = (FloatBuffer) projectionField.get(null);

            Field objectCoordsField = ActiveRenderInfo.class.getDeclaredField("field_74593_l");
            objectCoordsField.setAccessible(true);
            objectCoords = (FloatBuffer) objectCoordsField.get(null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
