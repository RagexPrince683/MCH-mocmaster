package mcheli.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;

/**
 * Optional client entry point discovered by NEI's config scanner. MCHeli never references this
 * package, so neither a server nor a client without NEI has to load NEI classes.
 */
@SideOnly(Side.CLIENT)
public class NEIMCHeliConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        if (!MCH_Config.EnableNEIHandler.prmBool) {
            return;
        }
        MCH_VehicleAmmoRecipeHandler handler = new MCH_VehicleAmmoRecipeHandler();
        API.registerRecipeHandler(handler);
        API.registerUsageHandler(handler);
    }

    @Override
    public String getName() {
        return "MCHeli Vehicle Ammunition";
    }

    @Override
    public String getVersion() {
        return MCH_MOD.VER;
    }
}
