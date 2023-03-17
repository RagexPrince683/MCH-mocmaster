package mcheli.sensors;

public class MCH_RadarContact{
	public double x,y,z;
	public int entityID;
	public float width,height,yaw;
	public int updated = 0;
	
	public MCH_RadarContact(double x, double y, double z, int id, float w, float h,float ya) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.entityID = id;
		this.width = w;
		this.height = h;
		this.yaw = ya;
	}

	public MCH_RadarContact(MCH_PacketRadar pkt) {
		this.x=pkt.x;
		this.y=pkt.y;
		this.z=pkt.z;
		this.entityID = pkt.entityID;
		this.width=pkt.width;
		this.height = pkt.height;
		this.yaw = pkt.yaw;
	}
}