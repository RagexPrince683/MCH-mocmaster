package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.gui.MCH_Gui;
import mcheli.hud.MCH_Hud;
import mcheli.weapon.MCH_EntityTvMissile;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public abstract class MCH_BaseVehicleCommonGui extends MCH_Gui {

   public MCH_BaseVehicleCommonGui(Minecraft minecraft) {
      super(minecraft);
   }

   public void drawHud(MCH_EntityBaseVehicle ac, EntityPlayer player, int seatId) {
      MCH_BaseVehicleInfo info = ac.getAcInfo();
      if(info != null) {
         if(ac.isMissileCameraMode(player) && ac.getTVMissile() != null && info.hudTvMissile != null) {
            info.hudTvMissile.draw(ac, player, super.smoothCamPartialTicks);
         } else {
            if(seatId < 0) {
               return;
            }

            if(seatId < info.hudList.size()) {
               MCH_Hud hud = (MCH_Hud)info.hudList.get(seatId);
               if(hud != null) {
                  hud.draw(ac, player, super.smoothCamPartialTicks);
               }
            }
         }

      }
   }

   public void drawDebugtInfo(MCH_EntityBaseVehicle ac) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.EnableMCHLibDebugLog.prmBool) {
         int LX = super.centerX - 100;
      }

   }

   public void drawNightVisionNoise() {
      GL11.glEnable(3042);
      GL11.glColor4f(0.0F, 1.0F, 0.0F, 0.3F);
      int srcBlend = GL11.glGetInteger(3041);
      int dstBlend = GL11.glGetInteger(3040);
      GL11.glBlendFunc(1, 1);
      W_McClient.MOD_bindTexture("textures/gui/alpha.png");
      this.drawTexturedModalRectRotate(0.0D, 0.0D, (double)super.width, (double)super.height, (double)super.rand.nextInt(256), (double)super.rand.nextInt(256), 256.0D, 256.0D, 0.0F);
      GL11.glBlendFunc(srcBlend, dstBlend);
      GL11.glDisable(3042);
   }

   public void drawHitBullet(int hs, int hsMax, int color) {
      if(hs > 0) {
         int cx = super.centerX;
         int cy = super.centerY;
         byte IVX = 10;
         byte IVY = 10;
         byte SZX = 5;
         byte SZY = 5;
         double[] ls = new double[]{(double)(cx - IVX), (double)(cy - IVY), (double)(cx - SZX), (double)(cy - SZY), (double)(cx - IVX), (double)(cy + IVY), (double)(cx - SZX), (double)(cy + SZY), (double)(cx + IVX), (double)(cy - IVY), (double)(cx + SZX), (double)(cy - SZY), (double)(cx + IVX), (double)(cy + IVY), (double)(cx + SZX), (double)(cy + SZY)};
         //TODO clean up random magic number slop
         MCH_Config var10000 = MCH_MOD.config;
         color = MCH_Config.hitMarkColorRGB;
         int alpha = hs * (256 / hsMax);
         MCH_Config var10001 = MCH_MOD.config;
         color |= (int)(MCH_Config.hitMarkColorAlpha * (float)alpha) << 24;
         this.drawLine(ls, color);
      }

   }

   public void drawHitBullet(MCH_EntityBaseVehicle ac, int color, int seatID) {
      this.drawHitBullet(ac.getHitStatus(), ac.getMaxHitStatus(), color);
   }

   protected void drawTvMissileNoise(MCH_EntityBaseVehicle ac, MCH_EntityTvMissile tvmissile) {
      GL11.glEnable(3042);
      GL11.glColor4f(0.5F, 0.5F, 0.5F, 0.4F);
      int srcBlend = GL11.glGetInteger(3041);
      int dstBlend = GL11.glGetInteger(3040);
      GL11.glBlendFunc(1, 1);
      W_McClient.MOD_bindTexture("textures/gui/noise.png");
      this.drawTexturedModalRectRotate(0.0D, 0.0D, (double)super.width, (double)super.height, (double)super.rand.nextInt(256), (double)super.rand.nextInt(256), 256.0D, 256.0D, 0.0F);
      GL11.glBlendFunc(srcBlend, dstBlend);
      GL11.glDisable(3042);
   }

   public void drawKeyBind(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, EntityPlayer player, int seatID, int RX, int LX, int colorActive, int colorInactive) {
      String msg = "";
      boolean c = false;
      StringBuilder var10000;
      MCH_Config var10001;
      if(seatID == 0) {
         this.drawString(ac.isVehicleAccessLocked() ? "Lock: LOCKED" : "Lock: UNLOCKED", LX, super.centerY + 50, colorActive);
      }
      this.drawRadarKeyBind(ac, player, seatID, RX, super.centerY + 20, colorActive, colorInactive);
      if(seatID == 0 && ac.canPutToRack()) {
         var10000 = (new StringBuilder()).append("PutRack : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyPutToRack.prmInt)).toString();
         this.drawString(msg, LX, super.centerY - 10, colorActive);
      }

      if(seatID == 0 && ac.canDownFromRack()) {
         var10000 = (new StringBuilder()).append("DownRack : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyDownFromRack.prmInt)).toString();
         this.drawString(msg, LX, super.centerY - 0, colorActive);
      }

      if(seatID == 0 && ac.canRideRack()) {
         var10000 = (new StringBuilder()).append("RideRack : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyPutToRack.prmInt)).toString();
         this.drawString(msg, LX, super.centerY + 10, colorActive);
      }

      if(seatID == 0 && ac.ridingEntity != null) {
         var10000 = (new StringBuilder()).append("DismountRack : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyDownFromRack.prmInt)).toString();
         this.drawString(msg, LX, super.centerY + 10, colorActive);
      }

      int c1;
      label133: {
         if(seatID <= 0 || ac.getSeatNum() <= 1) {
            MCH_Config var15 = MCH_MOD.config;
            if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
               break label133;
            }
         }

         c1 = seatID == 0?-208:colorActive;
         String var16;
         if(seatID == 0) {
            var10000 = new StringBuilder();
            var10001 = MCH_MOD.config;
            var16 = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt)).append(" + ").toString();
         } else {
            var16 = "";
         }

         String ws = var16;
         var10000 = (new StringBuilder()).append("NextSeat : ").append(ws);
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyGUI.prmInt)).toString();
         this.drawString(msg, RX, super.centerY - 70, c1);
         var10000 = (new StringBuilder()).append("PrevSeat : ").append(ws);
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
         this.drawString(msg, RX, super.centerY - 60, c1);
      }

      if(seatID >= 0 && seatID <= 1 && ac.haveFlare()) {
         c1 = ac.isFlarePreparation()?colorInactive:colorActive;
         var10000 = (new StringBuilder()).append("Flare : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt)).toString();
         this.drawString(msg, RX, super.centerY - 50, c1);
      }

      if(seatID == 0 && info.haveLandingGear()) {
         if(ac.canFoldLandingGear()) {
            var10000 = (new StringBuilder()).append("Gear Up : ");
            var10001 = MCH_MOD.config;
            msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyGearUpDown.prmInt)).toString();
            this.drawString(msg, RX, super.centerY - 40, colorActive);
         } else if(ac.canUnfoldLandingGear()) {
            var10000 = (new StringBuilder()).append("Gear Down : ");
            var10001 = MCH_MOD.config;
            msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyGearUpDown.prmInt)).toString();
            this.drawString(msg, RX, super.centerY - 40, colorActive);
         }
      }

      MCH_WeaponSet ws1 = ac.getCurrentWeapon(player);
      if(ac.getWeaponNum() > 1) {
         var10000 = (new StringBuilder()).append("Weapon : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchWeapon2.prmInt)).toString();
         this.drawString(msg, LX, super.centerY - 70, colorActive);
      }

      if(ws1.getCurrentWeapon().numMode > 0) {
         var10000 = (new StringBuilder()).append("WeaponMode : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt)).toString();
         this.drawString(msg, LX, super.centerY - 60, colorActive);
      }

      if(ac.canSwitchSearchLight(player)) {
         var10000 = (new StringBuilder()).append("SearchLight : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt)).toString();
         this.drawString(msg, LX, super.centerY - 50, colorActive);
      } else if(ac.canSwitchCameraMode(seatID)) {
         var10000 = (new StringBuilder()).append("CameraMode : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt)).toString();
         this.drawString(msg, LX, super.centerY - 50, colorActive);
      }

      this.drawDismountKeyBind(ac, info, player, seatID, LX, super.centerY - 30, colorActive);

      if(seatID == 0 && ac.canSwitchFreeLook() || seatID > 0 && ac.canSwitchGunnerModeOtherSeat(player)) {
         var10000 = (new StringBuilder()).append("FreeLook : ");
         var10001 = MCH_MOD.config;
         msg = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt)).toString();
         this.drawString(msg, LX, super.centerY - 20, colorActive);
      }

   }

   protected void drawRadarKeyBind(MCH_EntityBaseVehicle ac, EntityPlayer player, int seatID,
         int x, int y, int colorActive, int colorInactive) {
      if(ac == null || !ac.hasRadar() || seatID != 0 || !ac.isPilot(player)) {
         return;
      }
      boolean active = ac.isRadarActive();
      String msg = "Radar " + (active ? "ON" : "OFF") + " : "
            + MCH_KeyName.getDescOrName(MCH_Config.KeyRadar.prmInt);
      this.drawString(msg, x, y, active ? colorActive : colorInactive);
   }

   protected void drawDismountKeyBind(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, EntityPlayer player,
         int seatID, int x, int y, int colorActive) {
      if(!this.isPhysicallyMountedInSeat(ac, player, seatID)) {
         return;
      }

      int color = colorActive;
      String msg;
      if(seatID == 0 && info.isEnableParachuting && MCH_Lib.getBlockIdY(ac, 3, -10) == 0) {
         msg = "Parachuting : " + MCH_KeyName.getDescOrName(MCH_Config.KeyUnmount.prmInt);
      } else if(seatID == 0 && ac.canStartRepelling()) {
         msg = "Repelling : " + MCH_KeyName.getDescOrName(MCH_Config.KeyUnmount.prmInt);
         color = -256;
      } else {
         int remainingSeconds = 3;
         if(MCH_ClientCommonTickHandler.instance != null) {
            remainingSeconds = MCH_ClientCommonTickHandler.instance.getDismountHoldRemainingSeconds(player);
         }
         msg = "Dismount : Hold " + MCH_KeyName.getDescOrName(super.mc.gameSettings.keyBindSneak.getKeyCode())
               + " (" + remainingSeconds + "s)";
      }

      this.drawString(msg, x, y, color);
   }

   private boolean isPhysicallyMountedInSeat(MCH_EntityBaseVehicle ac, EntityPlayer player, int seatID) {
      if(ac == null || player == null || !ac.isValidSeatID(seatID) || ac.getSeatIdByEntity(player) != seatID) {
         return false;
      }

      if(seatID == 0) {
         return player.ridingEntity == ac;
      }

      if(player.ridingEntity instanceof MCH_EntitySeat) {
         MCH_EntitySeat seat = (MCH_EntitySeat)player.ridingEntity;
         return seat.getParent() == ac && seat.seatID + 1 == seatID;
      }

      return false;
   }
}
