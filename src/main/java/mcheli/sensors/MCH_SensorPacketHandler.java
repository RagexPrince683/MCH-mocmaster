package mcheli.sensors;

import com.google.common.io.ByteArrayDataInput;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;

public class MCH_SensorPacketHandler {
	
	public static void onPacketRadar(EntityPlayer entityPlayer, ByteArrayDataInput data) {
		if(entityPlayer.ridingEntity instanceof MCH_EntityAircraft) {
			MCH_EntityAircraft ac = (MCH_EntityAircraft)entityPlayer.ridingEntity;
			MCH_PacketRadar pkt = new MCH_PacketRadar();
			pkt.readData(data);
			MCH_RadarContact contact = new MCH_RadarContact(pkt);
			
			if(pkt.isShip) {
				updateContacts(ac.surfaceContacts, contact, ac);
			}else {
				updateContacts(ac.contacts, contact, ac);
			}
		}
	}
	
	public static void onPacketESM(EntityPlayer entityPlayer, ByteArrayDataInput data) {
		if(entityPlayer.ridingEntity instanceof MCH_EntityAircraft) {
			MCH_EntityAircraft ac = (MCH_EntityAircraft)entityPlayer.ridingEntity;
			MCH_PacketESM pkt = new MCH_PacketESM();
			pkt.readData(data);
			MCH_ESMContact contact = new MCH_ESMContact(pkt);
			for(MCH_ESMContact c : ac.ESMContacts) {
				if(c.contactID == contact.contactID) {
					c.xPos = contact.xPos;
					c.zPos = contact.zPos;
					c.updated=0;
					return;
				}
			}
			ac.ESMContacts.add(contact);
			//ac.print("Adding ESM contact");
		}
	}
	
	public static void updateContacts(ArrayList<MCH_RadarContact> list, MCH_RadarContact contact, MCH_EntityAircraft ac) {
		boolean update = false;
		
		for(MCH_RadarContact c : list) {
			if(c.entityID == contact.entityID) {
				c.x = contact.x;
				c.y = contact.y;
				c.z = contact.z;
				c.yaw = contact.yaw;
				c.updated = 0;
				update = true;
			}
		}
		if(update == false) {
			contact.updated = 0;
			list.add(contact);
		}
	}

	public static void onPacketSTT(EntityPlayer entityPlayer, ByteArrayDataInput data) {
		MCH_PacketSTT pkt = new MCH_PacketSTT();
		pkt.readData(data);
		
		MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(entityPlayer);
		if(ac == null) {return;}
		
		if(pkt.id == 0) {
			ac.radarMode = 0;
			ac.radarTarget = null;
		}else {
			for(MCH_RadarContact c : ac.contacts) {
				if(c.entityID == pkt.id) {
					ac.radarMode = 1;
					ac.radarTarget = c;
				}
			}
		}
		
	}
}
