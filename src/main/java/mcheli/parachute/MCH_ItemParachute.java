package mcheli.parachute;

import mcheli.wrapper.W_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemParachute extends W_Item {

   public MCH_ItemParachute(int par1) {
      super(par1);
      super.maxStackSize = 1;
   }

   public ItemStack onItemRightClick(ItemStack item, World world, EntityPlayer player) {
      if(!world.isRemote && player.ridingEntity == null && !player.onGround) {
         double x = player.posX + 0.5D;
         double y = player.posY + 3.5D;
         double z = player.posZ + 0.5D;
         MCH_EntityParachute entity = new MCH_EntityParachute(world, x, y, z);
         entity.rotationYaw = player.rotationYaw;
         entity.motionX = player.motionX;
         entity.motionY = player.motionY;
         entity.motionZ = player.motionZ;
         entity.fallDistance = player.fallDistance;
         player.fallDistance = 0.0F;
         entity.user = player;
         entity.setType(1);
         world.spawnEntityInWorld(entity);
      }

      if(!player.capabilities.isCreativeMode) {
         --item.stackSize;
      }

      return item;
   }
}
