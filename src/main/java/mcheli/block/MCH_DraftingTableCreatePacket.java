package mcheli.block;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mcheli.MCH_Lib;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_Network;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import mcheli.aircraft.MCH_VehiclePaint;
import java.util.ArrayList;

public class MCH_DraftingTableCreatePacket extends MCH_Packet {

   public Item outputItem;
   public Map map = new HashMap();
   public int paintColor = 0xFFFFFF;
   public int paintOpacity;
   public boolean useAsDefault;
   public ArrayList paintParts = new ArrayList();


   public int getMessageID() {
      return 537395216;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.outputItem = W_Item.getItemByName(data.readUTF());
         byte e = data.readByte();

         for(int i = 0; i < e; ++i) {
            String s = data.readUTF();
            byte num = data.readByte();
            Item item = W_Item.getItemByName(s);
            if(item != null) {
               this.map.put(item, Integer.valueOf(0 + num));
            }
         }
         this.paintColor = data.readInt();
         this.paintOpacity = data.readUnsignedByte();
         this.useAsDefault = data.readBoolean();
         int partCount = data.readUnsignedByte();
         for(int i = 0; i < partCount; ++i) this.paintParts.add(data.readUTF());
      } catch (Exception exception) {
         ;
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeUTF(this.getItemName(this.outputItem));
         dos.writeByte(this.map.size());
         Iterator e = this.map.keySet().iterator();

         while(e.hasNext()) {
            Item key = (Item)e.next();
            dos.writeUTF(this.getItemName(key));
            dos.writeByte(((Integer)this.map.get(key)).byteValue());
         }
         dos.writeInt(this.paintColor);
         dos.writeByte(this.paintOpacity);
         dos.writeBoolean(this.useAsDefault);
         dos.writeByte(Math.min(255, this.paintParts.size()));
         for(int i = 0; i < this.paintParts.size() && i < 255; ++i) dos.writeUTF((String)this.paintParts.get(i));
      } catch (IOException oException) {
         oException.printStackTrace();
      }

   }

   private String getItemName(Item item) {
      return W_Item.getNameForItem(item);
   }

   public static void send(IRecipe recipe) {
      send(recipe, new MCH_VehiclePaint.Design(0xFFFFFF, 0, new ArrayList()), false);
   }

   public static void send(IRecipe recipe, MCH_VehiclePaint.Design design, boolean useAsDefault) {
      if(recipe != null) {
         MCH_DraftingTableCreatePacket s = new MCH_DraftingTableCreatePacket();
         s.outputItem = recipe.getRecipeOutput() != null?recipe.getRecipeOutput().getItem():null;
         if(s.outputItem != null) {
            // Do not send recipe ingredients from the client.
            // Ore dictionary recipes cannot be represented safely as Map<Item, Integer>.
            // The server should resolve and validate the recipe from the output item.
            s.map = new HashMap();
            if(design != null) {
               s.paintColor = design.color;
               s.paintOpacity = design.opacity;
               s.paintParts.addAll(design.parts);
            }
            s.useAsDefault = useAsDefault;
            W_Network.sendToServer(s);
         }

         MCH_Lib.DbgLog(true, "MCH_DraftingTableCreatePacket.send outputItem = " + s.outputItem, new Object[0]);
      }

   }
}
