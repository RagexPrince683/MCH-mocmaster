package mcheli.tank;

import com.google.common.io.ByteArrayDataInput;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_TankPacketHandler {

   public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_TankPacketPlayerControl pc = new MCH_TankPacketPlayerControl();
         pc.readData(data);
         boolean isPilot = true;
         MCH_EntityTank tank = null;
         if(player.ridingEntity instanceof MCH_EntityTank) {
            tank = (MCH_EntityTank)player.ridingEntity;
         } else if(player.ridingEntity instanceof MCH_EntitySeat) {
            if(((MCH_EntitySeat)player.ridingEntity).getParent() instanceof MCH_EntityTank) {
               tank = (MCH_EntityTank)((MCH_EntitySeat)player.ridingEntity).getParent();
            }
         } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation ac = (MCH_EntityUavStation)player.ridingEntity;
            if(ac.getControlAircract() instanceof MCH_EntityTank) {
               tank = (MCH_EntityTank)ac.getControlAircract();
            }
         }

         if(tank != null) {
            if(pc.isUnmount == 1) {
               tank.unmountEntity();
            } else if(pc.isUnmount == 2) {
               tank.unmountCrew();
            } else if(pc.ejectSeat) {
               tank.ejectSeat(player);
            } else {
               if(pc.switchMode == 0) {
                  tank.switchGunnerMode(false);
               }

               if(pc.switchMode == 1) {
                  tank.switchGunnerMode(true);
               }

               if(pc.switchMode == 2) {
                  tank.switchHoveringMode(false);
               }

               if(pc.switchMode == 3) {
                  tank.switchHoveringMode(true);
               }

               if(pc.switchSearchLight) {
                  tank.setSearchLight(!tank.isSearchLightON());
               }

               if(pc.switchCameraMode > 0) {
                  tank.switchCameraMode(player, pc.switchCameraMode - 1);
               }

               if(pc.switchWeapon >= 0) {
                  tank.switchWeapon(player, pc.switchWeapon);
               }

               if(pc.useWeapon) {
                  MCH_WeaponParam dx = new MCH_WeaponParam();
                  dx.entity = tank;
                  dx.user = player;
                  dx.setPosAndRot(pc.useWeaponPosX, pc.useWeaponPosY, pc.useWeaponPosZ, 0.0F, 0.0F);
                  dx.option1 = pc.useWeaponOption1;
                  dx.option2 = pc.useWeaponOption2;
                  tank.useCurrentWeapon(dx);
               }

               if(tank.isPilot(player)) {
                  tank.throttleUp = pc.throttleUp;
                  tank.throttleDown = pc.throttleDown;
                  double dx1 = tank.posX - tank.prevPosX;
                  double dz = tank.posZ - tank.prevPosZ;
                  double dist = dx1 * dx1 + dz * dz;
                  if(pc.useBrake && tank.getCurrentThrottle() <= 0.03D && dist < 0.01D) {
                     tank.moveLeft = false;
                     tank.moveRight = false;
                  }

                  tank.setBrake(pc.useBrake);
               }

               if(pc.useFlareType > 0) {
                  tank.useFlare(pc.useFlareType);
               }

               if(pc.openGui) {
                  tank.openGui(player);
               }

               if(pc.switchHatch > 0 && tank.getAcInfo().haveHatch()) {
                  tank.foldHatch(pc.switchHatch == 2);
               }

               if(pc.switchFreeLook > 0) {
                  tank.switchFreeLookMode(pc.switchFreeLook == 1);
               }

               if(pc.switchGear == 1) {
                  tank.foldLandingGear();
               }

               if(pc.switchGear == 2) {
                  tank.unfoldLandingGear();
               }

               if(pc.putDownRack == 1) {
                  tank.mountEntityToRack();
               }

               if(pc.putDownRack == 2) {
                  tank.unmountEntityFromRack();
               }

               if(pc.putDownRack == 3) {
                  tank.rideRack();
               }

               if(pc.isUnmount == 3) {
                  tank.unmountAircraft();
               }
            }

         }
      }
   }
}
