package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.AxisAlignedBB;

/** Civilian compound-body sweeps. Every component keeps its real dimensions and coordinates. */
final class MCH_CarBodyMovement {
   private static final double CONTACT_EPSILON = 1.0E-5D;

   interface Collisions {
      List<AxisAlignedBB> query(AxisAlignedBB sweep);
   }

   static final class Result {
      final double x, y, z;
      final boolean grounded, stepped;
      Result(double x, double y, double z, boolean grounded, boolean stepped) {
         this.x = x; this.y = y; this.z = z;
         this.grounded = grounded; this.stepped = stepped;
      }
   }

   static boolean supported(List<AxisAlignedBB> body, Collisions collisions) {
      return supportedBody(axisAligned(body), collisions);
   }

   static Result resolve(List<AxisAlignedBB> initial, Collisions collisions,
         double x, double y, double z, double stepHeight, boolean wheelSupport) {
      return resolveBody(axisAligned(initial), collisions, x, y, z, stepHeight, wheelSupport);
   }

   static Result resolveBody(List<MCH_CarCollisionBox> initial, Collisions collisions,
         double x, double y, double z, double stepHeight, boolean wheelSupport) {
      List<MCH_CarCollisionBox> normal = copy(initial);
      double dy = move(normal, collisions, 1, y);
      double dx = move(normal, collisions, 0, x);
      double dz = move(normal, collisions, 2, z);
      // Pose changes can put a component through a tread before the translation
      // starts. An overlapping horizontal candidate is blocked, not free progress.
      if((x != 0 || z != 0) && !clear(normal, collisions)) { dx = 0; dz = 0; }
      Result result = new Result(dx, dy, dz, y < 0 && dy != y, false);
      if(stepHeight <= 0 || (dx == x && dz == z)) return result;
      // Support must exist now, or have been reached by this downward movement.
      // onGround, suspension flags and terrain within stepHeight are not evidence.
      boolean support = wheelSupport || supportedBody(initial, collisions) || (y < 0 && dy != y);
      if(!support) return result;

      List<MCH_CarCollisionBox> step = copy(initial);
      double rise = move(step, collisions, 1, stepHeight);
      double sx = move(step, collisions, 0, x);
      double sz = move(step, collisions, 2, z);
      // Settle by the actual permitted rise, not by the configured maximum.
      double down = move(step, collisions, 1, -rise);
      double netY = rise + down;
      if(rise > 0 && netY >= 0 && netY <= stepHeight
            && sx * sx + sz * sz > dx * dx + dz * dz
            && supportedBody(step, collisions) && clear(step, collisions)) {
         return new Result(sx, netY, sz, true, true);
      }
      return result;
   }

   private static boolean supportedBody(List<MCH_CarCollisionBox> body, Collisions collisions) {
      return clip(body, collisions, 1, -CONTACT_EPSILON) > -CONTACT_EPSILON;
   }

   private static boolean clear(List<MCH_CarCollisionBox> body, Collisions collisions) {
      for(MCH_CarCollisionBox component : body) {
         for(AxisAlignedBB obstacle : collisions.query(component.bounds)) {
            if(component.intersects(obstacle)) return false;
         }
      }
      return true;
   }

   private static List<MCH_CarCollisionBox> copy(List<MCH_CarCollisionBox> body) {
      List<MCH_CarCollisionBox> result = new ArrayList<MCH_CarCollisionBox>(body.size());
      for(MCH_CarCollisionBox component : body) result.add(component.copy());
      return result;
   }

   private static List<MCH_CarCollisionBox> axisAligned(List<AxisAlignedBB> body) {
      List<MCH_CarCollisionBox> result = new ArrayList<MCH_CarCollisionBox>(body.size());
      for(AxisAlignedBB component : body) result.add(new MCH_CarCollisionBox(component));
      return result;
   }

   private static double clip(List<MCH_CarCollisionBox> body, Collisions collisions, int axis, double requested) {
      double allowed = requested;
      for(MCH_CarCollisionBox component : body) {
         AxisAlignedBB sweep = component.bounds.addCoord(axis == 0 ? requested : 0,
               axis == 1 ? requested : 0, axis == 2 ? requested : 0);
         for(AxisAlignedBB obstacle : collisions.query(sweep)) {
            allowed = component.clip(obstacle, axis, allowed);
         }
      }
      return allowed;
   }

   private static double move(List<MCH_CarCollisionBox> body, Collisions collisions, int axis, double requested) {
      double allowed = clip(body, collisions, axis, requested);
      for(MCH_CarCollisionBox component : body) {
         component.offset(axis == 0 ? allowed : 0, axis == 1 ? allowed : 0, axis == 2 ? allowed : 0);
      }
      return allowed;
   }
}
