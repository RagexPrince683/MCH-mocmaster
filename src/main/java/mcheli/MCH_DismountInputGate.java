package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.MovementInput;

/** Called immediately after vanilla polls movement keys, before it can publish Sneak to the server. */
@SideOnly(Side.CLIENT)
public final class MCH_DismountInputGate {

   private MCH_DismountInputGate() {
   }

   public static void filterVehicleSneak(MovementInput movementInput) {
      MCH_ClientCommonTickHandler handler = MCH_ClientCommonTickHandler.instance;
      if(handler != null && handler.updateDismountHoldFromPhysicalInput()) {
         movementInput.sneak = false;
      }
   }
}
