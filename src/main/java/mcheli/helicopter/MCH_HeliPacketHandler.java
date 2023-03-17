package mcheli.helicopter;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Achievement;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.chain.MCH_EntityChain;
import mcheli.container.MCH_EntityContainer;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_HeliPacketHandler {

   public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_HeliPacketPlayerControl pc = new MCH_HeliPacketPlayerControl();
         pc.readData(data);
         MCH_EntityHeli heli = null;
         if(player.ridingEntity instanceof MCH_EntityHeli) {
            heli = (MCH_EntityHeli)player.ridingEntity;
         } else if(player.ridingEntity instanceof MCH_EntitySeat) {
            if(((MCH_EntitySeat)player.ridingEntity).getParent() instanceof MCH_EntityHeli) {
               heli = (MCH_EntityHeli)((MCH_EntitySeat)player.ridingEntity).getParent();
            }
         } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation ac = (MCH_EntityUavStation)player.ridingEntity;
            if(ac.getControlAircract() instanceof MCH_EntityHeli) {
               heli = (MCH_EntityHeli)ac.getControlAircract();
            }
         }

         if(heli != null) {
            if(pc.isUnmount == 1) {
               heli.unmountEntity();
            } else if(pc.isUnmount == 2) {
               heli.unmountCrew();
            } else {
               if(pc.switchFold == 0) {
                  heli.setFoldBladeStat((byte)3);
               }

               if(pc.switchFold == 1) {
                  heli.setFoldBladeStat((byte)1);
               }

               if(pc.switchMode == 0) {
                  heli.switchGunnerMode(false);
               }

               if(pc.switchMode == 1) {
                  heli.switchGunnerMode(true);
               }

               if(pc.switchMode == 2) {
                  heli.switchHoveringMode(false);
               }

               if(pc.switchMode == 3) {
                  heli.switchHoveringMode(true);
               }

               if(pc.switchSearchLight) {
                  heli.setSearchLight(!heli.isSearchLightON());
               }

               if(pc.switchCameraMode > 0) {
                  heli.switchCameraMode(player, pc.switchCameraMode - 1);
               }

               if(pc.switchWeapon >= 0) {
                  heli.switchWeapon(player, pc.switchWeapon);
               }

               if(pc.useWeapon) {
                  MCH_WeaponParam e = new MCH_WeaponParam();
                  e.entity = heli;
                  e.user = player;
                  e.setPosAndRot(pc.useWeaponPosX, pc.useWeaponPosY, pc.useWeaponPosZ, 0.0F, 0.0F);
                  e.option1 = pc.useWeaponOption1;
                  e.option2 = pc.useWeaponOption2;
                  heli.useCurrentWeapon(e);
               }

               if(heli.isPilot(player)) {
                  heli.throttleUp = pc.throttleUp;
                  heli.throttleDown = pc.throttleDown;
                  heli.moveLeft = pc.moveLeft;
                  heli.moveRight = pc.moveRight;
               }

               if(pc.useFlareType > 0) {
                  heli.useFlare(pc.useFlareType);
               }

               if(pc.unhitchChainId >= 0) {
                  Entity e1 = player.worldObj.getEntityByID(pc.unhitchChainId);
                  if(e1 instanceof MCH_EntityChain) {
                     if(((MCH_EntityChain)e1).towedEntity instanceof MCH_EntityContainer && MCH_Lib.getBlockIdY(heli, 3, -20) == 0) {
                        MCH_Achievement.addStat(player, MCH_Achievement.reliefSupplies, 1);
                     }

                     e1.setDead();
                  }
               }

               if(pc.openGui) {
                  heli.openGui(player);
               }

               if(pc.switchHatch > 0) {
                  heli.foldHatch(pc.switchHatch == 2);
               }

               if(pc.switchFreeLook > 0) {
                  heli.switchFreeLookMode(pc.switchFreeLook == 1);
               }

               if(pc.switchGear == 1) {
                  heli.foldLandingGear();
               }

               if(pc.switchGear == 2) {
                  heli.unfoldLandingGear();
               }

               if(pc.putDownRack == 1) {
                  heli.mountEntityToRack();
               }

               if(pc.putDownRack == 2) {
                  heli.unmountEntityFromRack();
               }

               if(pc.putDownRack == 3) {
                  heli.rideRack();
               }

               if(pc.isUnmount == 3) {
                  heli.unmountAircraft();
               }
            }

         }
      }
   }
}
