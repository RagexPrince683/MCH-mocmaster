package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import mcheli.aircraft.MCH_BoundingBox;
import mcheli.aircraft.MCH_ObbCollision;
import net.minecraft.util.AxisAlignedBB;

/** Side-effect-free physical-hull step planner using the vehicle OBB contact policy. */
public final class MCH_TankStepSolver {
   public static final double COLLISION_EPSILON=1.0E-4D, PRE_CONTACT_GAP=0.01D;
   private static final double SWEEP_INTERVAL=0.025D;
   private static final int SEARCH_ITERATIONS=14;

   public static final class Transform {
      public final double x,y,z; public final float yaw,pitch,roll;
      public Transform(double x,double y,double z,float yaw,float pitch,float roll){this.x=x;this.y=y;this.z=z;this.yaw=yaw;this.pitch=pitch;this.roll=roll;}
      Transform offset(double x,double y,double z){return new Transform(this.x+x,this.y+y,this.z+z,yaw,pitch,roll);}
   }
   public static final class Contact {
      public final int hullIndex; public final MCH_BoundingBox sourceHull; public final AxisAlignedBB block;
      public final double normalX,normalY,normalZ,penetration,shapeTop,hullBottom,safeFraction,blockedFraction;
      public final boolean existedAtStart,floor;
      Contact(int i,MCH_BoundingBox h,AxisAlignedBB b,MCH_ObbCollision.Contact c,double bottom,double safe,double blocked,boolean old,boolean floor){
         hullIndex=i;sourceHull=h;block=b;normalX=c.normalX;normalY=c.normalY;normalZ=c.normalZ;penetration=c.penetration;
         shapeTop=b.maxY;hullBottom=bottom;safeFraction=safe;blockedFraction=blocked;existedAtStart=old;this.floor=floor;
      }
   }
   public static final class Route {
      public final boolean stepped; public final Transform preContact,raised,raisedEnd,end;
      public final double safeFraction,blockedFraction,preContactGap,requiredRise,lift; public final List contacts;
      Route(boolean s,Transform p,Transform u,Transform a,Transform e,double safe,double blocked,double gap,double rise,double lift,List c){stepped=s;preContact=p;raised=u;raisedEnd=a;end=e;safeFraction=safe;blockedFraction=blocked;preContactGap=gap;requiredRise=rise;this.lift=lift;contacts=c;}
   }
   public static final class Result {
      public final Route normalRoute,stepRoute,selectedRoute; public final String failureSegment; public final AxisAlignedBB failureBlock;
      public final double failureNormalX,failureNormalY,failureNormalZ; public final Contact failureContact;
      Result(Route n,Route s,Route selected,String segment,Contact c){normalRoute=n;stepRoute=s;selectedRoute=selected;failureSegment=segment;failureContact=c;failureBlock=c==null?null:c.block;failureNormalX=c==null?0:c.normalX;failureNormalY=c==null?0:c.normalY;failureNormalZ=c==null?0:c.normalZ;}
   }
   private MCH_TankStepSolver(){}

