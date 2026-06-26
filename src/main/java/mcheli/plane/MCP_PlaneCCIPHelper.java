package mcheli.plane;

import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public final class MCP_PlaneCCIPHelper {
   public static final int MAX_STEPS = 260;
   private MCP_PlaneCCIPHelper() {}

   public static Result predict(World world, MCH_WeaponInfo info, Vec3 releasePos, Vec3 initialVelocity) {
      Result r = new Result();
      r.valid = false;
      r.reasonInvalid = "not_run";
      r.releasePos = copy(releasePos);
      r.initialVelocity = copy(initialVelocity);
      r.gravity = info != null ? (double)info.gravity : 0.0D;
      r.horizontalDrag = info != null && isBombLike(info) ? 0.999D : 1.0D;
      r.accelerationFactor = getAccelerationFactor(info);
      r.simulationTimeStep = r.accelerationFactor;
      if(world == null || info == null || releasePos == null || initialVelocity == null) {
         r.reasonInvalid = "missing_input";
         return r;
      }

      Vec3 pos = copy(releasePos);
      Vec3 vel = copy(initialVelocity);
      double accelerationFactor = r.accelerationFactor;

      int max = info.timeFuse > 0 ? Math.min(MAX_STEPS, info.timeFuse) : MAX_STEPS;
      for(int i = 0; i < max; ++i) {
         if(info.speedFactor != 0.0F && i > info.speedFactorStartTick && i < info.speedFactorEndTick) {
            double speed = Math.sqrt(vel.xCoord * vel.xCoord + vel.yCoord * vel.yCoord + vel.zCoord * vel.zCoord);
            if(speed > 1.0E-7D) {
               vel.xCoord += vel.xCoord / speed * (double)info.speedFactor;
               vel.yCoord += vel.yCoord / speed * (double)info.speedFactor;
               vel.zCoord += vel.zCoord / speed * (double)info.speedFactor;
            }
         }
         vel.yCoord += (double)info.gravity;
         Vec3 next = Vec3.createVectorHelper(pos.xCoord + vel.xCoord * r.accelerationFactor,
               pos.yCoord + vel.yCoord * r.accelerationFactor,
               pos.zCoord + vel.zCoord * r.accelerationFactor);
         MovingObjectPosition hit = world.rayTraceBlocks(prev, next);
         pos = next;
         if(isBombLike(info)) {
            vel.xCoord *= 0.999D;
            vel.zCoord *= 0.999D;
         }
         r.ticksSimulated = i + 1;
         if(hit != null && hit.hitVec != null) {
            r.valid = true;
            r.impact = hit.hitVec;
            r.finalVelocity = copy(vel);
            r.impactDistance = releasePos.distanceTo(r.impact);
            r.releaseAltitude = releasePos.yCoord - r.impact.yCoord;
            r.reasonInvalid = "";
            return r;
         }

         pos = next;
         if(isBombLike(info)) {
            vel.xCoord *= 0.999D;
            vel.zCoord *= 0.999D;
         }
         if(pos.yCoord < -64.0D || !world.blockExists((int)pos.xCoord, Math.max(0, (int)pos.yCoord), (int)pos.zCoord)) {
            r.reasonInvalid = "out_of_world";
            break;
         }
      }
      r.finalVelocity = copy(vel);
      if("not_run".equals(r.reasonInvalid)) r.reasonInvalid = "no_collision";
      return r;
   }

   private static Vec3 copy(Vec3 v) {
      return v != null ? Vec3.createVectorHelper(v.xCoord, v.yCoord, v.zCoord) : null;
   }

   public static boolean isBombWeapon(MCH_WeaponBase weapon) {
      return weapon != null && isBombLike(weapon.getInfo());
   }

   private static double getAccelerationFactor(MCH_WeaponInfo info) {
      if(info == null || info.acceleration <= 4.0F) return 1.0D;
      return (double)(info.acceleration / 4.0F);
   }

   public static boolean isBombLike(MCH_WeaponInfo info) {
      if(info == null || info.type == null) return false;
      return info.type.equalsIgnoreCase("bomb") || info.type.equalsIgnoreCase("dispenser")
            || (info.gravity < 0.0F && info.acceleration <= 1.0F && info.explosion > 0);
   }

   private static double getAccelerationFactor(MCH_WeaponInfo info) {
      if(info == null || info.type == null || info.acceleration <= 4.0F) return 1.0D;
      return isBulletLike(info) || isRocketLike(info) ? (double)(info.acceleration / 4.0F) : 1.0D;
   }

   private static boolean isBulletLike(MCH_WeaponInfo info) {
      return info != null && info.type != null && info.type.equalsIgnoreCase("bullet");
   }

   private static boolean isRocketLike(MCH_WeaponInfo info) {
      return info != null && info.type != null && info.type.equalsIgnoreCase("rocket");
   }

   public static class Result {
      public boolean valid;
      public Vec3 impact;
      public Vec3 releasePos;
      public int ticksSimulated;
      public double impactDistance;
      public double releaseAltitude;
      public Vec3 initialVelocity;
      public Vec3 finalVelocity;
      public double gravity;
      public double horizontalDrag;
      public double accelerationFactor;
      public double simulationTimeStep;
      public double accelerationFactor;
      public Vec3 ejectionVelocity;
      public Vec3 initialVelocityDeltaFromAircraft;
      public double initialVelocityUpDot;
      public double initialVelocitySideDot;
      public boolean warningImpossibleLaunch;
      public String releaseMode;
      public String reasonInvalid;
   }
}
