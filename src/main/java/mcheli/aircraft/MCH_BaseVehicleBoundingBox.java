package mcheli.aircraft;


import mcheli.ship.MCH_EntityShip;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;


public class MCH_BaseVehicleBoundingBox extends AxisAlignedBB {

   private final MCH_EntityBaseVehicle ac;


   protected MCH_BaseVehicleBoundingBox(MCH_EntityBaseVehicle ac) {
      super(ac.boundingBox.minX, ac.boundingBox.minY, ac.boundingBox.minZ, ac.boundingBox.maxX, ac.boundingBox.maxY, ac.boundingBox.maxZ);
      this.ac = ac;
   }

   public AxisAlignedBB NewAABB(double p_72324_1_, double p_72324_3_, double p_72324_5_, double p_72324_7_, double p_72324_9_, double p_72324_11_) {
      return (new MCH_BaseVehicleBoundingBox(this.ac)).setBounds(p_72324_1_, p_72324_3_, p_72324_5_, p_72324_7_, p_72324_9_, p_72324_11_);
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

   /**
    * Aircraft hit boxes are composite for ray tracing and damage, but only ships
    * expose those boxes as walkable world collision.  Resolving against each
    * component independently also prevents the small primary aircraft box from
    * blocking horizontal movement when a player is merely touching a deck box.
    */
   private boolean hasDeckCollision() {
      return this.ac instanceof MCH_EntityShip;
   }

   private boolean isShip() {
      return this.ac instanceof MCH_EntityShip;
   }

   private boolean isDeckTopContact(AxisAlignedBB deck, AxisAlignedBB other, boolean movingOnX) {
      final double bobTolerance = 0.6D;
      boolean overlapsPerpendicularAxis = movingOnX
              ? other.maxZ > deck.minZ && other.minZ < deck.maxZ
              : other.maxX > deck.minX && other.minX < deck.maxX;
      return overlapsPerpendicularAxis && this.isBaseDeckVerticalContact(deck, other, bobTolerance);
   }

   private boolean isBaseDeckVerticalContact(AxisAlignedBB deck, AxisAlignedBB other, double bobTolerance) {
      return other.minY >= deck.maxY - bobTolerance
              && other.minY <= deck.maxY + bobTolerance;
   }

   private boolean isBaseDeckTopContact(AxisAlignedBB other, double bobTolerance) {
      return other.maxX > super.minX + 1.0E-4D
              && other.minX < super.maxX - 1.0E-4D
              && other.maxZ > super.minZ + 1.0E-4D
              && other.minZ < super.maxZ - 1.0E-4D
              && this.isBaseDeckVerticalContact(this, other, bobTolerance);
   }

   private double getPreviousBaseTopSurfaceY() {
      return super.maxY - (this.ac.posY - this.ac.prevPosY);
   }

   private double calculatePreviousBaseDeckYOffset(AxisAlignedBB other, double offset, double supportTolerance) {
      if(offset >= 0.0D || !this.isBaseDeckTopContact(other, supportTolerance)) {
         return offset;
      }

      double candidate = this.getPreviousBaseTopSurfaceY() - other.minY;
      return candidate > offset ? candidate : offset;
   }

   private boolean isDeckTopContact(MCH_BoundingBox deck, AxisAlignedBB other) {
      final double bobTolerance = 0.6D;
      return deck.isEntityOnTop(other, 1.0E-4D, bobTolerance, bobTolerance);
   }

   @Override
   public double calculateXOffset(AxisAlignedBB other, double offset) {
      if(!this.hasDeckCollision()) {
         return offset;
      }

      if(!this.isShip() && !this.isDeckTopContact(this, other, true)) {
         offset = super.calculateXOffset(other, offset);
      }
      // Refresh calculated extra boxes at this collision entry point before any physical OBB offset queries.
      MCH_BoundingBox[] boxes = this.ac.getCalculatedExtraBoundingBoxes();
      for(MCH_BoundingBox bb : boxes) {
         if(!this.isDeckTopContact(bb, other)) {
            offset = this.isShip() ? bb.calculateXOffset(other, offset) : bb.boundingBox.calculateXOffset(other, offset);
         }
      }
      return offset;
   }

   @Override
   public double calculateYOffset(AxisAlignedBB other, double offset) {
      if(!this.hasDeckCollision()) {
         return offset;
      }

      if(!this.isShip()) {
         double previousBaseTopY = this.getPreviousBaseTopSurfaceY();
         offset = super.calculateYOffset(other, offset);
         if(this.ac.canFloatWater() && super.maxY > previousBaseTopY) {
            offset = this.calculatePreviousBaseDeckYOffset(other, offset, 0.6D);
         }
      }

      // Refresh calculated extra boxes at this collision entry point before any physical OBB offset queries.
      MCH_BoundingBox[] boxes = this.ac.getCalculatedExtraBoundingBoxes();
      for(MCH_BoundingBox bb : boxes) {
         final double supportTolerance = 0.6D;
         double previousTopY = bb.getPreviousTopSurfaceY();
         offset = bb.calculateDeckYOffset(other, offset, supportTolerance);

         // When a floating deck rises into an entity, vanilla's Y resolver sees
         // overlapping boxes and no longer treats the deck as floor support. Use
         // the previous OBB top for that one transition; finishDeckMovement then
         // carries the entity by the matching surface delta.
         if(this.ac.canFloatWater() && bb.getTopSurfaceY() > previousTopY) {
            offset = bb.calculatePreviousDeckYOffset(other, offset, supportTolerance);
         }
      }
      return offset;
   }

   @Override
   public double calculateZOffset(AxisAlignedBB other, double offset) {
      if(!this.hasDeckCollision()) {
         return offset;
      }

      if(!this.isShip() && !this.isDeckTopContact(this, other, false)) {
         offset = super.calculateZOffset(other, offset);
      }
      // Refresh calculated extra boxes at this collision entry point before any physical OBB offset queries.
      MCH_BoundingBox[] boxes = this.ac.getCalculatedExtraBoundingBoxes();
      for(MCH_BoundingBox bb : boxes) {
         if(!this.isDeckTopContact(bb, other)) {
            offset = this.isShip() ? bb.calculateZOffset(other, offset) : bb.boundingBox.calculateZOffset(other, offset);
         }
      }
      return offset;
   }

   public boolean intersectsWith(AxisAlignedBB aabb) {
      boolean ret = false;
      double dist = 1.0E7D;
      this.ac.lastBBDamageFactor = 1.0F;
      this.ac.lastHitBoundingBoxType = EnumBoundingBoxType.DEFAULT;
      if(!this.isShip() && super.intersectsWith(aabb)) {
         dist = this.getDistSq(aabb, this);
         ret = true;
      }

      // Refresh calculated extra boxes at this collision entry point before any physical OBB intersection queries.
      MCH_BoundingBox[] arr$ = this.ac.getCalculatedExtraBoundingBoxes();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_BoundingBox bb = arr$[i$];
         //wheelBoundingBox wb = arr$[i$];

         if((this.isShip() ? bb.intersectsAABB(aabb) : bb.boundingBox.intersectsWith(aabb))) {
            double dist2 = this.getDistSq(aabb, this.isShip() ? bb.getEnclosingAABB() : bb.boundingBox);
            if(dist2 < dist) {
               dist = dist2;
               this.ac.lastBBDamageFactor = bb.damegeFactor;
               this.ac.lastHitBoundingBoxType = bb.boundingBoxType;
            }

            ret = true;
         }
      }

      //wheelBoundingBox[] arr2$ = this.ac.extrawheelboundingbox;
      //int len2$ = arr2$.length;
//
      //for(int i2$ = 0; i2$ < len2$; ++i2$) {
      //   wheelBoundingBox wb = arr2$[i2$];
      //   //wheelBoundingBox wb = arr$[i$];
//
      //   if(wb.boundingBox.intersectsWith(aabb)) {
      //      double dist3 = this.getDistSq(aabb, this);
      //      if(dist3 < dist) {
      //         dist = dist3;
      //         this.ac.lastBBDamageFactor = wb.damegeFactor;
      //      }
//
      //      ret = true;
      //   }
      //}
      //new collision?



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
      this.ac.lastHitBoundingBoxType = EnumBoundingBoxType.DEFAULT;
      MovingObjectPosition mop = this.isShip()?null:super.calculateIntercept(v1, v2);
      double dist = 1.0E7D;
      if(mop != null) {
         dist = v1.distanceTo(mop.hitVec);
      }

      MCH_BoundingBox[] arr$ = this.ac.getCalculatedExtraBoundingBoxes();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_BoundingBox bb = arr$[i$];
         MovingObjectPosition mop2 = bb.calculateIntercept(v1, v2);
         if(mop2 != null) {
            double dist2 = v1.distanceTo(mop2.hitVec);
            if(dist2 < dist) {
               mop = mop2;
               dist = dist2;
               this.ac.lastBBDamageFactor = bb.damegeFactor;
               this.ac.lastHitBoundingBoxType = bb.boundingBoxType;
            }
         }
      }

      return mop;
   }
}
