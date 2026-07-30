package mcheli.plane;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.entity.Entity;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Deterministic client/server-safe ballistic predictor for gravity bombs and
 * dispenser projectiles. The operation order intentionally mirrors
 * MCH_EntityBaseBullet followed by the relevant projectile subclass:
 *
 * speedDependsAircraft (once) -> speedFactor -> gravity -> collision sweep ->
 * position update -> subclass horizontal drag -> water damping.
 */
public final class MCP_PlaneCCIPHelper {

   public static final int MAX_STEPS = 600;
   private static final double BOMB_HORIZONTAL_DRAG = 0.999D;
   private static final double EPSILON = 1.0E-7D;
   private static final int FALLBACK_TERRAIN_SEARCH_RADIUS = 1;

   private MCP_PlaneCCIPHelper() {}

   public static Result predict(World world, MCH_WeaponInfo info, Vec3 releasePos, Vec3 initialVelocity) {
      return predict(world, info, releasePos, initialVelocity, null);
   }

   public static Result predict(World world, MCH_WeaponInfo info, Vec3 releasePos,
         Vec3 initialVelocity, Vec3 aircraftMotion) {
      Result result = new Result();
      result.valid = false;
      result.reasonInvalid = "not_run";
      result.releasePos = copy(releasePos);
      result.initialVelocity = copy(initialVelocity);
      result.aircraftMotion = copy(aircraftMotion);
      result.speedDependsAircraft = info != null && info.speedDependsAircraft;
      result.predictedAccelerationBeforeAircraft = getConstructorAcceleration(info);
      result.predictedAccelerationAfterAircraft = result.predictedAccelerationBeforeAircraft;
      result.speedAddedFromAircraft = 0.0D;
      result.speedDependsAircraftApplied = false;
      result.gravity = info != null ? (double)info.gravity : 0.0D;
      result.horizontalDrag = getInitialHorizontalDrag(info, result.predictedAccelerationBeforeAircraft);
      result.accelerationFactor = getAccelerationFactor(info);
      result.simulationTimeStep = result.accelerationFactor;

      if(world == null || info == null || releasePos == null || initialVelocity == null) {
         result.reasonInvalid = "missing_input";
         return result;
      }
      if(!isBombLike(info)) {
         result.reasonInvalid = "not_bomb_like";
         return result;
      }
      if(info.gravity >= 0.0F) {
         result.reasonInvalid = "non_ballistic_gravity";
         return result;
      }

      Vec3 position = copy(releasePos);
      Vec3 velocity = copy(initialVelocity);
      double entityAcceleration = getConstructorAcceleration(info);
      entityAcceleration = applySpeedDependsAircraftFirstTick(
            info, aircraftMotion, velocity, entityAcceleration, result);

      int maxSteps = info.timeFuse > 0 ? Math.min(MAX_STEPS, info.timeFuse) : MAX_STEPS;
      for(int stepIndex = 0; stepIndex < maxSteps; ++stepIndex) {
         int entityTick = stepIndex + 1;

         if(info.speedFactor != 0.0F
               && entityTick > info.speedFactorStartTick
               && entityTick < info.speedFactorEndTick) {
            double speed = length(velocity);
            if(speed > EPSILON) {
               double factor = (double)info.speedFactor / speed;
               velocity.xCoord += velocity.xCoord * factor;
               velocity.yCoord += velocity.yCoord * factor;
               velocity.zCoord += velocity.zCoord * factor;
               entityAcceleration += (double)info.speedFactor;
            }
         }

         // MCH_EntityBaseBullet applies gravity before its collision sweep.
         velocity.yCoord += (double)info.gravity;

         Vec3 next = Vec3.createVectorHelper(
               position.xCoord + velocity.xCoord * result.accelerationFactor,
               position.yCoord + velocity.yCoord * result.accelerationFactor,
               position.zCoord + velocity.zCoord * result.accelerationFactor);

         MovingObjectPosition hit = traceLikeBullet(world, position, next);
         result.ticksSimulated = entityTick;
         if(hit != null && hit.hitVec != null) {
            result.valid = true;
            result.impact = copy(hit.hitVec);
            result.finalVelocity = copy(velocity);
            result.impactDistance = releasePos.distanceTo(result.impact);
            result.releaseAltitude = releasePos.yCoord - result.impact.yCoord;
            result.reasonInvalid = "";
            result.hitRealTerrain = true;
            result.syntheticFallback = false;
            return result;
         }

         if(!isChunkLoadedForPrediction(world, next)) {
            return buildUnloadedChunkFallback(world, result, info, releasePos, position, next,
                  velocity, entityAcceleration, entityTick, maxSteps);
         }

         position = next;

         // MCH_EntityBomb applies this once, after super.onUpdate().
         if(isGravityBomb(info)) {
            velocity.xCoord *= BOMB_HORIZONTAL_DRAG;
            velocity.zCoord *= BOMB_HORIZONTAL_DRAG;
         } else if(isDispenser(info) && entityAcceleration < 1.0E-4D) {
            // MCH_EntityDispensedItem only damps X/Z at near-zero acceleration.
            velocity.xCoord *= BOMB_HORIZONTAL_DRAG;
            velocity.zCoord *= BOMB_HORIZONTAL_DRAG;
         }

         // Both projectile subclasses apply water damping after horizontal drag.
         if(isWaterAt(world, position)) {
            velocity.xCoord *= (double)info.velocityInWater;
            velocity.yCoord *= (double)info.velocityInWater;
            velocity.zCoord *= (double)info.velocityInWater;
         }

         if(position.yCoord < -64.0D) {
            result.reasonInvalid = "below_world";
            break;
         }
      }

      result.finalVelocity = copy(velocity);
      if("not_run".equals(result.reasonInvalid)) {
         return buildEstimatedImpactFallback(world, result, info, releasePos, position, result.initialVelocity,
               result.predictedAccelerationAfterAircraft, maxSteps, "no_collision_estimated_fallback");
      }
      if("below_world".equals(result.reasonInvalid)) {
         return buildEstimatedImpactFallback(world, result, info, releasePos, position, result.initialVelocity,
               result.predictedAccelerationAfterAircraft, maxSteps, "below_world_estimated_fallback");
      }
      return result;
   }

