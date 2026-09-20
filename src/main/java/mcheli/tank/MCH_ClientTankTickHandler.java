package mcheli.tank;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_BaseVehicleClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_TankPacketPlayerControl;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientTankTickHandler extends MCH_BaseVehicleClientTickHandler {

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
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, super.KeyUseWeapon, super.KeyCurrentWeaponLock, super.KeyVehicleLock, super.KeyRadar, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyChaff, super.KeyMaintenance,super.KeyAPS, super.KeyExtra, super.KeyFreeLook, super.KeyGUI, super.KeyGearUpDown, super.KeyBrake, super.KeyPutToRack, super.KeyDownFromRack};
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
      EntityClientPlayerMP player2 = super.mc.thePlayer;
      MCH_EntityTank aircraftEntity = null;
      boolean isMounted = true;
      if(player2 != null) {
         if(player2.ridingEntity instanceof MCH_EntityTank) {
            aircraftEntity = (MCH_EntityTank)player2.ridingEntity;
         } else if(player2.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat seatEntity = (MCH_EntitySeat)player2.ridingEntity;
            if(seatEntity.getParent() instanceof MCH_EntityTank) {
               isMounted = false;
               aircraftEntity = (MCH_EntityTank)seatEntity.getParent();
            }
         } else if(player2.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation uavStationEntity = (MCH_EntityUavStation)player2.ridingEntity;
            if(uavStationEntity.getControlAircract() instanceof MCH_EntityTank) {
               aircraftEntity = (MCH_EntityTank)uavStationEntity.getControlAircract();
            }
         }
      }

      if(aircraftEntity != null && aircraftEntity.getAcInfo() != null) {
         this.update(player2, aircraftEntity);
         MCH_ViewEntityDummy viewEntityDummy2 = MCH_ViewEntityDummy.getInstance(super.mc.theWorld);
         viewEntityDummy2.update(aircraftEntity.camera);
         if(!inGUI) {
            if(!aircraftEntity.isDestroyed()) {
               this.playerControl(player2, aircraftEntity, isMounted);
            }
         } else {
            this.playerControlInGUI(player2, aircraftEntity, isMounted);
         }

         boolean hideHand = true;
         if((!isMounted || !aircraftEntity.isAlwaysCameraView()) && !aircraftEntity.getIsGunnerMode(player2) && aircraftEntity.getCameraId() <= 0) {
            MCH_Lib.setRenderViewEntity(player2);
            if(!isMounted && aircraftEntity.getCurrentWeaponID(player2) < 0) {
               hideHand = false;
            }
         } else {
            MCH_Lib.setRenderViewEntity(viewEntityDummy2);
         }

         if(hideHand) {
            MCH_Lib.disableFirstPersonItemRender(player2.getCurrentEquippedItem());
         }

         super.isRiding = true;
      } else {
         super.isRiding = false;
      }

      if (!this.isBeforeRiding && this.isRiding && aircraftEntity != null) {
         W_Reflection.setThirdPersonDistance(aircraftEntity.thirdPersonDist);
         MCH_ViewEntityDummy.getInstance(super.mc.theWorld).setPosition(aircraftEntity.posX, aircraftEntity.posY + 0.5D, aircraftEntity.posZ);
      } else if (this.isBeforeRiding && !this.isRiding) {
         W_Reflection.restoreDefaultThirdPersonDistance();
         MCH_Lib.enableFirstPersonItemRender();
         MCH_Lib.setRenderViewEntity(player2);
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
