package mcheli.tank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftCommonGui;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.gui.MCH_Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiTank extends MCH_AircraftCommonGui {

   public MCH_GuiTank(Minecraft minecraft) {
      super(minecraft);
   }

   public boolean isDrawGui(EntityPlayer player) {
      return MCH_EntityAircraft.getAircraft_RiddenOrControl(player) instanceof MCH_EntityTank;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
      if(ac instanceof MCH_EntityTank && !ac.isDestroyed()) {
         MCH_EntityTank tank = (MCH_EntityTank)ac;
         int seatID = ac.getSeatIdByEntity(player);
         GL11.glLineWidth((float)MCH_Gui.scaleFactor);
         if(tank.getCameraMode(player) == 1) {
            this.drawNightVisionNoise();
         }

         MCH_Config var10000;
         label36: {
            if(isThirdPersonView) {
               var10000 = MCH_MOD.config;
               if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                  break label36;
               }
            }

            this.drawHud(ac, player, seatID);
         }

         label43: {
            this.drawDebugtInfo(tank);
            if(isThirdPersonView) {
               var10000 = MCH_MOD.config;
               if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                  break label43;
               }
            }

            if(tank.getTVMissile() != null && (tank.getIsGunnerMode(player) || tank.isUAV())) {
               this.drawTvMissileNoise(tank, tank.getTVMissile());
            } else {
               this.drawKeybind(tank, player, seatID);
            }
         }

         this.drawHitBullet(tank, -14101432, seatID);
      }
   }

   public void drawDebugtInfo(MCH_EntityTank ac) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.DebugLog) {
         int LX = super.centerX - 100;
         super.drawDebugtInfo(ac);
      }

   }

   public void drawKeybind(MCH_EntityTank tank, EntityPlayer player, int seatID) {
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.HideKeybind.prmBool) {
         MCH_TankInfo info = tank.getTankInfo();
         if(info != null) {
            int colorActive = -1342177281;
            int colorInactive = -1349546097;
            int RX = super.centerX + 120;
            int LX = super.centerX - 200;
            this.drawKeyBind(tank, info, player, seatID, RX, LX, colorActive, colorInactive);
            String msg;
            StringBuilder var11;
            MCH_Config var10001;
            if(seatID == 0 && tank.hasBrake()) {
               var11 = (new StringBuilder()).append("Brake : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 30, colorActive);
            }

            if(seatID > 0 && tank.canSwitchGunnerModeOtherSeat(player)) {
               var11 = (new StringBuilder()).append(tank.getIsGunnerMode(player)?"Normal":"Camera").append(" : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 40, colorActive);
            }

            if(tank.getIsGunnerMode(player) && info.cameraZoom > 1) {
               var11 = (new StringBuilder()).append("Zoom : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 80, colorActive);
            } else if(seatID == 0 && (tank.canFoldHatch() || tank.canUnfoldHatch())) {
               var11 = (new StringBuilder()).append("OpenHatch : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 80, colorActive);
            }

         }
      }
   }
}
