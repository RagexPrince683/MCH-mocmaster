package mcheli.vehicle;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_BaseVehicleClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_PacketTurretPlayerControl;
import mcheli.vehicle.MCH_TurretInfo;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientTurretTickHandler extends MCH_BaseVehicleClientTickHandler {

   public MCH_Key KeySwitchMode;
   public MCH_Key KeySwitchHovering;
   public MCH_Key KeyZoom;
   public MCH_Key KeyExtra;
   public MCH_Key[] Keys;


   public MCH_ClientTurretTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft, config);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      super.updateKeybind(config);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
      this.KeySwitchHovering = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.KeyExtra = new MCH_Key(MCH_Config.KeyExtra.prmInt);
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, this.KeySwitchHovering, super.KeyUseWeapon, super.KeyCurrentWeaponLock, super.KeyVehicleLock, super.KeyRadar, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyChaff, super.KeyMaintenance,super.KeyAPS, this.KeyExtra, super.KeyFreeLook, super.KeyGUI};
   }

   protected void update(EntityPlayer player, MCH_EntityTurret vehicle, MCH_TurretInfo info) {
      if(info != null) {
         setRotLimitPitch(info.minRotationPitch, info.maxRotationPitch, player);
      }

      vehicle.updateCameraRotate(player.rotationYaw, player.rotationPitch);
      vehicle.updateRadar(5);
   }

   protected void onTick(boolean inGUI) {
      MCH_Key[] player = this.Keys;
      int vehicle = player.length;

      for(int isPilot = 0; isPilot < vehicle; ++isPilot) {
         MCH_Key viewEntityDummy = player[isPilot];
         viewEntityDummy.update();
      }

      super.isBeforeRiding = super.isRiding;
      EntityClientPlayerMP player2 = super.mc.thePlayer;
      MCH_EntityTurret turretEntity = null;
      boolean isMounted = true;
      if(player2 != null) {
         if(player2.ridingEntity instanceof MCH_EntityTurret) {
            turretEntity = (MCH_EntityTurret)player2.ridingEntity;
         } else if(player2.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat seatEntity = (MCH_EntitySeat)player2.ridingEntity;
            if(seatEntity.getParent() instanceof MCH_EntityTurret) {
               isMounted = false;
               turretEntity = (MCH_EntityTurret)seatEntity.getParent();
            }
         }
      }

      if(turretEntity != null && turretEntity.getAcInfo() != null) {
         MCH_Lib.disableFirstPersonItemRender(player2.getCurrentEquippedItem());
         this.update(player2, turretEntity, turretEntity.getTurretInfo());
         MCH_ViewEntityDummy viewEntityDummy2 = MCH_ViewEntityDummy.getInstance(super.mc.theWorld);
         viewEntityDummy2.update(turretEntity.camera);
         if(!inGUI) {
            if(!turretEntity.isDestroyed()) {
               this.playerControl(player2, turretEntity, isMounted);
            }
         } else {
            this.playerControlInGUI(player2, turretEntity, isMounted);
         }

         MCH_Lib.setRenderViewEntity(viewEntityDummy2);
         super.isRiding = true;
      } else {
         super.isRiding = false;
      }

      if (!this.isBeforeRiding && this.isRiding) {
         W_Reflection.setThirdPersonDistance(turretEntity.thirdPersonDist);
      } else if (this.isBeforeRiding && !this.isRiding) {
         W_Reflection.restoreDefaultThirdPersonDistance();
         MCH_Lib.enableFirstPersonItemRender();
         MCH_Lib.setRenderViewEntity(player2);
      }

   }

   protected void playerControlInGUI(EntityPlayer player, MCH_EntityTurret vehicle, boolean isPilot) {
      this.commonPlayerControlInGUI(player, vehicle, isPilot, new MCH_PacketTurretPlayerControl());
   }

   protected void playerControl(EntityPlayer player, MCH_EntityTurret vehicle, boolean isPilot) {
      MCH_PacketTurretPlayerControl pc = new MCH_PacketTurretPlayerControl();
      boolean send = false;
      send = this.commonPlayerControl(player, vehicle, isPilot, pc);
      if(vehicle.getAcInfo().defaultFreelook && pc.switchFreeLook > 0) {
         pc.switchFreeLook = 0;
      }
      if(pc.useWeapon) {
         pc.weaponAimYaw = player.rotationYaw;
         pc.weaponAimPitch = player.rotationPitch;
      }
      if(this.KeyExtra.isKeyDown()) {
         if(vehicle.getTowChainEntity() != null) {
            playSoundOK();
            pc.unhitchChainId = W_Entity.getEntityId(vehicle.getTowChainEntity());
            send = true;
         } else {
            playSoundNG();
         }
      }

      if(!this.KeySwitchHovering.isKeyDown() && this.KeySwitchMode.isKeyDown()) {
         ;
      }

      if(this.KeyZoom.isKeyDown()) {
         if(vehicle.canZoom()) {
            vehicle.zoomCamera();
            playSound("zoom", 0.5F, 1.0F);
         } else if(vehicle.getAcInfo().haveHatch()) {
            if(vehicle.canFoldHatch()) {
               pc.switchHatch = 2;
               send = true;
            } else if(vehicle.canUnfoldHatch()) {
               pc.switchHatch = 1;
               send = true;
            } else {
               playSoundNG();
            }
         }
      }

      if(send) {
         W_Network.sendToServer(pc);
      }

   }
}
