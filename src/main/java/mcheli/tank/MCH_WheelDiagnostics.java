package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.network.packets.PacketWheelDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Read-only, session-local subscriptions. Called only on the owning server thread. */
public final class MCH_WheelDiagnostics {
   private static final Map<EntityPlayerMP, Boolean> ENABLED = new WeakHashMap<EntityPlayerMP, Boolean>();
   private MCH_WheelDiagnostics() {}

   public static boolean isEnabled(EntityPlayerMP player) { return ENABLED.containsKey(player); }

   public static void setEnabled(EntityPlayerMP player, boolean enabled) {
      if(enabled) ENABLED.put(player, Boolean.TRUE);
      else ENABLED.remove(player);
      send(player);
   }

   /** Direct seats only; remote control, nearby vehicles and other riders' vehicles never qualify. */
   public static MCH_EntityTank riddenCar(EntityPlayer player) {
      if(player == null) return null;
      Entity ridden = player.ridingEntity;
      if(ridden instanceof MCH_EntitySeat) ridden = ((MCH_EntitySeat)ridden).getParent();
      if(!(ridden instanceof MCH_EntityTank)) return null;
      MCH_EntityTank car = (MCH_EntityTank)ridden;
      return !car.isDead && !car.isDestroyed() && car.getTankInfo() != null
            && car.getTankInfo().civilianCarGrip ? car : null;
   }

   public static boolean hasViewer(MCH_EntityTank car) {
      if(ENABLED.isEmpty()) return false;
      for(Object object : car.worldObj.playerEntities) {
         if(object instanceof EntityPlayerMP && isEnabled((EntityPlayerMP)object)
               && riddenCar((EntityPlayerMP)object) == car) return true;
      }
      return false;
   }

   public static void tickServer() {
      MinecraftServer server = MinecraftServer.getServer();
      if(server == null) return;
      for(Object object : server.getConfigurationManager().playerEntityList) {
         if(object instanceof EntityPlayerMP && isEnabled((EntityPlayerMP)object)) send((EntityPlayerMP)object);
      }
   }

   private static void send(EntityPlayerMP player) {
      MCH_EntityTank car = isEnabled(player) ? riddenCar(player) : null;
      List<String> left = new ArrayList<String>(), right = new ArrayList<String>();
      if(car != null) {
         boolean fresh = car.wheelDebugTick == car.ticksExisted && car.wheelDebugMovement != null;
         left.add("S vehicle tick=" + car.ticksExisted + " decision tick=" + car.wheelDebugTick);
         left.add("S movement=" + (fresh ? "current" : "not captured this tick") + " (blocks, blocks/tick)");
         MCH_WheelManager.CarContact contact = car.WheelMng.getCarGroundContact(false);
         left.add("S contact front/rear=" + (fresh ? contact.front + "/" + contact.rear : "N/A"));
         MCH_CarDrivetrain engine = car.carDrivetrain;
         boolean drivetrain = fresh && car.hasCarDrivetrain();
         left.add("S driven axle=" + (drivetrain ? car.getTankInfo().driveType : "N/A"));
         left.add("S throttle=" + (drivetrain ? n(engine.throttle) : "N/A")
               + " RPM=" + (drivetrain ? n(engine.rpm) : "N/A") + " gear=" + (drivetrain ? engine.gear : "N/A"));
         left.add("S W/S=" + car.throttleUp + "/" + car.throttleDown + " running=" + (drivetrain && engine.running));
         left.add("S service/handbrake=" + (drivetrain ? engine.serviceBrake + "/" + engine.handbrake : "N/A"));
         left.add("S horizontal velocity=" + n(car.motionX) + "," + n(car.motionZ)
               + " speed=" + n(Math.hypot(car.motionX, car.motionZ)));
         if(fresh) left.addAll(car.wheelDebugMovement);
         else left.add("S force/terrain/pose/step/movement/cleanup=N/A");
         for(int i = 0; i < car.WheelMng.wheels.length; ++i) {
            MCH_EntityWheel wheel = car.WheelMng.wheels[i];
            String prefix = "S W" + i + " ";
            if(!fresh || wheel == null || wheel.isDead || wheel.pos == null) {
               right.add(prefix + "axle/position/contact/suspension=N/A");
               continue;
            }
            right.add(prefix + "axle=" + (car.WheelMng.isFrontWheel(i) ? "front" : "rear")
                  + " physical contact=" + wheel.hasCarGroundContact());
            right.add(prefix + "local=" + xyz(wheel.pos.xCoord, wheel.pos.yCoord, wheel.pos.zCoord));
            right.add(prefix + "world=" + xyz(wheel.posX, wheel.posY, wheel.posZ));
            right.add(prefix + "support=" + wheel.suspensionSupported + " height=" + n(wheel.suspensionSupportY));
            right.add(prefix + "compression=" + (wheel.suspensionCompressionInitialized ? n(wheel.suspensionCompression) : "N/A")
                  + " rate=" + (wheel.suspensionCompressionInitialized ? n(wheel.suspensionCompressionRate) : "N/A"));
            right.add(prefix + "travel limit=" + n(car.getTankInfo().suspensionTravel));
         }
      }
      MCH_MOD.getPacketHandler().sendTo(new PacketWheelDiagnostics(player, car, isEnabled(player), left, right), player);
   }

