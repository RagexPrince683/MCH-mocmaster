package mcheli.particles;

import mcheli.MCH_Lib;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class MCH_EntityParticleMarkPoint extends MCH_EntityParticleBase {

   final Team taem;


   public MCH_EntityParticleMarkPoint(World par1World, double x, double y, double z, Team team) {
      super(par1World, x, y, z, 0.0D, 0.0D, 0.0D);
      this.setParticleMaxAge(30);
      this.taem = team;
   }

   public void onUpdate() {
      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
      if(player == null) {
         this.setDead();
      } else if(player.getTeam() == null && this.taem != null) {
         this.setDead();
      } else if(player.getTeam() != null && !player.isOnTeam(this.taem)) {
         this.setDead();
      }

   }

   public void setDead() {
      super.setDead();
      MCH_Lib.DbgLog(true, "MCH_EntityParticleMarkPoint.setDead : " + this, new Object[0]);
   }

   public int getFXLayer() {
      return 3;
   }

   public void renderParticle(Tessellator par1Tessellator, float par2, float par3, float par4, float par5, float par6, float par7) {
      //System.out.println("x " +this.posX+ " z " +this.posZ);
	   GL11.glPushMatrix();
      Minecraft mc = Minecraft.getMinecraft();
      EntityClientPlayerMP player = mc.thePlayer;
      if(player != null) {
         double ix = EntityFX.interpPosX;
         double iy = EntityFX.interpPosY;
         double iz = EntityFX.interpPosZ;
         if(mc.gameSettings.thirdPersonView > 0 && mc.renderViewEntity != null) {
            EntityLivingBase px = mc.renderViewEntity;
            double dist = (double)W_Reflection.getThirdPersonDistance();
            float yaw = mc.gameSettings.thirdPersonView != 2?-px.rotationYaw:-px.rotationYaw;
            float pz = mc.gameSettings.thirdPersonView != 2?-px.rotationPitch:-px.rotationPitch;
            Vec3 v = MCH_Lib.RotVec3(0.0D, 0.0D, -dist, yaw, pz);
            if(mc.gameSettings.thirdPersonView == 2) {
               v.xCoord = -v.xCoord;
               v.yCoord = -v.yCoord;
               v.zCoord = -v.zCoord;
            }

            Vec3 scale = Vec3.createVectorHelper(px.posX, px.posY + (double)px.getEyeHeight(), px.posZ);
            MovingObjectPosition mop = mc.renderViewEntity.worldObj.rayTraceBlocks(scale.addVector(0.0D, 0.0D, 0.0D), scale.addVector(v.xCoord, v.yCoord, v.zCoord));
            double block_dist = dist;
            if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
               block_dist = scale.distanceTo(mop.hitVec) - 0.4D;
               if(block_dist < 0.0D) {
                  block_dist = 0.0D;
               }
            }

            GL11.glTranslated(v.xCoord * (block_dist / dist), v.yCoord * (block_dist / dist), v.zCoord * (block_dist / dist));
            ix += v.xCoord * (block_dist / dist);
            iy += v.yCoord * (block_dist / dist);
            iz += v.zCoord * (block_dist / dist);
         }

         double px1 = (double)((float)(super.prevPosX + (super.posX - super.prevPosX) * (double)par2 - ix));
         double py = (double)((float)(super.prevPosY + (super.posY - super.prevPosY) * (double)par2 - iy));
         double pz1 = (double)((float)(super.prevPosZ + (super.posZ - super.prevPosZ) * (double)par2 - iz));
         double scale1 = Math.sqrt(px1 * px1 + py * py + pz1 * pz1) / 10.0D;
         if(scale1 < 1.0D) {
            scale1 = 1.0D;
         }

         MCH_GuiTargetMarker.addMarkEntityPos(100, this, px1 / scale1, py / scale1, pz1 / scale1, false);
         GL11.glPopMatrix();
      }
   }
}
