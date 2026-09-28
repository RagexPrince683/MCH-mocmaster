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
      final boolean grounded, stepped, blockedX, blockedZ;
      Result(double x, double y, double z, boolean grounded, boolean stepped, boolean blockedX, boolean blockedZ) {
         this.x = x; this.y = y; this.z = z;
         this.grounded = grounded; this.stepped = stepped;
         this.blockedX = blockedX; this.blockedZ = blockedZ;
      }
   }

   static boolean changed(double requested, double resolved) {
      return Math.abs(requested - resolved) > MCH_CarCollisionBox.EPSILON;
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
      List<MCH_CarCollisionBox> vertical = copy(initial);
      double dy = move(vertical, collisions, 1, y);
      List<MCH_CarCollisionBox> normal = copy(vertical);
      double dx = move(normal, collisions, 0, x);
      double dz = move(normal, collisions, 2, z);
      // Pose changes can put a component through a tread before the translation
      // starts. An overlapping horizontal candidate is blocked, not free progress.
      if((x != 0 || z != 0) && !clear(normal, collisions)) {
         dx = 0; dz = 0;
         normal = copy(vertical);
      }
      boolean landed = y < 0 && changed(y, dy);
      if(stepHeight <= 0 || (!changed(x, dx) && !changed(z, dz))) {
         return result(normal, collisions, x, z, dx, dy, dz, landed, false);
      }
      // Support must exist now, or have been reached by this downward movement.
      // onGround, suspension flags and terrain within stepHeight are not evidence.
      boolean support = wheelSupport || supportedBody(initial, collisions) || landed;
      if(!support) return result(normal, collisions, x, z, dx, dy, dz, landed, false);

      // Retain downward movement already resolved this tick. Starting again at the
      // higher initial pose can leave an otherwise clear retry above its landing.
      double baseY = Math.min(0, dy);
      List<MCH_CarCollisionBox> step = copy(dy < 0 ? vertical : initial);
      double rise = move(step, collisions, 1, stepHeight);
      double sx = move(step, collisions, 0, x);
      double sz = move(step, collisions, 2, z);
      // Settle by the actual permitted rise, not by the configured maximum.
      double down = move(step, collisions, 1, -rise);
      double netY = baseY + (rise + down);
      if(rise > 0 && netY >= baseY && netY <= stepHeight
            && Math.hypot(sx, sz) > Math.hypot(dx, dz) + MCH_CarCollisionBox.EPSILON
            && supportedBody(step, collisions) && clear(step, collisions)) {
         return result(step, collisions, x, z, sx, netY, sz, true, true);
      }
      return result(normal, collisions, x, z, dx, dy, dz, landed, false);
   }

   private static Result result(List<MCH_CarCollisionBox> body, Collisions collisions,
         double requestedX, double requestedZ, double x, double y, double z, boolean grounded, boolean stepped) {
      return new Result(x, y, z, grounded, stepped,
            blocked(body, collisions, 0, requestedX, x), blocked(body, collisions, 2, requestedZ, z));
   }

   private static boolean blocked(List<MCH_CarCollisionBox> body, Collisions collisions,
         int axis, double requested, double resolved) {
      if(!changed(requested, resolved)) return false;
      // A clipped raised sweep need not still be blocked after settling. Probe only
      // the accepted pose, in the requested direction, without advancing the body.
      double contact = Math.copySign(CONTACT_EPSILON, requested);
      return changed(contact, clip(body, collisions, axis, contact));
   }

   private static boolean supportedBody(List<MCH_CarCollisionBox> body, Collisions collisions) {
      return changed(-CONTACT_EPSILON, clip(body, collisions, 1, -CONTACT_EPSILON));
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