   private static Result buildEstimatedImpactFallback(World world, Result result, MCH_WeaponInfo info, Vec3 releasePos,
         Vec3 lastPosition, Vec3 lastVelocity, double entityAcceleration, int maxSteps, String reason) {
      Vec3 fallbackTarget = estimateBallisticGroundImpact(world, info, releasePos, lastPosition, lastVelocity,
            entityAcceleration, result.accelerationFactor, maxSteps);
      if(fallbackTarget == null) {
         result.reasonInvalid = reason;
         return result;
      }

      result.valid = true;
      result.unloadedChunkFallback = false;
      result.fallbackReason = reason;
      result.fallbackTargetY = fallbackTarget.yCoord;
      result.hitRealTerrain = false;
      result.syntheticFallback = true;
      result.impact = fallbackTarget;
      result.finalVelocity = copy(lastVelocity);
      result.impactDistance = releasePos.distanceTo(result.impact);
      result.releaseAltitude = releasePos.yCoord - result.impact.yCoord;
      result.reasonInvalid = reason;
      return result;
   }

   private static Vec3 estimateBallisticGroundImpact(World world, MCH_WeaponInfo info, Vec3 releasePos,
         Vec3 lastPosition, Vec3 lastVelocity, double entityAcceleration, double accelerationFactor, int maxSteps) {
      if(info == null || releasePos == null) {
         return null;
      }
      double targetY = findEstimatedFallbackY(world, releasePos, lastPosition);
      Vec3 position = copy(releasePos);
      Vec3 velocity = copy(lastVelocity);
      if(velocity == null) {
         return null;
      }
      Vec3 previous = copy(position);
      double simulatedAcceleration = entityAcceleration;
      for(int stepIndex = 0; stepIndex < maxSteps * 2; ++stepIndex) {
         int entityTick = stepIndex + 1;
         if(stepIndex > 0) {
            if(isGravityBomb(info)) {
               velocity.xCoord *= BOMB_HORIZONTAL_DRAG;
               velocity.zCoord *= BOMB_HORIZONTAL_DRAG;
            } else if(isDispenser(info) && simulatedAcceleration < 1.0E-4D) {
               velocity.xCoord *= BOMB_HORIZONTAL_DRAG;
               velocity.zCoord *= BOMB_HORIZONTAL_DRAG;
            }
         }
         if(info.speedFactor != 0.0F
               && entityTick > info.speedFactorStartTick
               && entityTick < info.speedFactorEndTick) {
            double speed = length(velocity);
            if(speed > EPSILON) {
               double factor = (double)info.speedFactor / speed;
               velocity.xCoord += velocity.xCoord * factor;
               velocity.yCoord += velocity.yCoord * factor;
               velocity.zCoord += velocity.zCoord * factor;
               simulatedAcceleration += (double)info.speedFactor;
            }
         }
         velocity.yCoord += (double)info.gravity;
         Vec3 next = Vec3.createVectorHelper(
               position.xCoord + velocity.xCoord * accelerationFactor,
               position.yCoord + velocity.yCoord * accelerationFactor,
               position.zCoord + velocity.zCoord * accelerationFactor);
         previous = position;
         if(next.yCoord <= targetY || next.yCoord < -64.0D) {
            return interpolateAtY(previous, next, Math.max(targetY, -64.0D));
         }
         position = next;
      }
      return position;
   }

