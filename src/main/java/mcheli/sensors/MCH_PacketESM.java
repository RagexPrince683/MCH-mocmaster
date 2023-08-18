package mcheli.sensors;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketESM extends MCH_Packet{
	public double x,y,z;
	public int tgtID;
	public boolean airborne;
	public int contactID;
	
	
	public static void sendToPlayer(EntityPlayer entity, MCH_ESMContact c) {
		//System.out.println("Sending!");
		if(entity instanceof EntityPlayerMP) {
			if(c != null) {
				MCH_PacketESM s = new MCH_PacketESM();
				s.x = c.xPos;
				s.y = c.yPos;
				s.z = c.zPos;
				s.tgtID = c.tgtEntityID;
				s.airborne = c.airborne;
				s.contactID = c.contactID;
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
			this.tgtID = data.readInt();
			this.airborne = data.readBoolean();
			this.contactID = data.readInt();
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
			dos.writeInt(this.tgtID);
			dos.writeBoolean(airborne);
			dos.writeInt(contactID);
		} catch (IOException var3) {
			var3.printStackTrace();
		}

	}

	@Override
	public int getMessageID() {
		return 546873982;
	}
}
