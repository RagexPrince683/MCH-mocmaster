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
      if(world == null || info == null || releasePos == null || initialVelocity == null) {
         r.reasonInvalid = "missing_input";
         return r;
      }

      Vec3 pos = Vec3.createVectorHelper(releasePos.xCoord, releasePos.yCoord, releasePos.zCoord);
      Vec3 vel = Vec3.createVectorHelper(initialVelocity.xCoord, initialVelocity.yCoord, initialVelocity.zCoord);
      r.initialVelocity = Vec3.createVectorHelper(vel.xCoord, vel.yCoord, vel.zCoord);

      int max = info.timeFuse > 0 ? Math.min(MAX_STEPS, info.timeFuse) : MAX_STEPS;
      for(int i = 0; i < max; ++i) {
         Vec3 prev = Vec3.createVectorHelper(pos.xCoord, pos.yCoord, pos.zCoord);
         if(info.speedFactor != 0.0F && i > info.speedFactorStartTick && i < info.speedFactorEndTick) {
            double speed = Math.sqrt(vel.xCoord * vel.xCoord + vel.yCoord * vel.yCoord + vel.zCoord * vel.zCoord);
            if(speed > 1.0E-7D) {
               vel.xCoord += vel.xCoord / speed * (double)info.speedFactor;
               vel.yCoord += vel.yCoord / speed * (double)info.speedFactor;
               vel.zCoord += vel.zCoord / speed * (double)info.speedFactor;
            }
         }
         vel.yCoord += (double)info.gravity;
         if(isBombLike(info)) {
            vel.xCoord *= 0.999D;
            vel.zCoord *= 0.999D;
         }
         pos.xCoord += vel.xCoord;
         pos.yCoord += vel.yCoord;
         pos.zCoord += vel.zCoord;
         MovingObjectPosition hit = world.rayTraceBlocks(prev, pos);
         r.ticksSimulated = i + 1;
         if(hit != null && hit.hitVec != null) {
            r.valid = true;
            r.impact = hit.hitVec;
            r.finalVelocity = Vec3.createVectorHelper(vel.xCoord, vel.yCoord, vel.zCoord);
            r.impactDistance = releasePos.distanceTo(r.impact);
            r.releaseAltitude = releasePos.yCoord - r.impact.yCoord;
            r.reasonInvalid = "";
            return r;
         }
         if(pos.yCoord < -64.0D || !world.blockExists((int)pos.xCoord, Math.max(0, (int)pos.yCoord), (int)pos.zCoord)) {
            r.reasonInvalid = "out_of_world";
            break;
         }
      }
      r.finalVelocity = Vec3.createVectorHelper(vel.xCoord, vel.yCoord, vel.zCoord);
      if("not_run".equals(r.reasonInvalid)) r.reasonInvalid = "no_collision";
      return r;
   }

   public static boolean isBombWeapon(MCH_WeaponBase weapon) {
      return weapon != null && isBombLike(weapon.getInfo());
   }

   public static boolean isBombLike(MCH_WeaponInfo info) {
      if(info == null || info.type == null) return false;
      return info.type.equalsIgnoreCase("bomb") || info.type.equalsIgnoreCase("dispenser")
            || (info.gravity < 0.0F && info.acceleration <= 1.0F && info.explosion > 0);
   }

   public static class Result {
      public boolean valid;
      public Vec3 impact;
      public int ticksSimulated;
      public double impactDistance;
      public double releaseAltitude;
      public Vec3 initialVelocity;
      public Vec3 finalVelocity;
      public String reasonInvalid;
   }
}
