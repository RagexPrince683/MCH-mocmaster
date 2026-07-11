package mcheli.vehicle;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_ItemBaseVehicle;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_TurretInfo;
import mcheli.vehicle.MCH_TurretInfoManager;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemTurret extends MCH_ItemBaseVehicle {

   public MCH_ItemTurret(int par1) {
      super(par1);
      super.maxStackSize = 1;
   }

   public MCH_BaseVehicleInfo getAircraftInfo() {
      return MCH_TurretInfoManager.getFromItem(this);
   }

   @Override
   protected boolean shouldPlaceInstantly() {
      return true;
   }

   public MCH_EntityTurret createAircraft(World world, double x, double y, double z, ItemStack item) {
      MCH_TurretInfo info = MCH_TurretInfoManager.getFromItem(this);
      if(info == null) {
         MCH_Lib.Log(world, "##### MCH_ItemTurret Turret info null %s", new Object[]{this.getUnlocalizedName()});
         return null;
      } else {
         if(!world.isRemote) {
            MCH_Lib.Log(world, "[VehiclePlacement] turret factory selected: item=%s info=%s category=%s dir=%s entityClass=%s", new Object[]{this.getUnlocalizedName(), info.name, info.category, info.getDirectoryName(), MCH_EntityTurret.class.getName()});
         }
         MCH_EntityTurret vehicle = new MCH_EntityTurret(world);
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
