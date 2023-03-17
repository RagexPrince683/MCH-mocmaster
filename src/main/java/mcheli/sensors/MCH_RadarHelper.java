package mcheli.sensors;

//import cuchaz.ships.EntityShip;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;

public class MCH_RadarHelper {
	public static final int RADIUS = 12742 * 2; //Radius of the earth for 1:500 earth map, in meters
	
	//Horizontal dist between two Vec3s 
	public static double getDist(Vec3 pos1, Vec3 pos2) {
		double x = pos2.xCoord - pos1.xCoord;
		double z = pos2.zCoord - pos1.zCoord;
		return Math.sqrt(x * x + z * z);
	}
	
	//Distance from pos to horizon, in meters
	public static double getHorizonDist(Vec3 pos) {
		double height = pos.yCoord - 62; //Horizon is calculated from surface, we'll approximate surface w/ sea level.
		return Math.sqrt(2 * height * RADIUS + Math.pow(height, 2));
		//Formula source: https://en.wikipedia.org/wiki/Radar_horizon#Definition
	}
	
	public static boolean isTargetVisible(Vec3 pos1, Vec3 pos2) {
		double dist = getDist(pos1, pos2);
		double horizonDist = getHorizonDist(pos1);
		//System.out.println("Dist: " + dist + " Horizon: " + horizonDist);
		double hT = pos2.yCoord - 62; //Adjust for SL
		return hT > Math.pow(dist - horizonDist, 2)/(2*RADIUS);
	}
	
	public static boolean isTargetVisible(Entity e1, Entity e2, int radarPower) {
		Vec3 pos1 = Vec3.createVectorHelper(e1.posX, e1.posY, e1.posZ);
		
		Vec3 pos2;
		//if(e2 instanceof EntityShip) {
		//	pos2 = Vec3.createVectorHelper(e2.posX, e2.boundingBox.maxY, e2.posZ);

		//}else {
			pos2 = Vec3.createVectorHelper(e2.posX, e2.posY, e2.posZ);
		//}
		if(isTargetVisible(pos1,pos2)) {
			return checkStealth(e2, radarPower, getDist(pos1, pos2));
		}else {
			//System.out.println("Horizon blocked");
			return false;
		}
	}
	
	public static boolean checkStealth(Entity e2, int radarPower, double d) {
		if(!(e2 instanceof MCH_EntityAircraft)) {return true;}
		MCH_EntityAircraft ac = (MCH_EntityAircraft)e2;
		double rcs = ac.getAcInfo().stealth;
		//System.out.println("RCS = " + rcs);
		if(rcs >= 5) {return true;}
		return getRadarRange(radarPower, rcs) >= d;
	}
	
	public static double getConstant(int radarpower) {
		return Math.pow(Math.pow(radarpower, 4)/5, 0.25);
	}
	
	public static long getRadarRange(int radarpower, double rcs) {
	   double constant = getConstant(radarpower);
	   long output = Math.round(constant*Math.pow(rcs, 0.25));
	//   System.out.println("Output = " + output);
	   return Math.min(output, radarpower);
	}
}
