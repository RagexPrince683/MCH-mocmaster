package mcheli.plane;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_ItemAircraft;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import net.minecraft.item.Item;
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
      /* 21 */     MCP_PlaneInfo info = MCP_PlaneInfoManager.getFromItem((Item)this);
      /* 22 */     if (info == null) {
         /* 23 */       MCH_Lib.Log(world, "##### MCP_EntityPlane Plane info null %s", new Object[] { getUnlocalizedName() });
         /* 24 */       return null;
         /*    */     }
      /* 26 */     MCP_EntityPlane plane = new MCP_EntityPlane(world);
      /* 27 */     plane.setPosition(x, y + plane.yOffset, z);
      /* 28 */     plane.prevPosX = x;
      /* 29 */     plane.prevPosY = y;
      /* 30 */     plane.prevPosZ = z;
      /* 31 */     plane.camera.setPosition(x, y, z);
      /* 32 */     plane.setTypeName(info.name);
      /* 33 */     if (!world.isRemote) {
         /* 34 */       plane.setTextureName(info.getTextureName());
         /*    */     }
      /*    */
      /* 37 */     return plane;
      /*    */   }
   /*    */ }
