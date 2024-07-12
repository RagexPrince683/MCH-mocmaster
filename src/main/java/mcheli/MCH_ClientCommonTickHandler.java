package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_ClientSeatTickHandler;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.command.MCH_GuiTitle;
import mcheli.gltd.MCH_ClientGLTDTickHandler;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.gltd.MCH_GuiGLTD;
import mcheli.gui.MCH_Gui;
import mcheli.helicopter.MCH_ClientHeliTickHandler;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_GuiHeli;
import mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import mcheli.lweapon.MCH_GuiLightWeapon;
import mcheli.multiplay.MCH_GuiScoreboard;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.multiplay.MCH_MultiplayClient;
import mcheli.plane.MCP_ClientPlaneTickHandler;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_GuiPlane;
import mcheli.tank.MCH_ClientTankTickHandler;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_GuiTank;
import mcheli.tool.MCH_ClientToolTickHandler;
import mcheli.tool.MCH_GuiWrench;
import mcheli.tool.MCH_ItemWrench;
import mcheli.tool.rangefinder.MCH_GuiRangeFinder;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_ClientVehicleTickHandler;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vehicle.MCH_GuiVehicle;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_Reflection;
import mcheli.wrapper.W_TickHandler;
import mcheli.wrapper.W_Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.Display;

@SideOnly(Side.CLIENT)
public class MCH_ClientCommonTickHandler extends W_TickHandler {
   public static MCH_ClientCommonTickHandler instance;

   public MCH_GuiCommon gui_Common;

   public MCH_Gui gui_Heli;

   public MCH_Gui gui_Plane;

   public MCH_Gui gui_Tank;

   public MCH_Gui gui_GLTD;

   public MCH_Gui gui_Vehicle;

   public MCH_Gui gui_LWeapon;

   public MCH_Gui gui_Wrench;

   public MCH_Gui gui_EMarker;

   public MCH_Gui gui_RngFndr;

   public MCH_Gui gui_Title;

   public MCH_Gui[] guis;

   public MCH_Gui[] guiTicks;

   public MCH_ClientTickHandlerBase[] ticks;

   public MCH_Key[] Keys;

   public MCH_Key KeyCamDistUp;

   public MCH_Key KeyCamDistDown;

   public MCH_Key KeyScoreboard;

   public MCH_Key KeyMultiplayManager;

   public static int cameraMode = 0;

   public static MCH_EntityAircraft ridingAircraft = null;

   public static boolean isDrawScoreboard = false;

   public static int sendLDCount = 0;

   public static boolean isLocked = false;

   public static int lockedSoundCount = 0;

   int debugcnt;

   private static double prevMouseDeltaX;

   private static double prevMouseDeltaY;

