package mcheli.ship;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_BaseVehicleClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientShipTickHandler extends MCH_BaseVehicleClientTickHandler {

    public MCH_Key KeySwitchMode;
    public MCH_Key KeyEjectSeat;
    public MCH_Key KeyZoom;
    public MCH_Key KeySubmarineAscend;
    public MCH_Key KeySubmarineDescend;
    public MCH_Key[] Keys;
    //public MCH_Key KeySwitchHovering;


    public MCH_ClientShipTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft, config);
        this.updateKeybind(config);
    }

    public void updateKeybind(MCH_Config config) {
        super.updateKeybind(config);
        this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
        this.KeyEjectSeat = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
        this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
        this.KeySubmarineAscend = new MCH_Key(MCH_Config.KeySubmarineAscend.prmInt);
        this.KeySubmarineDescend = new MCH_Key(MCH_Config.KeySubmarineDescend.prmInt);
        this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, this.KeyEjectSeat, this.KeySubmarineAscend, this.KeySubmarineDescend, super.KeyUseWeapon, super.KeyVehicleLock, super.KeyRadar, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyExtra, super.KeyFreeLook, super.KeyGUI, super.KeyGearUpDown, super.KeyPutToRack, super.KeyDownFromRack};
    }

    protected void update(EntityPlayer player, MCH_EntityShip plane) {
        if(plane.getIsGunnerMode(player)) {
            MCH_SeatInfo seatInfo = plane.getSeatInfo(player);
            if(seatInfo != null) {
                setRotLimitPitch(seatInfo.minPitch, seatInfo.maxPitch, player);
            }
        }

        plane.updateRadar(10);
        plane.updateCameraRotate(player.rotationYaw, player.rotationPitch);
    }

    protected void onTick(boolean inGUI) {
        MCH_Key[] player = this.Keys;
        int plane = player.length;

        for(int isPilot = 0; isPilot < plane; ++isPilot) {
            MCH_Key viewEntityDummy = player[isPilot];
            viewEntityDummy.update();
        }

        super.isBeforeRiding = super.isRiding;
        EntityClientPlayerMP player2 = super.mc.thePlayer;
        MCH_EntityShip shipEntity = null;
        boolean isMounted = true;
        if(player2 != null) {
            if(player2.ridingEntity instanceof MCH_EntityShip) {
                shipEntity = (MCH_EntityShip)player2.ridingEntity;
            } else if(player2.ridingEntity instanceof MCH_EntitySeat) {
                MCH_EntitySeat seatEntity = (MCH_EntitySeat)player2.ridingEntity;
                if(seatEntity.getParent() instanceof MCH_EntityShip) {
                    isMounted = false;
                    shipEntity = (MCH_EntityShip)seatEntity.getParent();
                }
            } else if(player2.ridingEntity instanceof MCH_EntityUavStation) {
                MCH_EntityUavStation uavStationEntity = (MCH_EntityUavStation)player2.ridingEntity;
                if(uavStationEntity.getControlAircract() instanceof MCH_EntityShip) {
                    shipEntity = (MCH_EntityShip)uavStationEntity.getControlAircract();
                }
            }
        }

        if(shipEntity != null && shipEntity.getAcInfo() != null) {
            this.update(player2, shipEntity);
            MCH_ViewEntityDummy viewEntityDummy2 = MCH_ViewEntityDummy.getInstance(super.mc.theWorld);
            viewEntityDummy2.update(shipEntity.camera);
            if(!inGUI) {
                if(!shipEntity.isDestroyed()) {
                    this.playerControl(player2, shipEntity, isMounted);
                }
            } else {
                this.playerControlInGUI(player2, shipEntity, isMounted);
            }

            boolean hideHand = true;
            if((!isMounted || !shipEntity.isAlwaysCameraView()) && !shipEntity.getIsGunnerMode(player2) && shipEntity.getCameraId() <= 0) {
                MCH_Lib.setRenderViewEntity(player2);
                if(!isMounted && shipEntity.getCurrentWeaponID(player2) < 0) {
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

        if(!super.isBeforeRiding && super.isRiding && shipEntity != null) {
            MCH_ViewEntityDummy.getInstance(super.mc.theWorld).setPosition(shipEntity.posX, shipEntity.posY + 0.5D, shipEntity.posZ);
        } else if(super.isBeforeRiding && !super.isRiding) {
            MCH_Lib.enableFirstPersonItemRender();
            MCH_Lib.setRenderViewEntity(player2);
            W_Reflection.setCameraRoll(0.0F);
        }

    }

    protected void playerControlInGUI(EntityPlayer player, MCH_EntityShip plane, boolean isPilot) {
        this.commonPlayerControlInGUI(player, plane, isPilot, new MCH_ShipPacketPlayerControl());
    }

    protected void playerControl(EntityPlayer player, MCH_EntityShip plane, boolean isPilot) {
        MCH_ShipPacketPlayerControl pc = new MCH_ShipPacketPlayerControl();
        boolean send = false;
        send = this.commonPlayerControl(player, plane, isPilot, pc);
        boolean isUav;
        if(isPilot) {
            boolean submarineAscend = plane.isDiving && this.KeySubmarineAscend.isKeyPress();
            boolean submarineDescend = plane.isDiving && this.KeySubmarineDescend.isKeyPress();
            if(submarineAscend != plane.submarineAscend || submarineDescend != plane.submarineDescend) {
                send = true;
            }
            pc.submarineAscend = plane.submarineAscend = submarineAscend;
            pc.submarineDescend = plane.submarineDescend = submarineDescend;

            if(this.KeySwitchMode.isKeyDown()) {
                if(plane.getIsGunnerMode(player) && plane.canSwitchCameraPos()) {
                    pc.switchMode = 0;
                    plane.switchGunnerMode(false);
                    send = true;
                    plane.setCameraId(1);
                } else if(plane.getCameraId() > 0) {
                    plane.setCameraId(plane.getCameraId() + 1);
                    if(plane.getCameraId() >= plane.getCameraPosNum()) {
                        plane.setCameraId(0);
                    }
                } else if(plane.canSwitchGunnerMode()) {
                    pc.switchMode = (byte)(plane.getIsGunnerMode(player)?0:1);
                    plane.switchGunnerMode(!plane.getIsGunnerMode(player));
                    send = true;
                    plane.setCameraId(0);
                } else if(plane.canSwitchCameraPos()) {
                    plane.setCameraId(1);
                } else {
                    playSoundNG();
                }
            }

            if(super.KeyExtra.isKeyDown()) {
                if(plane.canSwitchVtol()) {
                    isUav = plane.getNozzleStat();
                    if(!isUav) {
                        pc.switchVtol = 1;
                    } else {
                        pc.switchVtol = 0;
                    }

                    plane.swithVtolMode(!isUav);
                    //plane.startDiving();
                    send = true;
                } else {
                    //if theres no 'vtol' (diving in this case)
                    playSoundNG();
                }
            }
        } else if(this.KeySwitchMode.isKeyDown()) {
            if(plane.canSwitchGunnerModeOtherSeat(player)) {
                plane.switchGunnerModeOtherSeat(player);
                send = true;
            } else {
                playSoundNG();
            }
        }

        if(this.KeyZoom.isKeyDown()) {
            isUav = plane.isUAV() && !plane.getAcInfo().haveHatch() && !plane.getShipInfo().haveWing();
            if(!plane.getIsGunnerMode(player) && !isUav) {
                if(isPilot) {
                    if(plane.getAcInfo().haveHatch()) {
                        if(plane.canFoldHatch()) {
                            pc.switchHatch = 2;
                            send = true;
                        } else if(plane.canUnfoldHatch()) {
                            pc.switchHatch = 1;
                            send = true;
                        }
                    } else if(plane.canFoldWing()) {
                        pc.switchHatch = 2;
                        send = true;
                    } else if(plane.canUnfoldWing()) {
                        pc.switchHatch = 1;
                        send = true;
                    }
                }
            } else {
                plane.zoomCamera();
                playSound("zoom", 0.5F, 1.0F);
            }
        }

        if(this.KeyEjectSeat.isKeyDown() && plane.canEjectSeat(player)) {
            pc.ejectSeat = true;
            send = true;
        }

        if(send) {
            W_Network.sendToServer(pc);
        }

    }
}
