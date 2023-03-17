/*    */ package mcheli.command;
/*    */ 
/*    */

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;

import java.io.DataOutputStream;
import java.io.IOException;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MCH_PacketCommandSave
/*    */   extends MCH_Packet
/*    */ {
/* 18 */   public String str = "";
/*    */ 
/*    */ 
/*    */ 
/*    */   
/* 23 */   public int getMessageID() { return 536873729; }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public void readData(ByteArrayDataInput data) {
/*    */     try {
/* 32 */       this.str = data.readUTF();
/*    */     }
/* 34 */     catch (Exception e) {
/*    */       
/* 36 */       e.printStackTrace();
/*    */     } 
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public void writeData(DataOutputStream dos) {
/*    */     try {
/* 45 */       dos.writeUTF(this.str);
/*    */     }
/* 47 */     catch (IOException e) {
/*    */       
/* 49 */       e.printStackTrace();
/*    */     } 
/*    */   }
/*    */ 
/*    */   
/*    */   public static void send(String cmd) {
/* 55 */     MCH_PacketCommandSave s = new MCH_PacketCommandSave();
/* 56 */     s.str = cmd;
			 //System.out.println("u wot m8 " + s.str);
/* 57 */     W_Network.sendToServer(s);
/*    */   }
/*    */ }


/* Location:              C:\Users\tani\Desktop\!\mcheli\command\MCH_PacketCommandSave.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.0.7
 */