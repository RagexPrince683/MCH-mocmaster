/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.GLAllocation
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.entity.Render
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.ResourceLocation
 *  org.lwjgl.opengl.GL11
 */
package mcheli.wrapper;

import mcheli.MCH_Config;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public abstract class W_Render
extends Render {
    private static FloatBuffer colorBuffer = GLAllocation.createDirectFloatBuffer((int)16);
    protected static final ResourceLocation TEX_DEFAULT = new ResourceLocation(W_MOD.DOMAIN, "textures/default.png");
    public int srcBlend;
    public int dstBlend;

    protected void bindTexture(String path) {
        super.bindTexture(new ResourceLocation(W_MOD.DOMAIN, path));
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEX_DEFAULT;
    }

    public static FloatBuffer setColorBuffer(float p_74521_0_, float p_74521_1_, float p_74521_2_, float p_74521_3_) {
        colorBuffer.clear();
        colorBuffer.put(p_74521_0_).put(p_74521_1_).put(p_74521_2_).put(p_74521_3_);
        colorBuffer.flip();
        return colorBuffer;
    }

    public void setCommonRenderParam(boolean smoothShading, int lighting) {
        if (smoothShading && MCH_Config.SmoothShading.prmBool) {
            GL11.glShadeModel((int)7425);
        }
        GL11.glAlphaFunc((int)516, (float)0.001f);
        GL11.glEnable((int)2884);
        int j = lighting % 65536;
        int k = lighting / 65536;
        OpenGlHelper.setLightmapTextureCoords((int)OpenGlHelper.lightmapTexUnit, (float)((float)j / 1.0f), (float)((float)k / 1.0f));
        GL11.glColor4f((float)0.75f, (float)0.75f, (float)0.75f, (float)1.0f);
        GL11.glEnable((int)3042);
        this.srcBlend = GL11.glGetInteger((int)3041);
        this.dstBlend = GL11.glGetInteger((int)3040);
        GL11.glBlendFunc((int)770, (int)771);
    }

    public void restoreCommonRenderParam() {
        GL11.glBlendFunc((int)this.srcBlend, (int)this.dstBlend);
        GL11.glDisable((int)3042);
        GL11.glShadeModel((int)7424);
    }
}

