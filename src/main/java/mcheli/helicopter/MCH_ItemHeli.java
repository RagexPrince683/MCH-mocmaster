package mcheli.helicopter;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_ItemAircraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemHeli extends MCH_ItemAircraft {

   public MCH_ItemHeli(int par1) {
      super(par1);
      super.maxStackSize = 1;
   }

   public MCH_AircraftInfo getAircraftInfo() {
      return MCH_HeliInfoManager.getFromItem(this);
   }

   public MCH_EntityHeli createAircraft(World world, double x, double y, double z, ItemStack itemStack) {
      MCH_HeliInfo info = MCH_HeliInfoManager.getFromItem(this);
      if(info == null) {
         MCH_Lib.Log(world, "##### MCH_ItemHeli Heli info null %s", new Object[]{this.getUnlocalizedName()});
         return null;
      } else {
         MCH_EntityHeli heli = new MCH_EntityHeli(world);
         heli.setPosition(x, y + (double)heli.yOffset, z);
         heli.prevPosX = x;
         heli.prevPosY = y;
         heli.prevPosZ = z;
         heli.camera.setPosition(x, y, z);
         heli.setTypeName(info.name);
         if(!world.isRemote) {
            heli.setTextureName(info.getTextureName());
         }

         return heli;
      }
   }
}
