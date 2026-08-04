package mcheli.vehicle;

import com.google.common.io.ByteArrayDataInput;
import mcheli.chain.MCH_EntityChain;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_PacketTurretPlayerControl;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_TurretPacketHandler {

   public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {
      if(player.ridingEntity instanceof MCH_EntityTurret) {
         if(!player.worldObj.isRemote) {
            MCH_PacketTurretPlayerControl pc = new MCH_PacketTurretPlayerControl();
            pc.readData(data);
            MCH_EntityTurret vehicle = (MCH_EntityTurret)player.ridingEntity;
            mcheli.aircraft.MCH_BaseVehiclePacketHandler.handleVehicleAccessLockToggle(player, vehicle, pc);
            mcheli.aircraft.MCH_BaseVehiclePacketHandler.handleRadarToggle(player, vehicle, pc);
            if(pc.isUnmount == 1) {
               vehicle.unmountEntity();
            } else if(pc.isUnmount == 2) {
               vehicle.unmountCrew();
            } else {
               if(pc.switchSearchLight) {
                  vehicle.setSearchLight(!vehicle.isSearchLightON());
               }

               if(pc.switchCameraMode > 0) {
                  vehicle.switchCameraMode(player, pc.switchCameraMode - 1);
               }

               if(pc.switchFreeLook > 0 && vehicle.isPilot(player)
                     && vehicle.canSwitchFreeLook() && !vehicle.getAcInfo().defaultFreelook) {
                  vehicle.switchFreeLookMode(pc.switchFreeLook == 1);
               }

               if(pc.switchWeapon >= 0) {
                  vehicle.switchWeapon(player, pc.switchWeapon);
               }

               if(pc.useWeapon) {
                  player.rotationYaw = pc.weaponAimYaw;
                  player.rotationPitch = pc.weaponAimPitch;
                  vehicle.lastRiderYaw = pc.weaponAimYaw;
                  vehicle.lastRiderPitch = pc.weaponAimPitch;
                  MCH_WeaponParam e = new MCH_WeaponParam();
                  e.entity = vehicle;
                  e.user = player;
                  e.setPosAndRot(pc.useWeaponPosX, pc.useWeaponPosY, pc.useWeaponPosZ, 0.0F, 0.0F);
                  e.option1 = pc.useWeaponOption1;
                  e.option2 = pc.useWeaponOption2;
                  vehicle.useCurrentWeapon(e);
               }

               if(vehicle.isPilot(player)) {
                  vehicle.throttleUp = pc.throttleUp;
                  vehicle.throttleDown = pc.throttleDown;
                  vehicle.moveLeft = pc.moveLeft;
                  vehicle.moveRight = pc.moveRight;
               }

               if(pc.useFlareType > 0) {
                  vehicle.useFlare(pc.useFlareType);
               }

               if(pc.useChaff) {
                  vehicle.useChaff();
               }

               if(pc.useMaintenance) {
                  vehicle.useMaintenance();
               }

               if(pc.useAPS) {
                  vehicle.useAPS(player);
               }

               if(pc.unhitchChainId >= 0) {
                  Entity e1 = player.worldObj.getEntityByID(pc.unhitchChainId);
                  if(e1 instanceof MCH_EntityChain) {
                     e1.setDead();
                  }
               }

               if(pc.openGui) {
                  vehicle.openGui(player);
               }

               if(pc.switchHatch > 0) {
                  vehicle.foldHatch(pc.switchHatch == 2);
               }

               if(pc.isUnmount == 3) {
                  vehicle.unmountAircraft();
               }
            }

         }
      }
   }
}
