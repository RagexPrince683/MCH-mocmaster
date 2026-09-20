package mcheli.helicopter;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_BaseVehicleClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliPacketPlayerControl;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientHeliTickHandler extends MCH_BaseVehicleClientTickHandler {

   public MCH_Key KeySwitchMode;
   public MCH_Key KeySwitchHovering;
   public MCH_Key KeyZoom;
   public MCH_Key[] Keys;
   public MCH_Key KeyEjectSeat;


   public MCH_ClientHeliTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft, config);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      super.updateKeybind(config);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
      this.KeyEjectSeat = new MCH_Key(MCH_Config.KeyEjectHeli.prmInt);
      this.KeySwitchHovering = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, this.KeyEjectSeat, this.KeySwitchHovering, super.KeyUseWeapon, super.KeyCurrentWeaponLock, super.KeyVehicleLock, super.KeyRadar, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyChaff, super.KeyMaintenance, super.KeyAPS, super.KeyExtra, super.KeyFreeLook, super.KeyGUI, super.KeyGearUpDown, super.KeyPutToRack, super.KeyDownFromRack};
   }

   protected void update(EntityPlayer player, MCH_EntityHeli heli, boolean isPilot) {
      if(heli.getIsGunnerMode(player)) {
         MCH_SeatInfo seatInfo = heli.getSeatInfo(player);
         if(seatInfo != null) {
            setRotLimitPitch(seatInfo.minPitch, seatInfo.maxPitch, player);
         }
      }

      heli.updateCameraRotate(player.rotationYaw, player.rotationPitch);
      heli.updateRadar(5);
   }

   protected void onTick(boolean inGUI) {
      MCH_Key[] player = this.Keys;
      int heli = player.length;

      for(int isPilot = 0; isPilot < heli; ++isPilot) {
         MCH_Key viewEntityDummy = player[isPilot];
         viewEntityDummy.update();
      }

      super.isBeforeRiding = super.isRiding;
      EntityClientPlayerMP player2 = super.mc.thePlayer;
      MCH_EntityHeli aircraftEntity = null;
      boolean isMounted = true;
      if(player2 != null) {
         if(player2.ridingEntity instanceof MCH_EntityHeli) {
            aircraftEntity = (MCH_EntityHeli)player2.ridingEntity;
         } else if(player2.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat seatEntity = (MCH_EntitySeat)player2.ridingEntity;
            if(seatEntity.getParent() instanceof MCH_EntityHeli) {
               isMounted = false;
               aircraftEntity = (MCH_EntityHeli)seatEntity.getParent();
            }
         } else if(player2.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation uavStationEntity = (MCH_EntityUavStation)player2.ridingEntity;
            if(uavStationEntity.getControlAircract() instanceof MCH_EntityHeli) {
               aircraftEntity = (MCH_EntityHeli)uavStationEntity.getControlAircract();
            }
         }
      }

      if(aircraftEntity != null && aircraftEntity.getAcInfo() != null) {
         this.update(player2, aircraftEntity, isMounted);
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
         if((!isMounted || !aircraftEntity.isAlwaysCameraView()) && !aircraftEntity.getIsGunnerMode(player2)) {
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

      if (!this.isBeforeRiding && this.isRiding) {
         W_Reflection.setThirdPersonDistance(aircraftEntity.thirdPersonDist);
      } else if (this.isBeforeRiding && !this.isRiding) {
         W_Reflection.restoreDefaultThirdPersonDistance();
         W_Reflection.setCameraRoll(0.0F);
         MCH_Lib.enableFirstPersonItemRender();
         MCH_Lib.setRenderViewEntity(player2);
      }

   }

   protected void playerControlInGUI(EntityPlayer player, MCH_EntityHeli heli, boolean isPilot) {
      this.commonPlayerControlInGUI(player, heli, isPilot, new MCH_HeliPacketPlayerControl());
   }

   protected void playerControl(EntityPlayer player, MCH_EntityHeli heli, boolean isPilot) {
      MCH_HeliPacketPlayerControl pc = new MCH_HeliPacketPlayerControl();
      boolean send = false;
      send = this.commonPlayerControl(player, heli, isPilot, pc);
      if(isPilot) {
         if(super.KeyExtra.isKeyDown()) {
            if(heli.getTowChainEntity() != null) {
               playSoundOK();
               pc.unhitchChainId = W_Entity.getEntityId(heli.getTowChainEntity());
               send = true;
            } else if(heli.canSwitchFoldBlades()) {
               if(heli.isFoldBlades()) {
                  heli.unfoldBlades();
                  pc.switchFold = 0;
               } else {
                  heli.foldBlades();
                  pc.switchFold = 1;
               }

               send = true;
               playSoundOK();
            } else {
               playSoundNG();
            }
         }

         if(this.KeySwitchHovering.isKeyDown()) {
            if(heli.getIsGunnerMode(player)) {
               playSoundNG();
            } else if(heli.canSwitchHoveringMode()) {
               pc.switchMode = (byte)(heli.isHoveringMode()?2:3);
               heli.switchHoveringMode(!heli.isHoveringMode());
               send = true;
            } else {
               playSoundNG();
            }
         } else if(this.KeySwitchMode.isKeyDown()) {
            if(heli.canSwitchGunnerMode()) {
               pc.switchMode = (byte)(heli.getIsGunnerMode(player)?0:1);
               heli.switchGunnerMode(!heli.getIsGunnerMode(player));
               send = true;
            } else {
               playSoundNG();
            }
         }
      } else if(this.KeySwitchMode.isKeyDown()) {
         if(heli.canSwitchGunnerModeOtherSeat(player)) {
            heli.switchGunnerModeOtherSeat(player);
            send = true;
         } else {
            playSoundNG();
         }
      }

      if(this.KeyZoom.isKeyDown()) {
         boolean isUav = heli.isUAV() && !heli.getAcInfo().haveHatch();
         if(!heli.getIsGunnerMode(player) && !isUav) {
            if(isPilot && heli.getAcInfo().haveHatch()) {
               if(heli.canFoldHatch()) {
                  pc.switchHatch = 2;
                  send = true;
               } else if(heli.canUnfoldHatch()) {
                  pc.switchHatch = 1;
                  send = true;
               } else {
                  playSoundNG();
               }
            }
         } else {
            heli.zoomCamera();
            playSound("zoom", 0.5F, 1.0F);
         }
      }

      if(this.KeyEjectSeat.isKeyDown() && heli.canEjectSeat(player)) {
         pc.ejectSeat = true;
         send = true;
      }

      if(send) {
         W_Network.sendToServer(pc);
      }

   }
}
