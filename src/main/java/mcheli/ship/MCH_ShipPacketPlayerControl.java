package mcheli.ship;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.aircraft.MCH_PacketPlayerControlBase;

public class MCH_ShipPacketPlayerControl extends MCH_PacketPlayerControlBase {

    public byte switchVtol = -1;
    public boolean submarineAscend = false;
    public boolean submarineDescend = false;


    public int getMessageID() {
        return 536903698;
    }

    public void readData(ByteArrayDataInput data) {
        super.readData(data);

        try {
            this.switchVtol = data.readByte();
            byte submarineControl = data.readByte();
            this.submarineAscend = this.getBit(submarineControl, 0);
            this.submarineDescend = this.getBit(submarineControl, 1);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    public void writeData(DataOutputStream dos) {
        super.writeData(dos);

        try {
            dos.writeByte(this.switchVtol);
            byte submarineControl = 0;
            submarineControl = this.setBit(submarineControl, 0, this.submarineAscend);
            submarineControl = this.setBit(submarineControl, 1, this.submarineDescend);
            dos.writeByte(submarineControl);
        } catch (IOException var3) {
            var3.printStackTrace();
        }

    }
}
