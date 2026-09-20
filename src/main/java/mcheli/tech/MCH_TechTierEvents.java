package mcheli.tech;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import mcheli.aircraft.MCH_BaseVehicleInfo;

public final class MCH_TechTierEvents {
    @SubscribeEvent public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        MCH_BaseVehicleInfo info = MCH_TechTierManager.getInfo(event.crafting);
        if (info != null && !event.player.worldObj.isRemote
                && !MCH_TechTierManager.isUnlocked(info, event.player, event.player.worldObj)) {
            MCH_TechTierManager.notifyLocked(event.player, info);
            event.crafting.stackSize = 0;
        }
    }
}
