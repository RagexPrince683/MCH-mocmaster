package mcheli.vehicle;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.aircraft.MCH_PacketPlayerControlBase;

public class MCH_PacketTurretPlayerControl extends MCH_PacketPlayerControlBase {

   public byte switchFold = -1;
   public int unhitchChainId = -1;
   public float weaponAimYaw;
   public float weaponAimPitch;


   public int getMessageID() {
      return 537002000;
   }

   public void readData(ByteArrayDataInput data) {
      super.readData(data);

      try {
         this.switchFold = data.readByte();
         this.unhitchChainId = data.readInt();
         if(this.useWeapon) {
            this.weaponAimYaw = data.readFloat();
            this.weaponAimPitch = data.readFloat();
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      super.writeData(dos);

      try {
         dos.writeByte(this.switchFold);
         dos.writeInt(this.unhitchChainId);
         if(this.useWeapon) {
            dos.writeFloat(this.weaponAimYaw);
            dos.writeFloat(this.weaponAimPitch);
         }
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }
}
