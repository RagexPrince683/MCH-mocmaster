package mcheli.tank;

import java.util.List;
import java.util.Random;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
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

   public final MCH_EntityAircraft parent;
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
   //*unused GPT schizophrenia
   //public double lastGroundY = Double.NEGATIVE_INFINITY; // last measured solid surface Y
   //public double groundYFiltered = Double.NEGATIVE_INFINITY; // low-pass filter
   //public int lastContactTick = 0; // tick when last seen on ground
   //public double restDistance = 0.0D; // nominal wheel offset (from wheel spec)

   private final java.util.Map<Integer, Double> wheelGroundFilter = new java.util.HashMap<Integer, Double>();




   public MCH_WheelManager(MCH_EntityAircraft ac) {
      this.parent = ac;
      this.wheels = new MCH_EntityWheel[0];
      this.weightedCenter = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
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
         Vec3 wp = ((MCH_AircraftInfo.Wheel)list.get(i / 2)).pos;
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

   public void move(double x, double y, double z) {
      MCH_EntityAircraft ac = this.parent;
      if (ac.getAcInfo() == null) return;

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
      if ((!ac.onGround && MCH_Lib.getBlockIdY(ac, 1, -2) <= 0) || bumpDetected) {

         Vec3 var29 = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
         Vec3 var31 = ac.getTransformedPosition(this.weightedCenter);
         var31.xCoord -= ac.posX;
         var31.yCoord = this.weightedCenter.yCoord;
         var31.zCoord -= ac.posZ;

         for (int i = 0; i < pairCount; ++i) {
            MCH_EntityWheel wL = this.wheels[i * 2];
            MCH_EntityWheel wR = this.wheels[i * 2 + 1];
            if (wL == null || wR == null) continue;

            Vec3 ogrf = Vec3.createVectorHelper(wL.posX - (ac.posX + var31.xCoord),
                    wL.posY - (ac.posY + var31.yCoord),
                    wL.posZ - (ac.posZ + var31.zCoord));
            Vec3 arr$ = Vec3.createVectorHelper(wR.posX - (ac.posX + var31.xCoord),
                    wR.posY - (ac.posY + var31.yCoord),
                    wR.posZ - (ac.posZ + var31.zCoord));
            Vec3 len$ = wL.pos.zCoord >= 0.0D ? arr$.crossProduct(ogrf) : ogrf.crossProduct(arr$);
            len$ = len$.normalize();
            double i$ = Math.abs(wL.pos.zCoord / this.avgZ);
            if (!wL.onGround && !wR.onGround) {
               i$ = 0.0D;
            }

            var29.xCoord += len$.xCoord * i$;
            var29.yCoord += len$.yCoord * i$;
            var29.zCoord += len$.zCoord * i$;
         }

         // defensive normalize
         try {
            var29 = var29.normalize();
         } catch (Throwable t) {
            var29 = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);
         }

         // lateral nudge scaled by stability/speed (preserve a bit of previous behavior)
         if (var29.yCoord > 0.01D && var29.yCoord < 0.7D) {
            double speedScale = Math.max(0.12D, 1.0D - horizSpeed * 0.09D); // reduce at high speed
            double stabilityScale = (double)stableCount / (double)Math.max(1, this.wheels.length); // stable fraction
            double lateralScale = 1.0D * speedScale * stabilityScale;
            ac.motionX += var29.xCoord / 50.0D * lateralScale;
            ac.motionZ += var29.zCoord / 50.0D * lateralScale;
         }

         var29.rotateAroundY((float)((double)ac.getRotYaw() * Math.PI / 180.0D));
         float candidatePitch = (float)(90.0D - Math.atan2(var29.yCoord, var29.zCoord) * 180.0D / Math.PI);
         float candidateRoll  = -((float)(90.0D - Math.atan2(var29.yCoord, var29.xCoord) * 180.0D / Math.PI));

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
         // stable ground, no bump: smooth level toward zero
         float smoothFactor = 0.84F;
         this.targetPitch *= smoothFactor;
         this.targetRoll  *= smoothFactor;
         if (Math.abs(this.targetPitch) < 0.25F) this.targetPitch = 0.0F;
         if (Math.abs(this.targetRoll)  < 0.25F) this.targetRoll  = 0.0F;
         if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
            ac.setRotPitch(this.targetPitch);
            ac.setRotRoll(this.targetRoll);
         }
      }

      // final wheel placement clamped to transformed target
      for (int wi = 0; wi < this.wheels.length; ++wi) {
         MCH_EntityWheel w = this.wheels[wi];
         if (w == null) continue;
         Vec3 v = this.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord, ac, ac.getRotYaw(), this.targetPitch, this.targetRoll);
         double rangeH = 2.0D;
         double poy = (double)(w.stepHeight / 2.0F);
         if (w.posX > v.xCoord + rangeH) { w.posX = v.xCoord + rangeH; w.posY = v.yCoord + poy; }
         if (w.posX < v.xCoord - rangeH) { w.posX = v.xCoord - rangeH; w.posY = v.yCoord + poy; }
         if (w.posZ > v.zCoord + rangeH) { w.posZ = v.zCoord + rangeH; w.posY = v.yCoord + poy; }
         if (w.posZ < v.zCoord - rangeH) { w.posZ = v.zCoord - rangeH; w.posY = v.yCoord + poy; }
         w.setPositionAndRotation(w.posX, w.posY, w.posZ, 0.0F, 0.0F);
      }
   }



   /**
    * original code for this annoying bugged shitfest method (note, this works with tanks and everything else)
    *
    public void move(double x, double y, double z) {
    MCH_EntityAircraft ac = this.parent;
    if(ac.getAcInfo() != null) {
    boolean showLog = ac.ticksExisted % 1 == 1;
    if(showLog) {
    MCH_Lib.DbgLog(ac.worldObj, "[" + (ac.worldObj.isRemote?"Client":"Server") + "] ==============================", new Object[0]);
    }

    MCH_EntityWheel[] zmog = this.wheels;
    int rv = zmog.length;

    int wc;
    MCH_EntityWheel pitch;
    for(wc = 0; wc < rv; ++wc) {
    pitch = zmog[wc];
    pitch.prevPosX = pitch.posX;
    pitch.prevPosY = pitch.posY;
    pitch.prevPosZ = pitch.posZ;
    Vec3 roll = ac.getTransformedPosition(pitch.pos.xCoord, pitch.pos.yCoord, pitch.pos.zCoord);
    pitch.motionX = roll.xCoord - pitch.posX + x;
    pitch.motionY = roll.yCoord - pitch.posY;
    pitch.motionZ = roll.zCoord - pitch.posZ + z;
    }

    zmog = this.wheels;
    rv = zmog.length;

    for(wc = 0; wc < rv; ++wc) {
    pitch = zmog[wc];
    pitch.motionY *= 0.15D;
    pitch.moveEntity(pitch.motionX, pitch.motionY, pitch.motionZ);
    double var32 = 1.0D;
    pitch.moveEntity(0.0D, -0.1D * var32, 0.0D);
    }

    int var28 = -1;

    MCH_EntityWheel var30;
    for(rv = 0; rv < this.wheels.length / 2; ++rv) {
    var28 = rv;
    var30 = this.wheels[rv * 2 + 0];
    pitch = this.wheels[rv * 2 + 1];
    if(!var30.isPlus && (var30.onGround || pitch.onGround)) {
    var28 = -1;
    break;
    }
    }

    if(var28 >= 0) {
    this.wheels[var28 * 2 + 0].onGround = true;
    this.wheels[var28 * 2 + 1].onGround = true;
    }

    var28 = -1;

    for(rv = this.wheels.length / 2 - 1; rv >= 0; --rv) {
    var28 = rv;
    var30 = this.wheels[rv * 2 + 0];
    pitch = this.wheels[rv * 2 + 1];
    if(var30.isPlus && (var30.onGround || pitch.onGround)) {
    var28 = -1;
    break;
    }
    }

    if(var28 >= 0) {
    this.wheels[var28 * 2 + 0].onGround = true;
    this.wheels[var28 * 2 + 1].onGround = true;
    }

    Vec3 var29 = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
    Vec3 var31 = ac.getTransformedPosition(this.weightedCenter);
    var31.xCoord -= ac.posX;
    var31.yCoord = this.weightedCenter.yCoord;
    var31.zCoord -= ac.posZ;

    for(int var33 = 0; var33 < this.wheels.length / 2; ++var33) {
    MCH_EntityWheel var34 = this.wheels[var33 * 2 + 0];
    MCH_EntityWheel ogpf = this.wheels[var33 * 2 + 1];
    Vec3 ogrf = Vec3.createVectorHelper(var34.posX - (ac.posX + var31.xCoord), var34.posY - (ac.posY + var31.yCoord), var34.posZ - (ac.posZ + var31.zCoord));
    Vec3 arr$ = Vec3.createVectorHelper(ogpf.posX - (ac.posX + var31.xCoord), ogpf.posY - (ac.posY + var31.yCoord), ogpf.posZ - (ac.posZ + var31.zCoord));
    Vec3 len$ = var34.pos.zCoord >= 0.0D?arr$.crossProduct(ogrf):ogrf.crossProduct(arr$);
    len$ = len$.normalize();
    double i$ = Math.abs(var34.pos.zCoord / this.avgZ);
    if(!var34.onGround && !ogpf.onGround) {
    i$ = 0.0D;
    }

    var29.xCoord += len$.xCoord * i$;
    var29.yCoord += len$.yCoord * i$;
    var29.zCoord += len$.zCoord * i$;
    if(showLog) {
    len$.rotateAroundY((float)((double)ac.getRotYaw() * 3.141592653589793D / 180.0D));
    MCH_Lib.DbgLog(ac.worldObj, "%2d : %.2f :[%+.1f, %+.1f, %+.1f][%s %d %d][%+.2f(%+.2f), %+.2f(%+.2f)][%+.1f, %+.1f, %+.1f]", new Object[]{Integer.valueOf(var33), Double.valueOf(i$), Double.valueOf(len$.xCoord), Double.valueOf(len$.yCoord), Double.valueOf(len$.zCoord), var34.isPlus?"+":"-", Integer.valueOf(var34.onGround?1:0), Integer.valueOf(ogpf.onGround?1:0), Double.valueOf(var34.posY - var34.prevPosY), Double.valueOf(var34.motionY), Double.valueOf(ogpf.posY - ogpf.prevPosY), Double.valueOf(ogpf.motionY), Double.valueOf(len$.xCoord), Double.valueOf(len$.yCoord), Double.valueOf(len$.zCoord)});
    }
    }

    var29 = var29.normalize();
    if(var29.yCoord > 0.01D && var29.yCoord < 0.7D) {
    ac.motionX += var29.xCoord / 50.0D;
    ac.motionZ += var29.zCoord / 50.0D;
    }

    var29.rotateAroundY((float)((double)ac.getRotYaw() * 3.141592653589793D / 180.0D));
    float var35 = (float)(90.0D - Math.atan2(var29.yCoord, var29.zCoord) * 180.0D / 3.141592653589793D);
    float var36 = -((float)(90.0D - Math.atan2(var29.yCoord, var29.xCoord) * 180.0D / 3.141592653589793D));
    float var37 = ac.getAcInfo().onGroundPitchFactor;
    if(var35 - ac.getRotPitch() > var37) {
    var35 = ac.getRotPitch() + var37;
    }

    if(var35 - ac.getRotPitch() < -var37) {
    var35 = ac.getRotPitch() - var37;
    }

    float var38 = ac.getAcInfo().onGroundRollFactor;
    if(var36 - ac.getRotRoll() > var38) {
    var36 = ac.getRotRoll() + var38;
    }

    if(var36 - ac.getRotRoll() < -var38) {
    var36 = ac.getRotRoll() - var38;
    }

    this.targetPitch = var35;
    this.targetRoll = var36;
    if(!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
    ac.setRotPitch(var35);
    ac.setRotRoll(var36);
    }

    if(showLog) {
    MCH_Lib.DbgLog(ac.worldObj, "%+03d, %+03d :[%.2f, %.2f, %.2f] yaw=%.2f, pitch=%.2f, roll=%.2f", new Object[]{Integer.valueOf((int)var35), Integer.valueOf((int)var36), Double.valueOf(var29.xCoord), Double.valueOf(var29.yCoord), Double.valueOf(var29.zCoord), Float.valueOf(ac.getRotYaw()), Float.valueOf(this.targetPitch), Float.valueOf(this.targetRoll)});
    }

    MCH_EntityWheel[] var39 = this.wheels;
    int var40 = var39.length;

    for(int var41 = 0; var41 < var40; ++var41) {
    MCH_EntityWheel wheel = var39[var41];
    Vec3 v = this.getTransformedPosition(wheel.pos.xCoord, wheel.pos.yCoord, wheel.pos.zCoord, ac, ac.getRotYaw(), this.targetPitch, this.targetRoll);
    double offset = wheel.onGround?0.01D:-0.0D;
    double rangeH = 2.0D;
    double poy = (double)(wheel.stepHeight / 2.0F);
    int b = 0;
    if(wheel.posX > v.xCoord + rangeH) {
    wheel.posX = v.xCoord + rangeH;
    wheel.posY = v.yCoord + poy;
    b |= 1;
    }

    if(wheel.posX < v.xCoord - rangeH) {
    wheel.posX = v.xCoord - rangeH;
    wheel.posY = v.yCoord + poy;
    b |= 2;
    }

    if(wheel.posZ > v.zCoord + rangeH) {
    wheel.posZ = v.zCoord + rangeH;
    wheel.posY = v.yCoord + poy;
    b |= 4;
    }

    if(wheel.posZ < v.zCoord - rangeH) {
    wheel.posZ = v.zCoord - rangeH;
    wheel.posY = v.yCoord + poy;
    b |= 8;
    }

    wheel.setPositionAndRotation(wheel.posX, wheel.posY, wheel.posZ, 0.0F, 0.0F);
    }

    }
    }
    */



   public Vec3 getTransformedPosition(double x, double y, double z, MCH_EntityAircraft ac, float yaw, float pitch, float roll) {
      Vec3 v = MCH_Lib.RotVec3(x, y, z, -yaw, -pitch, -roll);
      return v.addVector(ac.posX, ac.posY, ac.posZ);
   }

   public void updateBlock() {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.Collision_DestroyBlock.prmBool) {
         MCH_EntityAircraft ac = this.parent;
         MCH_EntityWheel[] arr$ = this.wheels;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_EntityWheel w = arr$[i$];
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

   public void particleLandingGear() {
      if(this.wheels.length > 0) {
         MCH_EntityAircraft ac = this.parent;
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
