package mcheli.ship;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_ItemAircraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemShip extends MCH_ItemAircraft {

    public MCH_ItemShip(int par1) {
        super(par1);
        super.maxStackSize = 1;
    }

    public MCH_AircraftInfo getAircraftInfo() {
        return MCH_ShipInfoManager.getFromItem(this);
    }

    public MCH_EntityShip createAircraft(World world, double x, double y, double z, ItemStack itemStack) {
        /* 21 */     MCH_ShipInfo info = MCH_ShipInfoManager.getFromItem((Item)this);
        /* 22 */     if (info == null) {
            /* 23 */       MCH_Lib.Log(world, "##### MCH_EntityShip Ship info null %s", new Object[] { getUnlocalizedName() });
            /* 24 */       return null;
            /*    */     }
        /* 26 */     MCH_EntityShip plane = new MCH_EntityShip(world);
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
