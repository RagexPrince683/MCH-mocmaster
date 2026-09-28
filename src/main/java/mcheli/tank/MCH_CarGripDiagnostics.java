package mcheli.tank;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/** Explicit per-definition opt-in, separate CSV output, and no normal console logging. */
public final class MCH_CarGripDiagnostics {
   private static BufferedWriter writer;
   private static BufferedWriter clientWriter;
   private static BufferedWriter movementWriter;
   private static boolean failed;
   private static String writeError;

   private MCH_CarGripDiagnostics() {}

   public static String getWriteError() { return writeError; }

   /** Opt-in server trace taken after collision cleanup and wheel reconciliation. */
   static synchronized void recordMovement(MCH_EntityTank car, MCH_CarBodyMovement.Trace trace,
         MCH_CarBodyMovement.Result result, MCH_WheelManager.CarContact driveContact, double force,
         double oldX, double oldY, double oldZ, double x, double y, double z, double poseY,
         float oldYaw, float oldPitch, float oldRoll) {
      if(failed) return;
      try {
         if(movementWriter == null) {
            File file = new File("logs/car-body-movement.csv");
            File directory = file.getParentFile();
            if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create " + directory);
            movementWriter = new BufferedWriter(new FileWriter(file, true));
            // A session header also identifies the schema when appending to an older capture.
            movementWriter.write("vehicle,entity,tick,w,s,gear,throttle,rpm,service_brake,handbrake,drive_front,drive_rear,drive_force,old_x,old_y,old_z,request_x,request_y,request_z,pose_y,old_yaw,old_pitch,old_roll,yaw,pitch,roll,paths,step,blocked_x,blocked_z,grounded,final_front,final_rear,final_x,final_y,final_z,motion_x,motion_y,motion_z,input_x,input_z,accepted_x,accepted_y,accepted_z,selected,step_gate,step_height,step_base_y,vertical_y,wheel_support,body_support,cleanup_x,cleanup_z,desired_yaw,target_pitch,target_roll,pose_decision,pose_fraction,terrain_reason,terrain_pitch,terrain_front_y,terrain_rear_y,terrain_front_count,terrain_rear_count,running,front_wheel_speed,rear_wheel_speed\n");
         }
         MCH_CarDrivetrain engine = car.carDrivetrain;
         MCH_WheelManager.CarContact contact = car.WheelMng.getCarGroundContact(false);
         movementWriter.write(String.format(Locale.ROOT,
               "%s,%d,%d,%b,%b,%d,%.6f,%.2f,%b,%b,%d,%d,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%s,%b,%b,%b,%b,%d,%d,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f",
               car.getTankInfo().name.replace(',', '_').replace('\n', '_').replace('\r', '_'), car.getEntityId(), car.ticksExisted,
               car.throttleUp, car.throttleDown, engine.gear, engine.throttle, engine.rpm, engine.serviceBrake, engine.handbrake,
               driveContact != null ? driveContact.front : -1, driveContact != null ? driveContact.rear : -1, force,
               oldX, oldY, oldZ, x, y, z, poseY, oldYaw, oldPitch, oldRoll,
               car.getRotYaw(), car.getRotPitch(), car.getRotRoll(), trace.paths,
               result.stepped, result.blockedX, result.blockedZ, result.grounded, contact.front, contact.rear,
               car.posX, car.posY, car.posZ, car.motionX, car.motionY, car.motionZ));
         MCH_WheelManager.TerrainTrace terrain = car.WheelMng.carDiagnosticTerrain;
         movementWriter.write(String.format(Locale.ROOT,
               ",%.9f,%.9f,%.9f,%.9f,%.9f,%s,%s,%.9f,%.9f,%.9f,%b,%b,%s,%s,%.5f,%.5f,%.5f,%s,%.6f,%s,%.5f,%.9f,%.9f,%d,%d,%b,%.9f,%.9f",
               trace.inputX, trace.inputZ, result.x, result.y, result.z, trace.selected, trace.stepGate,
               trace.stepHeight, trace.baseY, trace.verticalY, trace.wheelSupport, trace.bodySupport,
               trace.cleanupX, trace.cleanupZ, trace.desiredYaw, trace.targetPitch, trace.targetRoll,
               trace.pose, trace.poseFraction, terrain != null ? terrain.reason : "not_sampled",
               terrain != null ? terrain.pitch : Float.NaN, terrain != null ? terrain.front : Double.NaN,
               terrain != null ? terrain.rear : Double.NaN, terrain != null ? terrain.frontCount : 0,
               terrain != null ? terrain.rearCount : 0, engine.running, engine.frontWheelSpeed, engine.rearWheelSpeed));
         movementWriter.newLine();
         movementWriter.flush();
      } catch(IOException ex) {
         writeError = ex.toString(); failed = true;
         if(movementWriter != null) try { movementWriter.close(); } catch(IOException ignored) {}
      }
   }

