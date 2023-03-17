package mcheli.vehicle;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_AircraftClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientVehicleTickHandler extends MCH_AircraftClientTickHandler {

   public MCH_Key KeySwitchMode;
   public MCH_Key KeySwitchHovering;
   public MCH_Key KeyZoom;
   public MCH_Key KeyExtra;
   public MCH_Key[] Keys;


   public MCH_ClientVehicleTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft, config);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      super.updateKeybind(config);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
      this.KeySwitchHovering = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.KeyExtra = new MCH_Key(MCH_Config.KeyExtra.prmInt);
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, this.KeySwitchHovering, super.KeyUseWeapon, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, this.KeyExtra, super.KeyGUI,tdcUp, tdcDown, tdcRight, tdcLeft, super.tdcModeIncr, tdcModeDecr, tdcLock};
   }

   protected void update(EntityPlayer player, MCH_EntityVehicle vehicle, MCH_VehicleInfo info) {
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
      EntityClientPlayerMP var6 = super.mc.thePlayer;
      MCH_EntityVehicle var7 = null;
      boolean var8 = true;
      if(var6 != null) {
         if(var6.ridingEntity instanceof MCH_EntityVehicle) {
            var7 = (MCH_EntityVehicle)var6.ridingEntity;
         } else if(var6.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat var9 = (MCH_EntitySeat)var6.ridingEntity;
            if(var9.getParent() instanceof MCH_EntityVehicle) {
               var8 = false;
               var7 = (MCH_EntityVehicle)var9.getParent();
            }
         }
      }

      if(var7 != null && var7.getAcInfo() != null) {
         MCH_Lib.disableFirstPersonItemRender(var6.getCurrentEquippedItem());
         this.update(var6, var7, var7.getVehicleInfo());
         MCH_ViewEntityDummy var10 = MCH_ViewEntityDummy.getInstance(super.mc.theWorld);
         var10.update(var7.camera);
         if(!inGUI) {
            if(!var7.isDestroyed()) {
               this.playerControl(var6, var7, var8);
            }
         } else {
            this.playerControlInGUI(var6, var7, var8);
         }

         MCH_Lib.setRenderViewEntity(var10);
         super.isRiding = true;
      } else {
         super.isRiding = false;
      }

      if((super.isBeforeRiding || !super.isRiding) && super.isBeforeRiding && !super.isRiding) {
         MCH_Lib.enableFirstPersonItemRender();
         MCH_Lib.setRenderViewEntity(var6);
      }

   }

   protected void playerControlInGUI(EntityPlayer player, MCH_EntityVehicle vehicle, boolean isPilot) {
      this.commonPlayerControlInGUI(player, vehicle, isPilot, new MCH_PacketVehiclePlayerControl());
   }

   protected void playerControl(EntityPlayer player, MCH_EntityVehicle vehicle, boolean isPilot) {
      MCH_PacketVehiclePlayerControl pc = new MCH_PacketVehiclePlayerControl();
      boolean send = false;
      send = this.commonPlayerControl(player, vehicle, isPilot, pc);
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
