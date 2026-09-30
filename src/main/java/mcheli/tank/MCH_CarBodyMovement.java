package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.AxisAlignedBB;

/** Civilian compound-body sweeps. Every component keeps its real dimensions and coordinates. */
final class MCH_CarBodyMovement {
   private static final double CONTACT_EPSILON = 1.0E-5D;

   interface Collisions {
      List<AxisAlignedBB> query(AxisAlignedBB sweep);
      default String describe(AxisAlignedBB obstacle) { return "source unavailable"; }
   }

   interface StepRotation {
      /** Check the whole angular path at this offset; null means obstructed. */
      List<MCH_CarCollisionBox> at(double x, double y, double z, float fraction);
   }

   static final class Trace {
      final StringBuilder paths = new StringBuilder();
      final List<String> decisions = new ArrayList<String>();
      String selected = "normal_none", stepGate = "not_checked";
      String cleanupX = "none", cleanupZ = "none", pose = "not_checked";
      double inputX, inputZ, preMotionX, preMotionZ, stepHeight, baseY, verticalY;
      double poseRise;
      float desiredYaw, targetPitch, targetRoll, poseFraction;
      boolean wheelSupport, bodySupport;
      int rotationClearanceRejected;
      void path(String stage, double x, double y, double z, double rise, boolean landed, boolean clear) {
         if(paths.length() != 0) paths.append(';');
         paths.append(stage).append(':').append(x).append('|').append(y).append('|').append(z)
               .append('|').append(rise).append('|').append(landed).append('|').append(clear);
      }
      void candidate(String stage, double y, double rise, double progress, boolean support, boolean clear, String reason) {
         decisions.add("S " + stage + " rise=" + MCH_WheelDiagnostics.n(rise)
               + " Y=" + MCH_WheelDiagnostics.n(y) + " progress=" + MCH_WheelDiagnostics.n(progress));
         decisions.add("S support/clear=" + support + "/" + clear + " decision=" + reason);
      }
   }