   private static double findEstimatedFallbackY(World world, Vec3 releasePos, Vec3 lastPosition) {
      if(world != null && releasePos != null) {
         int x = MathHelper.floor_double(releasePos.xCoord);
         int z = MathHelper.floor_double(releasePos.zCoord);
         if(isChunkLoadedForPrediction(world, releasePos)) {
            return (double)Math.max(0, world.getHeightValue(x, z));
         }
      }
      if(world != null && lastPosition != null && isChunkLoadedForPrediction(world, lastPosition)) {
         return (double)Math.max(0, world.getHeightValue(
               MathHelper.floor_double(lastPosition.xCoord), MathHelper.floor_double(lastPosition.zCoord)));
      }
      return 0.0D;
   }

   private static Result buildUnloadedChunkFallback(World world, Result result, MCH_WeaponInfo info, Vec3 releasePos,
         Vec3 segmentStart, Vec3 firstUnloadedPosition, Vec3 velocity, double entityAcceleration,
         int entityTick, int maxSteps) {
      Vec3 fallbackStart = segmentStart;
      Vec3 fallbackEnd = firstUnloadedPosition;
      Vec3 fallbackVelocity = copy(velocity);
      int fallbackTick = entityTick;
      double fallbackTargetY = findLoadedTerrainFallbackY(world, segmentStart, firstUnloadedPosition);
      String fallbackReason = fallbackTargetY > 0.0D ? "nearby_loaded_terrain_height" : "unloaded_chunk_safe_y0";

      if(fallbackEnd != null && fallbackEnd.yCoord > fallbackTargetY) {
         Vec3 position = copy(fallbackEnd);
         Vec3 simulatedVelocity = copy(velocity);
         double simulatedAcceleration = entityAcceleration;

         for(int stepIndex = entityTick; stepIndex < maxSteps; ++stepIndex) {
            if(isGravityBomb(info)) {
               simulatedVelocity.xCoord *= BOMB_HORIZONTAL_DRAG;
               simulatedVelocity.zCoord *= BOMB_HORIZONTAL_DRAG;
            } else if(isDispenser(info) && simulatedAcceleration < 1.0E-4D) {
               simulatedVelocity.xCoord *= BOMB_HORIZONTAL_DRAG;
               simulatedVelocity.zCoord *= BOMB_HORIZONTAL_DRAG;
            }

            int nextEntityTick = stepIndex + 1;
            if(info.speedFactor != 0.0F
                  && nextEntityTick > info.speedFactorStartTick
                  && nextEntityTick < info.speedFactorEndTick) {
               double speed = length(simulatedVelocity);
               if(speed > EPSILON) {
                  double factor = (double)info.speedFactor / speed;
                  simulatedVelocity.xCoord += simulatedVelocity.xCoord * factor;
                  simulatedVelocity.yCoord += simulatedVelocity.yCoord * factor;
                  simulatedVelocity.zCoord += simulatedVelocity.zCoord * factor;
                  simulatedAcceleration += (double)info.speedFactor;
               }
            }

            simulatedVelocity.yCoord += (double)info.gravity;
            Vec3 simulatedNext = Vec3.createVectorHelper(
                  position.xCoord + simulatedVelocity.xCoord * result.accelerationFactor,
                  position.yCoord + simulatedVelocity.yCoord * result.accelerationFactor,
                  position.zCoord + simulatedVelocity.zCoord * result.accelerationFactor);

            fallbackStart = position;
            fallbackEnd = simulatedNext;
            fallbackVelocity = copy(simulatedVelocity);
            fallbackTick = nextEntityTick;
            if(simulatedNext.yCoord <= fallbackTargetY || simulatedNext.yCoord < -64.0D) {
               break;
            }
            position = simulatedNext;
         }
      }

      result.valid = true;
      result.unloadedChunkFallback = true;
      result.firstUnloadedPosition = copy(firstUnloadedPosition);
      if(firstUnloadedPosition != null) {
         result.firstUnloadedChunkX = MathHelper.floor_double(firstUnloadedPosition.xCoord) >> 4;
         result.firstUnloadedChunkZ = MathHelper.floor_double(firstUnloadedPosition.zCoord) >> 4;
      }
      result.fallbackReason = fallbackReason;
      result.fallbackTargetY = fallbackTargetY;
      result.hitRealTerrain = false;
      result.syntheticFallback = true;
      result.impact = fallbackEnd != null && fallbackEnd.yCoord <= fallbackTargetY
            ? interpolateAtY(fallbackStart, fallbackEnd, fallbackTargetY)
            : copy(fallbackEnd);
      result.finalVelocity = copy(fallbackVelocity);
      result.ticksSimulated = fallbackTick;
      result.impactDistance = releasePos.distanceTo(result.impact);
      result.releaseAltitude = releasePos.yCoord - result.impact.yCoord;
      result.reasonInvalid = "unloaded_chunk_fallback";
      return result;
   }


