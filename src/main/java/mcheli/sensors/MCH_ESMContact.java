package mcheli.sensors;

public class MCH_ESMContact {
	public double xPos;
	public double yPos;
	public double zPos;
	//public double bearing;
	public double power;
	public int tgtEntityID;
	public boolean launched;
	public boolean airborne;
	public int contactID;
	public int updated;
	
	
	public MCH_ESMContact(double x, double y, double z, double p, boolean n) {
		xPos = x;
		yPos = y;
		zPos = z;
		power = p;
		airborne = n;
	}
	
	public MCH_ESMContact(double x, double y, double z, double p, boolean n, int t, boolean l, int i) {
		xPos = x;
		yPos = y;
		zPos = z;
		power = p;
		airborne = n;
		launched = l;
		tgtEntityID = t;
		contactID = i;
	}

	public MCH_ESMContact(MCH_PacketESM pkt) {
		xPos = pkt.x;
		yPos = pkt.y;
		zPos = pkt.z;
		airborne = pkt.airborne;
		tgtEntityID = pkt.tgtID;
		contactID = pkt.contactID;
	}
}
