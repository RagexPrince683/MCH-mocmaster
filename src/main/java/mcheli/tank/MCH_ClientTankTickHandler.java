package mcheli.tank;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_AircraftClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientTankTickHandler extends MCH_AircraftClientTickHandler {

   public MCH_Key KeySwitchMode;
   public MCH_Key KeyZoom;
   public MCH_Key[] Keys;


   public MCH_ClientTankTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft, config);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      super.updateKeybind(config);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.KeyThrottleUp = this.KeyUp;
      this.KeyThrottleDown = this.KeyDown;
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, super.KeyUseWeapon, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyExtra, super.KeyFreeLook, super.KeyGUI, super.KeyGearUpDown, super.KeyBrake, super.KeyPutToRack, super.KeyDownFromRack};
   }

   protected void update(EntityPlayer player, MCH_EntityTank tank) {
      if(tank.getIsGunnerMode(player)) {
         MCH_SeatInfo seatInfo = tank.getSeatInfo(player);
         if(seatInfo != null) {
            setRotLimitPitch(seatInfo.minPitch, seatInfo.maxPitch, player);
         }
      }

      tank.updateRadar(10);
      tank.updateCameraRotate(player.rotationYaw, player.rotationPitch);
   }

   protected void onTick(boolean inGUI) {
      MCH_Key[] player = this.Keys;
      int tank = player.length;

      for(int isPilot = 0; isPilot < tank; ++isPilot) {
         MCH_Key viewEntityDummy = player[isPilot];
         viewEntityDummy.update();
      }

      super.isBeforeRiding = super.isRiding;
      EntityClientPlayerMP var7 = super.mc.thePlayer;
      MCH_EntityTank var8 = null;
      boolean var9 = true;
      if(var7 != null) {
         if(var7.ridingEntity instanceof MCH_EntityTank) {
            var8 = (MCH_EntityTank)var7.ridingEntity;
         } else if(var7.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat var10 = (MCH_EntitySeat)var7.ridingEntity;
            if(var10.getParent() instanceof MCH_EntityTank) {
               var9 = false;
               var8 = (MCH_EntityTank)var10.getParent();
            }
         } else if(var7.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation var11 = (MCH_EntityUavStation)var7.ridingEntity;
            if(var11.getControlAircract() instanceof MCH_EntityTank) {
               var8 = (MCH_EntityTank)var11.getControlAircract();
            }
         }
      }

      if(var8 != null && var8.getAcInfo() != null) {
         this.update(var7, var8);
         MCH_ViewEntityDummy var12 = MCH_ViewEntityDummy.getInstance(super.mc.theWorld);
         var12.update(var8.camera);
         if(!inGUI) {
            if(!var8.isDestroyed()) {
               this.playerControl(var7, var8, var9);
            }
         } else {
            this.playerControlInGUI(var7, var8, var9);
         }

         boolean hideHand = true;
         if((!var9 || !var8.isAlwaysCameraView()) && !var8.getIsGunnerMode(var7) && var8.getCameraId() <= 0) {
            MCH_Lib.setRenderViewEntity(var7);
            if(!var9 && var8.getCurrentWeaponID(var7) < 0) {
               hideHand = false;
            }
         } else {
            MCH_Lib.setRenderViewEntity(var12);
         }

         if(hideHand) {
            MCH_Lib.disableFirstPersonItemRender(var7.getCurrentEquippedItem());
         }

         super.isRiding = true;
      } else {
         super.isRiding = false;
      }

      if(!super.isBeforeRiding && super.isRiding && var8 != null) {
         MCH_ViewEntityDummy.getInstance(super.mc.theWorld).setPosition(var8.posX, var8.posY + 0.5D, var8.posZ);
      } else if(super.isBeforeRiding && !super.isRiding) {
         MCH_Lib.enableFirstPersonItemRender();
         MCH_Lib.setRenderViewEntity(var7);
         W_Reflection.setCameraRoll(0.0F);
      }

   }

   protected void playerControlInGUI(EntityPlayer player, MCH_EntityTank tank, boolean isPilot) {
      this.commonPlayerControlInGUI(player, tank, isPilot, new MCH_TankPacketPlayerControl());
   }

   protected void playerControl(EntityPlayer player, MCH_EntityTank tank, boolean isPilot) {
      MCH_TankPacketPlayerControl pc = new MCH_TankPacketPlayerControl();
      boolean send = false;
      send = this.commonPlayerControl(player, tank, isPilot, pc);
      if(tank.getAcInfo().defaultFreelook && pc.switchFreeLook > 0) {
         pc.switchFreeLook = 0;
      }

      if(isPilot) {
         if(this.KeySwitchMode.isKeyDown()) {
            if(tank.getIsGunnerMode(player) && tank.canSwitchCameraPos()) {
               pc.switchMode = 0;
               tank.switchGunnerMode(false);
               send = true;
               tank.setCameraId(1);
            } else if(tank.getCameraId() > 0) {
               tank.setCameraId(tank.getCameraId() + 1);
               if(tank.getCameraId() >= tank.getCameraPosNum()) {
                  tank.setCameraId(0);
               }
            } else if(tank.canSwitchGunnerMode()) {
               pc.switchMode = (byte)(tank.getIsGunnerMode(player)?0:1);
               tank.switchGunnerMode(!tank.getIsGunnerMode(player));
               send = true;
               tank.setCameraId(0);
            } else if(tank.canSwitchCameraPos()) {
               tank.setCameraId(1);
            } else {
               playSoundNG();
            }
         }
      } else if(this.KeySwitchMode.isKeyDown()) {
         if(tank.canSwitchGunnerModeOtherSeat(player)) {
            tank.switchGunnerModeOtherSeat(player);
            send = true;
         } else {
            playSoundNG();
         }
      }

      if(this.KeyZoom.isKeyDown()) {
         boolean isUav = tank.isUAV() && !tank.getAcInfo().haveHatch();
         if(!tank.getIsGunnerMode(player) && !isUav) {
            if(isPilot && tank.getAcInfo().haveHatch()) {
               if(tank.canFoldHatch()) {
                  pc.switchHatch = 2;
                  send = true;
               } else if(tank.canUnfoldHatch()) {
                  pc.switchHatch = 1;
                  send = true;
               }
            }
         } else {
            tank.zoomCamera();
            playSound("zoom", 0.5F, 1.0F);
         }
      }

      if(send) {
         W_Network.sendToServer(pc);
      }

   }
}
