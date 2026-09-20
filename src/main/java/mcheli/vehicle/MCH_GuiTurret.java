package mcheli.vehicle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleCommonGui;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.gui.MCH_Gui;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_TurretInfo;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiTurret extends MCH_BaseVehicleCommonGui {

   static final int COLOR1 = -14066;
   static final int COLOR2 = -2161656;


   public MCH_GuiTurret(Minecraft minecraft) {
      super(minecraft);
   }

   public boolean isDrawGui(EntityPlayer player) {
      return this.getRiddenTurret(player) != null;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      MCH_EntityTurret vehicle = this.getRiddenTurret(player);
      if(vehicle != null) {
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
                  MCH_Config configuration = MCH_MOD.config;
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

   private MCH_EntityTurret getRiddenTurret(EntityPlayer player) {
      if(player.ridingEntity instanceof MCH_EntityTurret) {
         return (MCH_EntityTurret)player.ridingEntity;
      }
      if(player.ridingEntity instanceof MCH_EntitySeat
            && ((MCH_EntitySeat)player.ridingEntity).getParent() instanceof MCH_EntityTurret) {
         return (MCH_EntityTurret)((MCH_EntitySeat)player.ridingEntity).getParent();
      }
      return null;
   }

   public void drawKeyBind(MCH_EntityTurret vehicle, EntityPlayer player) {
      MCH_Config configuration = MCH_MOD.config;
      if(!MCH_Config.HideKeybind.prmBool) {
         MCH_TurretInfo info = vehicle.getTurretInfo();
         if(info != null) {
            int colorActive = -1342177281;
            int colorInactive = -1349546097;
            int RX = super.centerX + 120;
            int LX = super.centerX - 200;
            int seatID = vehicle.getSeatIdByEntity(player);
            this.drawRadarKeyBind(vehicle, player, seatID, RX, super.centerY + 20, colorActive, colorInactive);
            String msg;
            StringBuilder messageBuilder;
            MCH_Config configuration2;
            if(vehicle.haveFlare()) {
               int c = vehicle.isFlarePreparation()?colorInactive:colorActive;
               messageBuilder = (new StringBuilder()).append("Flare : ");
               configuration2 = MCH_MOD.config;
               msg = messageBuilder.append(MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 50, c);
            }

            if(vehicle.getSizeInventory() > 0) {
               ;
            }

            if(vehicle.getTowChainEntity() != null && !vehicle.getTowChainEntity().isDead) {
               messageBuilder = (new StringBuilder()).append("Drop  : ");
               configuration2 = MCH_MOD.config;
               msg = messageBuilder.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 30, colorActive);
            }

            if(vehicle.camera.getCameraZoom() > 1.0F) {
               messageBuilder = (new StringBuilder()).append("Zoom : ");
               configuration2 = MCH_MOD.config;
               msg = messageBuilder.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 80, colorActive);
            }

            MCH_WeaponSet ws = vehicle.getCurrentWeapon(player);
            if(vehicle.getWeaponNum() > 1) {
               messageBuilder = (new StringBuilder()).append("Weapon : ");
               configuration2 = MCH_MOD.config;
               msg = messageBuilder.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchWeapon2.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 70, colorActive);
            }

            if(ws.getCurrentWeapon().numMode > 0) {
               messageBuilder = (new StringBuilder()).append("WeaponMode : ");
               configuration2 = MCH_MOD.config;
               msg = messageBuilder.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 60, colorActive);
            }

            if(info.isEnableNightVision) {
               messageBuilder = (new StringBuilder()).append("CameraMode : ");
               configuration2 = MCH_MOD.config;
               msg = messageBuilder.append(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 50, colorActive);
            }

            this.drawDismountKeyBind(vehicle, info, player, vehicle.getSeatIdByEntity(player), LX,
                  super.centerY - 30, colorActive);

         }
      }
   }
}
