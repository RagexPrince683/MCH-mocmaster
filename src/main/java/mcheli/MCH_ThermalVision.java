package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

@SideOnly(Side.CLIENT)
public final class MCH_ThermalVision {

   private MCH_ThermalVision() {}

   /** Returns the thermal state of this client's active camera, never another player's camera. */
   public static boolean isActiveCameraThermal() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc != null && mc.thePlayer != null && mc.theWorld != null
         && MCH_ClientCommonTickHandler.cameraMode == MCH_Camera.MODE_THERMALVISION;
   }
}
