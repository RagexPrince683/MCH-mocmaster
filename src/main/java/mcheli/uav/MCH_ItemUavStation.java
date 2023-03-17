package mcheli.uav;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_ItemUavStation extends W_Item {

   public static int UAV_STATION_KIND_NUM = 2;
   public final int UavStationKind;


   public MCH_ItemUavStation(int par1, int kind) {
      super(par1);
      super.maxStackSize = 1;
      this.UavStationKind = kind;
   }

   public MCH_EntityUavStation createUavStation(World world, double x, double y, double z, int kind) {
      MCH_EntityUavStation uavst = new MCH_EntityUavStation(world);
      uavst.setPosition(x, y + (double)uavst.yOffset, z);
      uavst.prevPosX = x;
      uavst.prevPosY = y;
      uavst.prevPosZ = z;
      uavst.setKind(kind);
      return uavst;
   }

   public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer) {
      float f = 1.0F;
      float f1 = par3EntityPlayer.prevRotationPitch + (par3EntityPlayer.rotationPitch - par3EntityPlayer.prevRotationPitch) * f;
      float f2 = par3EntityPlayer.prevRotationYaw + (par3EntityPlayer.rotationYaw - par3EntityPlayer.prevRotationYaw) * f;
      double d0 = par3EntityPlayer.prevPosX + (par3EntityPlayer.posX - par3EntityPlayer.prevPosX) * (double)f;
      double d1 = par3EntityPlayer.prevPosY + (par3EntityPlayer.posY - par3EntityPlayer.prevPosY) * (double)f + 1.62D - (double)par3EntityPlayer.yOffset;
      double d2 = par3EntityPlayer.prevPosZ + (par3EntityPlayer.posZ - par3EntityPlayer.prevPosZ) * (double)f;
      Vec3 vec3 = W_WorldFunc.getWorldVec3(par2World, d0, d1, d2);
      float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
      float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
      float f5 = -MathHelper.cos(-f1 * 0.017453292F);
      float f6 = MathHelper.sin(-f1 * 0.017453292F);
      float f7 = f4 * f5;
      float f8 = f3 * f5;
      double d3 = 5.0D;
      Vec3 vec31 = vec3.addVector((double)f7 * d3, (double)f6 * d3, (double)f8 * d3);
      MovingObjectPosition movingobjectposition = W_WorldFunc.clip(par2World, vec3, vec31, true);
      if(movingobjectposition == null) {
         return par1ItemStack;
      } else {
         Vec3 vec32 = par3EntityPlayer.getLook(f);
         boolean flag = false;
         float f9 = 1.0F;
         List list = par2World.getEntitiesWithinAABBExcludingEntity(par3EntityPlayer, par3EntityPlayer.boundingBox.addCoord(vec32.xCoord * d3, vec32.yCoord * d3, vec32.zCoord * d3).expand((double)f9, (double)f9, (double)f9));

         int i;
         for(i = 0; i < list.size(); ++i) {
            Entity j = (Entity)list.get(i);
            if(j.canBeCollidedWith()) {
               float k = j.getCollisionBorderSize();
               AxisAlignedBB entityUavSt = j.boundingBox.expand((double)k, (double)k, (double)k);
               if(entityUavSt.isVecInside(vec3)) {
                  flag = true;
               }
            }
         }

         if(flag) {
            return par1ItemStack;
         } else {
            if(W_MovingObjectPosition.isHitTypeTile(movingobjectposition)) {
               i = movingobjectposition.blockX;
               int var33 = movingobjectposition.blockY;
               int var34 = movingobjectposition.blockZ;
               MCH_EntityUavStation var35 = this.createUavStation(par2World, (double)((float)i + 0.5F), (double)((float)var33 + 1.0F), (double)((float)var34 + 0.5F), this.UavStationKind);
               int rot = (int)(MCH_Lib.getRotate360((double)par3EntityPlayer.rotationYaw) + 45.0D);
               var35.rotationYaw = (float)(rot / 90 * 90 - 180);
               var35.initUavPostion();
               if(!par2World.getCollidingBoundingBoxes(var35, var35.boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()) {
                  return par1ItemStack;
               }

               if(!par2World.isRemote) {
                  par2World.spawnEntityInWorld(var35);
               }

               if(!par3EntityPlayer.capabilities.isCreativeMode) {
                  --par1ItemStack.stackSize;
               }
            }

            return par1ItemStack;
         }
      }
   }

}
