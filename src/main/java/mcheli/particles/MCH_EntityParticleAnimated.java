package mcheli.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public abstract class MCH_EntityParticleAnimated extends MCH_EntityParticleBase {
    //new ResourceLocation("textures/entity/explosion.png");
                                             //new ResourceLocation(W_MOD.DOMAIN, "textures/particles/explosion.png");

    public int nowCount;
    protected int endCount;
    protected TextureManager theRenderEngine;
    protected float size;

    protected int numrows, numcol;

    public MCH_EntityParticleAnimated(World w, double x, double y, double z, double size, double age, double mz) {
        super(w, x, y, z, 0.0D, 0.0D, 0.0D);
        this.theRenderEngine = Minecraft.getMinecraft().renderEngine;
        this.endCount = 1 + (int)age;
        this.size = (float)size;
    }

    protected abstract ResourceLocation getTexture();

    public void renderParticle(Tessellator tessellator, float p_70539_2_, float p_70539_3_, float p_70539_4_, float p_70539_5_, float p_70539_6_, float p_70539_7_) {
        int i = (int)(((float)this.nowCount + p_70539_2_) * 15.0F / (float)this.endCount);

        float colMult = 1.0f/numcol;
        float  rowMult = 1.0f/numrows;

        if(i <= 15) {
            GL11.glEnable(3042);
            int srcBlend = GL11.glGetInteger(3041);
            int dstBlend = GL11.glGetInteger(3040);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2884);
            this.theRenderEngine.bindTexture(getTexture());
            float row = nowCount / 10;
            float column = nowCount % 10;
            //System.out.println("Row: " + row + " Column: " + column);
            float f6 = colMult*column;
            float f7 = colMult*(column+1);
            float f8 = rowMult * row;
            float f9 = rowMult*(row+1);
            double f10 = size * 3;
            float f11 = (float)(super.prevPosX + (super.posX - super.prevPosX) * (double)p_70539_2_ - EntityFX.interpPosX);
            float f12 = (float)(super.prevPosY + (super.posY - super.prevPosY) * (double)p_70539_2_ - EntityFX.interpPosY);
            float f13 = (float)(super.prevPosZ + (super.posZ - super.prevPosZ) * (double)p_70539_2_ - EntityFX.interpPosZ);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderHelper.disableStandardItemLighting();
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA_F(super.particleRed, super.particleGreen, super.particleBlue, super.particleAlpha);
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            if(nowCount > 0) {
                tessellator.setBrightness(15728880);
                //tessellator.setBrightness(15728880/nowCount);
            }else {
                //tessellator.setBrightness(15728880);
                tessellator.setBrightness(15728880);

            }
            tessellator.addVertexWithUV((double)(f11 - p_70539_3_ * f10 - p_70539_6_ * f10), (double)(f12 - p_70539_4_ * f10), (double)(f13 - p_70539_5_ * f10 - p_70539_7_ * f10), (double)f7, (double)f9);
            tessellator.addVertexWithUV((double)(f11 - p_70539_3_ * f10 + p_70539_6_ * f10), (double)(f12 + p_70539_4_ * f10), (double)(f13 - p_70539_5_ * f10 + p_70539_7_ * f10), (double)f7, (double)f8);
            tessellator.addVertexWithUV((double)(f11 + p_70539_3_ * f10 + p_70539_6_ * f10), (double)(f12 + p_70539_4_ * f10), (double)(f13 + p_70539_5_ * f10 + p_70539_7_ * f10), (double)f6, (double)f8);
            tessellator.addVertexWithUV((double)(f11 + p_70539_3_ * f10 - p_70539_6_ * f10), (double)(f12 - p_70539_4_ * f10), (double)(f13 + p_70539_5_ * f10 - p_70539_7_ * f10), (double)f6, (double)f9);
            tessellator.draw();
            GL11.glPolygonOffset(0.0F, 0.0F);
            GL11.glEnable(2896);
            GL11.glEnable(2884);
            GL11.glBlendFunc(srcBlend, dstBlend);
            GL11.glDisable(3042);
        }

    }

    public int getBrightnessForRender(float p_70070_1_) {
        return 15728880;
    }

    public void onUpdate() {
        super.prevPosX = super.posX;
        super.prevPosY = super.posY;
        super.prevPosZ = super.posZ;
        ++this.nowCount;
        this.moveEntity(super.motionX, super.motionY, super.motionZ);

        if(this.nowCount == this.endCount) {
            this.setDead();
        }


    }

    public int getFXLayer() {
        return 3;
    }

}
