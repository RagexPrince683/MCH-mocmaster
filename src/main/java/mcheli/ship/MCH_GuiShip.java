package mcheli.ship;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftCommonGui;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.gui.MCH_Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiShip extends MCH_AircraftCommonGui {

    public MCH_GuiShip(Minecraft minecraft) {
        super(minecraft);
    }

    public boolean isDrawGui(EntityPlayer player) {
        return MCH_EntityAircraft.getAircraft_RiddenOrControl(player) instanceof MCH_EntityShip;
    }

    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if(ac instanceof MCH_EntityShip && !ac.isDestroyed()) {
            MCH_EntityShip ship = (MCH_EntityShip)ac;
            int seatID = ac.getSeatIdByEntity(player);
            GL11.glLineWidth((float) MCH_Gui.scaleFactor);
            if(ship.getCameraMode(player) == 1) {
                this.drawNightVisionNoise();
            }

            MCH_Config var10000;
            label50: {
                if(isThirdPersonView) {
                    var10000 = MCH_MOD.config;
                    if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                        break label50;
                    }
                }

                if(seatID == 0 && ship.getIsGunnerMode(player)) {
                    this.drawHud(ac, player, 1);
                } else {
                    this.drawHud(ac, player, seatID);
                }
            }

            label51: {
                this.drawDebugtInfo(ship);
                if(isThirdPersonView) {
                    var10000 = MCH_MOD.config;
                    if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                        break label51;
                    }
                }

                if(ship.getTVMissile() != null && (ship.getIsGunnerMode(player) || ship.isUAV())) {
                    this.drawTvMissileNoise(ship, ship.getTVMissile());
                } else {
                    this.drawKeybind(ship, player, seatID);
                }
            }

            this.drawHitBullet(ship, -14101432, seatID);
        }
    }

    public void drawKeybind(MCH_EntityShip ship, EntityPlayer player, int seatID) {
        MCH_Config var10000 = MCH_MOD.config;
        if(!MCH_Config.HideKeybind.prmBool) {
            MCH_ShipInfo info = ship.getShipInfo();
            if(info != null) {
                int colorActive = -1342177281;
                int colorInactive = -1349546097;
                int RX = super.centerX + 120;
                int LX = super.centerX - 200;
                this.drawKeyBind(ship, info, player, seatID, RX, LX, colorActive, colorInactive);
                String msg;
                StringBuilder var12;
                MCH_Config var10001;
                if(seatID == 0 && info.isEnableGunnerMode) {
                    var10000 = MCH_MOD.config;
                    if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                        int c = ship.isHoveringMode()?colorInactive:colorActive;
                        var12 = (new StringBuilder()).append(ship.getIsGunnerMode(player)?"Normal":"Gunner").append(" : ");
                        var10001 = MCH_MOD.config;
                        msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
                        this.drawString(msg, RX, super.centerY - 70, c);
                    }
                }

                if(seatID > 0 && ship.canSwitchGunnerModeOtherSeat(player)) {
                    var12 = (new StringBuilder()).append(ship.getIsGunnerMode(player)?"Normal":"Camera").append(" : ");
                    var10001 = MCH_MOD.config;
                    msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
                    this.drawString(msg, RX, super.centerY - 40, colorActive);
                }

                //if(seatID == 0 && info.isEnableVtol) {
                //    var10000 = MCH_MOD.config;
                //    if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                //        int stat = plane.getVtolMode();
                //        if(stat != 1) {
                //            var12 = (new StringBuilder()).append(stat == 0?"VTOL : ":"Normal : ");
                //            var10001 = MCH_MOD.config;
                //            msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
                //            this.drawString(msg, RX, super.centerY - 60, colorActive);
                //        }
                //    }
                //}

                if(ship.canEjectSeat(player)) {
                    var12 = (new StringBuilder()).append("Submerge: ");
                    var10001 = MCH_MOD.config;
                    msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt)).toString();
                    this.drawString(msg, RX, super.centerY - 30, colorActive);
                }

                if(ship.getIsGunnerMode(player) && info.cameraZoom > 1) {
                    var12 = (new StringBuilder()).append("Zoom : ");
                    var10001 = MCH_MOD.config;
                    msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                    this.drawString(msg, LX, super.centerY - 80, colorActive);
                } else if(seatID == 0) {
                    //if(!plane.canFoldWing() && !plane.canUnfoldWing()) {
                        if(ship.canFoldHatch() || ship.canUnfoldHatch()) {
                            var12 = (new StringBuilder()).append("OpenHatch : ");
                            var10001 = MCH_MOD.config;
                            msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                            this.drawString(msg, LX, super.centerY - 80, colorActive);
                        }
                    //}
                    //else {
                    //    var12 = (new StringBuilder()).append("FoldWing : ");
                    //    var10001 = MCH_MOD.config;
                    //    msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                    //    this.drawString(msg, LX, super.centerY - 80, colorActive);
                    //}
                }

            }
        }
    }


}
