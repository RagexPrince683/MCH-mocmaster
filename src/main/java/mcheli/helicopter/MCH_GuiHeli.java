package mcheli.helicopter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_Vector2;
import mcheli.aircraft.MCH_BaseVehicleCommonGui;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_HudShared;
import mcheli.gui.MCH_Gui;
import mcheli.weapon.MCH_EntityTvMissile;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import mcheli.hud.layout.MCH_HudLayoutManager;

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
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.flight_readouts", () -> this.drawNewHeliSharedHud(heli, player));
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.health", () -> this.drawNewHeliHealthHud(heli, player));
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.pitch_readout", () -> this.drawNewHeliPitchReadout(heli, player));
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.radar", () -> this.drawNewHeliRadarHud(heli));
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.weapon_list", () -> this.drawNewHeliWeaponHud(heli, player));
               }

               MCH_HudLayoutManager.renderBuiltin("heli", "heli.keybinds", () -> this.drawKeyBind(heli, player, seatID));
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
               if(seatID == 0) {
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.flight_readouts", () -> this.drawNewHeliSharedHud(heli, player));
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.health", () -> this.drawNewHeliHealthHud(heli, player));
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.pitch_readout", () -> this.drawNewHeliPitchReadout(heli, player));
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.weapon_list", () -> this.drawNewHeliWeaponHud(heli, player));
               }

               if(!heli.isMissileCameraMode(player)) {
                  MCH_HudLayoutManager.renderBuiltin("heli", "heli.keybinds", () -> this.drawKeyBind(heli, player, seatID));
               } else if(tvmissile != null) {
                  this.drawTvMissileNoise(heli, tvmissile);
               }
            }

            this.drawHitBullet(heli, -805306369, seatID);
         }

      }
   }

   private void drawNewHeliSharedHud(MCH_EntityHeli heli, EntityPlayer player) {
      if(!this.shouldDrawNewHeliHudAdditions(heli) || !MCH_Config.EnableNewHeliHudSharedReadouts.prmBool) {
         return;
      }

      MCH_HeliInfo info = heli.getHeliInfo();
      List lines = new ArrayList();
      lines.add(MCH_HudShared.formatSpeedKmh(heli));
      lines.add(MCH_HudShared.formatAltitude(heli));
      lines.add(MCH_HudShared.formatVerticalSpeed(heli));
      lines.add(String.format("PWR   %3d%%", new Object[]{Integer.valueOf(this.toPercent(heli.getEnginePowerOutput()))}));
      if(info != null && info.newHeliControlHudDisplay && heli.isHoverAssistActive()) {
         lines.add((heli.getIsGunnerMode(player)?"GNR":"SAS") + "   ON");
      }
      lines.add(MCH_HudShared.formatFuelMinutes(heli));
      int color = this.getNewHeliHudColor(heli, player);
      int x = super.centerX - 205;
      int y = super.centerY + 20;
      for(int i = 0; i < lines.size(); ++i) {
         this.drawNewHeliHudText((String)lines.get(i), x, y + i * 10, color, 0x5528D448, !heli.getIsGunnerMode(player));
      }
      this.drawStickInputGauge(x + 88, y + 6, color);
   }



   private void drawStickInputGauge(int x, int y, int color) {
      if(!MCH_Config.EnableNewVehicleStickInputGauge.prmBool) {
         return;
      }
      int size = 30;
      int half = size / 2;
      double max = Math.max(1.0D, MCH_ClientCommonTickHandler.getMaxStickLength());
      int sx = x + half + (int)Math.round(MathHelper.clamp_double(MCH_ClientCommonTickHandler.getCurrentStickX() / max, -1.0D, 1.0D) * (double)(half - 3));
      int sy = y + half - (int)Math.round(MathHelper.clamp_double(MCH_ClientCommonTickHandler.getCurrentStickY() / max, -1.0D, 1.0D) * (double)(half - 3));
      this.drawLine(new double[]{(double)x, (double)(y + half), (double)(x + size), (double)(y + half), (double)(x + half), (double)y, (double)(x + half), (double)(y + size)}, color, 1);
      drawRect(sx - 2, sy - 2, sx + 3, sy + 3, color);
   }

   private void drawNewHeliHealthHud(MCH_EntityHeli heli, EntityPlayer player) {
      if(!this.shouldDrawNewHeliHudAdditions(heli)) {
         return;
      }
      int hp = MCH_HudShared.getDamagePercent(heli);
      int color = heli.getIsGunnerMode(player) ? 0xFFFFFFFF : (hp > 20 ? -14101432 : -2162680);
      int x = super.centerX - 205;
      int y = super.centerY + 83;
      this.drawNewHeliHudText(String.format("HP    %3d%%", new Object[]{Integer.valueOf(hp)}), x, y, color, 0x5528D448, !heli.getIsGunnerMode(player));
      int barWidth = MathHelper.clamp_int(hp * 60 / 100, 0, 60);
      drawRect(x, y + 10, x + 60, y + 13, 0x66188428);
      drawRect(x, y + 10, x + barWidth, y + 13, color);
   }


   private void drawNewHeliPitchReadout(MCH_EntityHeli heli, EntityPlayer player) {
      if(!this.shouldDrawNewHeliHudAdditions(heli)) {
         return;
      }
      int x = super.centerX + 90;
      int y = super.centerY - 4;
      this.clearLegacyPitchReadoutBox();
      this.drawPitchReadoutBox(x, super.centerY);
      this.drawNewHeliHudText(String.format("%.0f", new Object[]{Float.valueOf(heli.getRotPitch())}), x, y, -14101432, 0x5528D448);
   }

   private void clearLegacyPitchReadoutBox() {
      drawRect(super.centerX + 116, super.centerY - 8, super.centerX + 154, super.centerY + 9, 0xAA000000);
   }

   private void drawPitchReadoutBox(int readoutX, int centerY) {
      int left = readoutX - 20;
      this.drawLine(new double[]{(double)left, (double)centerY, (double)(left + 5), (double)(centerY - 5),
            (double)(left + 30), (double)(centerY - 5), (double)(left + 30), (double)(centerY + 5),
            (double)(left + 5), (double)(centerY + 5), (double)left, (double)centerY}, -14101432, 1);
   }

   private void drawNewHeliRadarHud(MCH_EntityHeli heli) {
      if(!this.shouldDrawNewHeliHudAdditions(heli) || !heli.hasRadar() || !heli.isRadarActive()) {
         return;
      }
      int x = super.centerX + 144;
      int y = super.centerY + 65;
      int size = 64;
      int color = -14101432;
      this.drawNewHeliRadarTexture(heli, x, y, size);
      this.drawLine(new double[]{(double)x, (double)(y + size / 2), (double)(x + size), (double)(y + size / 2),
            (double)(x + size / 2), (double)y, (double)(x + size / 2), (double)(y + size)}, 0x80FFFFFF, 1);
      this.drawRadarPoints(heli.getRadarEntityList(), heli, x, y, size, color);
      this.drawRadarPoints(heli.getRadarEnemyList(), heli, x, y, size, 0xFFDF0408);
   }


   private void drawNewHeliRadarTexture(MCH_EntityHeli heli, int x, int y, int size) {
      GL11.glPushMatrix();
      boolean blend = GL11.glIsEnabled(3042);
      int srcBlend = GL11.glGetInteger(3041);
      int dstBlend = GL11.glGetInteger(3040);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      W_McClient.MOD_bindTexture("textures/gui/heli_hud.png");
      this.drawTexturedModalRectRotate((double)x, (double)y, (double)size, (double)size, 0.0D, 0.0D, 128.0D, 128.0D, 0.0F);
      this.drawTexturedModalRectRotate((double)(x + 16), (double)y, 32.0D, (double)size, 128.0D, 0.0D, 64.0D, 128.0D, (float)heli.getRadarRotate());
      GL11.glBlendFunc(srcBlend, dstBlend);
      if(!blend) {
         GL11.glDisable(3042);
      }
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   private void drawRadarPoints(ArrayList src, MCH_EntityHeli heli, int left, int top, int size, int color) {
      if(src == null || src.isEmpty()) {
         return;
      }
      double half = (double)size / 2.0D;
      double factor = (double)size / 64.0D;
      double[] points = new double[src.size() * 2];
      int idx = 0;
      for(Iterator it = src.iterator(); it.hasNext(); idx += 2) {
         MCH_Vector2 v = (MCH_Vector2)it.next();
         points[idx] = v.x / 2.0D * factor;
         points[idx + 1] = v.y / 2.0D * factor;
      }
      MCH_Lib.rotatePoints(points, -heli.getRotYaw() - 180.0F);
      ArrayList drawPoints = new ArrayList();
      for(int i = 0; i + 1 < points.length; i += 2) {
         if(points[i] > -half && points[i] < half && points[i + 1] > -half && points[i + 1] < half) {
            drawPoints.add(Double.valueOf(points[i] + (double)left + half));
            drawPoints.add(Double.valueOf(points[i + 1] + (double)top + half));
         }
      }
      this.drawPoints(drawPoints, color, MCH_Gui.scaleFactor * 2);
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
         this.drawNewHeliHudText((String)lines.get(i), x, y + i * 10, this.getNewHeliHudColor(heli, player), 0x5528D448, !heli.getIsGunnerMode(player));
      }
      this.drawWeaponOverheatBars(heli, x, y + lines.size() * 10 + 2, 72);
   }


   private void drawWeaponOverheatBars(MCH_EntityBaseVehicle ac, int x, int y, int width) {
      if(ac == null || ac.weapons == null) {
         return;
      }
      int row = 0;
      for(int i = 0; i < ac.weapons.length; ++i) {
         mcheli.weapon.MCH_WeaponSet ws = ac.weapons[i];
         if(ws != null && ws.getCurrentWeapon() != null && ws.getCurrentWeapon().getInfo() != null) {
            int maxHeat = ws.getCurrentWeapon().getInfo().maxHeatCount;
            if(maxHeat > 0) {
               int by = y + row * 5;
               int fill = MathHelper.clamp_int((int)Math.round((double)Math.max(0, ws.currentHeat) * (double)width / (double)maxHeat), 0, width);
               drawRect(x, by, x + width, by + 3, 0x66303030);
               drawRect(x, by, x + fill, by + 3, ws.currentHeat >= maxHeat ? 0xFFFF3030 : 0xFFFFAA30);
               ++row;
            }
         }
      }
   }

   private boolean shouldDrawNewHeliHudAdditions(MCH_EntityHeli heli) {
      MCH_HeliInfo info = heli.getHeliInfo();
      return info != null && heli.isNewHeliFlightModelEnabled() && !heli.isDestroyed();
   }

   private void drawNewHeliHudText(String text, int x, int y, int color, int glowColor) {
      this.drawNewHeliHudText(text, x, y, color, glowColor, true);
   }

   private void drawNewHeliHudText(String text, int x, int y, int color, int glowColor, boolean allowGlow) {
      if(allowGlow && this.isVehicleHudGlowEnabled()) {
         this.drawString(text, x + 1, y + 1, glowColor);
      }
      this.drawString(text, x, y, color);
   }

   private int getNewHeliHudColor(MCH_EntityHeli heli, EntityPlayer player) {
      return heli.getIsGunnerMode(player)?0xFFFFFFFF:-14101432;
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
