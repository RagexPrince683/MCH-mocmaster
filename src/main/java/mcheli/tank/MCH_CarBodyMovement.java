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

   static final class Trace {
      final StringBuilder paths = new StringBuilder();
      String selected = "normal_none", stepGate = "not_checked";
      String cleanupX = "none", cleanupZ = "none", pose = "not_checked";
      double inputX, inputZ, stepHeight, baseY, verticalY;
      float desiredYaw, targetPitch, targetRoll, poseFraction;
      boolean wheelSupport, bodySupport;
      void path(String stage, double x, double y, double z, double rise, boolean landed, boolean clear) {
         if(paths.length() != 0) paths.append(';');
         paths.append(stage).append(':').append(x).append('|').append(y).append('|').append(z)
               .append('|').append(rise).append('|').append(landed).append('|').append(clear);
      }
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
      return resolveBody(initial, collisions, x, y, z, stepHeight, wheelSupport, null);
   }

   static Result resolveBody(List<MCH_CarCollisionBox> initial, Collisions collisions,
         double x, double y, double z, double stepHeight, boolean wheelSupport, Trace trace) {
      if(trace != null) {
         trace.stepHeight = stepHeight;
         trace.wheelSupport = wheelSupport;
         trace.bodySupport = supportedBody(initial, collisions);
      }
      // Recovery is a separate bounded outward sweep. An overlapping start cannot
      // claim a landing from a rejected inward request or retain rejected velocity.
      if(!clear(initial, collisions)) {
         if(trace != null) {
            trace.selected = "embedded";
            trace.stepGate = "embedded";
            trace.cleanupX = x != 0 ? "embedded" : "none";
            trace.cleanupZ = z != 0 ? "embedded" : "none";
            trace.path("embedded", 0, 0, 0, 0, false, false);
         }
         return new Result(0, 0, 0, false, false, x != 0, z != 0);
      }
      List<MCH_CarCollisionBox> vertical = copy(initial);
      double dy = move(vertical, collisions, 1, y);
      if(trace != null) trace.verticalY = dy;
      Horizontal normal = horizontal(vertical, collisions, x, z, trace, dy);
      double dx = normal.x, dz = normal.z;
      boolean landed = y < 0 && changed(y, dy);
      if(stepHeight <= 0 || (!changed(x, dx) && !changed(z, dz))) {
         if(trace != null) trace.stepGate = stepHeight <= 0 ? "no_step_height" : "unclipped";
         return result(normal.body, collisions, x, z, dx, dy, dz, landed, false, trace);
      }
      // Support must exist now, or have been reached by this downward movement.
      // onGround, suspension flags and terrain within stepHeight are not evidence.
      boolean support = wheelSupport || supportedBody(initial, collisions) || landed;
      if(!support) {
         if(trace != null) trace.stepGate = "unsupported";
         return result(normal.body, collisions, x, z, dx, dy, dz, landed, false, trace);
      }

      // Retain downward movement already resolved this tick. Starting again at the
      // higher initial pose can leave an otherwise clear retry above its landing.
      double baseY = Math.min(0, dy);
      if(trace != null) trace.baseY = baseY;
      List<MCH_CarCollisionBox> step = copy(dy < 0 ? vertical : initial);
      double rise = move(step, collisions, 1, stepHeight);
      if(trace != null) trace.path("rise", 0, baseY, 0, rise, false, clear(step, collisions));
      // Compare after landing: the furthest raised path may have no support while
      // another sweep order reaches a valid tread. Every path sweeps real volumes.
      if(trace != null) trace.stepGate = rise > 0 ? "no_valid_improvement" : "no_rise";
      Result best = result(normal.body, collisions, x, z, dx, dy, dz, landed, false, trace);
      if(rise > 0) {
         int order = 0;
         for(Horizontal candidate : horizontalCandidates(step, collisions, x, z)) {
            double down = move(candidate.body, collisions, 1, -rise);
            double netY = baseY + rise + down;
            if(trace != null) trace.path("step_" + order, candidate.x, netY, candidate.z, rise,
                  supportedBody(candidate.body, collisions), clear(candidate.body, collisions));
            if(netY >= baseY && netY <= stepHeight
                  && Math.hypot(candidate.x, candidate.z) > Math.hypot(best.x, best.z) + MCH_CarCollisionBox.EPSILON
                  && supportedBody(candidate.body, collisions) && clear(candidate.body, collisions)) {
               best = result(candidate.body, collisions, x, z, candidate.x, netY, candidate.z, true, true, trace);
               if(trace != null) {
                  trace.selected = "step_" + order;
                  trace.stepGate = "accepted_step";
               }
            }
            ++order;
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
         if(trace != null) trace.path("normal_" + order, candidate.x, y, candidate.z, 0, false, clear(candidate.body, collisions));
         if(clear(candidate.body, collisions)
               && Math.hypot(candidate.x, candidate.z) > Math.hypot(best.x, best.z)) {
            best = candidate;
            if(trace != null) trace.selected = "normal_" + order;
         }
         ++order;
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
         double requestedX, double requestedZ, double x, double y, double z, boolean grounded, boolean stepped, Trace trace) {
      boolean rejectedX = changed(requestedX, x) && Math.hypot(x, z) <= MCH_CarCollisionBox.EPSILON;
      boolean rejectedZ = changed(requestedZ, z) && Math.hypot(x, z) <= MCH_CarCollisionBox.EPSILON;
      boolean blockedX = rejectedX || blocked(body, collisions, 0, requestedX, x);
      boolean blockedZ = rejectedZ || blocked(body, collisions, 2, requestedZ, z);
      if(trace != null) {
         trace.cleanupX = rejectedX ? "no_progress" : blockedX ? "final_contact" : "none";
         trace.cleanupZ = rejectedZ ? "no_progress" : blockedZ ? "final_contact" : "none";
      }
      return new Result(x, y, z, grounded, stepped, blockedX, blockedZ);
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