   private static double applySpeedDependsAircraftFirstTick(MCH_WeaponInfo info,
         Vec3 aircraftMotion, Vec3 velocity, double entityAcceleration, Result result) {
      if(info == null || aircraftMotion == null || velocity == null || result == null
            || !info.speedDependsAircraft) {
         return entityAcceleration;
      }

      double aircraftSpeed = length(aircraftMotion);
      double projectileSpeed = length(velocity);
      if(projectileSpeed <= EPSILON) {
         return entityAcceleration;
      }

      result.speedAddedFromAircraft = aircraftSpeed;
      result.predictedAccelerationBeforeAircraft = entityAcceleration;
      entityAcceleration += aircraftSpeed;
      result.predictedAccelerationAfterAircraft = entityAcceleration;

      double scale = entityAcceleration / projectileSpeed;
      velocity.xCoord *= scale;
      velocity.yCoord *= scale;
      velocity.zCoord *= scale;
      result.initialVelocity = copy(velocity);
      result.speedDependsAircraftApplied = true;
      return entityAcceleration;
   }

   /**
    * Mirrors MCH_EntityBaseBullet.onUpdateCollided's block trace. Breakable
    * blocks are treated as transparent without mutating the client world.
    */
   private static MovingObjectPosition traceLikeBullet(World world, Vec3 start, Vec3 end) {
      Vec3 traceStart = copy(start);
      double dx = end.xCoord - start.xCoord;
      double dy = end.yCoord - start.yCoord;
      double dz = end.zCoord - start.zCoord;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if(distance <= EPSILON) {
         return null;
      }

      double nx = dx / distance;
      double ny = dy / distance;
      double nz = dz / distance;

      for(int i = 0; i < 5; ++i) {
         MovingObjectPosition hit = W_WorldFunc.clip(world, traceStart, end);
         if(hit == null) {
            return null;
         }

         if(W_MovingObjectPosition.isHitTypeTile(hit)) {
            Block block = W_WorldFunc.getBlock(world, hit.blockX, hit.blockY, hit.blockZ);
            if(MCH_Config.bulletBreakableBlocks.contains(block) && hit.hitVec != null) {
               traceStart = Vec3.createVectorHelper(
                     hit.hitVec.xCoord + nx * 1.0E-4D,
                     hit.hitVec.yCoord + ny * 1.0E-4D,
                     hit.hitVec.zCoord + nz * 1.0E-4D);
               continue;
            }
         }
         return hit;
      }
      return null;
   }


