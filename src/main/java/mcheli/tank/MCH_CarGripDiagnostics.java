package mcheli.tank;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/** Explicit per-definition opt-in, separate CSV output, and no normal console logging. */
public final class MCH_CarGripDiagnostics {
   private static BufferedWriter writer;
   private static boolean failed;
   private static String writeError;

   private MCH_CarGripDiagnostics() {}

   public static String getWriteError() { return writeError; }

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
