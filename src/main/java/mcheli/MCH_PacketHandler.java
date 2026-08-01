package mcheli;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_CommonPacketHandler;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehiclePacketHandler;
import mcheli.aircraft.MCH_PacketBaseVehicleLocation;
import mcheli.block.MCH_DraftingTablePacketHandler;
import mcheli.command.MCH_CommandPacketHandler;
import mcheli.gltd.MCH_GLTDPacketHandler;
import mcheli.helicopter.MCH_HeliPacketHandler;
import mcheli.lweapon.MCH_LightWeaponPacketHandler;
import mcheli.multiplay.MCH_MultiplayPacketHandler;
import mcheli.plane.MCP_PlanePacketHandler;
//import mcheli.sensors.Mk1Eyeball;
import mcheli.ship.MCH_ShipPacketHandler;
import mcheli.tank.MCH_TankPacketHandler;
import mcheli.tool.MCH_ToolPacketHandler;
import mcheli.uav.MCH_UavPacketHandler;
import mcheli.vehicle.MCH_TurretPacketHandler;
import mcheli.wrapper.W_PacketHandler;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_PacketHandler extends W_PacketHandler {



   public void onPacket(ByteArrayDataInput data, EntityPlayer entityPlayer) {
      int msgid = this.getMessageId(data);
      switch(msgid) {
      case 0:
      default:
         MCH_Lib.DbgLog(entityPlayer.worldObj, "MCH_PacketHandler.onPacket invalid MSGID=0x%X(%d)", new Object[]{Integer.valueOf(msgid), Integer.valueOf(msgid)});
         break;
      case 268437520:
         MCH_CommonPacketHandler.onPacketEffectExplosion(entityPlayer, data);
         break;
      case 268437568:
         MCH_CommonPacketHandler.onPacketNotifyServerSettings(entityPlayer, data);
         break;
      case 268437761:
         MCH_MultiplayPacketHandler.onPacket_NotifySpotedEntity(entityPlayer, data);
         break;
      case 268437762:
         MCH_MultiplayPacketHandler.onPacket_NotifyMarkPoint(entityPlayer, data);
         break;
      case 268438032:
         MCH_MultiplayPacketHandler.onPacket_IndClient(entityPlayer, data);
         break;
      case 268438272:
         MCH_CommandPacketHandler.onPacketTitle(entityPlayer, data);
         break;
      case 268439569:
         MCH_BaseVehiclePacketHandler.onPacketSeatListResponse(entityPlayer, data);
         break;
      case 268439600:
         MCH_BaseVehiclePacketHandler.onPacketNotifyTVMissileEntity(entityPlayer, data);
         break;
      case 268439601:
         MCH_BaseVehiclePacketHandler.onPacketNotifyWeaponID(entityPlayer, data);
         break;
      case 268439602:
         MCH_BaseVehiclePacketHandler.onPacketNotifyHitBullet(entityPlayer, data);
         break;
      case 268439604:
         MCH_BaseVehiclePacketHandler.onPacketNotifyAmmoNum(entityPlayer, data);
         break;
      case 268439632:
         MCH_BaseVehiclePacketHandler.onPacketOnMountEntity(entityPlayer, data);
         break;
      case 268439649:
         MCH_BaseVehiclePacketHandler.onPacketStatusResponse(entityPlayer, data);
         break;
      case 268439650:
         MCH_TankPacketHandler.onPacket_TurretPop(entityPlayer, data);
         break;
      case 536872992:
         MCH_CommonPacketHandler.onPacketIndOpenScreen(entityPlayer, data);
         break;
      case 536873088:
         MCH_MultiplayPacketHandler.onPacket_Command(entityPlayer, data);
         break;
      case 536873216:
         MCH_ToolPacketHandler.onPacket_IndSpotEntity(entityPlayer, data);
         break;
      case 536873472:
         MCH_MultiplayPacketHandler.onPacket_LargeData(entityPlayer, data);
         break;
      case 536873473:
         MCH_MultiplayPacketHandler.onPacket_ModList(entityPlayer, data);
         break;
      case 536873729:
         MCH_CommandPacketHandler.onPacketSave(entityPlayer, data);
         break;
      case 536873984:
         MCH_CommonPacketHandler.onPacketNotifyLock(entityPlayer, data);
         break;
      case 536875024:
         MCH_BaseVehiclePacketHandler.onPacketSeatListRequest(entityPlayer, data);
         break;
      case 536875040:
         MCH_BaseVehiclePacketHandler.onPacket_PlayerControl(entityPlayer, data);
         break;
      case 536875059:
         MCH_BaseVehiclePacketHandler.onPacketIndReload(entityPlayer, data);
         break;
      case 536875061:
         MCH_BaseVehiclePacketHandler.onPacketIndNotifyAmmoNum(entityPlayer, data);
         break;
      case 536875062:
         MCH_BaseVehiclePacketHandler.onPacketIndRotation(entityPlayer, data);
         break;
      case 536875063:
         MCH_BaseVehiclePacketHandler.onPacketNotifyInfoReloaded(entityPlayer, data);
         break;
      case 536875072:
         MCH_BaseVehiclePacketHandler.onPacket_ClientSetting(entityPlayer, data);
         break;
      case 536875104:
         MCH_BaseVehiclePacketHandler.onPacketStatusRequest(entityPlayer, data);
         break;
      case 536879120:
         MCH_HeliPacketHandler.onPacket_PlayerControl(entityPlayer, data);
         break;
      case 536887312:
         MCH_GLTDPacketHandler.onPacket_GLTDPlayerControl(entityPlayer, data);
         break;
      case 536903696:
         MCP_PlanePacketHandler.onPacket_PlayerControl(entityPlayer, data);
         break;
      case 536903698:
         //System.out.println("onpacketplayercontrol for the worst shit ever");
         MCH_ShipPacketHandler.onPacket_PlayerControl(entityPlayer, data);
         break;
      case 536936464:
         MCH_LightWeaponPacketHandler.onPacket_PlayerControl(entityPlayer, data);
         break;
      case 537002000:
         MCH_TurretPacketHandler.onPacket_PlayerControl(entityPlayer, data);
         break;
      case 537133072:
         MCH_UavPacketHandler.onPacketUavStatus(entityPlayer, data);
         break;
      case 537395216:
         MCH_DraftingTablePacketHandler.onPacketCreate(entityPlayer, data);
         break;
      case 537919504:
         MCH_TankPacketHandler.onPacket_PlayerControl(entityPlayer, data);
         break;
      case 536875026:
         //System.out.println("pre onpacketaircraftlocation");
         // Base vehicle location packet
         MCH_BaseVehiclePacketHandler.onPacketAircraftLocation(entityPlayer, data);
         //System.out.println("onpacketaircraftlocation");
         break;

      }

   }



   protected int getMessageId(ByteArrayDataInput data) {
      try {
         return data.readInt();
      } catch (Exception var3) {
         var3.printStackTrace();
         return 0;
      }
   }
}