   private static boolean isChunkLoadedForPrediction(World world, Vec3 position) {
      if(world == null || position == null) {
         return false;
      }
      int x = MathHelper.floor_double(position.xCoord);
      int z = MathHelper.floor_double(position.zCoord);
      // Use a stable in-world Y so this is a chunk availability check, not an altitude check.
      return world.blockExists(x, 64, z) || world.blockExists(x, 1, z);
   }

   private static double findLoadedTerrainFallbackY(World world, Vec3 segmentStart, Vec3 firstUnloadedPosition) {
      if(world == null || firstUnloadedPosition == null) {
         return 0.0D;
      }
      int firstChunkX = MathHelper.floor_double(firstUnloadedPosition.xCoord) >> 4;
      int firstChunkZ = MathHelper.floor_double(firstUnloadedPosition.zCoord) >> 4;
      int centerX = MathHelper.floor_double(segmentStart != null ? segmentStart.xCoord : firstUnloadedPosition.xCoord);
      int centerZ = MathHelper.floor_double(segmentStart != null ? segmentStart.zCoord : firstUnloadedPosition.zCoord);
      double totalHeight = 0.0D;
      int samples = 0;

      for(int dx = -FALLBACK_TERRAIN_SEARCH_RADIUS; dx <= FALLBACK_TERRAIN_SEARCH_RADIUS; ++dx) {
         for(int dz = -FALLBACK_TERRAIN_SEARCH_RADIUS; dz <= FALLBACK_TERRAIN_SEARCH_RADIUS; ++dz) {
            int sampleX = centerX + dx * 16;
            int sampleZ = centerZ + dz * 16;
            if((sampleX >> 4) == firstChunkX && (sampleZ >> 4) == firstChunkZ) {
               continue;
            }
            if(isChunkLoadedForPrediction(world, Vec3.createVectorHelper((double)sampleX, 64.0D, (double)sampleZ))) {
               totalHeight += (double)Math.max(0, world.getHeightValue(sampleX, sampleZ));
               ++samples;
            }
         }
      }
      return samples > 0 ? totalHeight / (double)samples : 0.0D;
   }

   private static Vec3 interpolateAtY(Vec3 start, Vec3 end, double targetY) {
      if(start == null || end == null) {
         return end != null ? copy(end) : null;
      }
      double dy = end.yCoord - start.yCoord;
      if(Math.abs(dy) <= EPSILON) {
         return copy(end);
      }
      double t = MathHelper.clamp_double((targetY - start.yCoord) / dy, 0.0D, 1.0D);
      return Vec3.createVectorHelper(
            start.xCoord + (end.xCoord - start.xCoord) * t,
            targetY,
            start.zCoord + (end.zCoord - start.zCoord) * t);
   }

   private static boolean isWaterAt(World world, Vec3 position) {
      if(world == null || position == null) {
         return false;
      }
      int x = MathHelper.floor_double(position.xCoord);
      int y = MathHelper.floor_double(position.yCoord);
      int z = MathHelper.floor_double(position.zCoord);
      Block block = world.getBlock(x, y, z);
      return block != null && block.getMaterial() == Material.water;
   }

   private static double getConstructorAcceleration(MCH_WeaponInfo info) {
      if(info == null) {
         return 0.0D;
      }
      double effectiveAcceleration = MCH_WeaponBase.getEffectiveLaunchAcceleration(info, (double)info.acceleration);
      return Math.min(3.9D, Math.max(0.0D, effectiveAcceleration));
   }

