package mcheli.lweapon;

import mcheli.*;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponCreator;
import mcheli.weapon.MCH_WeaponGuidanceSystem;
import mcheli.wrapper.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class MCH_ClientLightWeaponTickHandler extends MCH_ClientTickHandlerBase {

   private static FloatBuffer screenPos = BufferUtils.createFloatBuffer(3);
   private static FloatBuffer screenPosBB = BufferUtils.createFloatBuffer(3);
   private static FloatBuffer matModel = BufferUtils.createFloatBuffer(16);
   private static FloatBuffer matProjection = BufferUtils.createFloatBuffer(16);
   private static IntBuffer matViewport = BufferUtils.createIntBuffer(16);
   protected boolean isHeldItem = false;
   protected boolean isBeforeHeldItem = false;
   protected EntityPlayer prevThePlayer = null;
   protected ItemStack prevItemStack = null;
   public MCH_Key KeyAttack;
   public MCH_Key KeyUseWeapon;
   public MCH_Key KeySwWeaponMode;
   public MCH_Key KeyZoom;
   public MCH_Key KeyCameraMode;
   public MCH_Key[] Keys;
   protected static MCH_WeaponBase weapon;
   public static int reloadCount;
   public static int lockonSoundCount;
   public static int weaponMode;
   public static int selectedZoom;
   public static Entity markEntity = null;
   public static Vec3 markPos = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
   public static MCH_WeaponGuidanceSystem gs = new MCH_WeaponGuidanceSystem();
   public static double lockRange = 120.0D;


   public MCH_ClientLightWeaponTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft);
      this.updateKeybind(config);
      gs.canLockInAir = false;
      gs.canLockOnGround = false;
      gs.canLockInWater = false;
      gs.setLockCountMax(40);
      gs.lockRange = 120.0D;
      lockonSoundCount = 0;
      this.initWeaponParam((EntityPlayer)null);
   }

   public static void markEntity(Entity entity, double x, double y, double z) {
      if(gs.getLockingEntity() == entity) {
         GL11.glGetFloat(2982, matModel);
         GL11.glGetFloat(2983, matProjection);
         GL11.glGetInteger(2978, matViewport);
         GLU.gluProject((float)x, (float)y, (float)z, matModel, matProjection, matViewport, screenPos);
         MCH_AircraftInfo i = entity instanceof MCH_EntityAircraft?((MCH_EntityAircraft)entity).getAcInfo():null;
         float w = i != null?i.markerWidth:(entity.width > entity.height?entity.width:entity.height);
         float h = i != null?i.markerHeight:entity.height;
         GLU.gluProject((float)x + w, (float)y + h, (float)z + w, matModel, matProjection, matViewport, screenPosBB);
         markEntity = entity;
      } 

   }

   public static Vec3 getMartEntityPos() {
      return gs.getLockingEntity() == markEntity && markEntity != null?Vec3.createVectorHelper((double)screenPos.get(0), (double)screenPos.get(1), (double)screenPos.get(2)):null;
   }

   public static Vec3 getMartEntityBBPos() {
      return gs.getLockingEntity() == markEntity && markEntity != null?Vec3.createVectorHelper((double)screenPosBB.get(0), (double)screenPosBB.get(1), (double)screenPosBB.get(2)):null;
   }

   public void initWeaponParam(EntityPlayer player) {
      reloadCount = 0;
      weaponMode = 0;
      selectedZoom = 0;
   }

   public void updateKeybind(MCH_Config config) {
      this.KeyAttack = new MCH_Key(MCH_Config.KeyAttack.prmInt);
      this.KeyUseWeapon = new MCH_Key(MCH_Config.KeyUseWeapon.prmInt);
      this.KeySwWeaponMode = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.KeyCameraMode = new MCH_Key(MCH_Config.KeyCameraMode.prmInt);
      this.Keys = new MCH_Key[]{this.KeyAttack, this.KeyUseWeapon, this.KeySwWeaponMode, this.KeyZoom, this.KeyCameraMode};
   }

   protected void onTick(boolean inGUI) {
      MCH_Key[] player = this.Keys;
      int is = player.length;

      for(int pc = 0; pc < is; ++pc) {
         MCH_Key RELOAD_CNT = player[pc];
         RELOAD_CNT.update();
      }

      this.isBeforeHeldItem = this.isHeldItem;
      EntityClientPlayerMP var6 = super.mc.thePlayer;
      if(this.prevThePlayer == null || this.prevThePlayer != var6) {
         this.initWeaponParam(var6);
         this.prevThePlayer = var6;
      }

      ItemStack var7 = var6 != null?var6.getHeldItem():null;
      if(var6 == null || var6.ridingEntity instanceof MCH_EntityGLTD || var6.ridingEntity instanceof MCH_EntityAircraft) {
         var7 = null;
      }

      if(gs.getLockingEntity() == null) {
         markEntity = null;
      }

      if(var7 != null && var7.getItem() instanceof MCH_ItemLightWeaponBase) {
         MCH_ItemLightWeaponBase var8 = (MCH_ItemLightWeaponBase)var7.getItem();
         if(this.prevItemStack == null || !this.prevItemStack.isItemEqual(var7) && !this.prevItemStack.getUnlocalizedName().equals(var7.getUnlocalizedName())) {
            this.initWeaponParam(var6);
            weapon = MCH_WeaponCreator.createWeapon(var6.worldObj, MCH_ItemLightWeaponBase.getName(var7), Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, (MCH_IEntityLockChecker)null, false);
            if(weapon != null && weapon.getInfo() != null ) {
            System.out.println("Name: " + MCH_ItemLightWeaponBase.getName(var7) + " info name " + weapon.getInfo().name + " type " +  weapon.weaponInfo.getWeaponTypeName());
               gs = (MCH_WeaponGuidanceSystem) weapon.getGuidanceSystem();
            }
         }

         if(weapon == null) {
            return;
         }

         gs.setWorld(var6.worldObj);
         gs.lockRange = lockRange;
         if(var6.getItemInUseDuration() > 10) {
            selectedZoom %= weapon.getInfo().zoom.length;
            W_Reflection.setCameraZoom(weapon.getInfo().zoom[selectedZoom]);
         } else {
            W_Reflection.restoreCameraZoom();
         }

         if(var7.getItemDamage() < var7.getMaxDamage()) {
            //if(var6.getItemInUseDuration() > 10) {
               gs.lock(var6);
               if(gs.getLockCount() > 0) {
                  if(lockonSoundCount > 0) {
                     --lockonSoundCount;
                  } else {
                     lockonSoundCount = 7;
                     lockonSoundCount = (int)((double)lockonSoundCount * (1.0D - (double)gs.getLockCount() / (double)gs.getLockCountMax()));
                     if(lockonSoundCount < 3) {
                        lockonSoundCount = 2;
                     }

                     W_McClient.MOD_playSoundFX("ir_lock_tone", 1.0F, 1.0F);

                  }
               }else {
            	   W_McClient.MOD_playSoundFX("ir_basic_tone", 1.0F, 1.0F);
               }
            //} else {
               
              // gs.clearLock();
               
           // }
            if(var6.getItemInUseDuration() < 10) {
            	W_Reflection.restoreCameraZoom();
            }
            reloadCount = 0;
         } else {
            lockonSoundCount = 0;
            if(W_EntityPlayer.hasItem(var6, var8.bullet) && var6.getItemInUseCount() <= 0) {
               if(reloadCount == 10) {
                  W_McClient.MOD_playSoundFX("fim92_reload", 1.0F, 1.0F);
               }

               boolean var10 = true;
               if(reloadCount < 40) {
                  ++reloadCount;
                  if(reloadCount == 40) {
                     this.onCompleteReload();
                  }
               }
            } else {
               reloadCount = 0;
            }

            gs.clearLock();
         }

         if(!inGUI) {
            this.playerControl(var6, var7, (MCH_ItemLightWeaponBase)var7.getItem());
         }

         this.isHeldItem = MCH_ItemLightWeaponBase.isHeld(var6);
      } else {
         lockonSoundCount = 0;
         reloadCount = 0;
         this.isHeldItem = false;
      }

      if(this.isBeforeHeldItem != this.isHeldItem) {
         MCH_Lib.DbgLog(true, "LWeapon cancel", new Object[0]);
         if(!this.isHeldItem) {
            if(getPotionNightVisionDuration(var6) < 250) {
               MCH_PacketLightWeaponPlayerControl var9 = new MCH_PacketLightWeaponPlayerControl();
               var9.camMode = 1;
               W_Network.sendToServer(var9);
               var6.removePotionEffectClient(Potion.nightVision.getId());
            }

            W_Reflection.restoreCameraZoom();
         }
      }

      this.prevItemStack = var7;
      gs.update();
   }

   protected void onCompleteReload() {
      MCH_PacketLightWeaponPlayerControl pc = new MCH_PacketLightWeaponPlayerControl();
      pc.cmpReload = 1;
      W_Network.sendToServer(pc);
   }

   protected void playerControl(EntityPlayer player, ItemStack is, MCH_ItemLightWeaponBase item) {
      MCH_PacketLightWeaponPlayerControl pc = new MCH_PacketLightWeaponPlayerControl();
      boolean send = false;
      boolean autoShot = false;
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.LWeaponAutoFire.prmBool && is.getItemDamage() < is.getMaxDamage() && gs.isLockComplete()) {
         autoShot = true;
      }

      if(this.KeySwWeaponMode.isKeyDown() && weapon.numMode > 1) {
         weaponMode = (weaponMode + 1) % weapon.numMode;
         W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
      }

      if(this.KeyAttack.isKeyPress() || autoShot) {
         boolean pe = false;
         if(is.getItemDamage() < is.getMaxDamage()) {
            boolean canFire = true;
            if(weaponMode > 0) {
               double dx = gs.getTargetEntity().posX - player.posX;
               double dz = gs.getTargetEntity().posZ - player.posZ;
               canFire = Math.sqrt(dx * dx + dz * dz) >= 40.0D;
            }

            if(canFire) {
               pc.useWeapon = true;
               pc.useWeaponOption1 = W_Entity.getEntityId(gs.lastLockEntity);
               pc.useWeaponOption2 = weaponMode;
               pc.useWeaponPosX = player.posX;
               pc.useWeaponPosY = player.posY;
               pc.useWeaponPosZ = player.posZ;
               gs.clearLock();
               send = true;
               pe = true;
            }
         }

         if(this.KeyAttack.isKeyDown() && !pe && player.getItemInUseDuration() > 5) {
            playSoundNG();
         }
      }

      if(this.KeyZoom.isKeyDown()) {
         int pe1 = selectedZoom;
         selectedZoom = (selectedZoom + 1) % weapon.getInfo().zoom.length;
         if(pe1 != selectedZoom) {
            playSound("zoom", 0.5F, 1.0F);
         }
      }

      if(this.KeyCameraMode.isKeyDown()) {
         PotionEffect pe2 = player.getActivePotionEffect(Potion.nightVision);
         MCH_Lib.DbgLog(true, "LWeapon NV %s", new Object[]{pe2 != null?"ON->OFF":"OFF->ON"});
         if(pe2 != null) {
            player.removePotionEffectClient(Potion.nightVision.getId());
            pc.camMode = 1;
            send = true;
            W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
         } else if(player.getItemInUseDuration() > 60) {
            pc.camMode = 2;
            send = true;
            W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
         } else {
            playSoundNG();
         }
      }

      if(send) {
         W_Network.sendToServer(pc);
      }

   }

   public static int getPotionNightVisionDuration(EntityPlayer player) {
      PotionEffect cpe = player.getActivePotionEffect(Potion.nightVision);
      return player != null && cpe != null?cpe.getDuration():0;
   }

}
