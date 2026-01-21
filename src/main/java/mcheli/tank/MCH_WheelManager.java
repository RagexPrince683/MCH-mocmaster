package mcheli.tank;

import java.util.List;
import java.util.Random;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WheelManager {

   public final MCH_EntityAircraft parent;
   public MCH_EntityWheel[] wheels;
   private double minZ;
   private double maxZ;
   private double avgZ;
   public Vec3 weightedCenter;
   public float targetPitch;
   public float targetRoll;
   public float prevYaw;

   private static final float LAND_GRAVITY = -0.61F;
   private static Random rand = new Random();

   public MCH_WheelManager(MCH_EntityAircraft ac) {
      this.parent = ac;
      this.wheels = new MCH_EntityWheel[0];
      this.weightedCenter = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
   }

   public void createWheels(World w, List list, Vec3 weightedCenter) {
      this.wheels = new MCH_EntityWheel[list.size() * 2];
      this.minZ = 999999.0D;
      this.maxZ = -999999.0D;
      this.weightedCenter = weightedCenter;

      for (int i = 0; i < this.wheels.length; ++i) {
         MCH_EntityWheel wheel = new MCH_EntityWheel(w);
         wheel.setParents(this.parent);
         Vec3 wp = ((MCH_AircraftInfo.Wheel) list.get(i / 2)).pos;
         wheel.setWheelPos(
                 Vec3.createVectorHelper(i % 2 == 0 ? wp.xCoord : -wp.xCoord, wp.yCoord, wp.zCoord),
                 this.weightedCenter
         );
         Vec3 v = this.parent.getTransformedPosition(wheel.pos.xCoord, wheel.pos.yCoord, wheel.pos.zCoord);
         wheel.setLocationAndAngles(v.xCoord, v.yCoord + 1.0D, v.zCoord, 0.0F, 0.0F);
         this.wheels[i] = wheel;
         if (wheel.pos.zCoord <= this.minZ) this.minZ = wheel.pos.zCoord;
         if (wheel.pos.zCoord >= this.maxZ) this.maxZ = wheel.pos.zCoord;
      }

      this.avgZ = this.maxZ - this.minZ;
   }

   public void move(double x, double y, double z) {
      MCH_EntityAircraft ac = this.parent;
      if (ac.getAcInfo() == null) return;

      // Compute horizontal speed (units where 1.8 ~ 180MPH for you)
      double horizSpeed = Math.sqrt(ac.motionX * ac.motionX + ac.motionZ * ac.motionZ);

      // === 1) Wheel desired motion (compute once per wheel) ===
      for (MCH_EntityWheel w : this.wheels) {
         w.prevPosX = w.posX;
         w.prevPosY = w.posY;
         w.prevPosZ = w.posZ;

         Vec3 desired = ac.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         w.motionX = desired.xCoord - w.posX + x;
         w.motionY = desired.yCoord - w.posY;
         w.motionZ = desired.zCoord - w.posZ + z;
      }

      // === 2) Move wheels once, with modest vertical responsiveness ===
      // vertical responsiveness scales with speed so at high speed wheels follow terrain quicker
      double wheelVertBase = 0.15D;
      double wheelVertResp = wheelVertBase + Math.min(0.85D, horizSpeed * 0.12D); // [0.15 .. 1.0]
      for (MCH_EntityWheel w : this.wheels) {
         w.motionY *= wheelVertResp;
         // single moveEntity call only
         w.moveEntity(w.motionX, w.motionY, w.motionZ);
         // NO unconditional big downward push here (removes aggressive digging)
      }

      // === 3) For each wheel, find ground under wheel and smooth posY toward it ===
      int searchDepth = 6; // blocks to look down for ground
      for (MCH_EntityWheel w : this.wheels) {
         int wx = MathHelper.floor_double(w.posX + 0.5D);
         int wz = MathHelper.floor_double(w.posZ + 0.5D);
         int startY = MathHelper.floor_double(w.posY + 1.0D);
         int groundY = Integer.MIN_VALUE;
         for (int yy = startY; yy >= startY - searchDepth; yy--) {
            Block b = ac.worldObj.getBlock(wx, yy, wz);
            if (b != Blocks.air && b != W_Block.getSnowLayer()) {
               groundY = yy;
               break;
            }
         }

         double targetY;
         boolean nowOnGround = false;
         if (groundY != Integer.MIN_VALUE) {
            targetY = groundY + 0.5D;
            // if wheel is reasonably close to ground, mark onGround
            if (Math.abs(w.posY - targetY) < 2.5D) nowOnGround = true;
         } else {
            targetY = w.posY; // no ground found; keep current Y
         }

         // alpha: wheels respond faster at higher speed (so they don't lag)
         double alpha = MathHelper.clamp_double(Math.abs(horizSpeed) / 8.0D, 0.12D, 0.95D);

         // if wheel penetrated below target, correct more aggressively but smoothly
         if (w.posY < targetY) {
            double penetration = targetY - w.posY;
            double penAlpha = MathHelper.clamp_double(0.25D + penetration * 0.35D, alpha, 0.95D);
            w.posY = w.posY * (1.0D - penAlpha) + targetY * penAlpha;
            w.motionY = 0.0D;
         } else {
            // normal smoothing
            w.posY = w.posY * (1.0D - alpha) + targetY * alpha;
         }

         // update onGround flag
         w.onGround = nowOnGround;
         w.setPositionAndRotation(w.posX, w.posY, w.posZ, 0.0F, 0.0F);
         w.prevPosY = w.posY;
      }

      // === 4) Compute ground normal using wheel positions (like original) ===
      Vec3 norm = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      Vec3 wc = ac.getTransformedPosition(this.weightedCenter);
      wc.xCoord -= ac.posX;
      wc.yCoord = this.weightedCenter.yCoord;
      wc.zCoord -= ac.posZ;

      for (int i = 0; i < this.wheels.length / 2; ++i) {
         MCH_EntityWheel a = this.wheels[i * 2];
         MCH_EntityWheel b = this.wheels[i * 2 + 1];
         Vec3 va = Vec3.createVectorHelper(a.posX - (ac.posX + wc.xCoord), a.posY - (ac.posY + wc.yCoord), a.posZ - (ac.posZ + wc.zCoord));
         Vec3 vb = Vec3.createVectorHelper(b.posX - (ac.posX + wc.xCoord), b.posY - (ac.posY + wc.yCoord), b.posZ - (ac.posZ + wc.zCoord));
         Vec3 n = a.pos.zCoord >= 0.0D ? vb.crossProduct(va) : va.crossProduct(vb);
         n = n.normalize();
         double weight = Math.abs(a.pos.zCoord / this.avgZ);
         if (!a.onGround && !b.onGround) weight = 0.0D;
         norm.xCoord += n.xCoord * weight;
         norm.yCoord += n.yCoord * weight;
         norm.zCoord += n.zCoord * weight;
      }

      // protect from zero vector
      double nx = norm.xCoord;
      double ny = norm.yCoord;
      double nz = norm.zCoord;
      if (nx == 0.0D && ny == 0.0D && nz == 0.0D) {
         // keep previous targets if available
      } else {
         norm = norm.normalize();
         norm.rotateAroundY((float) Math.toRadians(ac.getRotYaw()));

         float tgtPitch = (float) (90.0D - Math.atan2(norm.yCoord, norm.zCoord) * 180.0D / Math.PI);
         float tgtRoll  = (float) - (90.0D - Math.atan2(norm.yCoord, norm.xCoord) * 180.0D / Math.PI);

         // clamp by aircraft's on-ground factors (keep behavior)
         float maxPitchChange = ac.getAcInfo().onGroundPitchFactor;
         float maxRollChange  = ac.getAcInfo().onGroundRollFactor;
         if (tgtPitch - ac.getRotPitch() > maxPitchChange) tgtPitch = ac.getRotPitch() + maxPitchChange;
         if (tgtPitch - ac.getRotPitch() < -maxPitchChange) tgtPitch = ac.getRotPitch() - maxPitchChange;
         if (tgtRoll - ac.getRotRoll() > maxRollChange) tgtRoll = ac.getRotRoll() + maxRollChange;
         if (tgtRoll - ac.getRotRoll() < -maxRollChange) tgtRoll = ac.getRotRoll() - maxRollChange;

         this.targetPitch = tgtPitch;
         this.targetRoll  = tgtRoll;

         // === 5) Apply smoothed & rate-limited rotation to aircraft ===
         // speed-dependent max angle change per tick (lower at high speed)
         double speedNorm = MathHelper.clamp_double(horizSpeed / 8.0D, 0.0D, 1.0D);
         float maxDeltaLow = 3.5F;  // deg/tick at low speed
         float maxDeltaHigh = 1.0F; // deg/tick at high speed (more conservative)
         float maxDelta = (float) (maxDeltaLow * (1.0D - speedNorm) + maxDeltaHigh * speedNorm);

         // interpolation factor
         float alphaRot = (float) MathHelper.clamp_double(0.22D + speedNorm * 0.12D, 0.08D, 0.6D);

         float pitchDelta = (this.targetPitch - ac.getRotPitch()) * alphaRot;
         float rollDelta  = (this.targetRoll  - ac.getRotRoll())  * alphaRot;

         // clamp per-tick change
         if (pitchDelta > maxDelta) pitchDelta = maxDelta;
         if (pitchDelta < -maxDelta) pitchDelta = -maxDelta;
         if (rollDelta > maxDelta) rollDelta = maxDelta;
         if (rollDelta < -maxDelta) rollDelta = -maxDelta;

         if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
            ac.setRotPitch(ac.getRotPitch() + pitchDelta);
            ac.setRotRoll(ac.getRotRoll() + rollDelta);
         }
      }
   }

   public Vec3 getTransformedPosition(double x, double y, double z, MCH_EntityAircraft ac, float yaw, float pitch, float roll) {
      Vec3 v = MCH_Lib.RotVec3(x, y, z, -yaw, -pitch, -roll);
      return v.addVector(ac.posX, ac.posY, ac.posZ);
   }

   public void updateBlock() {
      MCH_Config var10000 = MCH_MOD.config;
      if (MCH_Config.Collision_DestroyBlock.prmBool) {
         MCH_EntityAircraft ac = this.parent;
         MCH_EntityWheel[] arr$ = this.wheels;
         int len$ = arr$.length;

         for (int i$ = 0; i$ < len$; ++i$) {
            MCH_EntityWheel w = arr$[i$];
            Vec3 v = ac.getTransformedPosition(w.pos);
            int x = (int) (v.xCoord + 0.5D);
            int y = (int) (v.yCoord + 0.5D);
            int z = (int) (v.zCoord + 0.5D);
            Block block = ac.worldObj.getBlock(x, y, z);
            if (block == W_Block.getSnowLayer()) {
               ac.worldObj.setBlockToAir(x, y, z);
            }

            if (block == Blocks.waterlily || block == Blocks.cake) {
               W_WorldFunc.destroyBlock(ac.worldObj, x, y, z, false);
            }
         }
      }
   }

   public void particleLandingGear() {
      if (this.wheels.length > 0) {
         MCH_EntityAircraft ac = this.parent;
         double d = ac.motionX * ac.motionX + ac.motionZ * ac.motionZ + (double) Math.abs(this.prevYaw - ac.getRotYaw());
         this.prevYaw = ac.getRotYaw();
         if (d > 0.001D) {
            for (int i = 0; i < 2; ++i) {
               MCH_EntityWheel w = this.wheels[rand.nextInt(this.wheels.length)];
               Vec3 v = ac.getTransformedPosition(w.pos);
               int x = MathHelper.floor_double(v.xCoord + 0.5D);
               int y = MathHelper.floor_double(v.yCoord - 0.5D);
               int z = MathHelper.floor_double(v.zCoord + 0.5D);
               Block block = ac.worldObj.getBlock(x, y, z);
               if (Block.isEqualTo(block, Blocks.air)) {
                  y = MathHelper.floor_double(v.yCoord + 0.5D);
                  block = ac.worldObj.getBlock(x, y, z);
               }

               if (!Block.isEqualTo(block, Blocks.air)) {
                  MCH_ParticlesUtil.spawnParticleTileCrack(ac.worldObj, x, y, z, v.xCoord + ((double) rand.nextFloat() - 0.5D), v.yCoord + 0.1D, v.zCoord + ((double) rand.nextFloat() - 0.5D), -ac.motionX * 4.0D + ((double) rand.nextFloat() - 0.5D) * 0.1D, (double) rand.nextFloat() * 0.5D, -ac.motionZ * 4.0D + ((double) rand.nextFloat() - 0.5D) * 0.1D);
               }
            }
         }
      }
   }
}