   private static double getInitialHorizontalDrag(MCH_WeaponInfo info, double entityAcceleration) {
      if(isGravityBomb(info)) {
         return BOMB_HORIZONTAL_DRAG;
      }
      if(isDispenser(info) && entityAcceleration < 1.0E-4D) {
         return BOMB_HORIZONTAL_DRAG;
      }
      return 1.0D;
   }

   private static double getAccelerationFactor(MCH_WeaponInfo info) {
      // MCH_EntityBaseBullet only raises accelerationFactor for MCH_EntityBullet
      // and MCH_EntityRocket. Bombs and dispensed items remain at 1.0.
      return 1.0D;
   }

   private static double length(Vec3 vector) {
      return vector == null ? 0.0D : Math.sqrt(
            vector.xCoord * vector.xCoord
                  + vector.yCoord * vector.yCoord
                  + vector.zCoord * vector.zCoord);
   }

   private static Vec3 copy(Vec3 vector) {
      return vector != null
            ? Vec3.createVectorHelper(vector.xCoord, vector.yCoord, vector.zCoord)
            : null;
   }


   public static Result predictCurrentWeapon(MCP_EntityPlane plane, Entity user) {
      if(plane == null || user == null) {
         Result result = new Result();
         result.valid = false;
         result.reasonInvalid = "missing_plane_or_user";
         return result;
      }
      MCH_WeaponSet ws = plane.getCurrentWeapon(user);
      MCH_WeaponBase weapon = ws != null ? ws.getCurrentWeapon() : null;
      if(!isBombWeapon(weapon)) {
         Result result = new Result();
         result.valid = false;
         result.reasonInvalid = "not_bomb_weapon";
         return result;
      }
      Vec3 aircraftMotion = Vec3.createVectorHelper(plane.motionX, plane.motionY, plane.motionZ);
      Vec3 shotOffset = weapon.getShotPos(plane);
      Vec3 release = Vec3.createVectorHelper(
            plane.posX + shotOffset.xCoord,
            plane.posY + shotOffset.yCoord,
            plane.posZ + shotOffset.zCoord);
      ReleaseKinematics k = getInitialBombVelocity(plane, ws, weapon, aircraftMotion);
      if("DISPENSER_EJECTED".equals(k.releaseMode)) {
         release.xCoord += k.initialVelocity.xCoord * 0.5D;
         release.yCoord += k.initialVelocity.yCoord * 0.5D;
         release.zCoord += k.initialVelocity.zCoord * 0.5D;
      }
      Result result = predict(plane.worldObj, weapon.getInfo(), release, k.initialVelocity, aircraftMotion);
      result.releaseMode = k.releaseMode;
      result.ejectionVelocity = k.ejectionVelocity;
      result.initialVelocityDeltaFromAircraft = k.initialVelocityDeltaFromAircraft;
      result.initialVelocityUpDot = k.initialVelocityUpDot;
      result.initialVelocitySideDot = k.initialVelocitySideDot;
      result.warningImpossibleLaunch = k.warningImpossibleLaunch;
      return result;
   }

