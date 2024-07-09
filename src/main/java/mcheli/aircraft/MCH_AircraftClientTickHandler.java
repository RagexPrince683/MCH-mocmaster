package mcheli.aircraft;

import mcheli.MCH_ClientTickHandlerBase;
import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_MOD;
import mcheli.MCH_PacketIndOpenScreen;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketPlayerControlBase;
import mcheli.aircraft.MCH_PacketSeatPlayerControl;
import mcheli.wrapper.W_Network;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

public abstract class MCH_AircraftClientTickHandler extends MCH_ClientTickHandlerBase {

   protected boolean isRiding = false;
   protected boolean isBeforeRiding = false;
   public MCH_Key KeyUp;
   public MCH_Key KeyDown;
   public MCH_Key KeyRight;
   public MCH_Key KeyLeft;
   public MCH_Key KeyUseWeapon;
   public MCH_Key KeySwitchWeapon1;
   public MCH_Key KeySwitchWeapon2;
   public MCH_Key KeySwWeaponMode;
   public MCH_Key KeyUnmount;
   public MCH_Key KeyUnmountForce;
   public MCH_Key KeyExtra;
   public MCH_Key KeyFlare;
   public MCH_Key KeyCameraMode;
   public MCH_Key KeyFreeLook;
   public MCH_Key KeyGUI;
   public MCH_Key KeyGearUpDown;
   public MCH_Key KeyPutToRack;
   public MCH_Key KeyDownFromRack;
   public MCH_Key KeyBrake;


