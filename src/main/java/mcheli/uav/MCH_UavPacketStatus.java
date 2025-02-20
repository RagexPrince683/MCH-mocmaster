package mcheli.uav;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.MCH_Packet;


public class MCH_UavPacketStatus
        /*    */   extends MCH_Packet {
    /* 10 */   public byte posUavX = 0;
    /* 11 */   public byte posUavY = 0;
    /* 12 */   public byte posUavZ = 0;
    /*    */
    /*    */   public boolean continueControl = false;
    /*    */
    /*    */   public int getMessageID() {
        /* 17 */     return 537133072;
        /*    */   }
    /*    */
    /*    */   public void readData(ByteArrayDataInput data) {
        /*    */     try {
            /* 22 */       this.posUavX = data.readByte();
            /* 23 */       this.posUavY = data.readByte();
            /* 24 */       this.posUavZ = data.readByte();
            /* 25 */       this.continueControl = (data.readByte() != 0);
            /* 26 */     } catch (Exception var3) {
            /* 27 */       var3.printStackTrace();
            /*    */     }
        /*    */   }
    /*    */
    /*    */
    /*    */   public void writeData(DataOutputStream dos) {
        /*    */     try {
            /* 34 */       dos.writeByte(this.posUavX);
            /* 35 */       dos.writeByte(this.posUavY);
            /* 36 */       dos.writeByte(this.posUavZ);
            /* 37 */       dos.writeByte(this.continueControl ? 1 : 0);
            /* 38 */     } catch (IOException var3) {
            /* 39 */       var3.printStackTrace();
            /*    */     }
        /*    */   }
    /*    */ }