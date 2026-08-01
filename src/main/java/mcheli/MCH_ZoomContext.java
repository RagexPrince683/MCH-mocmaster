package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.lweapon.MCH_ItemLightWeaponBase;
import mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Resolves both the owner of MCHeli zoom and the code path which consumes mouse input. */
@SideOnly(Side.CLIENT)
public final class MCH_ZoomContext {
   public static enum InputPath {
      NONE,
      VEHICLE,
      VANILLA_LOOK
   }

   public final InputPath inputPath;
   public final MCH_EntityBaseVehicle vehicle;
   public final double zoom;

   private MCH_ZoomContext(InputPath inputPath, MCH_EntityBaseVehicle vehicle, double zoom) {
      this.inputPath = inputPath;
      this.vehicle = vehicle;
      this.zoom = sanitizeZoom(zoom);
   }

   public static MCH_ZoomContext resolve(Minecraft minecraft, EntityPlayer player) {
      if(player == null) {
         return none();
      }

      MCH_EntityBaseVehicle vehicle = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      if(vehicle != null) {
         return new MCH_ZoomContext(InputPath.VEHICLE, vehicle, vehicle.camera != null?vehicle.camera.getCameraZoom():1.0D);
      }

      if(player.ridingEntity instanceof MCH_EntityGLTD) {
         MCH_EntityGLTD gltd = (MCH_EntityGLTD)player.ridingEntity;
         return new MCH_ZoomContext(InputPath.VANILLA_LOOK, null, gltd.camera != null?gltd.camera.getCameraZoom():1.0D);
      }

      ItemStack held = player.getCurrentEquippedItem();
      if(held != null && held.getItem() instanceof MCH_ItemLightWeaponBase && player.getItemInUseDuration() > 10) {
         return new MCH_ZoomContext(InputPath.VANILLA_LOOK, null, W_Reflection.getCameraZoom());
      }

      if(held != null && held.getItem() instanceof MCH_ItemRangeFinder && MCH_ItemRangeFinder.isUsingScope(player)) {
         return new MCH_ZoomContext(InputPath.VANILLA_LOOK, null, W_Reflection.getCameraZoom());
      }

      return none();
   }

   public double getSensitivityMultiplier() {
      double effectValue = MCH_Config.ZoomSensitivityEffect.prmDouble;
      if(Double.isNaN(effectValue) || Double.isInfinite(effectValue)) {
         effectValue = 100.0D;
      }
      return calculateSensitivityMultiplier(this.zoom, effectValue);
   }

   public static double calculateSensitivityMultiplier(double zoom, double effectValue) {
      zoom = sanitizeZoom(zoom);
      if(Double.isNaN(effectValue) || Double.isInfinite(effectValue)) {
         effectValue = 100.0D;
      }
      double effect = Math.max(0.0D, Math.min(100.0D, effectValue)) / 100.0D;
      return 1.0D / (1.0D + (zoom - 1.0D) * effect);
   }

   private static MCH_ZoomContext none() {
      return new MCH_ZoomContext(InputPath.NONE, null, 1.0D);
   }

   private static double sanitizeZoom(double zoom) {
      return Double.isNaN(zoom) || Double.isInfinite(zoom)?1.0D:Math.max(1.0D, zoom);
   }
}
