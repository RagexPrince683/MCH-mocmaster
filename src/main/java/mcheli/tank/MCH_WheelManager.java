package mcheli.tank;

import java.util.List;
import java.util.Random;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.tank.MCH_EntityWheel;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WheelManager {

   //from my understanding, wheels are essentially invisible entities that track the position of the 'aircraft's'
   // (vehicle general category) wheels behaving somewhat like suspension.

   public final MCH_EntityBaseVehicle parent;
   public MCH_EntityWheel[] wheels;
   private double minZ;
   private double maxZ;
   private double avgZ;
   public Vec3 weightedCenter;
   public float targetPitch;
   public float targetRoll;
   public float prevYaw;
   private static Random rand = new Random();

   // per-wheel state (persist during runtime)
   //*unused helper
   //public double lastGroundY = Double.NEGATIVE_INFINITY; // last measured solid surface Y
   //public double groundYFiltered = Double.NEGATIVE_INFINITY; // low-pass filter
   //public int lastContactTick = 0; // tick when last seen on ground
   //public double restDistance = 0.0D; // nominal wheel offset (from wheel spec)

   private final java.util.Map<Integer, Double> wheelGroundFilter = new java.util.HashMap<Integer, Double>();




   public MCH_WheelManager(MCH_EntityBaseVehicle ac) {
      this.parent = ac;
      this.wheels = new MCH_EntityWheel[0];
      this.weightedCenter = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
   }

   /** Returns whether any wheel currently has collision-derived ground contact. */
   public boolean hasWheelContact() {
      for(int i = 0; i < this.wheels.length; ++i) {
         if(this.wheels[i] != null && this.wheels[i].onGround) {
            return true;
         }
      }
      return false;
   }

   /** One read-only snapshot; paired onGround flags never supply grip. */
   public CarContact getCarGroundContact(boolean diagnostics) {
      double centerZ = (this.minZ + this.maxZ) * 0.5D;
      int front = 0, rear = 0, raw = 0, paired = 0;
      for(MCH_EntityWheel wheel : this.wheels) {
         if(wheel == null || wheel.isDead || wheel.pos == null) continue;
         if(diagnostics) {
            if(wheel.hasGroundContact()) ++raw;
            if(wheel.onGround) ++paired;
         }
         if(wheel.hasCarGroundContact()) {
            if(wheel.pos.zCoord >= centerZ) ++front;
            else ++rear;
         }
      }
      // Missing/dead wheels cannot raise the available grip by shrinking the denominator.
      return new CarContact(this.wheels.length, front, rear, raw, paired);
   }

   public static final class CarContact {
      public final int total, front, rear, rawProbe, pairedFlags;
      CarContact(int total, int front, int rear, int rawProbe, int pairedFlags) {
         this.total = total; this.front = front; this.rear = rear;
         this.rawProbe = rawProbe; this.pairedFlags = pairedFlags;
      }
      public double fraction() { return this.total > 0 ? (double)(this.front + this.rear) / this.total : 0.0D; }
      public double response(MCH_TankInfo info) {
         int supported = this.front + this.rear;
         return supported > 0 ? (this.front * MCH_CarTireGrip.response(info.frontTireSize)
                 + this.rear * MCH_CarTireGrip.response(info.rearTireSize)) / supported : 1.0D;
      }
   }

   // fast top-surface query (returns top solid/liquid block Y)
   private double getGroundYAt(double wx, double wz) {
      int ix = MathHelper.floor_double(wx + 0.5D);
      int iz = MathHelper.floor_double(wz + 0.5D);
      int top = this.parent.worldObj.getTopSolidOrLiquidBlock(ix, iz);
      return (double)top;
   }


   public void createWheels(World w, List list, Vec3 weightedCenter) {
      this.wheels = new MCH_EntityWheel[list.size() * 2];
      this.minZ = 999999.0D;
      this.maxZ = -999999.0D;
      this.weightedCenter = weightedCenter;

      for(int i = 0; i < this.wheels.length; ++i) {
         MCH_EntityWheel wheel = new MCH_EntityWheel(w);
         wheel.setParents(this.parent);
         Vec3 wp = ((MCH_BaseVehicleInfo.Wheel)list.get(i / 2)).pos;
         wheel.setWheelPos(Vec3.createVectorHelper(i % 2 == 0?wp.xCoord:-wp.xCoord, wp.yCoord, wp.zCoord), this.weightedCenter);
         Vec3 v = this.parent.getTransformedPosition(wheel.pos.xCoord, wheel.pos.yCoord, wheel.pos.zCoord);
         wheel.setLocationAndAngles(v.xCoord, v.yCoord + 1.0D, v.zCoord, 0.0F, 0.0F);
         this.wheels[i] = wheel;
         if(wheel.pos.zCoord <= this.minZ) {
            this.minZ = wheel.pos.zCoord;
         }

         if(wheel.pos.zCoord >= this.maxZ) {
            this.maxZ = wheel.pos.zCoord;
         }
      }

      this.avgZ = this.maxZ - this.minZ;
   }


   //new working shit, also has a speedcap, just higher (2.20 instead of 1.80)

   public void move(double x, double y, double z) {
      MCH_EntityBaseVehicle ac = this.parent;
      if (ac.getAcInfo() == null) return;

      MCH_TankInfo tankInfo = ac instanceof MCH_EntityTank ? ((MCH_EntityTank)ac).getTankInfo() : null;
      if(tankInfo != null && tankInfo.civilianCarGrip) {
         this.moveCivilianSuspension(x, y, z, tankInfo);
         return;
      }

      boolean unevenContact = false;
      double frontAvgY = 0.0D;
      double rearAvgY  = 0.0D;
      int frontCount = 0;
      int rearCount  = 0;

      for (MCH_EntityWheel w : this.wheels) {
         if (w == null || !w.onGround) continue;

         if (w.pos.zCoord >= 0.0D) {
            frontAvgY += w.posY;
            frontCount++;
         } else {
            rearAvgY += w.posY;
            rearCount++;
         }
      }

      if (frontCount > 0 && rearCount > 0) {
         frontAvgY /= frontCount;
         rearAvgY  /= rearCount;

         // 1 block step ≈ 1.0, so this is conservative
         if (Math.abs(frontAvgY - rearAvgY) > 0.08D) {
            unevenContact = true;
         }
      }

      // store prev wheel positions & compute wheel desired motion
      for (int wi = 0; wi < this.wheels.length; ++wi) {
         MCH_EntityWheel w = this.wheels[wi];
         if (w == null) continue;
         w.prevPosX = w.posX;
         w.prevPosY = w.posY;
         w.prevPosZ = w.posZ;
         Vec3 worldPos = ac.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         w.motionX = worldPos.xCoord - w.posX + x;
         w.motionY = worldPos.yCoord - w.posY;
         w.motionZ = worldPos.zCoord - w.posZ + z;
      }

      // move wheels (soft vertical damping)
      for (MCH_EntityWheel w : this.wheels) {
         if (w == null) continue;
         w.motionY *= 0.15D;
         w.moveEntity(w.motionX, w.motionY, w.motionZ);
      }

      // preserve original pairing-onGround behavior
      int pairCount = this.wheels.length / 2;
      for (int i = 0; i < pairCount; ++i) {
         MCH_EntityWheel a = this.wheels[i * 2];
         MCH_EntityWheel b = this.wheels[i * 2 + 1];
         if (a == null || b == null) continue;
         if ((!a.isPlus && (a.onGround || b.onGround)) || (a.isPlus && (a.onGround || b.onGround))) {
            a.onGround = true;
            b.onGround = true;
         }
      }

      // horizontal speed and small-stop guard (prevent jitter when stopped)
      double horizSpeed = Math.sqrt(ac.motionX * ac.motionX + ac.motionZ * ac.motionZ);
      boolean allowBumpCheck = horizSpeed > 0.02D;

      // per-wheel ground sampling + low-pass filtering
      int groundCount = 0;
      int stableCount = 0;
      double minWheelY = Double.POSITIVE_INFINITY;
      double maxWheelY = Double.NEGATIVE_INFINITY;

      for (int wi = 0; wi < this.wheels.length; ++wi) {
         MCH_EntityWheel w = this.wheels[wi];
         if (w == null) continue;
         Vec3 worldPos = ac.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         double sampleGroundY = getGroundYAt(worldPos.xCoord, worldPos.zCoord);

         // initialize filter if needed
         Double prevFiltered = this.wheelGroundFilter.get(wi);
         if (prevFiltered == null) prevFiltered = sampleGroundY;

         // low-pass filter: alpha near 0.8-0.9 for inertia (strong smoothing)
         double alpha = 0.86D;
         double filtered = prevFiltered * alpha + sampleGroundY * (1.0D - alpha);
         this.wheelGroundFilter.put(wi, filtered);

         // contact: wheel considered on surface if within stepHeight + tolerance of ground
         double wheelY = worldPos.yCoord;
         double contactTolerance = 0.6D; // tolerate a bit of vertical difference
         boolean onSurface = (wheelY - sampleGroundY) <= (w.stepHeight + contactTolerance);
         if (onSurface) groundCount++;

         // stability test: raw change vs filtered difference
         double rawDelta = Math.abs(sampleGroundY - prevFiltered);
         double filtDiff = Math.abs(filtered - sampleGroundY);

         // thresholds scale with speed slightly, and use conservative defaults
         double rawThreshold = Math.max(0.35D, horizSpeed * 0.02D);
         double filtThreshold = 0.35D;

         boolean wheelStable = rawDelta <= rawThreshold && filtDiff <= filtThreshold;
         if (wheelStable) stableCount++;

         // track min/max wheel world Y for spread test
         minWheelY = Math.min(minWheelY, wheelY);
         maxWheelY = Math.max(maxWheelY, wheelY);
      }

      // bump detection uses spread + per-wheel stability; disable when nearly stopped
      boolean bumpDetected = false;
      if (allowBumpCheck) {
         if (maxWheelY - minWheelY > Math.max(0.18D, horizSpeed * 0.03D)) bumpDetected = true;
         // if many wheels unstable, treat as bump/rough
         if (stableCount < Math.max(1, this.wheels.length / 4)) bumpDetected = true;
      }

      // majority contact check
      boolean mostlyGrounded = groundCount >= Math.max(1, this.wheels.length / 2);

      // apply weighted-center influence only when airborne or legitimately bumped
      if ((!ac.onGround && MCH_Lib.getBlockIdY(ac, 1, -2) <= 0)
              || bumpDetected
              || unevenContact) {

         Vec3 position = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
         Vec3 position2 = ac.getTransformedPosition(this.weightedCenter);
         position2.xCoord -= ac.posX;
         position2.yCoord = this.weightedCenter.yCoord;
         position2.zCoord -= ac.posZ;

         for (int i = 0; i < pairCount; ++i) {
            MCH_EntityWheel wL = this.wheels[i * 2];
            MCH_EntityWheel wR = this.wheels[i * 2 + 1];
            if (wL == null || wR == null) continue;

            Vec3 ogrf = Vec3.createVectorHelper(wL.posX - (ac.posX + position2.xCoord),
                    wL.posY - (ac.posY + position2.yCoord),
                    wL.posZ - (ac.posZ + position2.zCoord));
            Vec3 iteratedValues = Vec3.createVectorHelper(wR.posX - (ac.posX + position2.xCoord),
                    wR.posY - (ac.posY + position2.yCoord),
                    wR.posZ - (ac.posZ + position2.zCoord));
            Vec3 iteratedValueCount = wL.pos.zCoord >= 0.0D ? iteratedValues.crossProduct(ogrf) : ogrf.crossProduct(iteratedValues);
            iteratedValueCount = iteratedValueCount.normalize();
            double iteratedValueIndex = Math.abs(wL.pos.zCoord / this.avgZ);
            if (!wL.onGround && !wR.onGround) {
               iteratedValueIndex = 0.0D;
            }

            position.xCoord += iteratedValueCount.xCoord * iteratedValueIndex;
            position.yCoord += iteratedValueCount.yCoord * iteratedValueIndex;
            position.zCoord += iteratedValueCount.zCoord * iteratedValueIndex;
         }

         // defensive normalize
         try {
            double len = position.lengthVector();
            if (len > 0.0001D) {
               position.xCoord /= len;
               position.yCoord /= len;
               position.zCoord /= len;
            }

            // scale torque strength
            double groundScale = unevenContact ? 0.65D : 1.0D;
            position.xCoord *= groundScale;
            position.yCoord *= groundScale;
            position.zCoord *= groundScale;
         } catch (Throwable t) {
            position = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);
         }

         // lateral nudge scaled by stability/speed (preserve a bit of previous behavior)
         if (position.yCoord > 0.01D && position.yCoord < 0.7D) {
            double speedScale = Math.max(0.12D, 1.0D - horizSpeed * 0.09D); // reduce at high speed
            double stabilityScale = (double)stableCount / (double)Math.max(1, this.wheels.length); // stable fraction
            double lateralScale = 1.0D * speedScale * stabilityScale;
            ac.motionX += position.xCoord / 50.0D * lateralScale;
            ac.motionZ += position.zCoord / 50.0D * lateralScale;
         }

         position.rotateAroundY((float)((double)ac.getRotYaw() * Math.PI / 180.0D));
         float candidatePitch = (float)(90.0D - Math.atan2(position.yCoord, position.zCoord) * 180.0D / Math.PI);
         float candidateRoll  = -((float)(90.0D - Math.atan2(position.yCoord, position.xCoord) * 180.0D / Math.PI));

         // clamp per-tick delta and absolute safe angle
         float maxDelta = ac.getAcInfo().onGroundPitchFactor;
         if (maxDelta <= 0.0001F) maxDelta = 2.5F;
         candidatePitch = MathHelper.clamp_float(candidatePitch, ac.getRotPitch() - maxDelta, ac.getRotPitch() + maxDelta);
         candidateRoll  = MathHelper.clamp_float(candidateRoll,  ac.getRotRoll()  - maxDelta, ac.getRotRoll()  + maxDelta);

         float maxAbs = 18.0F;
         candidatePitch = MathHelper.clamp_float(candidatePitch, -maxAbs, maxAbs);
         candidateRoll  = MathHelper.clamp_float(candidateRoll,  -maxAbs, maxAbs);

         // stability-based rotation influence (0..1)
         double speedInfluence = Math.max(0.09D, 1.0D - horizSpeed * 0.12D);
         double contactInfluence = (double)groundCount / (double)Math.max(1, this.wheels.length);
         double stabilityInfluence = (double)stableCount / (double)Math.max(1, this.wheels.length);
         double rotationInfluence = MathHelper.clamp_double(speedInfluence * contactInfluence * stabilityInfluence, 0.0D, 1.0D);

         // PENETRATION CHECK USING FILTERED GROUND (no nudges)
         double worstPen = 0.0D;
         for (int wi = 0; wi < this.wheels.length; ++wi) {
            MCH_EntityWheel w = this.wheels[wi];
            if (w == null) continue;
            Vec3 test = this.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord, ac, ac.getRotYaw(), candidatePitch, candidateRoll);
            double groundY = this.wheelGroundFilter.containsKey(wi) ? this.wheelGroundFilter.get(wi) : getGroundYAt(test.xCoord, test.zCoord);
            double pen = (groundY + 0.05D) - test.yCoord;
            if (pen > worstPen) worstPen = pen;
         }
         if (worstPen > 0.04D) {
            double penFactor = Math.max(0.04D, 1.0D - Math.min(1.0D, worstPen * 10.0D));
            rotationInfluence *= penFactor;
         }

         // if too small, decay target slightly; otherwise apply a smooth lerp toward candidate
         if (rotationInfluence < 0.06D) {
            // decay to prevent micro-wobble
            this.targetPitch *= 0.92F;
            this.targetRoll  *= 0.92F;
            if (Math.abs(this.targetPitch) < 0.25F) this.targetPitch = 0.0F;
            if (Math.abs(this.targetRoll)  < 0.25F) this.targetRoll  = 0.0F;
            if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
               ac.setRotPitch(this.targetPitch);
               ac.setRotRoll(this.targetRoll);
            }
         } else {
            float smoothing = 0.42F; // how aggressively we move toward candidate
            float apply = (float)(rotationInfluence * smoothing);
            this.targetPitch = this.targetPitch + (candidatePitch - this.targetPitch) * apply;
            this.targetRoll  = this.targetRoll  + (candidateRoll  - this.targetRoll)  * apply;

            // small additional damping at low speed to stop jitter
            if (horizSpeed < 0.05D) {
               this.targetPitch *= 0.94F;
               this.targetRoll  *= 0.94F;
            }

            if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
               ac.setRotPitch(this.targetPitch);
               ac.setRotRoll(this.targetRoll);
            }
         }
      } else {
         // Only level if all wheels are basically even
         if (!unevenContact) {
            float smoothFactor = 0.85F;
            this.targetPitch *= smoothFactor;
            this.targetRoll  *= smoothFactor;

            if (Math.abs(this.targetPitch) < 0.2F) this.targetPitch = 0.0F;
            if (Math.abs(this.targetRoll)  < 0.2F) this.targetRoll  = 0.0F;
         }

         if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
            ac.setRotPitch(this.targetPitch);
            ac.setRotRoll(this.targetRoll);
         }
      }

      // The body is moved after the wheels, so their target for this tick includes the
      // pending horizontal body displacement. Comparing against the body's current
      // position made any speed above rangeH look like a runaway wheel. The recovery
      // then lifted the wheel by half its step height, removing real flat-ground contact.
      for (int wi = 0; wi < this.wheels.length; ++wi) {
         MCH_EntityWheel w = this.wheels[wi];
         if (w == null) continue;
         Vec3 v = this.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord, ac, ac.getRotYaw(), this.targetPitch, this.targetRoll);
         double rangeH = 2.0D;
         double targetX = v.xCoord + x;
         double targetZ = v.zCoord + z;
         w.posX = MathHelper.clamp_double(w.posX, targetX - rangeH, targetX + rangeH);
         w.posZ = MathHelper.clamp_double(w.posZ, targetZ - rangeH, targetZ + rangeH);
         w.setPositionAndRotation(w.posX, w.posY, w.posZ, 0.0F, 0.0F);
      }
   }

   /** Civilian-car-only suspension. Collision and vertical force remain server authoritative. */
   private void moveCivilianSuspension(double x, double y, double z, MCH_TankInfo info) {
      MCH_EntityBaseVehicle car = this.parent;
      double travel = info.suspensionTravel;
      double front = 0.0D;
      double rear = 0.0D;
      double left = 0.0D;
      double right = 0.0D;
      int frontCount = 0;
      int rearCount = 0;
      int leftCount = 0;
      int rightCount = 0;
      int supported = 0;
      double totalResponse = 0.0D;

      for(int i = 0; i < this.wheels.length; ++i) {
         MCH_EntityWheel wheel = this.wheels[i];
         if(wheel == null || wheel.pos == null) {
            continue;
         }

         wheel.prevPosX = wheel.posX;
         wheel.prevPosY = wheel.posY;
         wheel.prevPosZ = wheel.posZ;
         wheel.prevSuspensionCompression = wheel.suspensionCompression;

         Vec3 anchor = this.getTransformedPosition(wheel.pos.xCoord, wheel.pos.yCoord, wheel.pos.zCoord,
               car, car.getRotYaw(), this.targetPitch, this.targetRoll);
         double measured = wheel.measureSuspensionCompression(anchor, travel);
         float compression = (float)measured;
         if(wheel.suspensionSupported && !wheel.suspensionCompressionInitialized) {
            wheel.prevSuspensionCompression = compression;
            wheel.suspensionCompressionRate = 0.0F;
            wheel.suspensionCompressionInitialized = true;
         } else {
            float rawRate = compression - wheel.prevSuspensionCompression;
            wheel.suspensionCompressionRate = wheel.suspensionCompressionRate * 0.35F + rawRate * 0.65F;
         }
         wheel.suspensionCompression = compression;

         double wheelY = anchor.yCoord - travel + measured;
         wheel.setPosition(anchor.xCoord, wheelY, anchor.zCoord);

         if(!wheel.suspensionSupported) {
            continue;
         }

         ++supported;
         double normalized = measured / travel;
         double damping = wheel.suspensionCompressionRate >= 0.0F
               ? info.suspensionCompressionDamping : info.suspensionReboundDamping;
         double response = normalized * info.suspensionSpring + wheel.suspensionCompressionRate * damping;
         totalResponse += MathHelper.clamp_double(response, -0.08D, 0.08D);

         if(wheel.pos.zCoord >= this.weightedCenter.zCoord) {
            front += wheelY;
            ++frontCount;
         } else {
            rear += wheelY;
            ++rearCount;
         }
         if(wheel.pos.xCoord >= 0.0D) {
            right += wheelY;
            ++rightCount;
         } else {
            left += wheelY;
            ++leftCount;
         }
      }

      this.updateRenderNeutral(supported);

      if(supported > 0) {
         double horizontalMovement = Math.sqrt(x * x + z * z);
         boolean stationaryOnBodyContact = car.onGround && horizontalMovement < 1.0E-4D;
         if(!car.worldObj.isRemote && !stationaryOnBodyContact) {
            double springAcceleration = totalResponse / this.wheels.length;
            car.motionY += MathHelper.clamp_double(springAcceleration, -0.06D, 0.06D);
         }

         // A missing axle or side has no height. Never substitute world Y zero: that
         // turns a transient loss of support into a permanent nose/rear/side-down pose.
         float pitch = 0.0F;
         if(frontCount > 0 && rearCount > 0) {
            double frontHeight = front / frontCount;
            double rearHeight = rear / rearCount;
            pitch = (float)Math.toDegrees(Math.atan2(frontHeight - rearHeight,
                  Math.max(0.5D, this.maxZ - this.minZ)));
         }
         float roll = 0.0F;
         if(leftCount > 0 && rightCount > 0) {
            double leftHeight = left / leftCount;
            double rightHeight = right / rightCount;
            roll = (float)-Math.toDegrees(Math.atan2(rightHeight - leftHeight, this.getTrackWidth()));
         }
         pitch = MathHelper.clamp_float(pitch, -18.0F, 18.0F);
         roll = MathHelper.clamp_float(roll, -18.0F, 18.0F);
         float smoothing = car.worldObj.isRemote ? 0.28F : 0.45F;
         this.targetPitch += (pitch - this.targetPitch) * smoothing;
         this.targetRoll += (roll - this.targetRoll) * smoothing;
         if(Math.abs(pitch) < 0.01F && Math.abs(this.targetPitch) < 0.02F) {
            this.targetPitch = 0.0F;
         }
         if(Math.abs(roll) < 0.01F && Math.abs(this.targetRoll) < 0.02F) {
            this.targetRoll = 0.0F;
         }
      } else {
         this.targetPitch *= 0.94F;
         this.targetRoll *= 0.94F;
      }

      // This method runs every client tick, including while the local driver is stopped.
      // Applying the predicted pose here prevents the last rendered terrain roll from
      // waiting indefinitely for another steering/input angle update.
      car.setRotPitch(this.targetPitch);
      car.setRotRoll(this.targetRoll);
   }

   /**
    * Establishes rendered neutral travel only after the complete axle set is supported
    * at an even height. A single tire touching during spawn or landing must not become
    * the permanent model-space baseline for that tire.
    */
   private void updateRenderNeutral(int supported) {
      if(supported != this.wheels.length || supported == 0) {
         return;
      }

      float minimum = Float.POSITIVE_INFINITY;
      float maximum = Float.NEGATIVE_INFINITY;
      for(MCH_EntityWheel wheel : this.wheels) {
         minimum = Math.min(minimum, wheel.suspensionCompression);
         maximum = Math.max(maximum, wheel.suspensionCompression);
      }
      if(maximum - minimum > 0.02F) {
         return;
      }

      for(MCH_EntityWheel wheel : this.wheels) {
         if(Float.isNaN(wheel.suspensionRestCompression)) {
            wheel.suspensionRestCompression = wheel.suspensionCompression;
         } else {
            wheel.suspensionRestCompression +=
                  (wheel.suspensionCompression - wheel.suspensionRestCompression) * 0.2F;
         }
      }
   }

   private double getTrackWidth() {
      double minX = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      for(MCH_EntityWheel wheel : this.wheels) {
         if(wheel != null && wheel.pos != null) {
            minX = Math.min(minX, wheel.pos.xCoord);
            maxX = Math.max(maxX, wheel.pos.xCoord);
         }
      }
      return minX <= maxX ? Math.max(0.5D, maxX - minX) : 1.0D;
   }

   public float getRenderWheelTravel(double x, double z, float tickTime) {
      MCH_EntityWheel closest = null;
      double closestDistance = Double.POSITIVE_INFINITY;
      for(MCH_EntityWheel wheel : this.wheels) {
         if(wheel == null || wheel.pos == null) {
            continue;
         }
         double dx = wheel.pos.xCoord - x;
         double dz = wheel.pos.zCoord - z;
         double distance = dx * dx + dz * dz;
         if(distance < closestDistance) {
            closest = wheel;
            closestDistance = distance;
         }
      }
      // Model packs are free to use decorative or differently arranged wheels.
      if(closest == null || closestDistance > 0.85D * 0.85D) {
         return 0.0F;
      }
      float compression = closest.prevSuspensionCompression
            + (closest.suspensionCompression - closest.prevSuspensionCompression) * tickTime;
      return Float.isNaN(closest.suspensionRestCompression)
            ? 0.0F : compression - closest.suspensionRestCompression;
   }







   public Vec3 getTransformedPosition(double x, double y, double z, MCH_EntityBaseVehicle ac, float yaw, float pitch, float roll) {
      Vec3 v = MCH_Lib.RotVec3(x, y, z, -yaw, -pitch, -roll);
      return v.addVector(ac.posX, ac.posY, ac.posZ);
   }

   public void updateBlock() {
      MCH_Config configuration = MCH_MOD.config;
      this.trampleGrassUnderWheels();
      if(MCH_Config.Collision_DestroyBlock.prmBool) {
         MCH_EntityBaseVehicle ac = this.parent;
         MCH_EntityWheel[] iteratedValues = this.wheels;
         int iteratedValueCount = iteratedValues.length;

         for(int iteratedValueIndex = 0; iteratedValueIndex < iteratedValueCount; ++iteratedValueIndex) {
            MCH_EntityWheel w = iteratedValues[iteratedValueIndex];
            Vec3 v = ac.getTransformedPosition(w.pos);
            int x = (int)(v.xCoord + 0.5D);
            int y = (int)(v.yCoord + 0.5D);
            int z = (int)(v.zCoord + 0.5D);
            Block block = ac.worldObj.getBlock(x, y, z);
            if(block == W_Block.getSnowLayer()) {
               ac.worldObj.setBlockToAir(x, y, z);
            }

            if(block == Blocks.waterlily || block == Blocks.cake) {
               W_WorldFunc.destroyBlock(ac.worldObj, x, y, z, false);
            }
         }

      }
   }

   private void trampleGrassUnderWheels() {
      MCH_EntityBaseVehicle ac = this.parent;
      if(ac == null || ac.worldObj == null || ac.worldObj.isRemote || this.wheels == null || this.wheels.length <= 0) {
         return;
      }

      double horizontalMotionSq = ac.motionX * ac.motionX + ac.motionZ * ac.motionZ;
      if(horizontalMotionSq < 1.0E-4D && Math.abs(this.prevYaw - ac.getRotYaw()) < 0.05F) {
         return;
      }

      for(int i = 0; i < this.wheels.length; ++i) {
         MCH_EntityWheel w = this.wheels[i];
         if(w == null || w.pos == null) {
            continue;
         }

         Vec3 v = ac.getTransformedPosition(w.pos);
         int x = MathHelper.floor_double(v.xCoord + 0.5D);
         int y = MathHelper.floor_double(v.yCoord - 0.5D);
         int z = MathHelper.floor_double(v.zCoord + 0.5D);
         Block block = ac.worldObj.getBlock(x, y, z);
         if(Block.isEqualTo(block, Blocks.air)) {
            --y;
            block = ac.worldObj.getBlock(x, y, z);
         }

         if(Block.isEqualTo(block, Blocks.grass)) {
            ac.worldObj.setBlock(x, y, z, Blocks.dirt, 0, 3);
         }
      }
   }

   public void particleLandingGear() {
      if(this.wheels.length > 0) {
         MCH_EntityBaseVehicle ac = this.parent;
         double d = ac.motionX * ac.motionX + ac.motionZ * ac.motionZ + (double)Math.abs(this.prevYaw - ac.getRotYaw());
         this.prevYaw = ac.getRotYaw();
         if(d > 0.001D) {
            for(int i = 0; i < 2; ++i) {
               MCH_EntityWheel w = this.wheels[rand.nextInt(this.wheels.length)];
               Vec3 v = ac.getTransformedPosition(w.pos);
               int x = MathHelper.floor_double(v.xCoord + 0.5D);
               int y = MathHelper.floor_double(v.yCoord - 0.5D);
               int z = MathHelper.floor_double(v.zCoord + 0.5D);
               Block block = ac.worldObj.getBlock(x, y, z);
               if(Block.isEqualTo(block, Blocks.air)) {
                  y = MathHelper.floor_double(v.yCoord + 0.5D);
                  block = ac.worldObj.getBlock(x, y, z);
               }

               if(!Block.isEqualTo(block, Blocks.air)) {
                  MCH_ParticlesUtil.spawnParticleTileCrack(ac.worldObj, x, y, z, v.xCoord + ((double)rand.nextFloat() - 0.5D), v.yCoord + 0.1D, v.zCoord + ((double)rand.nextFloat() - 0.5D), -ac.motionX * 4.0D + ((double)rand.nextFloat() - 0.5D) * 0.1D, (double)rand.nextFloat() * 0.5D, -ac.motionZ * 4.0D + ((double)rand.nextFloat() - 0.5D) * 0.1D);
               }
            }
         }

      }
   }

}
