package mcheli.helicopter;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_AircraftClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientHeliTickHandler extends MCH_AircraftClientTickHandler {

   public MCH_Key KeySwitchMode;
   public MCH_Key KeySwitchHovering;
   public MCH_Key KeyZoom;
   public MCH_Key[] Keys;


   public MCH_ClientHeliTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft, config);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      super.updateKeybind(config);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
      this.KeySwitchHovering = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
     // this.KeyThrottleUp = this.KeyUp;
      //this.KeyThrottleDown = this.KeyDown;
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, this.KeySwitchHovering, super.KeyUseWeapon, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyExtra, super.KeyFreeLook, super.KeyGUI, super.KeyGearUpDown, super.KeyPutToRack, super.KeyDownFromRack, KeyThrottleDown, KeyThrottleUp, tdcRight, tdcLeft};
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
      EntityClientPlayerMP var7 = super.mc.thePlayer;
      MCH_EntityHeli var8 = null;
      boolean var9 = true;
      if(var7 != null) {
         if(var7.ridingEntity instanceof MCH_EntityHeli) {
            var8 = (MCH_EntityHeli)var7.ridingEntity;
         } else if(var7.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat var10 = (MCH_EntitySeat)var7.ridingEntity;
            if(var10.getParent() instanceof MCH_EntityHeli) {
               var9 = false;
               var8 = (MCH_EntityHeli)var10.getParent();
            }
         } else if(var7.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation var11 = (MCH_EntityUavStation)var7.ridingEntity;
            if(var11.getControlAircract() instanceof MCH_EntityHeli) {
               var8 = (MCH_EntityHeli)var11.getControlAircract();
            }
         }
      }

      if(var8 != null && var8.getAcInfo() != null) {
         this.update(var7, var8, var9);
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
         if((!var9 || !var8.isAlwaysCameraView()) && !var8.getIsGunnerMode(var7)) {
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

      if((super.isBeforeRiding || !super.isRiding) && super.isBeforeRiding && !super.isRiding) {
         W_Reflection.setCameraRoll(0.0F);
         MCH_Lib.enableFirstPersonItemRender();
         MCH_Lib.setRenderViewEntity(var7);
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
         
         if(tdcLeft.isKeyPress()) {
       	  	heli.yawLeft = true;
         }else {
        	 heli.yawLeft = false;
         }
         if(tdcRight.isKeyPress()) {
       	  	heli.yawRight = true;
         }else {
        	 heli.yawRight = false;
         }

         if(this.KeySwitchHovering.isKeyDown()) {
            if(heli.canSwitchHoveringMode()) {
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

      if(send) {
         W_Network.sendToServer(pc);
      }

   }
}
