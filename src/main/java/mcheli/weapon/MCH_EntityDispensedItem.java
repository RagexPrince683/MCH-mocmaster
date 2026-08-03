package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.MCH_HBMUtil;
import mcheli.MCH_Lib;
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
   }

   public MCH_EntityDispensedItem(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();

      if(this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize);
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
         MCH_WeaponInfo weaponInfo = this.getInfo();
         if(weaponInfo != null && weaponInfo.chemYield > 0) {
            MCH_HBMUtil.ExplosionChaos_spawnChlorine(super.worldObj, posX, posY + 0.5, posZ, this.getInfo().chemYield, this.getInfo().chemSpeed, this.getInfo().chemType);
         }

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

            item = MCH_WeaponInfoManager.resolveDispenseItem(weaponInfo);
            itemDamage = weaponInfo.dispenseDamege;
         }

         boolean directBlockPayload = item != null && weaponInfo != null
               && isHbmMine(weaponInfo.dispenseItemName);
         if(item != null && (directBlockPayload || player != null && !player.isDead)) {
            MCH_DummyEntityPlayer dummyPlayer = player != null && !player.isDead
                  ? new MCH_DummyEntityPlayer(super.worldObj, player) : null;
            if(dummyPlayer != null) dummyPlayer.rotationPitch = 90.0F;
            int RNG = this.getInfo().dispenseRange - 1;

            for(int x = -RNG; x <= RNG; ++x) {
               for(int y = -RNG; y <= RNG; ++y) {
                  int targetY = m.blockY + y;
                  if(targetY >= 0 && targetY < 256) {
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
      }
   }

   private void useItemToBlock(int x, int y, int z, Item item, int itemDamage, EntityPlayer dummyPlayer) {
      Block block = W_WorldFunc.getBlock(super.worldObj, x, y, z);
      Material blockMat = W_WorldFunc.getBlockMaterial(super.worldObj, x, y, z);

      if (block != Blocks.air && blockMat != Material.air) {
         if (this.placeHbmMineBlock(x, y, z, item, itemDamage)) return;
         if (dummyPlayer == null) return;

         dummyPlayer.posX = x + 0.5D;
         dummyPlayer.posY = y + 2.5D;
         dummyPlayer.posZ = z + 0.5D;
         dummyPlayer.rotationYaw = super.rand.nextInt(360);

         if (item == W_Item.getItemByName("water_bucket")) {
            if (MCH_MOD.config != null && MCH_MOD.config.Collision_DestroyBlock.prmBool) {
               if (blockMat == Material.fire) {
                  super.worldObj.setBlockToAir(x, y, z);
               } else if (blockMat == Material.lava) {
                  int metadata = super.worldObj.getBlockMetadata(x, y, z);
                  if (metadata == 0) {
                     W_WorldFunc.setBlock(super.worldObj, x, y, z, Blocks.obsidian);
                  } else if (metadata <= 4) {
                     W_WorldFunc.setBlock(super.worldObj, x, y, z, Blocks.cobblestone);
                  }
               }
            }
         } else if (item instanceof MCH_ItemThrowable) {
            MCH_EntityThrowable entity = new MCH_EntityThrowable(worldObj, dummyPlayer, 0);
            MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(item);
            if(info != null) {
               info.delayFuse = 0;
               entity.setInfo(info);
               worldObj.spawnEntityInWorld(entity);
            }
         } else {
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
               MCH_WeaponInfo info = this.getInfo();
               MCH_Lib.Log("Unexpected dispenser item failure for weapon '%s', item '%s': %s",
                     new Object[]{info != null ? info.name : this.getName(),
                           info != null ? info.dispenseItemName : W_Item.getNameForItem(item), e.toString()});
            }
         }
      }
   }

   private boolean placeHbmMineBlock(int x, int y, int z, Item item, int itemDamage) {
      MCH_WeaponInfo info = this.getInfo();
      String itemName = info != null ? info.dispenseItemName : W_Item.getNameForItem(item);
      if(!isHbmMine(itemName)) {
         return false;
      }

      Block mineBlock = Block.getBlockFromItem(item);
      if(mineBlock == null || mineBlock == Blocks.air) {
         mineBlock = (Block)Block.blockRegistry.getObject(itemName);
      }

      int placeY = y + 1;
      if(mineBlock == null || mineBlock == Blocks.air || y < 0 || y >= 255 || placeY >= 256
            || super.worldObj.isAirBlock(x, y, z) || !super.worldObj.isAirBlock(x, placeY, z)
            || !mineBlock.canPlaceBlockAt(super.worldObj, x, placeY, z)) {
         return false;
      }

      return super.worldObj.setBlock(x, placeY, z, mineBlock, itemDamage, 3);
   }

   private static boolean isHbmMine(String registryName) {
      return registryName != null && registryName.startsWith("hbm:tile.mine");
   }

   public void sprinkleBomblet() {

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
      }
   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Bomb;
   }
}
