package mcheli.tank;

import com.google.common.io.ByteArrayDataInput;
import io.netty.buffer.ByteBuf;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;

/** Server-authored detached turret transform and velocity snapshot. */
public class MCH_PacketTurretPop extends MCH_Packet {
   public int entityId, age;
   public boolean landed;
   public double x, y, z, mx, my, mz;
   public float yaw, pitch, roll, ay, ap, ar, frozenYaw, frozenPitch;

   public int getMessageID() { return 268439650; }

   public void readData(ByteArrayDataInput in) {
      try {
         this.entityId=in.readInt(); this.landed=in.readBoolean(); this.age=in.readInt();
         this.x=in.readDouble(); this.y=in.readDouble(); this.z=in.readDouble(); this.mx=in.readDouble(); this.my=in.readDouble(); this.mz=in.readDouble();
         this.yaw=in.readFloat(); this.pitch=in.readFloat(); this.roll=in.readFloat(); this.ay=in.readFloat(); this.ap=in.readFloat(); this.ar=in.readFloat();
         this.frozenYaw=in.readFloat(); this.frozenPitch=in.readFloat();
      } catch(Exception e) { e.printStackTrace(); }
   }

   public void writeData(DataOutputStream out) {
      try {
         out.writeInt(this.entityId); out.writeBoolean(this.landed); out.writeInt(this.age);
         out.writeDouble(this.x); out.writeDouble(this.y); out.writeDouble(this.z); out.writeDouble(this.mx); out.writeDouble(this.my); out.writeDouble(this.mz);
         out.writeFloat(this.yaw); out.writeFloat(this.pitch); out.writeFloat(this.roll); out.writeFloat(this.ay); out.writeFloat(this.ap); out.writeFloat(this.ar);
         out.writeFloat(this.frozenYaw); out.writeFloat(this.frozenPitch);
      } catch(IOException e) { e.printStackTrace(); }
   }

   private static MCH_PacketTurretPop from(MCH_EntityTank tank) {
      MCH_PacketTurretPop p=new MCH_PacketTurretPop(); p.entityId=W_Entity.getEntityId(tank); p.landed=tank.turretPopLanded; p.age=tank.turretPopAge;
      p.x=tank.turretPopX; p.y=tank.turretPopY; p.z=tank.turretPopZ; p.mx=tank.turretPopMotionX; p.my=tank.turretPopMotionY; p.mz=tank.turretPopMotionZ;
      p.yaw=tank.turretPopYaw; p.pitch=tank.turretPopPitch; p.roll=tank.turretPopRoll; p.ay=tank.turretPopAngularYaw; p.ap=tank.turretPopAngularPitch; p.ar=tank.turretPopAngularRoll;
      p.frozenYaw=tank.turretPopFrozenYaw; p.frozenPitch=tank.turretPopFrozenPitch; return p;
   }

   public static void send(MCH_EntityTank tank) { W_Network.sendToAllAround(from(tank), tank, 256.0D); }

   public static void writeState(ByteBuf out, MCH_EntityTank tank) {
      out.writeBoolean(tank.turretPopStarted); if(!tank.turretPopStarted) return;
      MCH_PacketTurretPop p=from(tank); out.writeBoolean(p.landed); out.writeInt(p.age);
      out.writeDouble(p.x); out.writeDouble(p.y); out.writeDouble(p.z); out.writeDouble(p.mx); out.writeDouble(p.my); out.writeDouble(p.mz);
      out.writeFloat(p.yaw); out.writeFloat(p.pitch); out.writeFloat(p.roll); out.writeFloat(p.ay); out.writeFloat(p.ap); out.writeFloat(p.ar); out.writeFloat(tank.turretPopFrozenYaw); out.writeFloat(tank.turretPopFrozenPitch);
   }

   public static void readState(ByteBuf in, MCH_EntityTank tank) {
      if(!in.readBoolean()) return; MCH_PacketTurretPop p=new MCH_PacketTurretPop(); p.landed=in.readBoolean(); p.age=in.readInt();
      p.x=in.readDouble(); p.y=in.readDouble(); p.z=in.readDouble(); p.mx=in.readDouble(); p.my=in.readDouble(); p.mz=in.readDouble();
      p.yaw=in.readFloat(); p.pitch=in.readFloat(); p.roll=in.readFloat(); p.ay=in.readFloat(); p.ap=in.readFloat(); p.ar=in.readFloat(); p.frozenYaw=in.readFloat(); p.frozenPitch=in.readFloat(); tank.applyTurretPopState(p);
   }
}