   public MCH_ClientCommonTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft);
      this.gui_Common = new MCH_GuiCommon(minecraft);
      this.gui_Heli = (MCH_Gui)new MCH_GuiHeli(minecraft);
      this.gui_Plane = (MCH_Gui)new MCP_GuiPlane(minecraft);
      this.gui_Tank = (MCH_Gui)new MCH_GuiTank(minecraft);
      this.gui_GLTD = (MCH_Gui)new MCH_GuiGLTD(minecraft);
      this.gui_Vehicle = (MCH_Gui)new MCH_GuiVehicle(minecraft);
      this.gui_LWeapon = (MCH_Gui)new MCH_GuiLightWeapon(minecraft);
      this.gui_Wrench = (MCH_Gui)new MCH_GuiWrench(minecraft);
      this.gui_RngFndr = (MCH_Gui)new MCH_GuiRangeFinder(minecraft);
      this.gui_EMarker = (MCH_Gui)new MCH_GuiTargetMarker(minecraft);
      this.gui_Title = (MCH_Gui)new MCH_GuiTitle(minecraft);
      this.guis = new MCH_Gui[] { this.gui_RngFndr, this.gui_LWeapon, this.gui_Heli, this.gui_Plane, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle };
      this.guiTicks = new MCH_Gui[] {
              (MCH_Gui)this.gui_Common, this.gui_Heli, this.gui_Plane, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle, this.gui_LWeapon, this.gui_Wrench, this.gui_RngFndr, this.gui_EMarker,
              this.gui_Title };
      this.ticks = new MCH_ClientTickHandlerBase[] { (MCH_ClientTickHandlerBase)new MCH_ClientHeliTickHandler(minecraft, config), (MCH_ClientTickHandlerBase)new MCP_ClientPlaneTickHandler(minecraft, config), (MCH_ClientTickHandlerBase)new MCH_ClientTankTickHandler(minecraft, config), (MCH_ClientTickHandlerBase)new MCH_ClientGLTDTickHandler(minecraft, config), (MCH_ClientTickHandlerBase)new MCH_ClientVehicleTickHandler(minecraft, config), (MCH_ClientTickHandlerBase)new MCH_ClientLightWeaponTickHandler(minecraft, config), (MCH_ClientTickHandlerBase)new MCH_ClientSeatTickHandler(minecraft, config), (MCH_ClientTickHandlerBase)new MCH_ClientToolTickHandler(minecraft, config) };
      updatekeybind(config);
   }

   public void updatekeybind(MCH_Config config) {
      this.KeyCamDistUp = new MCH_Key(MCH_Config.KeyCameraDistUp.prmInt);
      this.KeyCamDistDown = new MCH_Key(MCH_Config.KeyCameraDistDown.prmInt);
      this.KeyScoreboard = new MCH_Key(MCH_Config.KeyScoreboard.prmInt);
      this.KeyMultiplayManager = new MCH_Key(MCH_Config.KeyMultiplayManager.prmInt);
      this.Keys = new MCH_Key[] { this.KeyCamDistUp, this.KeyCamDistDown, this.KeyScoreboard, this.KeyMultiplayManager };
      for (MCH_ClientTickHandlerBase t : this.ticks)
         t.updateKeybind(config);
   }

   public String getLabel() {
      return null;
   }

   public void onTick() {
      MCH_ClientTickHandlerBase.initRotLimit();
      MCH_Key[] player = this.Keys;
      int inOtherGui = player.length;

      for(int ac = 0; ac < inOtherGui; ++ac) {
         MCH_Key len$ = player[ac];
         len$.update();
      }

      EntityClientPlayerMP var7 = super.mc.thePlayer;
      if(var7 != null && super.mc.currentScreen == null) {
         if(MCH_ServerSettings.enableCamDistChange && (this.KeyCamDistUp.isKeyDown() || this.KeyCamDistDown.isKeyDown())) {
            inOtherGui = (int)W_Reflection.getThirdPersonDistance();
            if(this.KeyCamDistUp.isKeyDown() && inOtherGui < 60) {
               inOtherGui += 4;
               if(inOtherGui > 60) {
                  inOtherGui = 60;
               }

               W_Reflection.setThirdPersonDistance((float)inOtherGui);
            } else if(this.KeyCamDistDown.isKeyDown()) {
               inOtherGui -= 4;
               if(inOtherGui < 4) {
                  inOtherGui = 4;
               }

               W_Reflection.setThirdPersonDistance((float)inOtherGui);
            }
         }

         if(super.mc.currentScreen == null) {
            label85: {
               if(super.mc.isSingleplayer()) {
                  MCH_Config var10000 = MCH_MOD.config;
                  if(!MCH_Config.DebugLog) {
                     break label85;
                  }
               }

               isDrawScoreboard = this.KeyScoreboard.isKeyPress();
               if(!isDrawScoreboard && this.KeyMultiplayManager.isKeyDown()) {
                  MCH_PacketIndOpenScreen.send(5);
               }
            }
         }
      }

      if(sendLDCount < 10) {
         ++sendLDCount;
      } else {
         MCH_MultiplayClient.sendImageData();
         sendLDCount = 0;
      }

      boolean var12 = super.mc.currentScreen != null;
      MCH_ClientTickHandlerBase[] var8 = this.ticks;
      int var10 = var8.length;

      int i$;
      for(i$ = 0; i$ < var10; ++i$) {
         MCH_ClientTickHandlerBase g = var8[i$];
         g.onTick(var12);
      }

      MCH_Gui[] var9 = this.guiTicks;
      var10 = var9.length;

      for(i$ = 0; i$ < var10; ++i$) {
         MCH_Gui var13 = var9[i$];
         var13.onTick();
      }

      MCH_EntityAircraft var11 = MCH_EntityAircraft.getAircraft_RiddenOrControl(var7);
      if(var7 != null && var11 != null && !var11.isDestroyed()) {
         if(isLocked && lockedSoundCount == 0) {
            isLocked = false;
            lockedSoundCount = 20;
            MCH_ClientTickHandlerBase.playSound("locked");
         }
      } else {
         lockedSoundCount = 0;
         isLocked = false;
      }

      if(lockedSoundCount > 0) {
         --lockedSoundCount;
      }
   }

   public void onTickPre() {
      if (super.mc.thePlayer != null && super.mc.theWorld != null)
         onTick();
   }

   public void onTickPost() {
      if (this.mc.thePlayer != null && this.mc.theWorld != null)
         MCH_GuiTargetMarker.onClientTick();
   }

   private static double mouseDeltaX = 0.0D;

   private static double mouseDeltaY = 0.0D;

   private static double mouseRollDeltaX = 0.0D;

   private static double mouseRollDeltaY = 0.0D;

   private static boolean isRideAircraft = false;

   private static float prevTick = 0.0F;

   public static double getCurrentStickX() {
      return mouseRollDeltaX;
   }

   public static double getCurrentStickY() {
      double inv = 1.0D;
      if ((Minecraft.getMinecraft()).gameSettings.invertMouse)
         inv = -inv;
      if (MCH_Config.InvertMouse.prmBool)
         inv = -inv;
      return mouseRollDeltaY * inv;
   }

   public static double getMaxStickLength() {
      return 40.0D;
   }

   public void updateMouseDelta(boolean stickMode, float partialTicks) {


      this.prevMouseDeltaX = this.mouseDeltaX;

      this.prevMouseDeltaY = this.mouseDeltaY;

      this.mouseDeltaX = 0.0D;

      this.mouseDeltaY = 0.0D;
      if (this.mc.inGameHasFocus && Display.isActive() && this.mc.currentScreen == null) {
         if (stickMode) {

            if (Math.abs(this.mouseRollDeltaX) < getMaxStickLength() * 0.2D) {
               this.mouseRollDeltaX *= (1.0F - 0.15F * partialTicks);
            }

            if (Math.abs(this.mouseRollDeltaY) < getMaxStickLength() * 0.2D) {
               this.mouseRollDeltaY *= (1.0F - 0.15F * partialTicks);
            }
         }
         this.mc.mouseHelper.mouseXYChange();
         float f1 = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
         float f2 = f1 * f1 * f1 * 8.0F;
         double ms = MCH_Config.MouseSensitivity.prmDouble * 0.1D;

         this.mouseDeltaX = ms * this.mc.mouseHelper.deltaX * f2;

         this.mouseDeltaY = ms * this.mc.mouseHelper.deltaY * f2;
         byte inv = 1;
         if (this.mc.gameSettings.invertMouse)
            inv = -1;
         if (MCH_Config.InvertMouse.prmBool)
            inv = (byte)(inv * -1);

         this.mouseRollDeltaX += this.mouseDeltaX;

         this.mouseRollDeltaY += this.mouseDeltaY * inv;
         double dist = this.mouseRollDeltaX * this.mouseRollDeltaX + this.mouseRollDeltaY * this.mouseRollDeltaY;
         if (dist > 1.0D) {
            dist = MathHelper.sqrt_double(dist);
            double d = dist;
            if (d > this.getMaxStickLength())
               d = getMaxStickLength();

            this.mouseRollDeltaX /= dist;

            this.mouseRollDeltaY /= dist;

            this.mouseRollDeltaX *= d;

            this.mouseRollDeltaY *= d;
         }
      }
   }

   public void onRenderTickPre(float partialTicks) {
      MCH_GuiTargetMarker.clearMarkEntityPos();
      if (!MCH_ServerSettings.enableDebugBoundingBox)
         RenderManager.debugBoundingBox = false;
      MCH_ClientEventHook.haveSearchLightAircraft.clear();
      if (this.mc != null && this.mc.theWorld != null)
         for (Object o : (Minecraft.getMinecraft()).theWorld.loadedEntityList) {
            if (o instanceof MCH_EntityAircraft)
               if (((MCH_EntityAircraft)o).haveSearchLight())
                  MCH_ClientEventHook.haveSearchLightAircraft.add((MCH_EntityAircraft)o);
         }
      if (W_McClient.isGamePaused())
         return;
      EntityClientPlayerMP entityClientPlayerMP = this.mc.thePlayer;
      if (entityClientPlayerMP == null)
         return;
      ItemStack currentItemstack = entityClientPlayerMP.getCurrentEquippedItem();
      if (currentItemstack != null && currentItemstack.getItem() instanceof MCH_ItemWrench)
         if (entityClientPlayerMP.getItemInUseCount() > 0)
            W_Reflection.setItemRendererProgress(1.0F);
      ridingAircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl((Entity)entityClientPlayerMP);
      if (ridingAircraft != null) {
         cameraMode = ridingAircraft.getCameraMode((EntityPlayer)entityClientPlayerMP);
      } else if (((EntityPlayer)entityClientPlayerMP).ridingEntity instanceof MCH_EntityGLTD) {
         MCH_EntityGLTD gltd = (MCH_EntityGLTD)((EntityPlayer)entityClientPlayerMP).ridingEntity;
         cameraMode = gltd.camera.getMode(0);
      } else {
         cameraMode = 0;
      }
      MCH_EntityAircraft ac = null;
      if (((EntityPlayer)entityClientPlayerMP).ridingEntity instanceof MCH_EntityHeli || ((EntityPlayer)entityClientPlayerMP).ridingEntity instanceof MCP_EntityPlane || ((EntityPlayer)entityClientPlayerMP).ridingEntity instanceof MCH_EntityTank) {
         ac = (MCH_EntityAircraft)((EntityPlayer)entityClientPlayerMP).ridingEntity;
      } else if (((EntityPlayer)entityClientPlayerMP).ridingEntity instanceof MCH_EntityUavStation) {
         ac = ((MCH_EntityUavStation)((EntityPlayer)entityClientPlayerMP).ridingEntity).getControlAircract();
      } else if (((EntityPlayer)entityClientPlayerMP).ridingEntity instanceof MCH_EntityVehicle) {
         MCH_EntityAircraft vehicle = (MCH_EntityAircraft)((EntityPlayer)entityClientPlayerMP).ridingEntity;
         vehicle.setupAllRiderRenderPosition(partialTicks, (EntityPlayer)entityClientPlayerMP);
      }
      boolean stickMode = false;
      if (ac instanceof MCH_EntityHeli)
         stickMode = MCH_Config.MouseControlStickModeHeli.prmBool;
      if (ac instanceof MCP_EntityPlane)
         stickMode = MCH_Config.MouseControlStickModePlane.prmBool;
      for (int i = 0; i < 10; ) {

         if (this.prevTick > partialTicks) {
            this.prevTick--;
            i++;
         }
      }
      if (ac != null && ac.canMouseRot()) {

         if (!this.isRideAircraft)
            ac.onInteractFirst((EntityPlayer)entityClientPlayerMP);
         this.isRideAircraft = true;
         updateMouseDelta(stickMode, partialTicks);
         boolean fixRot = false;
         float fixYaw = 0.0F;
         float fixPitch = 0.0F;
         MCH_SeatInfo seatInfo = ac.getSeatInfo((Entity)entityClientPlayerMP);
         if (seatInfo != null && seatInfo.fixRot && ac.getIsGunnerMode((Entity)entityClientPlayerMP) && !ac.isGunnerLookMode((EntityPlayer)entityClientPlayerMP)) {
            fixRot = true;
            fixYaw = seatInfo.fixYaw;
            fixPitch = seatInfo.fixPitch;
            this.mouseRollDeltaX *= 0.0D;

            this.mouseRollDeltaY *= 0.0D;

            this.mouseDeltaX *= 0.0D;

            this.mouseDeltaY *= 0.0D;
         } else if (ac.isPilot((Entity)entityClientPlayerMP)) {
            MCH_AircraftInfo.CameraPosition cp = ac.getCameraPosInfo();
            if (cp != null) {
               fixYaw = cp.yaw;
               fixPitch = cp.pitch;
            }
         }
         if (ac.getAcInfo() == null) {
            entityClientPlayerMP.setAngles((float)this.mouseDeltaX, (float)this.mouseDeltaY);
         } else {
            ac.setAngles((Entity)entityClientPlayerMP, fixRot, fixYaw, fixPitch, (float)(this.mouseDeltaX + this.prevMouseDeltaX) / 2.0F, (float)(this.mouseDeltaY + this.prevMouseDeltaY) / 2.0F, (float)this.mouseRollDeltaX, (float)this.mouseRollDeltaY, partialTicks - this.prevTick);
         }
         ac.setupAllRiderRenderPosition(partialTicks, (EntityPlayer)entityClientPlayerMP);
         double dist = MathHelper.sqrt_double(this.mouseRollDeltaX * this.mouseRollDeltaX + this.mouseRollDeltaY * this.mouseRollDeltaY);
         if (!stickMode || dist < getMaxStickLength() * 0.1D) {

            this.mouseRollDeltaX *= 0.95D;
            this.mouseRollDeltaY *= 0.95D;
         }
         float roll = MathHelper.wrapAngleTo180_float(ac.getRotRoll());
         float yaw = MathHelper.wrapAngleTo180_float(ac.getRotYaw() - ((EntityPlayer)entityClientPlayerMP).rotationYaw);
         roll *= MathHelper.cos((float)(yaw * Math.PI / 180.0D));
         if (ac.getTVMissile() != null && W_Lib.isClientPlayer((ac.getTVMissile()).shootingEntity) && ac.getIsGunnerMode((Entity)entityClientPlayerMP))
            roll = 0.0F;
         W_Reflection.setCameraRoll(roll);
         correctViewEntityDummy((Entity)entityClientPlayerMP);
      } else {
         MCH_EntitySeat seat = (((EntityPlayer)entityClientPlayerMP).ridingEntity instanceof MCH_EntitySeat) ? (MCH_EntitySeat)((EntityPlayer)entityClientPlayerMP).ridingEntity : null;
         if (seat != null && seat.getParent() != null) {
            updateMouseDelta(stickMode, partialTicks);
            ac = seat.getParent();
            boolean fixRot = false;
            MCH_SeatInfo seatInfo = ac.getSeatInfo((Entity)entityClientPlayerMP);
            if (seatInfo != null && seatInfo.fixRot && ac.getIsGunnerMode((Entity)entityClientPlayerMP) && !ac.isGunnerLookMode((EntityPlayer)entityClientPlayerMP)) {
               fixRot = true;

               this.mouseRollDeltaX *= 0.0D;

               this.mouseRollDeltaY *= 0.0D;

               this.mouseDeltaX *= 0.0D;

               this.mouseDeltaY *= 0.0D;
            }

            Vec3 v = Vec3.createVectorHelper(this.mouseDeltaX, this.mouseRollDeltaY, 0.0D);
            W_Vec3.rotateAroundZ((float)((ac.calcRotRoll(partialTicks) / 180.0F) * Math.PI), v);
            MCH_WeaponSet ws = ac.getCurrentWeapon((Entity)entityClientPlayerMP);

            this.mouseDeltaY *= (ws != null && ws.getInfo() != null) ? (ws.getInfo()).cameraRotationSpeedPitch : 1.0D;
            entityClientPlayerMP.setAngles((float)this.mouseDeltaX, (float)this.mouseDeltaY);
            float y = ac.getRotYaw();
            float p = ac.getRotPitch();
            float r = ac.getRotRoll();
            ac.setRotYaw(ac.calcRotYaw(partialTicks));
            ac.setRotPitch(ac.calcRotPitch(partialTicks));
            ac.setRotRoll(ac.calcRotRoll(partialTicks));
            float revRoll = 0.0F;
            if (fixRot) {
               ((EntityPlayer)entityClientPlayerMP).rotationYaw = ac.getRotYaw() + seatInfo.fixYaw;
               ((EntityPlayer)entityClientPlayerMP).rotationPitch = ac.getRotPitch() + seatInfo.fixPitch;
               if (((EntityPlayer)entityClientPlayerMP).rotationPitch > 90.0F) {
                  ((EntityPlayer)entityClientPlayerMP).prevRotationPitch -= (((EntityPlayer)entityClientPlayerMP).rotationPitch - 90.0F) * 2.0F;
                  ((EntityPlayer)entityClientPlayerMP).rotationPitch -= (((EntityPlayer)entityClientPlayerMP).rotationPitch - 90.0F) * 2.0F;
                  ((EntityPlayer)entityClientPlayerMP).prevRotationYaw += 180.0F;
                  ((EntityPlayer)entityClientPlayerMP).rotationYaw += 180.0F;
                  revRoll = 180.0F;
               } else if (((EntityPlayer)entityClientPlayerMP).rotationPitch < -90.0F) {
                  ((EntityPlayer)entityClientPlayerMP).prevRotationPitch -= (((EntityPlayer)entityClientPlayerMP).rotationPitch - 90.0F) * 2.0F;
                  ((EntityPlayer)entityClientPlayerMP).rotationPitch -= (((EntityPlayer)entityClientPlayerMP).rotationPitch - 90.0F) * 2.0F;
                  ((EntityPlayer)entityClientPlayerMP).prevRotationYaw += 180.0F;
                  ((EntityPlayer)entityClientPlayerMP).rotationYaw += 180.0F;
                  revRoll = 180.0F;
               }
            }
            ac.setupAllRiderRenderPosition(partialTicks, (EntityPlayer)entityClientPlayerMP);
            ac.setRotYaw(y);
            ac.setRotPitch(p);
            ac.setRotRoll(r);

            this.mouseRollDeltaX *= 0.9D;

            this.mouseRollDeltaY *= 0.9D;
            float roll = MathHelper.wrapAngleTo180_float(ac.getRotRoll());
            float yaw = MathHelper.wrapAngleTo180_float(ac.getRotYaw() - ((EntityPlayer)entityClientPlayerMP).rotationYaw);
            roll *= MathHelper.cos((float)(yaw * Math.PI / 180.0D));
            if (ac.getTVMissile() != null && W_Lib.isClientPlayer((ac.getTVMissile()).shootingEntity) && ac.getIsGunnerMode((Entity)entityClientPlayerMP))
               roll = 0.0F;
            W_Reflection.setCameraRoll(roll + revRoll);
            correctViewEntityDummy((Entity)entityClientPlayerMP);
         } else {

            if (this.isRideAircraft) {
               W_Reflection.setCameraRoll(0.0F);

               this.isRideAircraft = false;
            }

            this.mouseRollDeltaX = 0.0D;

            this.mouseRollDeltaY = 0.0D;
         }
      }
      if (ac != null) {
         if (ac.getSeatIdByEntity((Entity)entityClientPlayerMP) == 0 && !ac.isDestroyed()) {
            ac.lastRiderYaw = ((EntityPlayer)entityClientPlayerMP).rotationYaw;
            ac.prevLastRiderYaw = ((EntityPlayer)entityClientPlayerMP).prevRotationYaw;
            ac.lastRiderPitch = ((EntityPlayer)entityClientPlayerMP).rotationPitch;
            ac.prevLastRiderPitch = ((EntityPlayer)entityClientPlayerMP).prevRotationPitch;
         }
         ac.updateWeaponsRotation();
      }
      MCH_ViewEntityDummy mCH_ViewEntityDummy = MCH_ViewEntityDummy.getInstance(((EntityPlayer)entityClientPlayerMP).worldObj);
      if (mCH_ViewEntityDummy != null) {
         ((Entity)mCH_ViewEntityDummy).rotationYaw = ((EntityPlayer)entityClientPlayerMP).rotationYaw;
         ((Entity)mCH_ViewEntityDummy).prevRotationYaw = ((EntityPlayer)entityClientPlayerMP).prevRotationYaw;
         if (ac != null) {
            MCH_WeaponSet wi = ac.getCurrentWeapon((Entity)entityClientPlayerMP);
            if (wi != null && wi.getInfo() != null && (wi.getInfo()).fixCameraPitch)
               ((Entity)mCH_ViewEntityDummy).rotationPitch = ((Entity)mCH_ViewEntityDummy).prevRotationPitch = 0.0F;
         }
      }

      this.prevTick = partialTicks;
   }

   public void correctViewEntityDummy(Entity entity) {
      //todo check
      MCH_ViewEntityDummy mCH_ViewEntityDummy = MCH_ViewEntityDummy.getInstance(entity.worldObj);
      if (mCH_ViewEntityDummy != null)
         if (((Entity)mCH_ViewEntityDummy).rotationYaw - ((Entity)mCH_ViewEntityDummy).prevRotationYaw > 180.0F) {
            ((Entity)mCH_ViewEntityDummy).prevRotationYaw += 360.0F;
         } else if (((Entity)mCH_ViewEntityDummy).rotationYaw - ((Entity)mCH_ViewEntityDummy).prevRotationYaw < -180.0F) {
            ((Entity)mCH_ViewEntityDummy).prevRotationYaw -= 360.0F;
         }
   }

   public void onPlayerTickPre(EntityPlayer player) {
      if (player.worldObj.isRemote) {
         ItemStack currentItemstack = player.getCurrentEquippedItem();
         if (currentItemstack != null && currentItemstack.getItem() instanceof MCH_ItemWrench)
            if (player.getItemInUseCount() > 0 && player.getItemInUse() != currentItemstack) {
               int maxdm = currentItemstack.getMaxDamage();
               int dm = currentItemstack.getItemDamage();
               if (dm <= maxdm && dm > 0)
                  player.setItemInUse(currentItemstack, player.getItemInUseCount());
            }
      }
   }

   public void onPlayerTickPost(EntityPlayer player) {}

   public void onRenderTickPost(float partialTicks) {
      if (this.mc.thePlayer != null) {
         MCH_ClientTickHandlerBase.applyRotLimit((Entity)this.mc.thePlayer);
         MCH_ViewEntityDummy mCH_ViewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.thePlayer.worldObj);
         if (mCH_ViewEntityDummy != null) {
            ((Entity)mCH_ViewEntityDummy).rotationPitch = this.mc.thePlayer.rotationPitch;
            ((Entity)mCH_ViewEntityDummy).rotationYaw = this.mc.thePlayer.rotationYaw;
            ((Entity)mCH_ViewEntityDummy).prevRotationPitch = this.mc.thePlayer.prevRotationPitch;
            ((Entity)mCH_ViewEntityDummy).prevRotationYaw = this.mc.thePlayer.prevRotationYaw;
         }
      }
      if (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat || this.mc.currentScreen.getClass().toString().indexOf("GuiDriveableController") >= 0) {
         for (MCH_Gui gui : this.guis) {
            if (drawGui(gui, partialTicks))
               break;
         }
         drawGui((MCH_Gui)this.gui_Common, partialTicks);
         drawGui(this.gui_Wrench, partialTicks);
         drawGui(this.gui_EMarker, partialTicks);
         if (isDrawScoreboard)
            MCH_GuiScoreboard.drawList(this.mc, this.mc.fontRenderer, false);
         drawGui(this.gui_Title, partialTicks);
      }
   }

   public boolean drawGui(MCH_Gui gui, float partialTicks) {
      if (gui.isDrawGui((EntityPlayer)this.mc.thePlayer)) {
         gui.drawScreen(0, 0, partialTicks);
         return true;
      }
      return false;
   }
}
