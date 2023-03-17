package mcheli.aircraft;

import net.minecraft.util.Vec3;

import java.util.ArrayList;

public class MCH_Hardpoint {
	public float yaw;
	public float pitch;
	public boolean canUsePilot;
	public int seatID;
	public float defaultYaw;
	public float minYaw;
	public float maxYaw;
	public float minPitch;
	public float maxPitch;
	float x,y,z;
	ArrayList<WeaponData> weapons = new ArrayList<WeaponData>();
	int active;
	Vec3 pos;
	
	public MCH_Hardpoint(float x, float y, float z, float ya, float p, boolean c, int s, float dy, float my, float mx, float mn, float mpx) {
		this.x=x;
		this.y=y;
		this.z=z;
		this.yaw = ya;
		this.pitch = p;
		this.canUsePilot=c;
		this.seatID=s;
		this.defaultYaw = dy;
		this.minYaw = my;
		this.maxYaw = mx;
		this.minPitch = mn;
		this.maxPitch =mpx;
		this.pos = Vec3.createVectorHelper(x, y, z);
		active = -1;
	}

	public void addWeaponData(String type, int qty){
		weapons.add(new WeaponData(type, qty));
	}

	public int getMaxQty(String type){
		for(WeaponData data : weapons){
			if(type.equalsIgnoreCase(data.type)){
				return data.maxQty;
			}
		}
		return -1;
	}
	
	public Vec3 getPos() {
		return this.pos;
	}

}

class WeaponData{
	public String type;
	public int maxQty;

	public WeaponData(String t, int q){
		type=t;
		maxQty=q;
	}
}
