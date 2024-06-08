package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.throwable.MCH_EntityThrowable;
import mcheli.throwable.MCH_ItemThrowable;
import mcheli.throwable.MCH_ThrowableInfo;
import mcheli.throwable.MCH_ThrowableInfoManager;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityDispensedItem extends MCH_EntityBaseBullet {

   public MCH_EntityDispensedItem(World par1World) {
      super(par1World);
   }

   public MCH_EntityDispensedItem(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize);
      }

      if(!super.worldObj.isRemote && this.getInfo() != null) {
         if(super.acceleration < 1.0E-4D) {
            super.motionX *= 0.999D;
            super.motionZ *= 0.999D;
         }

         if(this.isInWater()) {
            super.motionX *= (double)this.getInfo().velocityInWater;
            super.motionY *= (double)this.getInfo().velocityInWater;
            super.motionZ *= (double)this.getInfo().velocityInWater;
         }
      }

      this.onUpdateBomblet();
   }

   public void onImpact(MovingObjectPosition m, float damageFactor) {
      if(!super.worldObj.isRemote) {
         super.boundingBox.maxY += 2000.0D;
         super.boundingBox.minY += 2000.0D;
         EntityPlayer player = null;
         Item item = null;
         int itemDamage = 0;
         if(m != null && this.getInfo() != null) {
            if(super.shootingAircraft instanceof EntityPlayer) {
               player = (EntityPlayer)super.shootingAircraft;
            }

            if(super.shootingEntity instanceof EntityPlayer) {
               player = (EntityPlayer)super.shootingEntity;
            }

            item = this.getInfo().dispenseItem;
            itemDamage = this.getInfo().dispenseDamege;
         }

         if(player != null && !player.isDead && item != null) {
            MCH_DummyEntityPlayer dummyPlayer = new MCH_DummyEntityPlayer(super.worldObj, player);
            dummyPlayer.rotationPitch = 90.0F;
            int RNG = this.getInfo().dispenseRange - 1;

            for(int x = -RNG; x <= RNG; ++x) {
               for(int y = -RNG; y <= RNG; ++y) {
                  if(y >= 0 && y < 256) {
                     for(int z = -RNG; z <= RNG; ++z) {
                        int dist = x * x + y * y + z * z;
                        if(dist <= RNG * RNG) {
                           if((double)dist <= 0.5D * (double)RNG * (double)RNG) {
                              this.useItemToBlock(m.blockX + x, m.blockY + y, m.blockZ + z, item, itemDamage, dummyPlayer);
                           } else if(super.rand.nextInt(2) == 0) {
                              this.useItemToBlock(m.blockX + x, m.blockY + y, m.blockZ + z, item, itemDamage, dummyPlayer);
                           }
                        }
                     }
                  }
               }
            }
         }

         this.setDead();
      }

   }

   private void useItemToBlock(int x, int y, int z, Item item, int itemDamage, EntityPlayer dummyPlayer) {
      dummyPlayer.posX = (double)x + 0.5D;
      dummyPlayer.posY = (double)y + 2.5D;
      dummyPlayer.posZ = (double)z + 0.5D;
      dummyPlayer.rotationYaw = (float)super.rand.nextInt(360);
      Block block = W_WorldFunc.getBlock(super.worldObj, x, y, z);
      Material blockMat = W_WorldFunc.getBlockMaterial(super.worldObj, x, y, z);
      if(block != Blocks.air && blockMat != Material.air) {
         if(item == W_Item.getItemByName("water_bucket")) {
            MCH_Config var10000 = MCH_MOD.config;
            if(MCH_Config.Collision_DestroyBlock.prmBool) {
               if(blockMat == Material.fire) {
                  super.worldObj.setBlockToAir(x, y, z);
               } else if(blockMat == Material.lava) {
                  int metadata = super.worldObj.getBlockMetadata(x, y, z);
                  if(metadata == 0) {
                     W_WorldFunc.setBlock(super.worldObj, x, y, z, Blocks.obsidian);
                  } else if(metadata <= 4) {
                     W_WorldFunc.setBlock(super.worldObj, x, y, z, Blocks.cobblestone);
                  }
               }
            }
         }else if(item instanceof MCH_ItemThrowable){
            MCH_ItemThrowable throwable = ((MCH_ItemThrowable)item);
            MCH_EntityThrowable entity = new MCH_EntityThrowable(worldObj, dummyPlayer, 0);
            MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(item);
            info.delayFuse = 0;
            entity.setInfo(info);
            worldObj.spawnEntityInWorld(entity);
         } else if(!item.onItemUseFirst(new ItemStack(item, 1, itemDamage), dummyPlayer, super.worldObj, x, y, z, 1, (float)x, (float)y, (float)z) && !item.onItemUse(new ItemStack(item, 1, itemDamage), dummyPlayer, super.worldObj, x, y, z, 1, (float)x, (float)y, (float)z)) {
            item.onItemRightClick(new ItemStack(item, 1, itemDamage), super.worldObj, dummyPlayer);
         }
      }

   }

   public void sprinkleBomblet() {
      if(!super.worldObj.isRemote) {
         MCH_EntityDispensedItem e = new MCH_EntityDispensedItem(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float)super.rand.nextInt(360), 0.0F, super.acceleration);
         e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
         e.setName(this.getName());
         float MOTION = 1.0F;
         float RANDOM = this.getInfo().bombletDiff;
         e.motionX = super.motionX * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.motionY = super.motionY * 1.0D / 2.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
         e.motionZ = super.motionZ * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.setBomblet();
         super.worldObj.spawnEntityInWorld(e);
      }

   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Bomb;
   }
}
