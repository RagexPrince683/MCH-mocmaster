package mcheli.vehicle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftCommonGui;
import mcheli.gui.MCH_Gui;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiVehicle extends MCH_AircraftCommonGui {

   static final int COLOR1 = -14066;
   static final int COLOR2 = -2161656;


   public MCH_GuiVehicle(Minecraft minecraft) {
      super(minecraft);
   }

   public boolean isDrawGui(EntityPlayer player) {
      return player.ridingEntity != null && player.ridingEntity instanceof MCH_EntityVehicle;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      if(player.ridingEntity != null && player.ridingEntity instanceof MCH_EntityVehicle) {
         MCH_EntityVehicle vehicle = (MCH_EntityVehicle)player.ridingEntity;
         if(!vehicle.isDestroyed()) {
            int seatID = vehicle.getSeatIdByEntity(player);
            GL11.glLineWidth((float)MCH_Gui.scaleFactor);
            if(vehicle.getCameraMode(player) == 1) {
               this.drawNightVisionNoise();
            }

            if(vehicle.getIsGunnerMode(player) && vehicle.getTVMissile() != null) {
               this.drawTvMissileNoise(vehicle, vehicle.getTVMissile());
            }

            label24: {
               this.drawDebugtInfo(vehicle);
               if(isThirdPersonView) {
                  MCH_Config var10000 = MCH_MOD.config;
                  if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                     break label24;
                  }
               }

               this.drawHud(vehicle, player, seatID);
               this.drawKeyBind(vehicle, player);
            }

            this.drawHitBullet(vehicle, -14066, seatID);
         }
      }
   }

   public void drawKeyBind(MCH_EntityVehicle vehicle, EntityPlayer player) {
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.HideKeybind.prmBool) {
         MCH_VehicleInfo info = vehicle.getVehicleInfo();
         if(info != null) {
            int colorActive = -1342177281;
            int colorInactive = -1349546097;
            int RX = super.centerX + 120;
            int LX = super.centerX - 200;
            String msg;
            StringBuilder var11;
            MCH_Config var10001;
            if(vehicle.haveFlare()) {
               int c = vehicle.isFlarePreparation()?colorInactive:colorActive;
               var11 = (new StringBuilder()).append("Flare : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 50, c);
            }

            if(vehicle.getSizeInventory() > 0) {
               ;
            }

            if(vehicle.getTowChainEntity() != null && !vehicle.getTowChainEntity().isDead) {
               var11 = (new StringBuilder()).append("Drop  : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 30, colorActive);
            }

            if(vehicle.camera.getCameraZoom() > 1.0F) {
               var11 = (new StringBuilder()).append("Zoom : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 80, colorActive);
            }

            MCH_WeaponSet ws = vehicle.getCurrentWeapon(player);
            if(vehicle.getWeaponNum() > 1) {
               var11 = (new StringBuilder()).append("Weapon : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchWeapon2.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 70, colorActive);
            }

            if(ws.getCurrentWeapon().numMode > 0) {
               var11 = (new StringBuilder()).append("WeaponMode : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 60, colorActive);
            }

            if(info.isEnableNightVision) {
               var11 = (new StringBuilder()).append("CameraMode : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 50, colorActive);
            }

            msg = "Dismount all : LShift";
            this.drawString(msg, LX, super.centerY - 40, colorActive);
            if(vehicle.getSeatNum() >= 2) {
               var11 = (new StringBuilder()).append("Dismount : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyUnmount.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 30, colorActive);
            }

         }
      }
   }
}
