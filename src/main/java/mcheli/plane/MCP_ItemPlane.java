package mcheli.plane;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_ItemAircraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCP_ItemPlane extends MCH_ItemAircraft {

   public MCP_ItemPlane(int par1) {
      super(par1);
      super.maxStackSize = 1;
   }

   public MCH_AircraftInfo getAircraftInfo() {
      return MCP_PlaneInfoManager.getFromItem(this);
   }

   public MCP_EntityPlane createAircraft(World world, double x, double y, double z, ItemStack itemStack) {
      MCP_PlaneInfo info = MCP_PlaneInfoManager.getFromItem(this);
      if(info == null) {
         MCH_Lib.Log(world, "##### MCP_EntityPlane Plane info null %s", new Object[]{this.getUnlocalizedName()});
         return null;
      } else {
         MCP_EntityPlane plane = new MCP_EntityPlane(world);
         plane.setPosition(x, y + (double)plane.yOffset, z);
         plane.prevPosX = x;
         plane.prevPosY = y;
         plane.prevPosZ = z;
         plane.camera.setPosition(x, y, z);
         plane.setTypeName(info.name);
         if(!world.isRemote) {
            plane.setTextureName(info.getTextureName());
         }

         return plane;
      }
   }
}
