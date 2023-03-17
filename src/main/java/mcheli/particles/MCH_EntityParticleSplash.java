package mcheli.particles;

import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

public class MCH_EntityParticleSplash extends MCH_EntityParticleBase {

   public MCH_EntityParticleSplash(World par1World, double x, double y, double z, double mx, double my, double mz) {
      super(par1World, x, y, z, mx, my, mz);
      super.particleRed = super.particleGreen = super.particleBlue = super.rand.nextFloat() * 0.3F + 0.7F;
      this.setParticleScale(super.rand.nextFloat() * 0.5F + 5.0F);
      this.setParticleMaxAge((int)(80.0D / ((double)super.rand.nextFloat() * 0.8D + 0.2D)) + 2);
   }

   public void onUpdate() {
      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      if(super.particleAge < super.particleMaxAge) {
         this.setParticleTextureIndex((int)(8.0D * (double)super.particleAge / (double)super.particleMaxAge));
         ++super.particleAge;
      } else {
         this.setDead();
      }

      super.motionY -= 0.05999999865889549D;
      Block block = W_WorldFunc.getBlock(super.worldObj, (int)(super.posX + 0.5D), (int)(super.posY + 0.5D), (int)(super.posZ + 0.5D));
      boolean beforeInWater = W_Block.isEqualTo(block, W_Block.getWater());
      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      block = W_WorldFunc.getBlock(super.worldObj, (int)(super.posX + 0.5D), (int)(super.posY + 0.5D), (int)(super.posZ + 0.5D));
      boolean nowInWater = W_Block.isEqualTo(block, W_Block.getWater());
      if(super.motionY < -0.6D && !beforeInWater && nowInWater) {
         double p = -super.motionY * 10.0D;

         for(int i = 0; (double)i < p; ++i) {
            super.worldObj.spawnParticle("splash", super.posX + 0.5D + (super.rand.nextDouble() - 0.5D) * 2.0D, super.posY + super.rand.nextDouble(), super.posZ + 0.5D + (super.rand.nextDouble() - 0.5D) * 2.0D, (super.rand.nextDouble() - 0.5D) * 2.0D, 4.0D, (super.rand.nextDouble() - 0.5D) * 2.0D);
            super.worldObj.spawnParticle("bubble", super.posX + 0.5D + (super.rand.nextDouble() - 0.5D) * 2.0D, super.posY - super.rand.nextDouble(), super.posZ + 0.5D + (super.rand.nextDouble() - 0.5D) * 2.0D, (super.rand.nextDouble() - 0.5D) * 2.0D, -0.5D, (super.rand.nextDouble() - 0.5D) * 2.0D);
         }
      } else if(super.onGround) {
         this.setDead();
      }

      super.motionX *= 0.9D;
      super.motionZ *= 0.9D;
   }

   public void renderParticle(Tessellator par1Tessellator, float par2, float par3, float par4, float par5, float par6, float par7) {
      W_McClient.MOD_bindTexture("textures/particles/smoke.png");
      float f6 = (float)super.particleTextureIndexX / 8.0F;
      float f7 = f6 + 0.125F;
      float f8 = 0.0F;
      float f9 = 1.0F;
      float f10 = 0.1F * super.particleScale;
      float f11 = (float)(super.prevPosX + (super.posX - super.prevPosX) * (double)par2 - EntityFX.interpPosX);
      float f12 = (float)(super.prevPosY + (super.posY - super.prevPosY) * (double)par2 - EntityFX.interpPosY);
      float f13 = (float)(super.prevPosZ + (super.posZ - super.prevPosZ) * (double)par2 - EntityFX.interpPosZ);
      float f14 = 1.0F;
      par1Tessellator.setColorRGBA_F(super.particleRed * f14, super.particleGreen * f14, super.particleBlue * f14, super.particleAlpha);
      par1Tessellator.addVertexWithUV((double)(f11 - par3 * f10 - par6 * f10), (double)(f12 - par4 * f10), (double)(f13 - par5 * f10 - par7 * f10), (double)f7, (double)f9);
      par1Tessellator.addVertexWithUV((double)(f11 - par3 * f10 + par6 * f10), (double)(f12 + par4 * f10), (double)(f13 - par5 * f10 + par7 * f10), (double)f7, (double)f8);
      par1Tessellator.addVertexWithUV((double)(f11 + par3 * f10 + par6 * f10), (double)(f12 + par4 * f10), (double)(f13 + par5 * f10 + par7 * f10), (double)f6, (double)f8);
      par1Tessellator.addVertexWithUV((double)(f11 + par3 * f10 - par6 * f10), (double)(f12 - par4 * f10), (double)(f13 + par5 * f10 - par7 * f10), (double)f6, (double)f9);
   }
}
