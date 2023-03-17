package mcheli.hud;

import mcheli.MCH_Vector2;
import mcheli.sensors.MCH_ESMContact;

import java.util.ArrayList;

public class MCH_HudItemRadar extends MCH_HudItem {

	private final String rot;
	private final String left;
	private final String top;
	private final String width;
	private final String height;
	private  boolean isEntityRadar = false;
	private  boolean isESM = false;

	public MCH_HudItemRadar(int fileLine, String mode, String rot, String left, String top, String width, String height) {
		super(fileLine);
		if(mode.equalsIgnoreCase("DrawEntityRadar")) {
			this.isEntityRadar = true;
		}else if(mode.equalsIgnoreCase("RWR")) {
			this.isESM = true;
		}
		//this.isEntityRadar = isEntityRadar;
		//this.isESM = isESM;
		this.rot = toFormula(rot);
		this.left = toFormula(left);
		this.top = toFormula(top);
		this.width = toFormula(width);
		this.height = toFormula(height);
	}

	@SuppressWarnings("unchecked")
	public void execute() {
		if(this.isESM) {
			this.drawESM((float)calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height));
			return;
		}
		if(this.isEntityRadar) {
			if(MCH_HudItem.EntityList != null && MCH_HudItem.EntityList.size() > 0) {
				this.drawEntityList(MCH_HudItem.EntityList, (float)calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height), true);
			}
		} else if(MCH_HudItem.EnemyList != null && MCH_HudItem.EnemyList.size() > 0) {
			if(ac.radarMode == 0) { //RWS
				if(ac.getAcInfo().radarMax >= 180) {
					this.drawEntityList(MCH_HudItem.EnemyList, (float)calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height), false);
				}else {
					this.drawEntityList(MCH_HudItem.EnemyList, (float)calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height), true);
				}
			}else if(ac.radarMode == 1) { //STT
				MCH_Vector2 vec = new MCH_Vector2(ac.radarTarget.x, ac.radarTarget.z);
				double scale = ac.getAcInfo().radarPower / calc(this.width);
				this.drawPrimary(vec, MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height), scale);
			}
		}
	}
	
	private void drawESM(float rot, double left, double top, double width, double height) {
		
		for(MCH_ESMContact contact : this.ac.ESMContacts) {
			MCH_Vector2 vec = new MCH_Vector2(contact.xPos, contact.zPos);
			double az = getBearingToVec2(vec) + rot;
			System.out.println("Bearing " + az);
			double dist = 24;
			double[] pos = getRelOffset(left,top,az,dist,0);
			int x = (int)pos[0];
			int y = (int)pos[1];
			double[] line = {x-5, y+2, x, y-5};
			this.drawLine(line, colorSetting);
			double[] line2 = {x, y-5, x+5, y+2};
			this.drawLine(line2, colorSetting);
			
			if(contact.airborne) {
				this.drawCenteredString("A", x, y, this.colorSetting);
			}else {
				this.drawCenteredString("S", x, y, this.colorSetting);
				double[] line3 = {x-5, y+8, x-5, y+2};
				double[] line4 = {x+5, y+8, x+5, y+2};
				this.drawLine(line3, colorSetting);
				this.drawLine(line4, colorSetting);
			}
		}
	}

	protected void drawEntityList(ArrayList<MCH_Vector2> src, float r, double left, double top, double w, double h, boolean bscope) {
		if(bscope) {
			drawBscope(src, r, left, top, w, h);
			return;
		}else {
			double width = 5;
			double height = 2;
			double scale = ac.getAcInfo().radarPower / w;
			for(MCH_Vector2 vec : src ) {
				double az = getBearingToVec2(vec) + r;
				double dist = getDistanceToVec2(vec) / scale;
				
				//System.out.println("Az " + az + " dist " + dist);
				double[] pos = getRelOffset(left,top,az,dist,0);
				//System.out.println("X " + pos[0] + " z " + pos[1]);
				drawRect(pos[0]-width/2, pos[1]-height/2, pos[0]+width/2, pos[1]+height/2, MCH_HudItem.colorSetting);
			}
			
		}
	}
	

	public double getBearingToVec2(MCH_Vector2 c) {
		double delta_x = c.x - this.ac.posX;
		double delta_z = ac.posZ - c.y;
		double angle = Math.atan2(delta_x, delta_z);
		angle = Math.toDegrees(angle);
		if(angle < 0) { angle += 360;}
		angle -= ac.getYaw();
		
		return angle;
	}
	
	public double getDistanceToVec2(MCH_Vector2 v) {
		double dx = v.x - ac.posX;
		double dz = v.y - ac.posZ;
		return Math.sqrt(dx * dx + dz * dz);
	}

	public static double[] getRelOffset(double x, double z, double angle, double offX, double offZ) {
		angle = Math.toRadians(angle);
		double xPrime = x * Math.cos(angle) + z * Math.sin(angle);
		double zPrime = -x  * Math.sin(angle) + z * Math.cos(angle);
		
		zPrime += offZ;
		xPrime += offX;

		double x2 = xPrime * Math.cos(angle) - zPrime * Math.sin(angle);
		double z2 = xPrime  * Math.sin(angle) + zPrime * Math.cos(angle);
		
		return new double[] {x2,z2};
	}
	
	protected void drawPrimary(MCH_Vector2 vec, double left, double top, double w, double h, double scale) {
		if(this.ac.radarTarget == null) {
			ac.radarMode = 0;
			return;
		}
		
		double az = left + w/2+ getBearingToVec2(vec)/360 * 64;
		double dist = (top + h) - getDistanceToVec2(vec) / scale;
		double angle = ac.radarTarget.yaw - ac.rotationYaw;
		
		double[] offset = getRelOffset(az,dist,angle,3,0);
		//System.out.println("Az " + az +  " dist " + dist + " offset " + offset[0] + " offset2 " + offset[1]);
		double[] o2 = getRelOffset(az,dist,angle,-3,0);
        this.drawLine(new double[]{offset[0],offset[1], o2[0],o2[1]}, MCH_HudItem.colorSetting, 3);
        
        offset = getRelOffset(az,dist,angle,0,-6);
        this.drawLine(new double[]{az,dist,offset[0],offset[1]}, MCH_HudItem.colorSetting, 3);
        
        offset = getRelOffset(az,dist,angle,3,3);
        o2 = getRelOffset(az,dist,angle,-3,3);
        this.drawLine(new double[]{offset[0],offset[1], az, dist,o2[0],o2[1]}, MCH_HudItem.colorSetting, 3);
        
        this.drawString(("B"+(int)ac.getTargetAz()), (int)MCH_HudItem.centerX + 210, (int)MCH_HudItem.centerY + 20, 0xFF28d448);
        this.drawString("R"+Math.round(ac.rangeToTarget()), (int)MCH_HudItem.centerX + 210, (int)MCH_HudItem.centerY + 30, 0xFF28d448);
        this.drawString("A"+(int)((ac.radarTarget.y)), (int)MCH_HudItem.centerX + 210, (int)MCH_HudItem.centerY + 40, 0xFF28d448);

	}
	
	protected void drawVec(MCH_Vector2 vec, double left, double top, double w, double h, double scale) {
		double raw_az = getBearingToVec2(vec);
		if(Math.abs(raw_az) > ac.getAcInfo().radarMax) {
			//System.out.println("Returning. Az = " + raw_az);
			return;
		}
		double az = left + w/2+ raw_az/360 * 64;
		double dist = (top + h) - getDistanceToVec2(vec) / scale;
		double width = 5;
		double height = 2;
		
		//System.out.println("Az = " + az);
		drawRect(az-width/2, dist-height/2, az+width/2, dist+height/2, MCH_HudItem.colorSetting);
	}
	
	protected void drawBscope(ArrayList<MCH_Vector2> src, float r, double left, double top, double w, double h) {
		double scale = ac.getAcInfo().radarPower / w;

		for(MCH_Vector2 vec : src) {
			drawVec(vec, left, top, w, h, scale);		    
		}
	}

}
