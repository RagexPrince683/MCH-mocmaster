package mcheli.aircraft;

import mcheli.MCH_Lib;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class MCH_BoundingBox {

	public final AxisAlignedBB boundingBox;
	public final AxisAlignedBB backupBoundingBox;
	public final double offsetX;
	public final double offsetY;
	public final double offsetZ;
	public final float width;
	public final float height;
	public Vec3 rotatedOffset;
	public Vec3 nowPos;
	public Vec3 prevPos;
	public final float damegeFactor;
	public final String type;
	public final int armor;

	public MCH_BoundingBox(double x, double y, double z, float w, float h, float df) {
		this.offsetX = x;
		this.offsetY = y;
		this.offsetZ = z;
		this.width = w;
		this.height = h;
		this.damegeFactor = df;
		this.boundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
		this.backupBoundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
		this.nowPos = Vec3.createVectorHelper(x, y, z);
		this.prevPos = Vec3.createVectorHelper(x, y, z);
		this.updatePosition(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
		type = null;
		armor = 0;
	}

	public MCH_BoundingBox(double x, double y, double z, float w, float h, float df, int mm) {
		this.offsetX = x;
		this.offsetY = y;
		this.offsetZ = z;
		this.width = w;
		this.height = h;
		this.damegeFactor = df;
		this.boundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
		this.backupBoundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
		this.nowPos = Vec3.createVectorHelper(x, y, z);
		this.prevPos = Vec3.createVectorHelper(x, y, z);
		this.updatePosition(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
		type = null;
		this.armor = mm;
		//System.out.println("Armor = " + this.armor);
	}
	
	public int getArmor() {
		System.out.println("Getting armor " + this.armor);
		return this.armor;
	}

	public MCH_BoundingBox(double x, double y, double z, float w, float h, float df, String t) {
		this.offsetX = x;
		this.offsetY = y;
		this.offsetZ = z;
		this.width = w;
		this.height = h;
		this.damegeFactor = df;
		this.boundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
		this.backupBoundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
		this.nowPos = Vec3.createVectorHelper(x, y, z);
		this.prevPos = Vec3.createVectorHelper(x, y, z);
		this.updatePosition(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
		this.type = t;
		armor = 0;
	}

	public MCH_BoundingBox copy() {
		return new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor, this.armor);
	}

	public void updatePosition(double posX, double posY, double posZ, float yaw, float pitch, float roll) {
		Vec3 v = Vec3.createVectorHelper(this.offsetX, this.offsetY, this.offsetZ);
		this.rotatedOffset = MCH_Lib.RotVec3(v, -yaw, -pitch, -roll);
		float w = this.width;
		float h = this.height;
		double x = posX + this.rotatedOffset.xCoord;
		double y = posY + this.rotatedOffset.yCoord;
		double z = posZ + this.rotatedOffset.zCoord;
		this.prevPos.xCoord = this.nowPos.xCoord;
		this.prevPos.yCoord = this.nowPos.yCoord;
		this.prevPos.zCoord = this.nowPos.zCoord;
		this.nowPos.xCoord = x;
		this.nowPos.yCoord = y;
		this.nowPos.zCoord = z;
		this.backupBoundingBox.setBB(this.boundingBox);
		this.boundingBox.setBounds(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
	}
}
