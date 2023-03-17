package mcheli.vehicle;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_ItemAircraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemVehicle extends MCH_ItemAircraft {

   public MCH_ItemVehicle(int par1) {
      super(par1);
      super.maxStackSize = 1;
   }

   public MCH_AircraftInfo getAircraftInfo() {
      return MCH_VehicleInfoManager.getFromItem(this);
   }

   public MCH_EntityVehicle createAircraft(World world, double x, double y, double z, ItemStack item) {
      MCH_VehicleInfo info = MCH_VehicleInfoManager.getFromItem(this);
      if(info == null) {
         MCH_Lib.Log(world, "##### MCH_ItemVehicle Vehicle info null %s", new Object[]{this.getUnlocalizedName()});
         return null;
      } else {
         MCH_EntityVehicle vehicle = new MCH_EntityVehicle(world);
         vehicle.setPosition(x, y + (double)vehicle.yOffset, z);
         vehicle.prevPosX = x;
         vehicle.prevPosY = y;
         vehicle.prevPosZ = z;
         vehicle.camera.setPosition(x, y, z);
         vehicle.setTypeName(info.name);
         if(!world.isRemote) {
            vehicle.setTextureName(info.getTextureName());
         }

         return vehicle;
      }
   }
}
