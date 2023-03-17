package mcheli.sensors;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketSTT  extends MCH_Packet{
	public int id;

	@Override
	public void readData(ByteArrayDataInput data) {
		try {
			this.id = data.readInt();
		} catch (Exception var3) {
			var3.printStackTrace();
		}
	}

	@Override
	public void writeData(DataOutputStream dos) {
		try {
			dos.writeInt(this.id);
		} catch (IOException var3) {
			var3.printStackTrace();
		}
	}

	@Override
	public int getMessageID() {
		return 546873974;
	}
	
	
}