   public MCH_AircraftClientTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      this.KeyUp = new MCH_Key(MCH_Config.KeyUp.prmInt);
      this.KeyDown = new MCH_Key(MCH_Config.KeyDown.prmInt);
      this.KeyRight = new MCH_Key(MCH_Config.KeyRight.prmInt);
      this.KeyLeft = new MCH_Key(MCH_Config.KeyLeft.prmInt);
      this.KeyUseWeapon = new MCH_Key(MCH_Config.KeyUseWeapon.prmInt);
      this.KeySwitchWeapon1 = new MCH_Key(MCH_Config.KeySwitchWeapon1.prmInt);
      this.KeySwitchWeapon2 = new MCH_Key(MCH_Config.KeySwitchWeapon2.prmInt);
      this.KeySwWeaponMode = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
      this.KeyUnmount = new MCH_Key(MCH_Config.KeyUnmount.prmInt);
      this.KeyUnmountForce = new MCH_Key(42);
      this.KeyExtra = new MCH_Key(MCH_Config.KeyExtra.prmInt);
      this.KeyFlare = new MCH_Key(MCH_Config.KeyFlare.prmInt);
      this.KeyCameraMode = new MCH_Key(MCH_Config.KeyCameraMode.prmInt);
      this.KeyFreeLook = new MCH_Key(MCH_Config.KeyFreeLook.prmInt);
      this.KeyGUI = new MCH_Key(MCH_Config.KeyGUI.prmInt);
      this.KeyGearUpDown = new MCH_Key(MCH_Config.KeyGearUpDown.prmInt);
      this.KeyPutToRack = new MCH_Key(MCH_Config.KeyPutToRack.prmInt);
      this.KeyDownFromRack = new MCH_Key(MCH_Config.KeyDownFromRack.prmInt);
      this.KeyBrake = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
   }

   protected void commonPlayerControlInGUI(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot, MCH_PacketPlayerControlBase pc) {}

   public boolean commonPlayerControl(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot, MCH_PacketPlayerControlBase pc) {
      MCH_Config var10000 = MCH_MOD.config;
      MCH_PacketSeatPlayerControl send;
      if(Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
         if(this.KeyGUI.isKeyDown() || this.KeyExtra.isKeyDown()) {
            send = new MCH_PacketSeatPlayerControl();
            if(isPilot) {
               send.switchSeat = (byte)(this.KeyGUI.isKeyDown()?1:2);
            } else {
               ac.keepOnRideRotation = true;
               send.switchSeat = 3;
            }

            W_Network.sendToServer(send);
            return false;
         }
      } else if(!isPilot && ac.getSeatNum() > 1) {
         send = new MCH_PacketSeatPlayerControl();
         if(this.KeyGUI.isKeyDown()) {
            send.switchSeat = 1;
            W_Network.sendToServer(send);
            return false;
         }

         if(this.KeyExtra.isKeyDown()) {
            send.switchSeat = 2;
            W_Network.sendToServer(send);
            return false;
         }
      }

      boolean alsosend = false;
      if(this.KeyCameraMode.isKeyDown()) {
         if(ac.haveSearchLight()) {
            if(ac.canSwitchSearchLight(player)) {
               pc.switchSearchLight = true;
               playSoundOK();
               alsosend = true;
            }
         } else if(ac.canSwitchCameraMode()) {
            int dKey = ac.getCameraMode(player);
            ac.switchCameraMode(player);
            int arr$ = ac.getCameraMode(player);
            if(arr$ != dKey) {
               pc.switchCameraMode = (byte)(arr$ + 1);
               playSoundOK();
               alsosend = true;
            }
         } else {
            playSoundNG();
         }
      }

      if(this.KeyUnmount.isKeyDown() && !ac.isDestroyed() && ac.getSizeInventory() > 0 && !isPilot) {
         MCH_PacketIndOpenScreen.send(3);
      }

      if(isPilot) {
         if(this.KeyUnmount.isKeyDown()) {
            pc.isUnmount = 2;
            alsosend = true;
         }

         if(this.KeyPutToRack.isKeyDown()) {
            ac.checkRideRack();
            if(ac.canRideRack()) {
               pc.putDownRack = 3;
               alsosend = true;
            } else if(ac.canPutToRack()) {
               pc.putDownRack = 1;
               alsosend = true;
            }
         } else if(this.KeyDownFromRack.isKeyDown()) {
            if(ac.ridingEntity != null) {
               pc.isUnmount = 3;
               alsosend = true;
            } else if(ac.canDownFromRack()) {
               pc.putDownRack = 2;
               alsosend = true;
            }
         }

         if(this.KeyGearUpDown.isKeyDown() && ac.getAcInfo().haveLandingGear()) {
            if(ac.canFoldLandingGear()) {
               pc.switchGear = 1;
               alsosend = true;
            } else if(ac.canUnfoldLandingGear()) {
               pc.switchGear = 2;
               alsosend = true;
            }
         }

         if(this.KeyFreeLook.isKeyDown() && ac.canSwitchFreeLook()) {
            pc.switchFreeLook = (byte)(ac.isFreeLookMode()?2:1);
            alsosend = true;
         }

         if(this.KeyGUI.isKeyDown()) {
            pc.openGui = true;
            alsosend = true;
         }

         if(ac.isRepelling()) {
            pc.throttleDown = ac.throttleDown = false;
            pc.throttleUp = ac.throttleUp = false;
            pc.moveRight = ac.moveRight = false;
            pc.moveLeft = ac.moveLeft = false;
         } else if(ac.hasBrake() && this.KeyBrake.isKeyPress()) {
            alsosend |= this.KeyBrake.isKeyDown();
            pc.throttleDown = ac.throttleDown = false;
            pc.throttleUp = ac.throttleUp = false;
            double var14 = ac.posX - ac.prevPosX;
            double var16 = ac.posZ - ac.prevPosZ;
            double var17 = var14 * var14 + var16 * var16;
            if(ac.getCurrentThrottle() <= 0.03D && var17 < 0.01D) {
               pc.moveRight = ac.moveRight = false;
               pc.moveLeft = ac.moveLeft = false;
            }

            pc.useBrake = true;
         } else {
            alsosend |= this.KeyBrake.isKeyUp();
            MCH_Key[] var13 = new MCH_Key[]{this.KeyUp, this.KeyDown, this.KeyRight, this.KeyLeft};
            MCH_Key[] var15 = var13;
            int len$ = var13.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               MCH_Key k = var15[i$];
               if(k.isKeyDown() || k.isKeyUp()) {
                  alsosend = true;
                  break;
               }
            }

            pc.throttleDown = ac.throttleDown = this.KeyDown.isKeyPress();
            pc.throttleUp = ac.throttleUp = this.KeyUp.isKeyPress();
            pc.moveRight = ac.moveRight = this.KeyRight.isKeyPress();
            pc.moveLeft = ac.moveLeft = this.KeyLeft.isKeyPress();
         }
      }

      if(!ac.isDestroyed() && this.KeyFlare.isKeyDown() && ac.getSeatIdByEntity(player) <= 1) {
         if(ac.canUseFlare() && ac.useFlare(ac.getCurrentFlareType())) {
            pc.useFlareType = (byte)ac.getCurrentFlareType();
            ac.nextFlareType();
            alsosend = true;
         } else {
            playSoundNG();
         }
      }

      if (!ac.isDestroyed() && !ac.isPilotReloading()) {
         boolean isSwitchWeapon1Down = this.KeySwitchWeapon1.isKeyDown();
         boolean isSwitchWeapon2Down = this.KeySwitchWeapon2.isKeyDown();
         int value = getMouseWheel();
         boolean isSwWeaponModeDown = this.KeySwWeaponMode.isKeyDown();
         boolean isUseWeaponPress = this.KeyUseWeapon.isKeyPress();

         System.out.println("ac is not destroyed and pilot is not reloading");
         System.out.println("KeySwitchWeapon1 is " + (isSwitchWeapon1Down ? "down" : "up"));
         System.out.println("KeySwitchWeapon2 is " + (isSwitchWeapon2Down ? "down" : "up"));
         System.out.println("MouseWheel value: " + value);

         if (!isSwitchWeapon1Down || isSwitchWeapon2Down && value != 0) {
            if (isSwWeaponModeDown) {
               System.out.println("Switching current weapon mode");
               ac.switchCurrentWeaponMode(player);
            } else if (isUseWeaponPress && ac.useCurrentWeapon((Entity) player)) {
               System.out.println("Using current weapon");
               pc.useWeapon = true;
               pc.useWeaponOption1 = ac.getCurrentWeapon(player).getLastUsedOptionParameter1();
               pc.useWeaponOption2 = ac.getCurrentWeapon(player).getLastUsedOptionParameter2();
               pc.useWeaponPosX = ac.prevPosX;
               pc.useWeaponPosY = ac.prevPosY;
               pc.useWeaponPosZ = ac.prevPosZ;
               alsosend = true;
            }
         } else {
            if (value > 0) {
               System.out.println("Switching to next weapon (mouse wheel up)");
               pc.switchWeapon = (byte) ac.getNextWeaponID(player, -1);
               ac.switchWeapon(player, pc.switchWeapon - 1);
            } else if (value < 0) { // Ensure we handle both directions
               System.out.println("Switching to next weapon (mouse wheel down)");
               pc.switchWeapon = (byte) ac.getNextWeaponID(player, 1);
               ac.switchWeapon(player, pc.switchWeapon + 1);
            }

            MCH_ClientTickHandlerBase.setMouseWheel(0);
            ac.switchWeapon(player, pc.switchWeapon);
            alsosend = true;
         }
      }

      return alsosend || player.ticksExisted % 100 == 0;
   }
}
