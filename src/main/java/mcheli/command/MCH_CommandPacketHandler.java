package mcheli.command;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.plane.MCP_EntityPlane;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

public class MCH_CommandPacketHandler {

   public static void onPacketTitle(EntityPlayer player, ByteArrayDataInput data) {
      if(player != null && player.worldObj.isRemote) {
         MCH_PacketTitle req = new MCH_PacketTitle();
         req.readData(data);
         MCH_MOD.proxy.printChatMessage(req.chatComponent, req.showTime, req.position);
      }
   }

   public static void onPacketSave(EntityPlayer player, ByteArrayDataInput data) {
      if(player != null && !player.worldObj.isRemote) {
    	  System.out.println("here we go");
         MCH_PacketCommandSave req = new MCH_PacketCommandSave();
         req.readData(data);
         MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
         if(ac != null) {
        	 String[] split = req.str.split(" ");
        	 MCP_EntityPlane p = null;
        	 System.out.println("Received Command: " + split[0]);
        	 if(ac instanceof MCP_EntityPlane) {
        		 p = ((MCP_EntityPlane)ac);
        	 }
        	 if(split[0].equalsIgnoreCase("fuel")) {
        		 ac.setFuel(ac.getMaxFuel());
        	 }
			  if(split[0].equalsIgnoreCase("tgt")) {
				  System.out.println(req.str);
				  ac.target = Vec3.createVectorHelper(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
			  }else if(split[0].equalsIgnoreCase("base") && p != null){
				  p.base.x = p.posX;
				  p.base.y = p.posZ;
			  }else if(split[0].equalsIgnoreCase("hardpoint")) {
				  ac.weaponTest(split[1], split[2]);
			  }else if(split[0].equalsIgnoreCase("debug")) {
				  MCH_EntityAircraft.debug = !MCH_EntityAircraft.debug;
			  }else if(split[0].equalsIgnoreCase("team") && ac instanceof MCH_EntityVehicle){
				  ((MCH_EntityVehicle)ac).team = split[1];
			  }
         }
      }
   }
}
