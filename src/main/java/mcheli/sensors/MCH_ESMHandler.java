package mcheli.sensors;

import mcheli.aircraft.MCH_EntityAircraft;

import java.util.ArrayList;

public class MCH_ESMHandler {
	public static MCH_ESMHandler instance;
	public ArrayList<MCH_ESMContact> contacts = new ArrayList<MCH_ESMContact>();
	
	public MCH_ESMHandler() {}
	
	public static void setInstance() {
		if(instance != null) {return;}
		instance = new MCH_ESMHandler();
	}
	
	public void onTick() {
		contacts.clear();
	}
	
	public static MCH_ESMHandler getInstance() {
		if(instance == null) {
			setInstance();
		}
		return instance;
	}
	
	public int getTargetID(MCH_EntityAircraft ac) {
		if(ac.radarTarget != null) {
			return ac.radarTarget.entityID;
		}else {
			return -1;
		}
	}
	
	public void addEmitter(MCH_EntityAircraft e) {
		contacts.add(new MCH_ESMContact(e.posX, e.posY, e.posZ, e.getAcInfo().radarPower, e.isAirBorne, getTargetID(e), false, e.getEntityId()));
	}
	
	private double getDistance(MCH_EntityAircraft ac, MCH_ESMContact c) {
		double dx = c.xPos - ac.posX;
		double dz = c.zPos - ac.posZ;
		return Math.sqrt(dx * dx + dz * dz);
	}
	
	public boolean canDetect(MCH_EntityAircraft ac, MCH_ESMContact c) {
		double sensitivity = ac.getAcInfo().esmPower;
		double distance = getDistance(ac, c);
		if(distance <= sensitivity * c.power && distance >= 1) { 
			return true;
		}
		return false;
	}
	
	public void getESMContacts(MCH_EntityAircraft ac) { //Get contacts that can be observed by a given aircraft
		
		for(MCH_ESMContact c : contacts) {
			double distance = getDistance(ac, c);
			if(canDetect(ac, c)) { 
				//ac.print("Contact " + )
				//MCH_PacketESM pkt = new MCH_PacketESM();
				
				MCH_PacketESM.sendToPlayer(ac.getFirstMountPlayer(), c);
			}
		}
	}
}
