package mcheli.plane;

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

public class MCP_ClientPlaneTickHandler extends MCH_AircraftClientTickHandler {

   public MCH_Key KeySwitchMode;
   public MCH_Key KeyEjectSeat;
   public MCH_Key KeyZoom;

   public MCH_Key[] Keys;


   public MCP_ClientPlaneTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft, config);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      super.updateKeybind(config);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
      this.KeyEjectSeat = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      
     
      
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, this.KeyEjectSeat, super.KeyUseWeapon, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyExtra, super.KeyFreeLook, super.KeyGUI, super.KeyGearUpDown, super.KeyPutToRack, super.KeyDownFromRack, super.tdcUp, tdcDown, tdcRight, tdcLeft, super.tdcModeIncr, tdcModeDecr, tdcLock, KeyThrottleDown, KeyThrottleUp};
   }

   protected void update(EntityPlayer player, MCP_EntityPlane plane) {
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
      EntityClientPlayerMP var7 = super.mc.thePlayer;
      MCP_EntityPlane var8 = null;
      boolean var9 = true;
      if(var7 != null) {
         if(var7.ridingEntity instanceof MCP_EntityPlane) {
            var8 = (MCP_EntityPlane)var7.ridingEntity;
         } else if(var7.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat var10 = (MCH_EntitySeat)var7.ridingEntity;
            if(var10.getParent() instanceof MCP_EntityPlane) {
               var9 = false;
               var8 = (MCP_EntityPlane)var10.getParent();
            }
         } else if(var7.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation var11 = (MCH_EntityUavStation)var7.ridingEntity;
            if(var11.getControlAircract() instanceof MCP_EntityPlane) {
               var8 = (MCP_EntityPlane)var11.getControlAircract();
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

   protected void playerControlInGUI(EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      this.commonPlayerControlInGUI(player, plane, isPilot, new MCP_PlanePacketPlayerControl());
   }

   protected void playerControl(EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      MCP_PlanePacketPlayerControl pc = new MCP_PlanePacketPlayerControl();
      boolean send = false;
      send = this.commonPlayerControl(player, plane, isPilot, pc);
      boolean isUav;
      if(isPilot) {
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
               send = true;
            } else {
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
         isUav = plane.isUAV() && !plane.getAcInfo().haveHatch() && !plane.getPlaneInfo().haveWing();
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