   static List<String> movement(MCH_EntityTank car, MCH_CarBodyMovement.Trace trace,
         MCH_CarBodyMovement.Result result, double force, MCH_WheelManager.CarContact driveContact,
         double x, double y, double z, double poseY) {
      List<String> lines = new ArrayList<String>();
      lines.add("S drive force=" + n(force) + " drive contacts F/R="
            + (driveContact == null ? "N/A" : driveContact.front + "/" + driveContact.rear));
      MCH_WheelManager.TerrainTrace terrain = car.WheelMng.carDiagnosticTerrain;
      lines.add("S terrain result=" + (terrain == null ? "N/A" : terrain.reason));
      lines.add("S sample heights F/R=" + (terrain == null ? "N/A" : n(terrain.front) + "/" + n(terrain.rear)));
      lines.add("S sample counts F/R=" + (terrain == null ? "N/A" : terrain.frontCount + "/" + terrain.rearCount));
      lines.add("S terrain pitch=" + (terrain == null ? "N/A" : n(terrain.pitch)) + " deg");
      lines.add("S target pitch=" + n(trace.targetPitch) + " body pitch=" + n(car.getRotPitch()) + " deg");
      lines.add("S pose=" + trace.pose + " fraction=" + n(trace.poseFraction));
      lines.add("S pose rise=" + n(trace.poseRise) + " pose Y=" + n(poseY));
      lines.add("S step gate=" + trace.stepGate);
      lines.add("S selected path=" + trace.selected + " step height=" + n(trace.stepHeight));
      lines.add("S support wheel/body=" + trace.wheelSupport + "/" + trace.bodySupport);
      lines.add("S input X/Z=" + n(trace.inputX) + "," + n(trace.inputZ));
      lines.add("S requested XYZ=" + xyz(x, y, z));
      lines.add("S accepted XYZ=" + xyz(result.x, result.y, result.z));
      lines.add("S pre-move velocity X/Z=" + n(trace.preMotionX) + "," + n(trace.preMotionZ));
      lines.add("S post-move velocity X/Z=" + n(car.motionX) + "," + n(car.motionZ));
      double yaw = Math.toRadians(car.getRotYaw());
      lines.add("S translation basis yaw=" + n(car.getRotYaw()));
      lines.add("S requested forward/side=" + n(-x * Math.sin(yaw) + z * Math.cos(yaw))
            + "/" + n(x * Math.cos(yaw) + z * Math.sin(yaw)));
      lines.add("S accepted forward/side=" + n(-result.x * Math.sin(yaw) + result.z * Math.cos(yaw))
            + "/" + n(result.x * Math.cos(yaw) + result.z * Math.sin(yaw)));
      lines.add("S total accepted Y=" + n(result.y + poseY));
      // Intermediate candidate evaluations can overwrite the CSV trace's cleanup fields.
      // Describe the actual returned result that controlled velocity cleanup.
      boolean noProgress = Math.hypot(result.x, result.z) <= MCH_CarCollisionBox.EPSILON;
      String cleanupX = result.blockedX ? ("embedded".equals(trace.selected) ? "embedded" : noProgress ? "no_progress" : "final_contact") : "none";
      String cleanupZ = result.blockedZ ? ("embedded".equals(trace.selected) ? "embedded" : noProgress ? "no_progress" : "final_contact") : "none";
      lines.add("S cleanup X/Z=" + cleanupX + "/" + cleanupZ);
      lines.add("S blocked X/Z=" + result.blockedX + "/" + result.blockedZ + " => zero axis velocity");
      lines.add("S no_progress: clipped request AND travel <=1e-7");
      lines.add("S final_contact: clipped request AND 1e-5 axis probe clipped >1e-7");
      lines.add("S embedded: initial overlap AND nonzero axis request");
      // Include sweep-order and diagonal candidates already recorded by the solver.
      int rotationCount = 0, rotationClear = 0, rotationLanded = 0;
      for(String path : trace.paths.toString().split(";")) {
         int colon = path.indexOf(':');
         if(colon < 0) continue;
         String[] values = path.substring(colon + 1).split("\\|");
         if(values.length != 6) continue;
         String stage = path.substring(0, colon);
         if(stage.startsWith("step_rotate_")) {
            ++rotationCount;
            if(Boolean.parseBoolean(values[4])) ++rotationLanded;
            if(Boolean.parseBoolean(values[5])) ++rotationClear;
            continue; // Detailed decisions below include every rotation candidate.
         }
         lines.add("S " + stage + " XYZ=" + xyz(Double.parseDouble(values[0]),
               Double.parseDouble(values[1]), Double.parseDouble(values[2]))
               + " rise=" + n(Double.parseDouble(values[3])) + " L/C="
               + (stage.startsWith("normal_") || stage.equals("embedded") || stage.equals("rise") ? "N/A" : values[4]) + "/" + values[5]);
      }
      lines.add("S rotation paths/landed/clear=" + rotationCount + "/" + rotationLanded + "/" + rotationClear);
      lines.add("S rotation clearance rejections=" + trace.rotationClearanceRejected);
      lines.add("S paths: 0=X then Z, 1=Z then X, 2=diagonal");
      lines.add("S L/C=landed/clear (N/A=not sampled)");
      lines.addAll(trace.decisions);
      return lines;
   }

   public static String n(double value) {
      return Double.isNaN(value) || Double.isInfinite(value) ? "N/A" : String.format(Locale.ROOT, "%.6f", value);
   }
   public static String xyz(double x, double y, double z) { return n(x) + "," + n(y) + "," + n(z); }
}