   public static Result solve(Transform start,List definitions,List blocks,double dx,double dz,double supportPlane,double stepHeight,boolean supported){
      ArrayList hulls=copy(definitions), baseline=contacts(start,hulls,blocks,0,0,null);
      if(hulls.isEmpty()||dx*dx+dz*dz<=COLLISION_EPSILON*COLLISION_EPSILON){Route n=normal(start,dx,dz,1,1);return new Result(n,null,n,"classification",null);}
      Sweep first=sweep(start,hulls,blocks,baseline,dx,0,dz,false);
      Route normal=normal(start,dx,dz,first.safe,first.blocked);
      if(!first.blockedMovement||!supported||stepHeight<=0)return new Result(normal,null,normal,"classification",first.contact);
      ArrayList hit=contacts(start.offset(dx*first.blocked,0,dz*first.blocked),hulls,blocks,first.safe,first.blocked,baseline);
      ArrayList risers=new ArrayList(); double landingRise=0,clearanceLift=0;
      for(int i=0;i<hit.size();i++){
         Contact c=(Contact)hit.get(i);
         if(c.floor||(c.existedAtStart&&!deeperThanBaseline(c,baseline)))continue;
         double horizontal=Math.sqrt(c.normalX*c.normalX+c.normalZ*c.normalZ), into=dx*c.normalX+dz*c.normalZ;
         double rise=c.shapeTop-supportPlane;
         if(horizontal<0.25D||into>=-COLLISION_EPSILON||c.normalY>0.7D||rise<=COLLISION_EPSILON||rise>stepHeight+COLLISION_EPSILON)
            return new Result(normal,null,normal,"classification",c);
         risers.add(c); landingRise=Math.max(landingRise,rise);
         clearanceLift=Math.max(clearanceLift,c.shapeTop+COLLISION_EPSILON-c.hullBottom);
      }
      if(risers.isEmpty())return new Result(normal,null,normal,"classification",first.contact);
      clearanceLift=Math.max(landingRise,clearanceLift);
      double distance=Math.sqrt(dx*dx+dz*dz),gapFraction=Math.min(first.safe,PRE_CONTACT_GAP/distance);
      Failure failure=new Failure("lift",first.contact);
      for(int retreat=0;retreat<=SEARCH_ITERATIONS;retreat++){
         double retreatFraction=gapFraction+(first.safe-gapFraction)*retreat/(double)SEARCH_ITERATIONS;
         double safe=Math.max(0,first.safe-retreatFraction),gap=(first.safe-safe)*distance;
         Transform pre=start.offset(dx*safe,0,dz*safe),up=pre.offset(0,clearanceLift,0),across=up.offset(dx*(1-safe),0,dz*(1-safe));
         Transform end=across.offset(0,landingRise-clearanceLift,0);
         if(!segmentSafe(pre,up,hulls,blocks,"lift",failure))continue;
         if(!segmentSafe(up,across,hulls,blocks,"raised-horizontal",failure))continue;
         if(!segmentSafe(across,end,hulls,blocks,"lower",failure))continue;
         if(!hasSupport(end,hulls,blocks,supportPlane+landingRise)){failure.segment="support";continue;}
         Route step=new Route(true,pre,up,across,end,safe,first.blocked,gap,landingRise,clearanceLift,risers);
         Route selected=progress(start,end)>progress(start,normal.end)+COLLISION_EPSILON?step:normal;
         return new Result(normal,step,selected,null,null);
      }
      return new Result(normal,null,normal,failure.segment,failure.contact);
   }
   private static Route normal(Transform s,double x,double z,double safe,double blocked){Transform e=s.offset(x*safe,0,z*safe);return new Route(false,e,e,e,e,safe,blocked,0,0,0,new ArrayList());}
   private static double progress(Transform a,Transform b){double x=b.x-a.x,z=b.z-a.z;return Math.sqrt(x*x+z*z);}
   private static ArrayList copy(List d){ArrayList a=new ArrayList();for(int i=0;i<d.size();i++)a.add(((MCH_BoundingBox)d.get(i)).copy());return a;}
   private static final class Sweep{double safe=1,blocked=1;boolean blockedMovement;Contact contact;}
   private static Sweep sweep(Transform s,List h,List b,List baseline,double dx,double dy,double dz,boolean allowFloor){
      Sweep out=new Sweep();double len=Math.sqrt(dx*dx+dy*dy+dz*dz);int count=Math.max(1,(int)Math.ceil(len/SWEEP_INTERVAL));double previous=0;
      for(int i=1;i<=count;i++){double f=i/(double)count;Contact hit=blocking(s.offset(dx*f,dy*f,dz*f),h,b,baseline,allowFloor);
         if(hit!=null){double safe=previous,blocked=f;for(int n=0;n<SEARCH_ITERATIONS;n++){double m=(safe+blocked)*.5;if(blocking(s.offset(dx*m,dy*m,dz*m),h,b,baseline,allowFloor)==null)safe=m;else blocked=m;}
            out.safe=safe;out.blocked=blocked;out.blockedMovement=true;out.contact=blocking(s.offset(dx*blocked,dy*blocked,dz*blocked),h,b,baseline,allowFloor);return out;}previous=f;}
      return out;
   }
   private static Contact blocking(Transform t,List h,List b,List baseline,boolean allowFloor){ArrayList cs=contacts(t,h,b,0,0,baseline);for(int i=0;i<cs.size();i++){Contact c=(Contact)cs.get(i);if(c.floor&&allowFloor)continue;if(c.floor)continue;if(c.existedAtStart&&!deeperThanBaseline(c,baseline))continue;return c;}return null;}
   private static boolean deeperThanBaseline(Contact c,List baseline){for(int i=0;i<baseline.size();i++){Contact old=(Contact)baseline.get(i);if(sameSide(c,old))return c.penetration>old.penetration+COLLISION_EPSILON;}return true;}
   private static boolean sameSide(Contact a,Contact b){return a.hullIndex==b.hullIndex&&sameBounds(a.block,b.block)&&side(a)==side(b);}
   private static int side(Contact c){double ax=Math.abs(c.normalX),ay=Math.abs(c.normalY),az=Math.abs(c.normalZ);return ay>=ax&&ay>=az?(c.normalY<0?-2:2):ax>=az?(c.normalX<0?-1:1):(c.normalZ<0?-3:3);}
   private static boolean sameBounds(AxisAlignedBB a,AxisAlignedBB b){return a.minX==b.minX&&a.minY==b.minY&&a.minZ==b.minZ&&a.maxX==b.maxX&&a.maxY==b.maxY&&a.maxZ==b.maxZ;}
   private static ArrayList contacts(Transform t,List hulls,List blocks,double safe,double blocked,List baseline){ArrayList out=new ArrayList();for(int i=0;i<hulls.size();i++){MCH_BoundingBox h=(MCH_BoundingBox)hulls.get(i);h.updatePosition(t.x,t.y,t.z,t.yaw,t.pitch,t.roll);double bottom=h.boundingBox.minY;for(int j=0;j<blocks.size();j++){AxisAlignedBB b=(AxisAlignedBB)blocks.get(j);MCH_ObbCollision.Contact sat=h.getCollisionContact(b);if(sat==null)continue;boolean floor=sat.normalY>0.7D&&b.maxY<=h.nowPos.yCoord+COLLISION_EPSILON;Contact c=new Contact(i,(MCH_BoundingBox)h.copy(),b,sat,bottom,safe,blocked,false,floor);boolean old=false;if(baseline!=null)for(int k=0;k<baseline.size();k++)if(sameSide(c,(Contact)baseline.get(k))){old=true;break;}out.add(new Contact(i,(MCH_BoundingBox)h.copy(),b,sat,bottom,safe,blocked,old,floor));}}return out;}
   private static boolean segmentSafe(Transform a,Transform b,List h,List blocks,String name,Failure failure){ArrayList base=contacts(a,h,blocks,0,0,null);Sweep s=sweep(a,h,blocks,base,b.x-a.x,b.y-a.y,b.z-a.z,true);if(!s.blockedMovement)return true;failure.segment=name;failure.contact=s.contact;return false;}
   private static boolean hasSupport(Transform t,List hulls,List blocks,double top){for(int i=0;i<hulls.size();i++){MCH_BoundingBox h=(MCH_BoundingBox)hulls.get(i);h.updatePosition(t.x,t.y,t.z,t.yaw,t.pitch,t.roll);for(int j=0;j<blocks.size();j++){AxisAlignedBB b=(AxisAlignedBB)blocks.get(j);if(Math.abs(b.maxY-top)<=COLLISION_EPSILON&&h.boundingBox.maxX>b.minX+COLLISION_EPSILON&&h.boundingBox.minX<b.maxX-COLLISION_EPSILON&&h.boundingBox.maxZ>b.minZ+COLLISION_EPSILON&&h.boundingBox.minZ<b.maxZ-COLLISION_EPSILON)return true;}}return false;}
   private static final class Failure{String segment;Contact contact;Failure(String s,Contact c){segment=s;contact=c;}}
}
