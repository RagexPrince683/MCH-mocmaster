package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_EntityRenderer;
import mcheli.wrapper.W_Network;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketNotifyClientSetting extends MCH_Packet {

   public boolean dismountAll = true;
   public boolean heliAutoThrottleDown;
   public boolean planeAutoThrottleDown;
   public boolean tankAutoThrottleDown;
   public boolean shaderSupport = false;


   public int getMessageID() {
      return 536875072;
   }

   public void readData(ByteArrayDataInput di) {
      try {
         boolean e = false;
         byte e1 = di.readByte();
         this.dismountAll = this.getBit(e1, 0);
         this.heliAutoThrottleDown = this.getBit(e1, 1);
         this.planeAutoThrottleDown = this.getBit(e1, 2);
         this.tankAutoThrottleDown = this.getBit(e1, 3);
         this.shaderSupport = this.getBit(e1, 4);
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         byte e = 0;
         byte e1 = this.setBit(e, 0, this.dismountAll);
         e1 = this.setBit(e1, 1, this.heliAutoThrottleDown);
         e1 = this.setBit(e1, 2, this.planeAutoThrottleDown);
         e1 = this.setBit(e1, 3, this.tankAutoThrottleDown);
         e1 = this.setBit(e1, 4, this.shaderSupport);
         dos.writeByte(e1);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public static void send() {
      MCH_PacketNotifyClientSetting s = new MCH_PacketNotifyClientSetting();
      MCH_Config var10001 = MCH_MOD.config;
      s.dismountAll = MCH_Config.DismountAll.prmBool;
      var10001 = MCH_MOD.config;
      s.heliAutoThrottleDown = MCH_Config.AutoThrottleDownHeli.prmBool;
      var10001 = MCH_MOD.config;
      s.planeAutoThrottleDown = MCH_Config.AutoThrottleDownPlane.prmBool;
      var10001 = MCH_MOD.config;
      s.tankAutoThrottleDown = MCH_Config.AutoThrottleDownTank.prmBool;
      s.shaderSupport = W_EntityRenderer.isShaderSupport();
      W_Network.sendToServer(s);
   }
}
