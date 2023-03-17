package mcheli.chain;

import mcheli.aircraft.MCH_EntityHitBox;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.parachute.MCH_EntityParachute;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.List;

public class MCH_ItemChain extends W_Item {

   public MCH_ItemChain(int par1) {
      super(par1);
      this.setMaxStackSize(1);
   }

   public static void interactEntity(ItemStack item, Entity entity, EntityPlayer player, World world) {
      if(!world.isRemote && entity != null && !entity.isDead) {
         if(entity instanceof EntityItem) {
            return;
         }

         if(entity instanceof MCH_EntityChain) {
            return;
         }

         if(entity instanceof MCH_EntityHitBox) {
            return;
         }

         if(entity instanceof MCH_EntitySeat) {
            return;
         }

         if(entity instanceof MCH_EntityUavStation) {
            return;
         }

         if(entity instanceof MCH_EntityParachute) {
            return;
         }

         if(W_Lib.isEntityLivingBase(entity)) {
            return;
         }

         MCH_EntityChain towingChain = getTowedEntityChain(entity);
         if(towingChain != null) {
            towingChain.setDead();
            return;
         }

         Entity entityTowed = getTowedEntity(item, world);
         if(entityTowed == null) {
            playConnectTowedEntity(entity);
            setTowedEntity(item, entity);
         } else {
            if(W_Entity.isEqual(entityTowed, entity)) {
               return;
            }

            double diff = (double)entity.getDistanceToEntity(entityTowed);
            if(diff < 2.0D || diff > 16.0D) {
               return;
            }

            MCH_EntityChain chain = new MCH_EntityChain(world, (entityTowed.posX + entity.posX) / 2.0D, (entityTowed.posY + entity.posY) / 2.0D, (entityTowed.posZ + entity.posZ) / 2.0D);
            chain.setChainLength((int)diff);
            chain.setTowEntity(entityTowed, entity);
            chain.prevPosX = chain.posX;
            chain.prevPosY = chain.posY;
            chain.prevPosZ = chain.posZ;
            world.spawnEntityInWorld(chain);
            playConnectTowingEntity(entity);
            setTowedEntity(item, (Entity)null);
         }
      }

   }

   public static void playConnectTowingEntity(Entity e) {
      W_WorldFunc.MOD_playSoundEffect(e.worldObj, e.posX, e.posY, e.posZ, "chain_ct", 1.0F, 1.0F);
   }

   public static void playConnectTowedEntity(Entity e) {
      W_WorldFunc.MOD_playSoundEffect(e.worldObj, e.posX, e.posY, e.posZ, "chain", 1.0F, 1.0F);
   }

   public void onCreated(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer) {}

   public static MCH_EntityChain getTowedEntityChain(Entity entity) {
      List list = entity.worldObj.getEntitiesWithinAABB(MCH_EntityChain.class, entity.boundingBox.expand(25.0D, 25.0D, 25.0D));
      if(list == null) {
         return null;
      } else {
         for(int i = 0; i < list.size(); ++i) {
            MCH_EntityChain chain = (MCH_EntityChain)list.get(i);
            if(chain.isTowingEntity()) {
               if(W_Entity.isEqual(chain.towEntity, entity)) {
                  return chain;
               }

               if(W_Entity.isEqual(chain.towedEntity, entity)) {
                  return chain;
               }
            }
         }

         return null;
      }
   }

   public static void setTowedEntity(ItemStack item, Entity entity) {
      NBTTagCompound nbt = item.getTagCompound();
      if(nbt == null) {
         nbt = new NBTTagCompound();
         item.setTagCompound(nbt);
      }

      if(entity != null && !entity.isDead) {
         nbt.setInteger("TowedEntityId", W_Entity.getEntityId(entity));
         nbt.setString("TowedEntityUUID", entity.getPersistentID().toString());
      } else {
         nbt.setInteger("TowedEntityId", 0);
         nbt.setString("TowedEntityUUID", "");
      }

   }

   public static Entity getTowedEntity(ItemStack item, World world) {
      NBTTagCompound nbt = item.getTagCompound();
      if(nbt == null) {
         nbt = new NBTTagCompound();
         item.setTagCompound(nbt);
      } else if(nbt.hasKey("TowedEntityId") && nbt.hasKey("TowedEntityUUID")) {
         int id = nbt.getInteger("TowedEntityId");
         String uuid = nbt.getString("TowedEntityUUID");
         Entity entity = world.getEntityByID(id);
         if(entity != null && !entity.isDead && uuid.compareTo(entity.getPersistentID().toString()) == 0) {
            return entity;
         }
      }

      return null;
   }
}