   static final class Result {
      final double x, y, z;
      final boolean grounded, stepped, blockedX, blockedZ;
      final float rotationFraction;
      Result(double x, double y, double z, boolean grounded, boolean stepped, boolean blockedX, boolean blockedZ) {
         this(x, y, z, grounded, stepped, blockedX, blockedZ, 0);
      }
      Result(double x, double y, double z, boolean grounded, boolean stepped, boolean blockedX, boolean blockedZ,
            float rotationFraction) {
         this.x = x; this.y = y; this.z = z;
         this.grounded = grounded; this.stepped = stepped;
         this.blockedX = blockedX; this.blockedZ = blockedZ;
         this.rotationFraction = rotationFraction;
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
         double x, double y, double z, double stepHeight, boolean wheelSupport, Trace trace, StepRotation rotation) {
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
      if(stepHeight <= 0 || (rotation == null && !changed(x, dx) && !changed(z, dz))) {
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
         for(Horizontal candidate : horizontalCandidates(step, collisions, x, z, trace != null)) {
            if(trace != null && !candidate.blocker.isEmpty()) trace.decisions.add("S step_" + order + " clipped: " + candidate.blocker);
            double down = move(candidate.body, collisions, 1, -rise);
            double netY = baseY + rise + down;
            if(trace != null) trace.path("step_" + order, candidate.x, netY, candidate.z, rise,
                  supportedBody(candidate.body, collisions), clear(candidate.body, collisions));
            if(trace != null) trace.candidate("step_" + order, netY, rise, progress(candidate.x, candidate.z, x, z),
                  supportedBody(candidate.body, collisions), clear(candidate.body, collisions),
                  netY < baseY || netY > stepHeight ? "outside_vertical_budget"
                  : !supportedBody(candidate.body, collisions) ? "no_final_support"
                  : !clear(candidate.body, collisions) ? "final_overlap"
                  : progress(candidate.x, candidate.z, x, z) <= progress(best.x, best.z, x, z) + MCH_CarCollisionBox.EPSILON
                        ? "no_progress_improvement" : "eligible");
            if(netY >= baseY && netY <= stepHeight
                  && progress(candidate.x, candidate.z, x, z) > progress(best.x, best.z, x, z) + MCH_CarCollisionBox.EPSILON
                  && supportedBody(candidate.body, collisions) && clear(candidate.body, collisions)) {
               best = result(candidate.body, collisions, x, z, candidate.x, netY, candidate.z, true, true, trace);
               if(trace != null) {
                  trace.selected = "step_" + order;
                  trace.stepGate = "accepted_step";
               }
            }
            ++order;
         }
         if(rotation != null) {
            // Rotation can need the step's clearance, or the space beyond the
            // riser. Try both orders using this same swept lift, never a second
            // rise or a momentum-only continuation probe.
            for(float fraction = 1; fraction >= 0.015625F; fraction *= 0.5F) {
               for(int rotationOrder = 0; rotationOrder < 2; ++rotationOrder) {
                  List<MCH_CarCollisionBox> raised = rotationOrder == 0
                        ? rotation.at(0, baseY + rise, 0, fraction) : step;
                  if(raised == null) {
                     if(trace != null) ++trace.rotationClearanceRejected;
                     if(trace != null) trace.decisions.add("S rotate_first f=" + fraction + " rise="
                           + MCH_WheelDiagnostics.n(rise) + " progress/support=N/A clear=false: rejected before sweep");
                     continue;
                  }
                  order = 0;
                  for(Horizontal candidate : horizontalCandidates(raised, collisions, x, z, trace != null)) {
                     if(trace != null && !candidate.blocker.isEmpty()) trace.decisions.add("S rotate_"
                           + rotationOrder + "_" + order + " f=" + fraction + " clipped: " + candidate.blocker);
                     List<MCH_CarCollisionBox> rotated = rotationOrder == 0 ? candidate.body
                           : rotation.at(candidate.x, baseY + rise, candidate.z, fraction);
                     if(rotated == null) {
                        if(trace != null) ++trace.rotationClearanceRejected;
                        if(trace != null) trace.decisions.add("S rotate_last_" + order + " f=" + fraction
                              + " rise=" + MCH_WheelDiagnostics.n(rise) + " progress="
                              + MCH_WheelDiagnostics.n(Math.hypot(candidate.x, candidate.z))
                              + " support=N/A clear=false: rotation rejected before landing");
                        ++order;
                        continue;
                     }
                     // Rotation can free a lower landing even when the old pose
                     // blocked gravity. Consume only the original downward request.
                     double down = move(rotated, collisions, 1, -rise + Math.min(0, y - baseY));
                     double netY = baseY + rise + down;
                     double progress = progress(candidate.x, candidate.z, x, z);
                     double bestProgress = progress(best.x, best.z, x, z);
                     boolean landedRotation = supportedBody(rotated, collisions);
                     boolean clearRotation = clear(rotated, collisions);
                     String stage = rotationOrder == 0 ? "step_rotate_first_" : "step_rotate_last_";
                     if(trace != null) trace.path(stage + order + "_" + fraction, candidate.x, netY,
                           candidate.z, rise, landedRotation, clearRotation);
                     if(trace != null) trace.candidate(stage + order + "_" + fraction, netY, rise, progress,
                           landedRotation, clearRotation,
                           netY < Math.min(baseY, y) - MCH_CarCollisionBox.EPSILON || netY > stepHeight
                                 ? "outside_vertical_budget" : !landedRotation ? "no_final_support"
                           : !clearRotation ? "final_overlap"
                           : progress > bestProgress + MCH_CarCollisionBox.EPSILON
                                 || rotationImproves(best, progress, bestProgress, netY, fraction)
                                       ? "eligible" : "no_travel_or_pose_improvement");
                     // At meaningful equal travel, prefer a clear rotation with at
                     // most the pose solver's small lift. Otherwise the level step
                     // wins indefinitely and the pitch never catches the incline.
                     if(netY >= Math.min(baseY, y) - MCH_CarCollisionBox.EPSILON
                           && netY <= stepHeight && landedRotation && clearRotation
                           && (progress > bestProgress + MCH_CarCollisionBox.EPSILON
                                 || rotationImproves(best, progress, bestProgress, netY, fraction))) {
                        Result moved = result(rotated, collisions, x, z, candidate.x, netY, candidate.z,
                              true, true, trace);
                        best = new Result(moved.x, moved.y, moved.z, moved.grounded, moved.stepped,
                              moved.blockedX, moved.blockedZ, fraction);
                        if(trace != null) {
                           trace.selected = stage + order + "_" + fraction;
                           trace.stepGate = "accepted_step_rotation";
                        }
                     }
                     ++order;
                  }
               }
            }
         }
      }
      return best;
   }

   private static final class Horizontal {
      final List<MCH_CarCollisionBox> body;
      final double x, z;
      String blocker = "";
      Horizontal(List<MCH_CarCollisionBox> body, double x, double z) {
         this.body = body; this.x = x; this.z = z;
      }
   }

   private static boolean rotationImproves(Result best, double progress, double bestProgress,
         double y, float fraction) {
      return progress > MCH_CarCollisionBox.EPSILON
            && progress >= bestProgress - (best.rotationFraction == 0 ? 0.02D : 0.0D)
            && y <= best.y + 0.1D
            && (best.rotationFraction < fraction || progress >= bestProgress
                  && y < best.y - MCH_CarCollisionBox.EPSILON);
   }

   /** Travel in the requested direction, with equal cost for unwanted sideways motion. */
   private static double progress(double x, double z, double requestedX, double requestedZ) {
      double length = Math.hypot(requestedX, requestedZ);
      return length <= MCH_CarCollisionBox.EPSILON ? 0
            : (x * requestedX + z * requestedZ - Math.abs(x * requestedZ - z * requestedX)) / length;
   }

