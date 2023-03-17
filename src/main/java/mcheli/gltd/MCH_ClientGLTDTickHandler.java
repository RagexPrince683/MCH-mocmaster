package mcheli.gltd;

import mcheli.*;
import mcheli.wrapper.W_Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientGLTDTickHandler extends MCH_ClientTickHandlerBase {

   protected boolean isRiding = false;
   protected boolean isBeforeRiding = false;
   public MCH_Key KeyUseWeapon;
   public MCH_Key KeySwitchWeapon1;
   public MCH_Key KeySwitchWeapon2;
   public MCH_Key KeySwWeaponMode;
   public MCH_Key KeyZoom;
   public MCH_Key KeyCameraMode;
   public MCH_Key KeyUnmount;
   public MCH_Key KeyUnmount_1_6;
   public MCH_Key[] Keys;


   public MCH_ClientGLTDTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      this.KeyUseWeapon = new MCH_Key(MCH_Config.KeyUseWeapon.prmInt);
      this.KeySwitchWeapon1 = new MCH_Key(MCH_Config.KeySwitchWeapon1.prmInt);
      this.KeySwitchWeapon2 = new MCH_Key(MCH_Config.KeySwitchWeapon2.prmInt);
      this.KeySwWeaponMode = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.KeyCameraMode = new MCH_Key(MCH_Config.KeyCameraMode.prmInt);
      this.KeyUnmount = new MCH_Key(MCH_Config.KeyUnmount.prmInt);
      this.KeyUnmount_1_6 = new MCH_Key(42);
      this.Keys = new MCH_Key[]{this.KeyUseWeapon, this.KeySwWeaponMode, this.KeySwitchWeapon1, this.KeySwitchWeapon2, this.KeyZoom, this.KeyCameraMode, this.KeyUnmount, this.KeyUnmount_1_6};
   }

   protected void updateGLTD(EntityPlayer player, MCH_EntityGLTD gltd) {
      if(player.rotationPitch < -70.0F) {
         player.rotationPitch = -70.0F;
      }

      if(player.rotationPitch > 70.0F) {
         player.rotationPitch = 70.0F;
      }

      float yaw = gltd.rotationYaw;
      if(player.rotationYaw < yaw - 70.0F) {
         player.rotationYaw = yaw - 70.0F;
      }

      if(player.rotationYaw > yaw + 70.0F) {
         player.rotationYaw = yaw + 70.0F;
      }

      gltd.camera.rotationYaw = player.rotationYaw;
      gltd.camera.rotationPitch = player.rotationPitch;
   }

   protected void onTick(boolean inGUI) {
      MCH_Key[] player = this.Keys;
      int viewEntityDummy = player.length;

      for(int gltd = 0; gltd < viewEntityDummy; ++gltd) {
         MCH_Key k = player[gltd];
         k.update();
      }

      this.isBeforeRiding = this.isRiding;
      EntityClientPlayerMP var6 = super.mc.thePlayer;
      MCH_ViewEntityDummy var7 = null;
      if(var6 != null && var6.ridingEntity instanceof MCH_EntityGLTD) {
         MCH_EntityGLTD var8 = (MCH_EntityGLTD)var6.ridingEntity;
         this.updateGLTD(var6, var8);
         MCH_Lib.disableFirstPersonItemRender(var6.getCurrentEquippedItem());
         var7 = MCH_ViewEntityDummy.getInstance(super.mc.theWorld);
         var7.update(var8.camera);
         if(!inGUI) {
            this.playerControl(var6, var8);
         }

         MCH_Lib.setRenderViewEntity(var7);
         this.isRiding = true;
      } else {
         this.isRiding = false;
      }

      if(this.isBeforeRiding != this.isRiding) {
         if(this.isRiding) {
            if(var7 != null) {
               var7.prevPosX = var7.posX;
               var7.prevPosY = var7.posY;
               var7.prevPosZ = var7.posZ;
            }
         } else {
            MCH_Lib.enableFirstPersonItemRender();
            MCH_Lib.setRenderViewEntity(var6);
         }
      }

   }

   protected void playerControl(EntityPlayer player, MCH_EntityGLTD gltd) {
      MCH_PacketGLTDPlayerControl pc = new MCH_PacketGLTDPlayerControl();
      boolean send = false;
      if(this.KeyUnmount.isKeyDown()) {
         pc.unmount = true;
         send = true;
      }

      if((!this.KeySwitchWeapon1.isKeyDown() || !this.KeySwitchWeapon2.isKeyDown()) && this.KeyUseWeapon.isKeyPress()) {
         if(gltd.useCurrentWeapon(0, 0)) {
            pc.useWeapon = true;
            send = true;
         } else if(this.KeyUseWeapon.isKeyDown()) {
            playSoundNG();
         }
      }

      float prevZoom = gltd.camera.getCameraZoom();
      if(this.KeyZoom.isKeyPress() && !this.KeySwWeaponMode.isKeyPress()) {
         gltd.zoomCamera(0.1F * gltd.camera.getCameraZoom());
      }

      if(!this.KeyZoom.isKeyPress() && this.KeySwWeaponMode.isKeyPress()) {
         gltd.zoomCamera(-0.1F * gltd.camera.getCameraZoom());
      }

      if(prevZoom != gltd.camera.getCameraZoom()) {
         playSound("zoom", 0.1F, prevZoom < gltd.camera.getCameraZoom()?1.0F:0.85F);
      }

      if(this.KeyCameraMode.isKeyDown()) {
         int beforeMode = gltd.camera.getMode(0);
         gltd.camera.setMode(0, gltd.camera.getMode(0) + 1);
         int mode = gltd.camera.getMode(0);
         if(mode != beforeMode) {
            pc.switchCameraMode = (byte)mode;
            playSoundOK();
            send = true;
         }
      }

      if(send) {
         W_Network.sendToServer(pc);
      }

   }
}
