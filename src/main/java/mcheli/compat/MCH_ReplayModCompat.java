package mcheli.compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/** Optional, class-name-only integration with Replay Mod playback. */
@SideOnly(Side.CLIENT)
public final class MCH_ReplayModCompat {
   private static final String CAMERA_ENTITY = "com.replaymod.replay.camera.CameraEntity";
   private static Class lastPlayerClass;
   private static boolean lastPlayerClassIsReplayCamera;
   private static boolean playbackActive;
   private static long nextBlockedWriteLogTime;

   private MCH_ReplayModCompat() {}

   public static boolean isReplayPlaybackActive() {
      Minecraft mc = Minecraft.getMinecraft();
      Entity player = mc != null?mc.thePlayer:null;
      if(player == null) {
         return false;
      }
      Class playerClass = player.getClass();
      if(playerClass != lastPlayerClass) {
         lastPlayerClass = playerClass;
         lastPlayerClassIsReplayCamera = isReplayCameraClass(playerClass);
      }
      return lastPlayerClassIsReplayCamera;
   }

   private static boolean isReplayCameraClass(Class type) {
      for(Class current = type; current != null; current = current.getSuperclass()) {
         if(CAMERA_ENTITY.equals(current.getName())) {
            return true;
         }
      }
      return false;
   }

   /** Records playback transitions. State release is performed by the client tick owner. */
   public static boolean updatePlaybackState() {
      boolean active = isReplayPlaybackActive();
      if(active != playbackActive) {
         playbackActive = active;
         debug(active?"Replay playback detected":"Replay playback ended");
      }
      return active;
   }

   public static void logCameraOwnershipReleased() {
      debug("MCHeli camera ownership released");
   }

   public static void logCameraHandlingRestored() {
      debug("Normal MCHeli camera handling restored");
   }

   public static void logBlockedCameraWrite(String owner) {
      long now = System.currentTimeMillis();
      if(now >= nextBlockedWriteLogTime) {
         nextBlockedWriteLogTime = now + 1000L;
         debug("Blocked camera write from %s while Replay Mod owns playback camera", owner);
      }
   }

   private static void debug(String message, Object ... args) {
      if(MCH_Config.EnableMCHLibDebugLog != null && MCH_Config.EnableMCHLibDebugLog.prmBool) {
         MCH_Lib.DbgLog(true, "[ReplayModCompat] " + message, args);
      }
   }
}