   private static Horizontal horizontal(List<MCH_CarCollisionBox> initial, Collisions collisions,
         double x, double z, Trace trace, double y) {
      Horizontal best = new Horizontal(copy(initial), 0, 0);
      int order = 0;
      for(Horizontal candidate : horizontalCandidates(initial, collisions, x, z, trace != null)) {
         if(trace != null && !candidate.blocker.isEmpty()) trace.decisions.add("S normal_" + order + " clipped: " + candidate.blocker);
         if(trace != null) trace.path("normal_" + order, candidate.x, y, candidate.z, 0, false, clear(candidate.body, collisions));
         if(clear(candidate.body, collisions)
               && progress(candidate.x, candidate.z, x, z) > progress(best.x, best.z, x, z)) {
            best = candidate;
            if(trace != null) trace.selected = "normal_" + order;
         }
         ++order;
      }
      return best;
   }

   private static List<Horizontal> horizontalCandidates(List<MCH_CarCollisionBox> initial,
         Collisions collisions, double x, double z, boolean diagnostics) {
      List<Horizontal> candidates = new ArrayList<Horizontal>(3);
      List<MCH_CarCollisionBox> xz = copy(initial);
      StringBuilder blockers = diagnostics ? new StringBuilder() : null;
      double dx = move(xz, collisions, 0, x, blockers), dz = move(xz, collisions, 2, z, blockers);
      Horizontal first = new Horizontal(xz, dx, dz);
      if(blockers != null) first.blocker = blockers.toString();
      candidates.add(first);
      if(!changed(x, dx) && !changed(z, dz) && clear(xz, collisions)) return candidates;
      if(x != 0 && z != 0) {
         List<MCH_CarCollisionBox> zx = copy(initial);
         if(blockers != null) blockers.setLength(0);
         double sz = move(zx, collisions, 2, z, blockers), sx = move(zx, collisions, 0, x, blockers);
         Horizontal second = new Horizontal(zx, sx, sz);
         if(blockers != null) second.blocker = blockers.toString();
         candidates.add(second);
         List<MCH_CarCollisionBox> diagonal = copy(initial);
         double fraction = 1;
         String diagonalBlocker = "";
         int componentIndex = 0;
         for(MCH_CarCollisionBox component : diagonal) {
            for(AxisAlignedBB obstacle : collisions.query(component.bounds.addCoord(x, 0, z))) {
               double clipped = component.clipFraction(obstacle, x, 0, z);
               if(diagnostics && clipped < fraction) diagonalBlocker = "component=" + componentIndex + " " + collisions.describe(obstacle);
               fraction = Math.min(fraction, clipped);
            }
            ++componentIndex;
         }
         for(MCH_CarCollisionBox component : diagonal) component.offset(x * fraction, 0, z * fraction);
         Horizontal third = new Horizontal(diagonal, x * fraction, z * fraction);
         third.blocker = diagonalBlocker;
         candidates.add(third);
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
      // A supported partial step has already made forward progress. Keep its
      // heading for the next collision sweep instead of killing one velocity axis.
      boolean movingStep = stepped && progress(x, z, requestedX, requestedZ) > MCH_CarCollisionBox.EPSILON;
      boolean blockedX = !movingStep && (rejectedX || blocked(body, collisions, 0, requestedX, x));
      boolean blockedZ = !movingStep && (rejectedZ || blocked(body, collisions, 2, requestedZ, z));
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

   /** Diagnostic only; uses the same obstacles and SAT overlap as clear(). */
   static String obstruction(List<MCH_CarCollisionBox> body, Collisions collisions) {
      for(int i = 0; i < body.size(); ++i) {
         MCH_CarCollisionBox component = body.get(i);
         for(AxisAlignedBB obstacle : collisions.query(component.bounds)) {
            if(component.intersects(obstacle)) return "component=" + i + " " + collisions.describe(obstacle);
         }
      }
      return "no overlap";
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
      return clip(body, collisions, axis, requested, null);
   }

   private static double clip(List<MCH_CarCollisionBox> body, Collisions collisions, int axis,
         double requested, StringBuilder blockers) {
      double allowed = requested;
      String blocker = "";
      int componentIndex = 0;
      for(MCH_CarCollisionBox component : body) {
         AxisAlignedBB sweep = component.bounds.addCoord(axis == 0 ? requested : 0,
               axis == 1 ? requested : 0, axis == 2 ? requested : 0);
         for(AxisAlignedBB obstacle : collisions.query(sweep)) {
            double clipped = component.clip(obstacle, axis, allowed);
            if(blockers != null && Math.abs(clipped) < Math.abs(allowed))
               blocker = "axis=" + axis + " component=" + componentIndex + " " + collisions.describe(obstacle);
            allowed = clipped;
         }
         ++componentIndex;
      }
      if(blockers != null && !blocker.isEmpty()) blockers.append(blocker).append(' ');
      return allowed;
   }

   static double move(List<MCH_CarCollisionBox> body, Collisions collisions, int axis, double requested) {
      return move(body, collisions, axis, requested, null);
   }

   private static double move(List<MCH_CarCollisionBox> body, Collisions collisions, int axis,
         double requested, StringBuilder blockers) {
      double allowed = clip(body, collisions, axis, requested, blockers);
      for(MCH_CarCollisionBox component : body) {
         component.offset(axis == 0 ? allowed : 0, axis == 1 ? allowed : 0, axis == 2 ? allowed : 0);
      }
      return allowed;
   }
}
