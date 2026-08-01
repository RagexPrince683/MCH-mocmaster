package mcheli.particles;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.particles.MCH_EntityParticleBase;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class MCH_EntityParticleSmoke extends MCH_EntityParticleBase implements MCH_ISmokeParticle {

   private boolean visibleInThermal;

   public MCH_EntityParticleSmoke(World par1World, double x, double y, double z, double mx, double my, double mz) {
      super(par1World, x, y, z, mx, my, mz);
      super.particleRed = super.particleGreen = super.particleBlue = super.rand.nextFloat() * 0.3F + 0.7F;
      this.setParticleScale(super.rand.nextFloat() * 0.5F + 5.0F);
      this.setParticleMaxAge((int)(16.0D / ((double)super.rand.nextFloat() * 0.8D + 0.2D)) + 2);
      super.noClip = true;
   }

   public void setVisibleInThermal(boolean visible) {
      this.visibleInThermal = visible;
   }

   public boolean isVisibleInThermal() {
      return this.visibleInThermal;
   }

   public void onUpdate() {
      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      if(super.particleAge < super.particleMaxAge) {
         this.setParticleTextureIndex((int)(8.0D * (double)super.particleAge / (double)super.particleMaxAge));
         ++super.particleAge;
         if(super.diffusible && super.particleScale < super.particleMaxScale) {
            super.particleScale += 0.8F;
         }

         if(super.toWhite) {
            float mn = this.getMinColor();
            float mx = this.getMaxColor();
            float dist = mx - mn;
            if((double)dist > 0.2D) {
               super.particleRed += (mx - super.particleRed) * 0.016F;
               super.particleGreen += (mx - super.particleGreen) * 0.016F;
               super.particleBlue += (mx - super.particleBlue) * 0.016F;
            }
         }

         this.effectWind();
         if((double)super.particleAge / (double)super.particleMaxAge > (double)super.moutionYUpAge) {
            super.motionY += 0.02D;
         } else {
            super.motionY += (double)super.gravity;
         }

         this.moveEntity(super.motionX, super.motionY, super.motionZ);
         if(super.diffusible) {
            super.motionX *= 0.96D;
            super.motionZ *= 0.96D;
            super.motionY *= 0.96D;
         } else {
            super.motionX *= 0.9D;
            super.motionZ *= 0.9D;
         }

      } else {
         this.setDead();
      }
   }

   public float getMinColor() {
      return this.min(this.min(super.particleBlue, super.particleGreen), super.particleRed);
   }

   public float getMaxColor() {
      return this.max(this.max(super.particleBlue, super.particleGreen), super.particleRed);
   }

   public float min(float a, float b) {
      return a < b?a:b;
   }

   public float max(float a, float b) {
      return a > b?a:b;
   }

   public void effectWind() {
      if(super.isEffectedWind && (super.particleAge & 3) == 0) {
         boolean range = true;
         List list = super.worldObj.getEntitiesWithinAABB(MCH_EntityBaseVehicle.class, this.getBoundingBox().expand(15.0D, 15.0D, 15.0D));

         for(int i = 0; i < list.size(); ++i) {
            MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)list.get(i);
            if(ac.getThrottle() > 0.10000000149011612D) {
               float dist = this.getDistanceToEntity(ac);
               double vel = (23.0D - (double)dist) * 0.009999999776482582D * ac.getThrottle();
               double mx = ac.posX - super.posX;
               double mz = ac.posZ - super.posZ;
               super.motionX -= mx * vel;
               super.motionZ -= mz * vel;
            }
         }
      }

   }

   public int getFXLayer() {
      return 3;
   }

   @SideOnly(Side.CLIENT)
   public int getBrightnessForRender(float p_70070_1_) {
      double y = super.posY;
      super.posY += 3000.0D;
      int i = super.getBrightnessForRender(p_70070_1_);
      super.posY = y;
      return i;
   }

   public void renderParticle(Tessellator par1Tessellator, float par2, float par3, float par4, float par5, float par6, float par7) {
      if(!this.isInRenderRange(256.0D)) {
         return;
      }

      GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT |
         GL11.GL_LIGHTING_BIT | GL11.GL_POLYGON_BIT | GL11.GL_TEXTURE_BIT);
      MCH_ParticleTexture.bind();
      boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
      int alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
      float alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, 1.0F / 255.0F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(2896);
      GL11.glDisable(2884);
      float f6 = MCH_ParticleTexture.minU(super.particleTextureIndexX);
      float f7 = MCH_ParticleTexture.maxU(super.particleTextureIndexX);
      float f8 = MCH_ParticleTexture.minV();
      float f9 = MCH_ParticleTexture.maxV();
      float f10 = 0.1F * super.particleScale;
      float f11 = (float)(super.prevPosX + (super.posX - super.prevPosX) * (double)par2 - EntityFX.interpPosX);
      float f12 = (float)(super.prevPosY + (super.posY - super.prevPosY) * (double)par2 - EntityFX.interpPosY);
      float f13 = (float)(super.prevPosZ + (super.posZ - super.prevPosZ) * (double)par2 - EntityFX.interpPosZ);
      par1Tessellator.startDrawingQuads();
      par1Tessellator.setColorRGBA_F(super.particleRed, super.particleGreen, super.particleBlue, super.particleAlpha);
      par1Tessellator.setBrightness(this.getBrightnessForRender(par2));
      par1Tessellator.setNormal(0.0F, 1.0F, 0.0F);
      par1Tessellator.addVertexWithUV((double)(f11 - par3 * f10 - par6 * f10), (double)(f12 - par4 * f10), (double)(f13 - par5 * f10 - par7 * f10), (double)f7, (double)f9);
      par1Tessellator.addVertexWithUV((double)(f11 - par3 * f10 + par6 * f10), (double)(f12 + par4 * f10), (double)(f13 - par5 * f10 + par7 * f10), (double)f7, (double)f8);
      par1Tessellator.addVertexWithUV((double)(f11 + par3 * f10 + par6 * f10), (double)(f12 + par4 * f10), (double)(f13 + par5 * f10 + par7 * f10), (double)f6, (double)f8);
      par1Tessellator.addVertexWithUV((double)(f11 + par3 * f10 - par6 * f10), (double)(f12 - par4 * f10), (double)(f13 + par5 * f10 - par7 * f10), (double)f6, (double)f9);
      par1Tessellator.draw();
      GL11.glAlphaFunc(alphaFunction, alphaReference);
      if(!alphaTest) GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glPopAttrib();
   }
}
