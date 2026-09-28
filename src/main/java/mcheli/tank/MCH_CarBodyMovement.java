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

   interface StepPose {
      /** A clear angular sweep at the raised position, or null to retain the current pose. */
      List<MCH_CarCollisionBox> at(double lift);
   }

   static final class Trace {
      final StringBuilder paths = new StringBuilder();
      void path(String stage, double x, double y, double z, double rise, boolean landed, boolean clear) {
         if(paths.length() != 0) paths.append(';');
         paths.append(stage).append(':').append(x).append('|').append(y).append('|').append(z)
               .append('|').append(rise).append('|').append(landed).append('|').append(clear);
      }
   }

   static final class Result {
      final double x, y, z;
      final boolean grounded, stepped, rotated, blockedX, blockedZ;
      Result(double x, double y, double z, boolean grounded, boolean stepped, boolean rotated, boolean blockedX, boolean blockedZ) {
         this.x = x; this.y = y; this.z = z;
         this.grounded = grounded; this.stepped = stepped; this.rotated = rotated;
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
      return resolveBody(initial, collisions, x, y, z, stepHeight, wheelSupport, null);
   }

   static Result resolveBody(List<MCH_CarCollisionBox> initial, Collisions collisions,
         double x, double y, double z, double stepHeight, boolean wheelSupport, Trace trace) {
      return resolveBody(initial, collisions, x, y, z, stepHeight, wheelSupport, trace, null);
   }

   static Result resolveBody(List<MCH_CarCollisionBox> initial, Collisions collisions,
         double x, double y, double z, double stepHeight, boolean wheelSupport, Trace trace, StepPose stepPose) {
      // Recovery is a separate bounded outward sweep. An overlapping start cannot
      // claim a landing from a rejected inward request or retain rejected velocity.
      if(!clear(initial, collisions)) {
         if(trace != null) trace.path("embedded", 0, 0, 0, 0, false, false);
         return new Result(0, 0, 0, false, false, false, x != 0, z != 0);
      }
      List<MCH_CarCollisionBox> vertical = copy(initial);
      double dy = move(vertical, collisions, 1, y);
      Horizontal normal = horizontal(vertical, collisions, x, z, trace, dy);
      double dx = normal.x, dz = normal.z;
      boolean landed = y < 0 && changed(y, dy);
      if(stepHeight <= 0 || (!changed(x, dx) && !changed(z, dz))) {
         return result(normal.body, collisions, x, z, dx, dy, dz, landed, false);
      }
      // Support must exist now, or have been reached by this downward movement.
      // onGround, suspension flags and terrain within stepHeight are not evidence.
      boolean support = wheelSupport || supportedBody(initial, collisions) || landed;
      if(!support) return result(normal.body, collisions, x, z, dx, dy, dz, landed, false);

      // Retain downward movement already resolved this tick. Starting again at the
      // higher initial pose can leave an otherwise clear retry above its landing.
      double baseY = Math.min(0, dy);
      List<MCH_CarCollisionBox> step = copy(dy < 0 ? vertical : initial);
      double rise = move(step, collisions, 1, stepHeight);
      if(trace != null) trace.path("rise", 0, baseY, 0, rise, false, clear(step, collisions));
      // Compare after landing: the furthest raised path may have no support while
      // another sweep order reaches a valid tread. Every path sweeps real volumes.
      Result best = result(normal.body, collisions, x, z, dx, dy, dz, landed, false);
      if(rise > 0) {
         // A riser can prevent rotation at the original height. Use the same
         // permitted step lift to rotate before translation, never a second lift.
         List<MCH_CarCollisionBox> rotated = stepPose != null ? stepPose.at(baseY + rise) : null;
         for(int pose = 0; pose < (rotated != null ? 2 : 1); ++pose) {
            int order = 0;
            for(Horizontal candidate : horizontalCandidates(pose == 0 ? step : rotated, collisions, x, z)) {
               double down = move(candidate.body, collisions, 1, -rise);
               double netY = baseY + rise + down;
               double progress = Math.hypot(candidate.x, candidate.z);
               double bestProgress = Math.hypot(best.x, best.z);
               if(trace != null) trace.path((pose == 0 ? "step_" : "step_pose_") + order,
                     candidate.x, netY, candidate.z, rise, supportedBody(candidate.body, collisions), clear(candidate.body, collisions));
               if(netY >= baseY && netY <= stepHeight
                     && (progress > bestProgress + MCH_CarCollisionBox.EPSILON
                           || pose != 0 && best.stepped && !best.rotated
                                 && Math.abs(progress - bestProgress) <= MCH_CarCollisionBox.EPSILON)
                     && supportedBody(candidate.body, collisions) && clear(candidate.body, collisions)) {
                  best = result(candidate.body, collisions, x, z, candidate.x, netY, candidate.z, true, true,
                        pose != 0, stepHeight);
               }
               ++order;
            }
         }
      }
      return best;
   }

   private static final class Horizontal {
      final List<MCH_CarCollisionBox> body;
      final double x, z;
      Horizontal(List<MCH_CarCollisionBox> body, double x, double z) {
         this.body = body; this.x = x; this.z = z;
      }
   }

   private static Horizontal horizontal(List<MCH_CarCollisionBox> initial, Collisions collisions,
         double x, double z, Trace trace, double y) {
      Horizontal best = new Horizontal(copy(initial), 0, 0);
      int order = 0;
      for(Horizontal candidate : horizontalCandidates(initial, collisions, x, z)) {
         if(trace != null) trace.path("normal_" + order++, candidate.x, y, candidate.z, 0, false, clear(candidate.body, collisions));
         if(clear(candidate.body, collisions)
               && Math.hypot(candidate.x, candidate.z) > Math.hypot(best.x, best.z)) best = candidate;
      }
      return best;
   }

   private static List<Horizontal> horizontalCandidates(List<MCH_CarCollisionBox> initial,
         Collisions collisions, double x, double z) {
      List<Horizontal> candidates = new ArrayList<Horizontal>(3);
      List<MCH_CarCollisionBox> xz = copy(initial);
      double dx = move(xz, collisions, 0, x), dz = move(xz, collisions, 2, z);
      candidates.add(new Horizontal(xz, dx, dz));
      if(!changed(x, dx) && !changed(z, dz) && clear(xz, collisions)) return candidates;
      if(x != 0 && z != 0) {
         List<MCH_CarCollisionBox> zx = copy(initial);
         double sz = move(zx, collisions, 2, z), sx = move(zx, collisions, 0, x);
         candidates.add(new Horizontal(zx, sx, sz));
         List<MCH_CarCollisionBox> diagonal = copy(initial);
         double fraction = 1;
         for(MCH_CarCollisionBox component : diagonal) {
            for(AxisAlignedBB obstacle : collisions.query(component.bounds.addCoord(x, 0, z))) {
               fraction = Math.min(fraction, component.clipFraction(obstacle, x, 0, z));
            }
         }
         for(MCH_CarCollisionBox component : diagonal) component.offset(x * fraction, 0, z * fraction);
         candidates.add(new Horizontal(diagonal, x * fraction, z * fraction));
      }
      return candidates;
   }

   /** Bounded removal of existing overlap; never enters a new obstacle or crosses an inward face. */
   static double recoverUp(List<MCH_CarCollisionBox> body, Collisions collisions, double limit, double stepHeight) {
      double required = 0, allowed = limit;
      for(MCH_CarCollisionBox component : body) {
         for(AxisAlignedBB obstacle : collisions.query(component.bounds.addCoord(0, stepHeight, 0))) {
            if(component.intersects(obstacle)) {
               double escape = component.upwardEscape(obstacle);
               if(escape > stepHeight) return 0;
               required = Math.max(required, escape + MCH_CarCollisionBox.EPSILON);
            } else {
               allowed = component.clip(obstacle, 1, allowed);
            }
         }
      }
      return Math.min(required, allowed);
   }

   private static Result result(List<MCH_CarCollisionBox> body, Collisions collisions,
         double requestedX, double requestedZ, double x, double y, double z, boolean grounded, boolean stepped) {
      return result(body, collisions, requestedX, requestedZ, x, y, z, grounded, stepped, false, 0);
   }

   private static Result result(List<MCH_CarCollisionBox> body, Collisions collisions,
         double requestedX, double requestedZ, double x, double y, double z,
         boolean grounded, boolean stepped, boolean rotated, double stepHeight) {
      return new Result(x, y, z, grounded, stepped, rotated,
            changed(requestedX, x) && Math.hypot(x, z) <= MCH_CarCollisionBox.EPSILON
                  || blocked(body, collisions, 0, requestedX, x)
                        && (!stepped || !canContinueStep(body, collisions, 0, requestedX, stepHeight)),
            changed(requestedZ, z) && Math.hypot(x, z) <= MCH_CarCollisionBox.EPSILON
                  || blocked(body, collisions, 2, requestedZ, z)
                        && (!stepped || !canContinueStep(body, collisions, 2, requestedZ, stepHeight)));
   }

   /** Classify the next riser without moving the accepted body or granting another rise this tick. */
   private static boolean canContinueStep(List<MCH_CarCollisionBox> body, Collisions collisions,
         int axis, double requested, double stepHeight) {
      List<MCH_CarCollisionBox> next = copy(body);
      double rise = move(next, collisions, 1, stepHeight);
      double contact = Math.copySign(CONTACT_EPSILON, requested);
      if(rise <= 0 || changed(contact, move(next, collisions, axis, contact))) return false;
      double down = move(next, collisions, 1, -rise);
      return rise + down >= 0 && rise + down <= stepHeight
            && supportedBody(next, collisions) && clear(next, collisions);
   }

   private static boolean blocked(List<MCH_CarCollisionBox> body, Collisions collisions,
         int axis, double requested, double resolved) {
      if(!changed(requested, resolved)) return false;
      // A clipped raised sweep need not still be blocked after settling. Probe only
      // the accepted pose, in the requested direction, without advancing the body.
      double contact = Math.copySign(CONTACT_EPSILON, requested);
      return changed(contact, clip(body, collisions, axis, contact));
   }

   static boolean supportedBody(List<MCH_CarCollisionBox> body, Collisions collisions) {
      return changed(-CONTACT_EPSILON, clip(body, collisions, 1, -CONTACT_EPSILON));
   }

   static boolean clear(List<MCH_CarCollisionBox> body, Collisions collisions) {
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

   static double move(List<MCH_CarCollisionBox> body, Collisions collisions, int axis, double requested) {
      double allowed = clip(body, collisions, axis, requested);
      for(MCH_CarCollisionBox component : body) {
         component.offset(axis == 0 ? allowed : 0, axis == 1 ? allowed : 0, axis == 2 ? allowed : 0);
      }
      return allowed;
   }
}
