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

   // === NEW: angular velocity (physics state) ===
   private float pitchVel = 0.0F;
   private float rollVel  = 0.0F;

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

      // ================= WHEEL MOTION (UNCHANGED) =================
      for (MCH_EntityWheel w : this.wheels) {
         w.prevPosX = w.posX;
         w.prevPosY = w.posY;
         w.prevPosZ = w.posZ;

         Vec3 v = ac.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         w.motionX = v.xCoord - w.posX + x;
         w.motionY = v.yCoord - w.posY;
         w.motionZ = v.zCoord - w.posZ + z;
      }


      for (MCH_EntityWheel w : this.wheels) {
         w.motionY *= 0.15D;
         w.moveEntity(w.motionX, w.motionY, w.motionZ);
         w.moveEntity(0.0D, -0.1D, 0.0D);
      }

      // ================= GROUND NORMAL CALCULATION =================
      Vec3 normal = Vec3.createVectorHelper(0, 0, 0);
      Vec3 wc = ac.getTransformedPosition(this.weightedCenter);
      wc.xCoord -= ac.posX;
      wc.yCoord = this.weightedCenter.yCoord;
      wc.zCoord -= ac.posZ;

      for (int i = 0; i < this.wheels.length / 2; i++) {
         MCH_EntityWheel a = this.wheels[i * 2];
         MCH_EntityWheel b = this.wheels[i * 2 + 1];

         Vec3 va = Vec3.createVectorHelper(a.posX - (ac.posX + wc.xCoord),
                 a.posY - (ac.posY + wc.yCoord),
                 a.posZ - (ac.posZ + wc.zCoord));

         Vec3 vb = Vec3.createVectorHelper(b.posX - (ac.posX + wc.xCoord),
                 b.posY - (ac.posY + wc.yCoord),
                 b.posZ - (ac.posZ + wc.zCoord));

         Vec3 n = a.pos.zCoord >= 0 ? vb.crossProduct(va) : va.crossProduct(vb);
         n = n.normalize();

         double w = Math.abs(a.pos.zCoord / this.avgZ);
         if (!a.onGround && !b.onGround) w = 0;

         normal.xCoord += n.xCoord * w;
         normal.yCoord += n.yCoord * w;
         normal.zCoord += n.zCoord * w;
      }

      normal = normal.normalize();
      normal.rotateAroundY((float) Math.toRadians(ac.getRotYaw()));

      float pitchTarget = (float) (90.0D - Math.atan2(normal.yCoord, normal.zCoord) * 180.0D / Math.PI);
      float rollTarget  = (float)-(90.0D - Math.atan2(normal.yCoord, normal.xCoord) * 180.0D / Math.PI);

      this.targetPitch = pitchTarget;
      this.targetRoll  = rollTarget;

      // ==================== PHYSICS STABILIZATION ====================
      float stiffness = 0.08F * (-LAND_GRAVITY);
      float damping   = 0.22F;

      pitchVel += (targetPitch - ac.getRotPitch()) * stiffness - pitchVel * damping;
      rollVel  += (targetRoll  - ac.getRotRoll())  * stiffness - rollVel  * damping;

      pitchVel *= 0.90F;
      rollVel  *= 0.90F;

      pitchVel = MathHelper.clamp_float(pitchVel, -4.0F, 4.0F);
      rollVel  = MathHelper.clamp_float(rollVel,  -4.0F, 4.0F);

      if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
         ac.setRotPitch(ac.getRotPitch() + pitchVel);
         ac.setRotRoll (ac.getRotRoll()  + rollVel);
      }
   }

   //public Vec3 getTransformedPosition(double x, double y, double z, MCH_EntityAircraft ac, float yaw, float pitch, float roll) {
   //   Vec3 v = MCH_Lib.RotVec3(x, y, z, -yaw, -pitch, -roll);
   //   return v.addVector(ac.posX, ac.posY, ac.posZ);
   //} literally just not used btw thanks

   // ===== Remaining methods unchanged =====
   public void updateBlock() { /* unchanged */ }
   public void particleLandingGear() { /* unchanged */ }
}
