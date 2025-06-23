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

// [Lines unchanged before here]
public class MCH_EntityDispensedItem extends MCH_EntityBaseBullet {

   public MCH_EntityDispensedItem(World par1World) {
      super(par1World);
      System.out.println("[DispensedItem] Constructor called: World");
   }

   public MCH_EntityDispensedItem(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
      System.out.println("[DispensedItem] Constructor called: Full Params");
   }

   public void onUpdate() {
      super.onUpdate();
      System.out.println("[DispensedItem] onUpdate called");

      if(this.getInfo() != null && !this.getInfo().disableSmoke) {
         System.out.println("[DispensedItem] Spawning particles");
         this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize);
      }

      if(!super.worldObj.isRemote && this.getInfo() != null) {
         if(super.acceleration < 1.0E-4D) {
            System.out.println("[DispensedItem] Low acceleration, damping motion");
            super.motionX *= 0.999D;
            super.motionZ *= 0.999D;
         }

         if(this.isInWater()) {
            System.out.println("[DispensedItem] In water, adjusting velocity");
            super.motionX *= (double)this.getInfo().velocityInWater;
            super.motionY *= (double)this.getInfo().velocityInWater;
            super.motionZ *= (double)this.getInfo().velocityInWater;
         }
      }

      this.onUpdateBomblet();
   }

   public void onImpact(MovingObjectPosition m, float damageFactor) {
      System.out.println("[DispensedItem] onImpact called");

      if(!super.worldObj.isRemote) {
         System.out.println("[DispensedItem] Handling impact on server");
         super.boundingBox.maxY += 2000.0D;
         super.boundingBox.minY += 2000.0D;

         EntityPlayer player = null;
         Item item = null;
         int itemDamage = 0;

         if(m != null && this.getInfo() != null) {
            System.out.println("[DispensedItem] Valid impact and info");

            if(super.shootingAircraft instanceof EntityPlayer) {
               player = (EntityPlayer)super.shootingAircraft;
            }

            if(super.shootingEntity instanceof EntityPlayer) {
               player = (EntityPlayer)super.shootingEntity;
            }

            item = this.getInfo().dispenseItem;
            itemDamage = this.getInfo().dispenseDamege;
         }

         // Handle nulls and re-registration
         //if (item == null) {
         //   System.out.println("[DispensedItem] Item was null, attempting re-registration...");
         //   try {
         //      MCH_WeaponInfoManager.reload();
         //      item = this.getInfo().dispenseItem;
         //      if (item == null) {
         //         System.out.println("[DispensedItem] Still null after reload!");
         //      } else {
         //         System.out.println("[DispensedItem] Item re-registered successfully.");
         //      }
         //   } catch (Exception e) {
         //      System.out.println("[DispensedItem] Exception during reload: " + e.getMessage());
         //      e.printStackTrace();
         //   }
         //}
         //don't do that.

         if(player != null && !player.isDead && item != null) {
            System.out.println("[DispensedItem] Dispensing item to blocks");
            MCH_DummyEntityPlayer dummyPlayer = new MCH_DummyEntityPlayer(super.worldObj, player);
            dummyPlayer.rotationPitch = 90.0F;
            int RNG = this.getInfo().dispenseRange - 1;

            for(int x = -RNG; x <= RNG; ++x) {
               for(int y = -RNG; y <= RNG; ++y) {
                  if(y >= 0 && y < 256) {
                     for(int z = -RNG; z <= RNG; ++z) {
                        int dist = x * x + y * y + z * z;
                        if(dist <= RNG * RNG) {
                           if((double)dist <= 0.5D * (double)RNG * (double)RNG || super.rand.nextInt(2) == 0) {
                              this.useItemToBlock(m.blockX + x, m.blockY + y, m.blockZ + z, item, itemDamage, dummyPlayer);
                           }
                        }
                     }
                  }
               }
            }
         }

         this.setDead();
         System.out.println("[DispensedItem] Entity marked as dead");
      }
   }

   private void useItemToBlock(int x, int y, int z, Item item, int itemDamage, EntityPlayer dummyPlayer) {
      System.out.println("[DispensedItem] Attempting to use item at " + x + ", " + y + ", " + z);
      dummyPlayer.posX = x + 0.5D;
      dummyPlayer.posY = y + 2.5D;
      dummyPlayer.posZ = z + 0.5D;
      dummyPlayer.rotationYaw = super.rand.nextInt(360);

      Block block = W_WorldFunc.getBlock(super.worldObj, x, y, z);
      Material blockMat = W_WorldFunc.getBlockMaterial(super.worldObj, x, y, z);

      if (block != Blocks.air && blockMat != Material.air) {
         System.out.println("[DispensedItem] Block is not air: " + block);

         if (item == W_Item.getItemByName("water_bucket")) {
            System.out.println("[DispensedItem] Using water bucket");
            if (MCH_MOD.config != null && MCH_MOD.config.Collision_DestroyBlock.prmBool) {
               if (blockMat == Material.fire) {
                  super.worldObj.setBlockToAir(x, y, z);
                  System.out.println("[DispensedItem] Extinguished fire");
               } else if (blockMat == Material.lava) {
                  int metadata = super.worldObj.getBlockMetadata(x, y, z);
                  if (metadata == 0) {
                     W_WorldFunc.setBlock(super.worldObj, x, y, z, Blocks.obsidian);
                  } else if (metadata <= 4) {
                     W_WorldFunc.setBlock(super.worldObj, x, y, z, Blocks.cobblestone);
                  }
                  System.out.println("[DispensedItem] Converted lava");
               }
            }
         } else if (item instanceof MCH_ItemThrowable) {
            System.out.println("[DispensedItem] Spawning throwable");
            MCH_ItemThrowable throwable = (MCH_ItemThrowable)item;
            MCH_EntityThrowable entity = new MCH_EntityThrowable(worldObj, dummyPlayer, 0);
            MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(item);
            info.delayFuse = 0;
            entity.setInfo(info);
            worldObj.spawnEntityInWorld(entity);
         } else {
            System.out.println("[DispensedItem] Using generic item");
            ItemStack stack = new ItemStack(item, 1, itemDamage);
            try {
               boolean used = item.onItemUseFirst(stack, dummyPlayer, worldObj, x, y, z, 1, x, y, z);
               if (!used) {
                  used = item.onItemUse(stack, dummyPlayer, worldObj, x, y, z, 1, x, y, z);
               }
               if (!used) {
                  item.onItemRightClick(stack, worldObj, dummyPlayer);
               }
            } catch (Exception e) {
               System.err.println("[MCH] Skipped item use due to fake player crash risk: " + e.getMessage());
               e.printStackTrace();
            }
         }
      }
   }

   public void sprinkleBomblet() {
      System.out.println("[DispensedItem] sprinkleBomblet called");

      if(!super.worldObj.isRemote) {
         MCH_EntityDispensedItem e = new MCH_EntityDispensedItem(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float)super.rand.nextInt(360), 0.0F, super.acceleration);
         e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
         e.setName(this.getName());

         float RANDOM = this.getInfo().bombletDiff;
         e.motionX = super.motionX * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.motionY = super.motionY * 1.0D / 2.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
         e.motionZ = super.motionZ * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);

         e.setBomblet();
         super.worldObj.spawnEntityInWorld(e);
         System.out.println("[DispensedItem] Bomblet spawned");
      }
   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Bomb;
   }
}

