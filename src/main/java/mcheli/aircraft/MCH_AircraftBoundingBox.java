package mcheli.aircraft;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class MCH_AircraftBoundingBox extends AxisAlignedBB {

   private final MCH_EntityAircraft ac;
   private MCH_BoundingBox lastBB;


   protected MCH_AircraftBoundingBox(MCH_EntityAircraft ac) {
      super(ac.boundingBox.minX, ac.boundingBox.minY, ac.boundingBox.minZ, ac.boundingBox.maxX, ac.boundingBox.maxY, ac.boundingBox.maxZ);
      this.ac = ac;
   }

   public AxisAlignedBB NewAABB(double p_72324_1_, double p_72324_3_, double p_72324_5_, double p_72324_7_, double p_72324_9_, double p_72324_11_) {
      return (new MCH_AircraftBoundingBox(this.ac)).setBounds(p_72324_1_, p_72324_3_, p_72324_5_, p_72324_7_, p_72324_9_, p_72324_11_);
   }

   public double getDistSq(AxisAlignedBB a1, AxisAlignedBB a2) {
      double x1 = (a1.maxX + a1.minX) / 2.0D;
      double y1 = (a1.maxY + a1.minY) / 2.0D;
      double z1 = (a1.maxZ + a1.minZ) / 2.0D;
      double x2 = (a2.maxX + a2.minX) / 2.0D;
      double y2 = (a2.maxY + a2.minY) / 2.0D;
      double z2 = (a2.maxZ + a2.minZ) / 2.0D;
      double dx = x1 - x2;
      double dy = y1 - y2;
      double dz = z1 - z2;
      return dx * dx + dy * dy + dz * dz;
   }

   public boolean intersectsWith(AxisAlignedBB aabb) {
	  
      boolean ret = false;
      double dist = 1.0E7D;
      this.ac.lastBBDamageFactor = 1.0F;
      ac.lastBB = null;
      if(super.intersectsWith(aabb)) {
         dist = this.getDistSq(aabb, this);
         ret = true;
      }

      
      
      MCH_BoundingBox[] arr$ = this.ac.extraBoundingBox;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_BoundingBox bb = arr$[i$];
         if(bb.boundingBox.intersectsWith(aabb)) {
            double dist2 = this.getDistSq(aabb, this);
            if(dist2 < dist) {
               dist = dist2;
               this.ac.lastBBDamageFactor = bb.damegeFactor;
               this.ac.lastBB = bb;
             // System.out.println("Updating BB " + bb.armor);
            }

            ret = true;
         }
      }

      return ret;
   }

   public AxisAlignedBB expand(double p_72314_1_, double p_72314_3_, double p_72314_5_) {
      double d3 = super.minX - p_72314_1_;
      double d4 = super.minY - p_72314_3_;
      double d5 = super.minZ - p_72314_5_;
      double d6 = super.maxX + p_72314_1_;
      double d7 = super.maxY + p_72314_3_;
      double d8 = super.maxZ + p_72314_5_;
      return this.NewAABB(d3, d4, d5, d6, d7, d8);
   }

   public AxisAlignedBB func_111270_a(AxisAlignedBB p_111270_1_) {
      double d0 = Math.min(super.minX, p_111270_1_.minX);
      double d1 = Math.min(super.minY, p_111270_1_.minY);
      double d2 = Math.min(super.minZ, p_111270_1_.minZ);
      double d3 = Math.max(super.maxX, p_111270_1_.maxX);
      double d4 = Math.max(super.maxY, p_111270_1_.maxY);
      double d5 = Math.max(super.maxZ, p_111270_1_.maxZ);
      return this.NewAABB(d0, d1, d2, d3, d4, d5);
   }

   public AxisAlignedBB addCoord(double p_72321_1_, double p_72321_3_, double p_72321_5_) {
      double d3 = super.minX;
      double d4 = super.minY;
      double d5 = super.minZ;
      double d6 = super.maxX;
      double d7 = super.maxY;
      double d8 = super.maxZ;
      if(p_72321_1_ < 0.0D) {
         d3 += p_72321_1_;
      }

      if(p_72321_1_ > 0.0D) {
         d6 += p_72321_1_;
      }

      if(p_72321_3_ < 0.0D) {
         d4 += p_72321_3_;
      }

      if(p_72321_3_ > 0.0D) {
         d7 += p_72321_3_;
      }

      if(p_72321_5_ < 0.0D) {
         d5 += p_72321_5_;
      }

      if(p_72321_5_ > 0.0D) {
         d8 += p_72321_5_;
      }

      return this.NewAABB(d3, d4, d5, d6, d7, d8);
   }

   public AxisAlignedBB contract(double p_72331_1_, double p_72331_3_, double p_72331_5_) {
      double d3 = super.minX + p_72331_1_;
      double d4 = super.minY + p_72331_3_;
      double d5 = super.minZ + p_72331_5_;
      double d6 = super.maxX - p_72331_1_;
      double d7 = super.maxY - p_72331_3_;
      double d8 = super.maxZ - p_72331_5_;
      return this.NewAABB(d3, d4, d5, d6, d7, d8);
   }

   public AxisAlignedBB copy() {
      return this.NewAABB(super.minX, super.minY, super.minZ, super.maxX, super.maxY, super.maxZ);
   }

   public AxisAlignedBB getOffsetBoundingBox(double x, double y, double z) {
      return this.NewAABB(super.minX + x, super.minY + y, super.minZ + z, super.maxX + x, super.maxY + y, super.maxZ + z);
   }

   public MovingObjectPosition calculateIntercept(Vec3 v1, Vec3 v2) {
      this.ac.lastBBDamageFactor = 1.0F;
      MovingObjectPosition mop = super.calculateIntercept(v1, v2);
      double dist = 1.0E7D;
      if(mop != null) {
         dist = v1.distanceTo(mop.hitVec);
      }

      MCH_BoundingBox[] arr$ = this.ac.extraBoundingBox;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_BoundingBox bb = arr$[i$];
         MovingObjectPosition mop2 = bb.boundingBox.calculateIntercept(v1, v2);
         if(mop2 != null) {
            double dist2 = v1.distanceTo(mop2.hitVec);
            if(dist2 < dist) {
               mop = mop2;
               dist = dist2;
               this.ac.lastBBDamageFactor = bb.damegeFactor;
            }
         }
      }

      return mop;
   }
}
