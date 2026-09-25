package mcheli;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntitySeat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/** Focused, opt-in dismount tracing kept independent from the general debug log switch. */
public final class MCH_DismountDiagnostics {
   private static final Map<EntityPlayer, Session> SESSIONS =
         Collections.synchronizedMap(new WeakHashMap<EntityPlayer, Session>());
   private static final Map<EntityPlayer, Entity> UPDATE_MOUNTS =
         Collections.synchronizedMap(new WeakHashMap<EntityPlayer, Entity>());
   private static final Map<EntityPlayer, Boolean> STACK_LOGGED =
         Collections.synchronizedMap(new WeakHashMap<EntityPlayer, Boolean>());
   private static final Map<EntityPlayer, Entity> MOUNT_ENTITY_CHANGES =
         Collections.synchronizedMap(new WeakHashMap<EntityPlayer, Entity>());
   private static final Map<EntityPlayer, String> INPUT_SAMPLES =
         Collections.synchronizedMap(new WeakHashMap<EntityPlayer, String>());
   private static final Map<String, Boolean> CALLBACKS =
         Collections.synchronizedMap(new java.util.HashMap<String, Boolean>());

   private MCH_DismountDiagnostics() {
   }

   private static final class Session {
      final Entity mount;
      final long started;
      final String id;

      Session(EntityPlayer player, Entity mount, long started) {
         this.mount = mount;
         this.started = started;
         this.id = player.getUniqueID() + "/" + identity(mount) + "/" + started;
      }
   }

   public static boolean enabled() {
      return MCH_Config.DebugDismount != null && MCH_Config.DebugDismount.prmBool;
   }

   public static void startup(String version, String source, String side, Object mixins) {
      if(enabled()) {
         log(null, "startup version=%s source=%s launchSide=%s mixins=%s",
               version, source, side, mixins);
      }
   }

   public static void callback(String name, World world) {
      if(enabled() && CALLBACKS.put(name, Boolean.TRUE) == null) {
         log(world, "mixin-callback-first name=%s", name);
      }
   }

   public static String startSession(EntityPlayer player, Entity mount, long started) {
      if(player == null) {
         return "none";
      }
      Session session = new Session(player, mount, started);
      SESSIONS.put(player, session);
      STACK_LOGGED.remove(player);
      INPUT_SAMPLES.remove(player);
      return session.id;
   }

   public static String session(EntityPlayer player) {
      Session session = player != null ? SESSIONS.get(player) : null;
      if(session != null) {
         return session.id;
      }
      return player == null ? "none" : player.getUniqueID() + "/" + identity(player.ridingEntity) + "/no-hold";
   }

   public static void inputGuard(String guard, EntityPlayer player, Object input, boolean before, boolean after) {
      if(enabled()) {
         String sample = session(player) + "/" + guard + "/" + before + "/" + after + "/"
               + (input != null ? input.getClass().getName() : "null");
         String previous = INPUT_SAMPLES.get(player);
         String marker = guard + "=" + sample;
         if(previous != null && previous.contains(marker)) {
            return;
         }
         INPUT_SAMPLES.put(player, (previous != null ? previous + ";" : "") + marker);
         log(player != null ? player.worldObj : null,
               "session=%s input-guard=%s inputClass=%s sneakBefore=%s sneakAfter=%s player=%s mount=%s parent=%s seat=%d",
               session(player), guard, input != null ? input.getClass().getName() : "null",
               before, after, identity(player), identity(player != null ? player.ridingEntity : null),
               identity(parent(player != null ? player.ridingEntity : null)), seat(player != null ? player.ridingEntity : null));
      }
   }

   public static void updateHead(EntityPlayer player) {
      callback("EntityPlayer.updateRidden", player.worldObj);
      UPDATE_MOUNTS.put(player, player.ridingEntity);
      if(enabled() && player.isSneaking() && parent(player.ridingEntity) != null) {
         log(player.worldObj, "session=%s vanilla-sneak-action attempt=true actualDetach=false player=%s mount=%s",
               session(player), identity(player), identity(player.ridingEntity));
      }
   }

   public static void updateReturn(EntityPlayer player) {
      Entity before = UPDATE_MOUNTS.remove(player);
      if(before != player.ridingEntity && (parent(before) != null || parent(player.ridingEntity) != null)) {
         Entity observed = MOUNT_ENTITY_CHANGES.remove(player);
         if(observed != player.ridingEntity) {
            mountChange(player, before, player.ridingEntity, "reference-change-across-update", true);
         }
      }
   }

   public static void mountChange(EntityPlayer player, Entity oldMount, Entity newMount, String caller, boolean bypass) {
      if(!enabled() || (parent(oldMount) == null && parent(newMount) == null)) {
         return;
      }
      String stack = "suppressed-after-first-unexpected-detach";
      boolean detach = oldMount != null && newMount == null;
      Session session = SESSIONS.get(player);
      boolean unexpected = detach && (session == null || System.nanoTime() - session.started < 3000000000L);
      if(unexpected && STACK_LOGGED.put(player, Boolean.TRUE) == null) {
         stack = shortStack();
      }
      log(player.worldObj,
            "session=%s ACTUAL-MOUNT-CHANGE detach=%s bypassMountEntity=%s player=%s old=%s new=%s caller=%s stack=%s",
            session(player), detach, bypass, identity(player), identity(oldMount), identity(newMount), caller, stack);
   }

   public static void observedMountEntity(EntityPlayer player, Entity newMount) {
      MOUNT_ENTITY_CHANGES.put(player, newMount);
   }

   public static MCH_EntityBaseVehicle parent(Entity mount) {
      if(mount instanceof MCH_EntityBaseVehicle) {
         return (MCH_EntityBaseVehicle)mount;
      }
      return mount instanceof MCH_EntitySeat ? ((MCH_EntitySeat)mount).getParent() : null;
   }

   public static int seat(Entity mount) {
      return mount instanceof MCH_EntitySeat ? ((MCH_EntitySeat)mount).seatID : (mount instanceof MCH_EntityBaseVehicle ? 0 : -1);
   }

   public static String identity(Object value) {
      if(!(value instanceof Entity)) {
         return value == null ? "null" : value.getClass().getName();
      }
      Entity entity = (Entity)value;
      return entity.getClass().getName() + "#" + entity.getEntityId() + "@" + Integer.toHexString(System.identityHashCode(entity));
   }

   public static void log(World world, String format, Object... values) {
      if(enabled()) {
         String side = world == null ? "UNKNOWN" : (world.isRemote ? "CLIENT" : "SERVER");
         MCH_Lib.Log("[MCH-DISMOUNT-DIAG] side=" + side + " thread=" + Thread.currentThread().getName()
               + " " + String.format(format, values));
      }
   }

   private static String shortStack() {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      StringBuilder result = new StringBuilder();
      for(int i = 3; i < trace.length && i < 9; ++i) {
         if(result.length() > 0) {
            result.append(" <- ");
         }
         result.append(trace[i].getClassName()).append('.').append(trace[i].getMethodName())
               .append(':').append(trace[i].getLineNumber());
      }
      return result.toString();
   }
}
