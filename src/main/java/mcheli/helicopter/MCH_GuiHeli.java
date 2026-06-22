package mcheli.helicopter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleCommonGui;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_HudShared;
import mcheli.gui.MCH_Gui;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.weapon.MCH_EntityTvMissile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiHeli extends MCH_BaseVehicleCommonGui {

   public MCH_GuiHeli(Minecraft minecraft) {
      super(minecraft);
   }

   public boolean isDrawGui(EntityPlayer player) {
      return MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player) instanceof MCH_EntityHeli;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      if(ac instanceof MCH_EntityHeli && !ac.isDestroyed()) {
         MCH_EntityHeli heli = (MCH_EntityHeli)ac;
         int seatID = ac.getSeatIdByEntity(player);
         GL11.glLineWidth((float)MCH_Gui.scaleFactor);
         if(heli.getCameraMode(player) == 1) {
            this.drawNightVisionNoise();
         }

         MCH_Config var10000;
         label57: {
            if(isThirdPersonView) {
               var10000 = MCH_MOD.config;
               if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                  break label57;
               }
            }

            if(seatID == 0 && heli.getIsGunnerMode(player)) {
               this.drawHud(ac, player, 1);
            } else {
               this.drawHud(ac, player, seatID);
            }
         }

         this.drawDebugtInfo(heli);
         if(!heli.getIsGunnerMode(player)) {
            label39: {
               if(isThirdPersonView) {
                  var10000 = MCH_MOD.config;
                  if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                     break label39;
                  }
               }

               if(seatID == 0) {
                  this.drawNewHeliControlHud(heli);
                  this.drawNewHeliSharedHud(heli, player);
                  this.drawNewHeliWeaponHud(heli, player);
               }

               this.drawKeyBind(heli, player, seatID);
            }

            this.drawHitBullet(heli, -14101432, seatID);
         } else {
            label34: {
               if(isThirdPersonView) {
                  var10000 = MCH_MOD.config;
                  if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                     break label34;
                  }
               }

               MCH_EntityTvMissile tvmissile = heli.getTVMissile();
               if(!heli.isMissileCameraMode(player)) {
                  this.drawKeyBind(heli, player, seatID);
               } else if(tvmissile != null) {
                  this.drawTvMissileNoise(heli, tvmissile);
               }
            }

            this.drawHitBullet(heli, -805306369, seatID);
         }

      }
   }

   private void drawNewHeliControlHud(MCH_EntityHeli heli) {
      MCH_HeliInfo info = heli.getHeliInfo();
      if(info == null || !heli.isNewHeliFlightModelEnabled() || !info.newHeliControlHudDisplay) {
         return;
      }

      int power = this.toPercent(heli.getEnginePowerOutput());
      int color = -14101432; // 0xFF28D448: existing heli HUD green.
      int x = super.centerX + 120;
      int y = super.centerY + 55;
      this.drawString(String.format("PWR: %3d%%", new Object[]{Integer.valueOf(power)}), x, y, color);
      if(heli.isHoverAssistActive()) {
         this.drawString("SAS: ON", x, y + 10, color);
      }
   }

   private void drawNewHeliSharedHud(MCH_EntityHeli heli, EntityPlayer player) {
      if(!this.shouldDrawNewHeliHudAdditions(heli) || !MCH_Config.EnableNewHeliHudSharedReadouts.prmBool) {
         return;
      }

      List lines = new ArrayList();
      lines.add(MCH_HudShared.formatSpeedKmh(heli));
      lines.add(MCH_HudShared.formatAltitude(heli));
      lines.add(MCH_HudShared.formatVerticalSpeed(heli));
      lines.add(MCH_HudShared.formatFuelMinutes(heli));
      int color = -14101432;
      int x = super.centerX - 200;
      int y = super.centerY + 55;
      for(int i = 0; i < lines.size(); ++i) {
         this.drawNewHeliHudText((String)lines.get(i), x, y + i * 10, color, 0x5528D448);
      }
   }

   private void drawNewHeliWeaponHud(MCH_EntityHeli heli, EntityPlayer player) {
      if(!this.shouldDrawNewHeliHudAdditions(heli) || !MCH_Config.EnableNewHeliWeaponHud.prmBool) {
         return;
      }
      int x = super.centerX + 120;
      int y = super.centerY - 15;
      int maxTextWidth = Math.max(90, super.width - x - 8);
      List lines = MCH_HudShared.collectWeaponAmmoLines(heli, super.mc, maxTextWidth, true, heli.getCurrentWeaponID(player));
      if(lines.isEmpty()) {
         return;
      }
      for(int i = 0; i < lines.size(); ++i) {
         this.drawNewHeliHudText((String)lines.get(i), x, y + i * 10, -14101432, 0x5528D448);
      }
   }

   private boolean shouldDrawNewHeliHudAdditions(MCH_EntityHeli heli) {
      MCH_HeliInfo info = heli.getHeliInfo();
      return info != null && heli.isNewHeliFlightModelEnabled() && !heli.isDestroyed();
   }

   private void drawNewHeliHudText(String text, int x, int y, int color, int glowColor) {
      if(this.isVehicleHudGlowEnabled()) {
         this.drawString(text, x + 1, y + 1, glowColor);
      }
      this.drawString(text, x, y, color);
   }

   private boolean isVehicleHudGlowEnabled() {
      return MCH_Config.EnableNewVehicleHudGlow != null ? MCH_Config.EnableNewVehicleHudGlow.prmBool : MCH_Config.EnableNewPlaneHudGlow.prmBool;
   }

   private int toPercent(float value) {
      return Math.round(MathHelper.clamp_float(value, 0.0F, 1.0F) * 100.0F);
   }

   public void drawKeyBind(MCH_EntityHeli heli, EntityPlayer player, int seatID) {
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.HideKeybind.prmBool) {
         MCH_HeliInfo info = heli.getHeliInfo();
         if(info != null) {
            int colorActive = -1342177281;
            int colorInactive = -1349546097;
            int RX = super.centerX + 120;
            int LX = super.centerX - 200;
            this.drawKeyBind(heli, info, player, seatID, RX, LX, colorActive, colorInactive);
            String msg;
            int c;
            StringBuilder var11;
            MCH_Config var10001;
            if(seatID == 0 && info.isEnableGunnerMode) {
               var10000 = MCH_MOD.config;
               if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                  c = heli.isHoveringMode()?colorInactive:colorActive;
                  var11 = (new StringBuilder()).append(heli.getIsGunnerMode(player)?"Normal":"Gunner").append(" : ");
                  var10001 = MCH_MOD.config;
                  msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
                  this.drawString(msg, RX, super.centerY - 70, c);
               }
            }

            if(seatID > 0 && heli.canSwitchGunnerModeOtherSeat(player)) {
               var11 = (new StringBuilder()).append(heli.getIsGunnerMode(player)?"Normal":"Camera").append(" : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 40, colorActive);
            }

            if(seatID == 0) {
               var10000 = MCH_MOD.config;
               if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                  c = heli.getIsGunnerMode(player)?colorInactive:colorActive;
                  var11 = (new StringBuilder()).append(heli.getIsGunnerMode(player)?"Normal":"Hovering").append(" : ");
                  var10001 = MCH_MOD.config;
                  msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt)).toString();
                  this.drawString(msg, RX, super.centerY - 60, c);
               }
            }

            if(heli.canEjectSeat(player)) {
               var11 = (new StringBuilder()).append("Eject seat: ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyEjectHeli.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 30, colorActive);
            }

            if(seatID == 0) {
               if(heli.getTowChainEntity() != null && !heli.getTowChainEntity().isDead) {
                  var11 = (new StringBuilder()).append("Drop  : ");
                  var10001 = MCH_MOD.config;
                  msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
                  this.drawString(msg, RX, super.centerY - 30, colorActive);
               } else if(info.isEnableFoldBlade && MCH_Lib.getBlockIdY(heli.worldObj, heli.posX, heli.posY, heli.posZ, 1, -2, true) > 0 && heli.getCurrentThrottle() <= 0.01D) {
                  var11 = (new StringBuilder()).append("FoldBlade  : ");
                  var10001 = MCH_MOD.config;
                  msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
                  this.drawString(msg, RX, super.centerY - 30, colorActive);
               }
            }

            if((heli.getIsGunnerMode(player) || heli.isUAV()) && info.cameraZoom > 1) {
               var11 = (new StringBuilder()).append("Zoom : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 80, colorActive);
            } else if(seatID == 0 && (heli.canFoldHatch() || heli.canUnfoldHatch())) {
               var11 = (new StringBuilder()).append("OpenHatch : ");
               var10001 = MCH_MOD.config;
               msg = var11.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 80, colorActive);
            }

         }
      }
   }
}
