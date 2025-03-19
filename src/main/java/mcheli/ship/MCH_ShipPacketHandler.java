package mcheli.ship;

import com.google.common.io.ByteArrayDataInput;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ShipPacketHandler {

    public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {
        if(!player.worldObj.isRemote) {
            MCH_ShipPacketPlayerControl pc = new MCH_ShipPacketPlayerControl();
            pc.readData(data);
            boolean isPilot = true;
            MCH_EntityShip ship = null;
            if(player.ridingEntity instanceof MCH_EntityShip) {
                System.out.println("MCH_ShipPacketHandler.onPacket_PlayerControl: player.ridingEntity instanceof MCH_EntityShip");
                ship = (MCH_EntityShip)player.ridingEntity;
            } else if(player.ridingEntity instanceof MCH_EntitySeat) {
                if(((MCH_EntitySeat)player.ridingEntity).getParent() instanceof MCH_EntityShip) {
                    ship = (MCH_EntityShip)((MCH_EntitySeat)player.ridingEntity).getParent();
                }
            } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
                MCH_EntityUavStation ac = (MCH_EntityUavStation)player.ridingEntity;
                if(ac.getControlAircract() instanceof MCH_EntityShip) {
                    ship = (MCH_EntityShip)ac.getControlAircract();
                }
            }

            if(ship != null) {
                if(pc.isUnmount == 1) {
                    ship.unmountEntity();
                } else if(pc.isUnmount == 2) {
                    ship.unmountCrew();
                } else if(pc.ejectSeat) {
                    ship.ejectSeat(player);
                } else {
                   // if(pc.switchVtol == 0) {
                   //     plane.swithVtolMode(false);
                   // }
//
                   // if(pc.switchVtol == 1) {
                   //     plane.swithVtolMode(true);
                   // }

                    if(pc.switchMode == 0) {
                        ship.switchGunnerMode(false);
                    }

                    if(pc.switchMode == 1) {
                        ship.switchGunnerMode(true);
                    }

                    if(pc.switchMode == 2) {
                        ship.switchHoveringMode(false);
                    }

                    if(pc.switchMode == 3) {
                        ship.switchHoveringMode(true);
                    }

                    if(pc.switchSearchLight) {
                        ship.setSearchLight(!ship.isSearchLightON());
                    }

                    if(pc.switchCameraMode > 0) {
                        ship.switchCameraMode(player, pc.switchCameraMode - 1);
                    }

                    if(pc.switchWeapon >= 0) {
                        ship.switchWeapon(player, pc.switchWeapon);
                    }

                    if(pc.useWeapon) {
                        MCH_WeaponParam prm = new MCH_WeaponParam();
                        prm.entity = ship;
                        prm.user = player;
                        prm.setPosAndRot(pc.useWeaponPosX, pc.useWeaponPosY, pc.useWeaponPosZ, 0.0F, 0.0F);
                        prm.option1 = pc.useWeaponOption1;
                        prm.option2 = pc.useWeaponOption2;
                        ship.useCurrentWeapon(prm);
                    }

                    if(ship.isPilot(player)) {
                        ship.throttleUp = pc.throttleUp;
                        ship.throttleDown = pc.throttleDown;
                        ship.moveLeft = pc.moveLeft;
                        ship.moveRight = pc.moveRight;
                    }

                    if(pc.useFlareType > 0) {
                        ship.useFlare(pc.useFlareType);
                    }

                    if(pc.openGui) {
                        ship.openGui(player);
                    }

                    if(pc.switchHatch > 0) {
                        if(ship.getAcInfo().haveHatch()) {
                            ship.foldHatch(pc.switchHatch == 2);
                        } else {
                            //plane.foldWing(pc.switchHatch == 2);
                        }
                    }

                    if(pc.switchFreeLook > 0) {
                        ship.switchFreeLookMode(pc.switchFreeLook == 1);
                    }

                   // if(pc.switchGear == 1) {
                   //     plane.foldLandingGear();
                   // }
//
                   // if(pc.switchGear == 2) {
                   //     plane.unfoldLandingGear();
                   // }

                    if(pc.putDownRack == 1) {
                        ship.mountEntityToRack();
                    }

                    if(pc.putDownRack == 2) {
                        ship.unmountEntityFromRack();
                    }

                    if(pc.putDownRack == 3) {
                        ship.rideRack();
                    }

                    if(pc.isUnmount == 3) {
                        ship.unmountAircraft();
                    }
                }

            }
        }
    }

}
