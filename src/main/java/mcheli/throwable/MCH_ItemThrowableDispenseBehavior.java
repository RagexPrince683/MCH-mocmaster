package mcheli.throwable;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class MCH_ItemThrowableDispenseBehavior extends BehaviorDefaultDispenseItem {

   public ItemStack dispenseStack(IBlockSource bs, ItemStack itemStack) {
      EnumFacing enumfacing = W_BlockDispenser.getFacing(bs.getBlockMetadata());
      double x = bs.getX() + (double)enumfacing.getFrontOffsetX() * 2.0D;
      double y = bs.getY() + (double)enumfacing.getFrontOffsetY() * 2.0D;
      double z = bs.getZ() + (double)enumfacing.getFrontOffsetZ() * 2.0D;
      if(itemStack.getItem() instanceof MCH_ItemThrowable) {
         MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(itemStack.getItem());
         if(info != null) {
            bs.getWorld().playSound(x, y, z, "random.bow", 0.5F, 0.4F / (bs.getWorld().rand.nextFloat() * 0.4F + 0.8F), false);
            if(!bs.getWorld().isRemote) {
               MCH_Lib.DbgLog(bs.getWorld(), "MCH_ItemThrowableDispenseBehavior.dispenseStack(%s)", new Object[]{info.name});
               MCH_EntityThrowable entity = new MCH_EntityThrowable(bs.getWorld(), x, y, z);
               entity.motionX = (double)enumfacing.getFrontOffsetX() * (double)info.dispenseAcceleration;
               entity.motionY = (double)enumfacing.getFrontOffsetY() * (double)info.dispenseAcceleration;
               entity.motionZ = (double)enumfacing.getFrontOffsetZ() * (double)info.dispenseAcceleration;
               entity.setInfo(info);
               bs.getWorld().spawnEntityInWorld(entity);
               itemStack.splitStack(1);
            }
         }
      }

      return itemStack;
   }
}