   static synchronized void record(Snapshot snapshot) {
      if(failed) return;
      try {
         if(writer == null) {
            File file = new File("logs/car-tire-grip.csv");
            File directory = file.getParentFile();
            if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create " + directory);
            boolean header = !file.isFile() || file.length() == 0;
            writer = new BufferedWriter(new FileWriter(file, true));
            if(header) writer.write("vehicle,entity,tick,total,front,rear,raw_probe,paired_flags,sideways,requested,applied,limit,reason,yaw_requested,yaw_applied\n");
         }
         writer.write(snapshot.toCsv());
         writer.newLine();
         writer.flush();
      } catch(IOException ex) {
         // Exposed through the diagnostic accessor; never spam normal logs or interrupt physics.
         writeError = ex.toString();
         failed = true;
         if(writer != null) try { writer.close(); } catch(IOException ignored) {}
      }
   }

   static synchronized void recordClient(String vehicle, int entity, int tick, boolean left, boolean right,
                                         float requested, float applied, double speed, double contact,
                                         float tickDelta) {
      if(failed) return;
      try {
         if(clientWriter == null) {
            File file = new File("logs/car-steering-client.csv");
            File directory = file.getParentFile();
            if(!directory.isDirectory() && !directory.mkdirs()) {
               throw new IOException("Cannot create " + directory);
            }
            boolean header = !file.isFile() || file.length() == 0;
            clientWriter = new BufferedWriter(new FileWriter(file, true));
            if(header) {
               clientWriter.write("vehicle,entity,tick,left,right,input_requested,yaw_applied,speed,contact,tick_delta\n");
            }
         }
         clientWriter.write(String.format(Locale.ROOT, "%s,%d,%d,%b,%b,%.5f,%.5f,%.8f,%.5f,%.5f",
                 vehicle.replace(',', '_').replace('\n', '_').replace('\r', '_'), entity, tick, left, right,
                 requested, applied, speed, contact, tickDelta));
         clientWriter.newLine();
         clientWriter.flush();
      } catch(IOException ex) {
         writeError = ex.toString();
         failed = true;
         if(clientWriter != null) {
            try {
               clientWriter.close();
            } catch(IOException ignored) {
            }
         }
      }
   }

   public static final class Snapshot {
      public final String vehicle, reason;
      public final int entity, tick;
      public final MCH_WheelManager.CarContact contact;
      public final double sideways, requested, applied, limit;
      public final float yawRequested, yawApplied;

      Snapshot(String vehicle, int entity, int tick, MCH_WheelManager.CarContact contact,
               double sideways, double requested, double applied, double limit, String reason,
               float yawRequested, float yawApplied) {
         this.vehicle = vehicle; this.entity = entity; this.tick = tick; this.contact = contact;
         this.sideways = sideways; this.requested = requested; this.applied = applied;
         this.limit = limit; this.reason = reason;
         this.yawRequested = yawRequested; this.yawApplied = yawApplied;
      }

      public String toCsv() {
         return String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%d,%d,%.8f,%.8f,%.8f,%.8f,%s,%.5f,%.5f",
                 vehicle.replace(',', '_').replace('\n', '_').replace('\r', '_'), entity, tick,
                 contact.total, contact.front, contact.rear, contact.rawProbe, contact.pairedFlags,
                 sideways, requested, applied, limit, reason, yawRequested, yawApplied);
      }
   }
}
