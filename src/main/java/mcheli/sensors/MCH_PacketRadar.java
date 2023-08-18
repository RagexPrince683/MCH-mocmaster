package mcheli.sensors;

import com.google.common.io.ByteArrayDataInput;
import cuchaz.ships.EntityShip;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketRadar extends MCH_Packet{
	public double x,y,z;
	public int entityID;
	public float width,height,yaw;
	public boolean isShip = false;

	public static void send(Entity target) {
		if(target != null) {
			MCH_PacketRadar s = new MCH_PacketRadar();
			s.x = target.posX;
			s.y = target.posY;
			s.z = target.posZ;
			s.entityID = target.getEntityId();
			s.width = target.width;
			s.height = target.height;
			s.yaw = target.rotationYaw;
			s.isShip = (target instanceof EntityShip);
			W_Network.sendToServer(s);
		}
	}

	public static void sendToPlayer(EntityPlayer entity, Entity target) {
		//System.out.println("Sending!");
		if(entity instanceof EntityPlayerMP) {
			if(target != null) {
				MCH_PacketRadar s = new MCH_PacketRadar();
				s.x = target.posX;
				s.y = target.posY;
				s.z = target.posZ;
				s.entityID = target.getEntityId();
				s.width = target.width;
				s.height = target.height;
				s.yaw = target.rotationYaw;
				s.isShip = (target instanceof EntityShip);
				W_Network.sendToPlayer(s, entity);
				//System.out.println("Packet sent! " + s.x);
			}
			//W_Network.sendToPlayer(s, entity);
		}
	}

	@Override
	public void readData(ByteArrayDataInput data) {
		//System.out.println("Reading data!");
		try {
			this.x = data.readDouble();
			this.y = data.readDouble();
			this.z = data.readDouble();
			this.entityID = data.readInt();
			this.width = data.readFloat();
			this.height = data.readFloat();
			this.yaw = data.readFloat();
			this.isShip = data.readBoolean();
		} catch (Exception var3) {
			var3.printStackTrace();
		}
		
	}

	@Override
	public void writeData(DataOutputStream dos) {
		try {
			dos.writeDouble(this.x);
			dos.writeDouble(this.y);
			dos.writeDouble(this.z);
			dos.writeInt(this.entityID);
			dos.writeFloat(this.width);
			dos.writeFloat(this.height);
			dos.writeFloat(yaw);
			dos.writeBoolean(isShip);
		} catch (IOException var3) {
			var3.printStackTrace();
		}

	}

	@Override
	public int getMessageID() {
		return 546873984;
	}

}
