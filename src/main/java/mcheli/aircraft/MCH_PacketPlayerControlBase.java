package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class MCH_PacketPlayerControlBase extends MCH_Packet {

   public byte isUnmount = 0;
   public byte switchMode = -1;
   public byte switchCameraMode = 0;
   public byte switchWeapon = -1;
   public byte useFlareType = 0;
   public boolean useWeapon = false;
   public int useWeaponOption1 = 0;
   public int useWeaponOption2 = 0;
   public double useWeaponPosX = 0.0D;
   public double useWeaponPosY = 0.0D;
   public double useWeaponPosZ = 0.0D;
   public boolean throttleUp = false;
   public boolean throttleDown = false;
   public boolean moveLeft = false;
   public boolean moveRight = false;
   public boolean moveUp = false;
   public boolean moveDown = false;
   public boolean openGui;
   public byte switchHatch = 0;
   public byte switchFreeLook = 0;
   public byte switchGear = 0;
   public boolean ejectSeat = false;
   public byte putDownRack = 0;
   public boolean switchSearchLight = false;
   public boolean useBrake = false;
   public byte currentHardpoint;
   public int radarMode;


   public void readData(ByteArrayDataInput data) {
      try {
         short e = data.readShort();
         this.useWeapon = this.getBit(e, 0);
         this.throttleUp = this.getBit(e, 1);
         this.throttleDown = this.getBit(e, 2);
         this.moveLeft = this.getBit(e, 3);
         this.moveRight = this.getBit(e, 4);
         this.switchSearchLight = this.getBit(e, 5);
         this.ejectSeat = this.getBit(e, 6);
         this.openGui = this.getBit(e, 7);
         this.useBrake = this.getBit(e, 8);
         e = (short)data.readByte();
         this.putDownRack = (byte)(e >> 6 & 3);
         this.isUnmount = (byte)(e >> 4 & 3);
         this.useFlareType = (byte)(e >> 0 & 15);
         this.switchMode = data.readByte();
         this.switchWeapon = data.readByte();
         if(this.useWeapon) {
            this.useWeaponOption1 = data.readInt();
            this.useWeaponOption2 = data.readInt();
            this.useWeaponPosX = data.readDouble();
            this.useWeaponPosY = data.readDouble();
            this.useWeaponPosZ = data.readDouble();
         }

         e = (short)data.readByte();
         this.switchCameraMode = (byte)(e >> 6 & 3);
         this.switchHatch = (byte)(e >> 4 & 3);
         this.switchFreeLook = (byte)(e >> 2 & 3);
         this.switchGear = (byte)(e >> 0 & 3);
         this.currentHardpoint = data.readByte();
         this.moveUp = data.readBoolean();
         this.moveDown = data.readBoolean();
         this.radarMode = data.readInt();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         byte e = 0;
         short e1 = this.setBit(e, 0, this.useWeapon);
         e1 = this.setBit(e1, 1, this.throttleUp);
         e1 = this.setBit(e1, 2, this.throttleDown);
         e1 = this.setBit(e1, 3, this.moveLeft);
         e1 = this.setBit(e1, 4, this.moveRight);
         e1 = this.setBit(e1, 5, this.switchSearchLight);
         e1 = this.setBit(e1, 6, this.ejectSeat);
         e1 = this.setBit(e1, 7, this.openGui);
         e1 = this.setBit(e1, 8, this.useBrake);
         dos.writeShort(e1);
         e1 = (short)((byte)((this.putDownRack & 3) << 6 | (this.isUnmount & 3) << 4 | this.useFlareType & 15));
         dos.writeByte(e1);
         dos.writeByte(this.switchMode);
         dos.writeByte(this.switchWeapon);
         if(this.useWeapon) {
            dos.writeInt(this.useWeaponOption1);
            dos.writeInt(this.useWeaponOption2);
            dos.writeDouble(this.useWeaponPosX);
            dos.writeDouble(this.useWeaponPosY);
            dos.writeDouble(this.useWeaponPosZ);
         }

         e1 = (short)((byte)((this.switchCameraMode & 3) << 6 | (this.switchHatch & 3) << 4 | (this.switchFreeLook & 3) << 2 | (this.switchGear & 3) << 0));
         dos.writeByte(e1);
         dos.writeByte(currentHardpoint);
         dos.writeBoolean(this.moveUp);
         dos.writeBoolean(this.moveDown);
         dos.writeInt(this.radarMode);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }
}