   private static ReleaseKinematics getInitialBombVelocity(MCP_EntityPlane plane, MCH_WeaponSet ws,
         MCH_WeaponBase weapon, Vec3 aircraftMotion) {
      ReleaseKinematics k = new ReleaseKinematics();
      k.releaseMode = "GRAVITY_BOMB";
      k.ejectionVelocity = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      k.initialVelocity = Vec3.createVectorHelper(aircraftMotion.xCoord, aircraftMotion.yCoord, aircraftMotion.zCoord);
      if(weapon != null && weapon.getInfo() != null && weapon.getInfo().type != null
            && weapon.getInfo().type.equalsIgnoreCase("dispenser")) {
         k.releaseMode = "DISPENSER_EJECTED";
         float yaw = plane.rotationYaw + (ws != null ? ws.rotationYaw : 0.0F) + weapon.fixRotationYaw;
         float pitch = plane.rotationPitch + (ws != null ? ws.rotationPitch : 0.0F) + weapon.fixRotationPitch;
         float roll = plane.getRotRoll();
         Vec3 direction = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -yaw, -pitch, -roll);
         double length = direction.lengthVector();
         if(length > EPSILON) {
            double constructorSpeed = Math.min(3.9D, weapon.getEffectiveLaunchAcceleration());
            double ejectionScale = constructorSpeed * 0.5D / length;
            k.ejectionVelocity = Vec3.createVectorHelper(direction.xCoord * ejectionScale, direction.yCoord * ejectionScale, direction.zCoord * ejectionScale);
            k.initialVelocity.xCoord += k.ejectionVelocity.xCoord;
            k.initialVelocity.yCoord += k.ejectionVelocity.yCoord;
            k.initialVelocity.zCoord += k.ejectionVelocity.zCoord;
         }
      }
      k.initialVelocityDeltaFromAircraft = Vec3.createVectorHelper(k.initialVelocity.xCoord - aircraftMotion.xCoord, k.initialVelocity.yCoord - aircraftMotion.yCoord, k.initialVelocity.zCoord - aircraftMotion.zCoord);
      k.initialVelocityUpDot = k.initialVelocityDeltaFromAircraft.yCoord;
      Vec3 side = MCH_Lib.Rot2Vec3(plane.rotationYaw + 90.0F, 0.0F);
      k.initialVelocitySideDot = k.initialVelocityDeltaFromAircraft.xCoord * side.xCoord + k.initialVelocityDeltaFromAircraft.zCoord * side.zCoord;
      double delta = k.initialVelocityDeltaFromAircraft.lengthVector();
      boolean gravityBomb = "GRAVITY_BOMB".equals(k.releaseMode);
      k.warningImpossibleLaunch = (gravityBomb && (Math.abs(k.initialVelocitySideDot) > 0.05D || k.initialVelocityUpDot > 0.05D || delta > 0.10D)) || (!gravityBomb && delta > 4.0D);
      return k;
   }

   private static class ReleaseKinematics {
      Vec3 initialVelocity;
      Vec3 ejectionVelocity;
      Vec3 initialVelocityDeltaFromAircraft;
      double initialVelocityUpDot;
      double initialVelocitySideDot;
      boolean warningImpossibleLaunch;
      String releaseMode;
   }

   public static boolean isBombWeapon(MCH_WeaponBase weapon) {
      return weapon != null && isBombLike(weapon.getInfo());
   }

   public static boolean isBombLike(MCH_WeaponInfo info) {
      if(info == null || info.type == null) {
         return false;
      }
      return isGravityBomb(info) || isDispenser(info)
            || (info.gravity < 0.0F && info.acceleration <= 1.0F && info.explosion > 0);
   }

   public static boolean isGravityBomb(MCH_WeaponInfo info) {
      return info != null && info.type != null && info.type.equalsIgnoreCase("bomb");
   }

   public static boolean isDispenser(MCH_WeaponInfo info) {
      return info != null && info.type != null && info.type.equalsIgnoreCase("dispenser");
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
      public Vec3 aircraftMotion;
      public double gravity;
      public double horizontalDrag;
      public double accelerationFactor;
      public double simulationTimeStep;
      public boolean speedDependsAircraft;
      public boolean speedDependsAircraftApplied;
      public double speedAddedFromAircraft;
      public double predictedAccelerationBeforeAircraft;
      public double predictedAccelerationAfterAircraft;
      public boolean unloadedChunkFallback;
      public Vec3 firstUnloadedPosition;
      public int firstUnloadedChunkX;
      public int firstUnloadedChunkZ;
      public String fallbackReason;
      public double fallbackTargetY;
      public boolean hitRealTerrain;
      public boolean syntheticFallback;
      public boolean hysteresisReused;
      public int hysteresisAgeTicks;
      public Vec3 ejectionVelocity;
      public Vec3 initialVelocityDeltaFromAircraft;
      public double initialVelocityUpDot;
      public double initialVelocitySideDot;
      public boolean warningImpossibleLaunch;
      public String releaseMode;
      public String reasonInvalid;
   }
}
