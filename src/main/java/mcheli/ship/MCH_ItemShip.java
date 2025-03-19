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
             MCH_ShipInfo info = MCH_ShipInfoManager.getFromItem((Item)this);
             if (info == null) {
                   MCH_Lib.Log(world, "##### MCP_EntityPlane Plane info null %s", new Object[] { getUnlocalizedName() });
                   return null;
                 }
             MCH_EntityShip plane = new MCH_EntityShip(world);
             plane.setPosition(x, y + plane.yOffset, z);
             plane.prevPosX = x;
             plane.prevPosY = y;
             plane.prevPosZ = z;
             plane.camera.setPosition(x, y, z);
             plane.setTypeName(info.name);
             if (!world.isRemote) {
                   plane.setTextureName(info.getTextureName());
                 }

             return plane;
           }
     }
     //todo double check this is right
