package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_InfoManagerBase;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_PacketIndNotifyAmmoNum;
import mcheli.aircraft.MCH_PacketIndReload;
import mcheli.aircraft.MCH_PacketIndRotation;
import mcheli.aircraft.MCH_PacketNotifyAmmoNum;
import mcheli.aircraft.MCH_PacketNotifyClientSetting;
import mcheli.aircraft.MCH_PacketNotifyHitBullet;
import mcheli.aircraft.MCH_PacketNotifyInfoReloaded;
import mcheli.aircraft.MCH_PacketNotifyOnMountEntity;
import mcheli.aircraft.MCH_PacketNotifyTVMissileEntity;
import mcheli.aircraft.MCH_PacketNotifyWeaponID;
import mcheli.aircraft.MCH_PacketSeatListRequest;
import mcheli.aircraft.MCH_PacketSeatListResponse;
import mcheli.aircraft.MCH_PacketSeatPlayerControl;
import mcheli.aircraft.MCH_PacketStatusRequest;
import mcheli.aircraft.MCH_PacketStatusResponse;
//import mcheli.sensors.Mk1Eyeball;
import mcheli.weapon.MCH_EntityTvMissile;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class MCH_BaseVehiclePacketHandler {
   private static final int MAX_PENDING_MOUNTS = 64;
   private static final int PENDING_MOUNT_TICKS = 100;
   private static final List<PendingMount> pendingMounts = new ArrayList<PendingMount>();
   private static int pendingWorldIdentity;
   private static int pendingPlayerIdentity;
   private static final Map<Integer, Integer> lastMountSequences = new HashMap<Integer, Integer>();

   private static final class PendingMount {
      final int aircraftId;
      final int riderId;
      final int seatId;
      final UUID aircraftUUID;
      final UUID riderUUID;
      int ticksLeft = PENDING_MOUNT_TICKS;

      PendingMount(MCH_PacketNotifyOnMountEntity packet) {
         this.aircraftId = packet.entityID_Ac;
         this.riderId = packet.entityID_rider;
         this.seatId = packet.seatID;
         this.aircraftUUID = packet.aircraftUUID;
         this.riderUUID = packet.riderUUID;
      }
   }

   public static void clearPendingMounts() {
      pendingMounts.clear();
      lastMountSequences.clear();
      pendingWorldIdentity = 0;
      pendingPlayerIdentity = 0;
   }

   public static void tickPendingMounts(EntityPlayer player) {
      if(player == null || !player.worldObj.isRemote) return;
      int worldIdentity = System.identityHashCode(player.worldObj);
      int playerIdentity = System.identityHashCode(player);
      if(pendingWorldIdentity != worldIdentity || pendingPlayerIdentity != playerIdentity) {
         clearPendingMounts();
         pendingWorldIdentity = worldIdentity;
         pendingPlayerIdentity = playerIdentity;
      }
      for(int i = pendingMounts.size() - 1; i >= 0; --i) {
         PendingMount pending = pendingMounts.get(i);
         if(applyMount(player, pending.aircraftId, pending.riderId, pending.seatId,
               pending.aircraftUUID, pending.riderUUID) || --pending.ticksLeft <= 0) {
            pendingMounts.remove(i);
         }
      }
   }

   private static void queueMount(EntityPlayer player, MCH_PacketNotifyOnMountEntity packet) {
      int worldIdentity = System.identityHashCode(player.worldObj);
      int playerIdentity = System.identityHashCode(player);
      if(pendingWorldIdentity != worldIdentity || pendingPlayerIdentity != playerIdentity) {
         clearPendingMounts();
         pendingWorldIdentity = worldIdentity;
         pendingPlayerIdentity = playerIdentity;
      }
      for(int i = pendingMounts.size() - 1; i >= 0; --i) {
         if(pendingMounts.get(i).riderId == packet.entityID_rider) pendingMounts.remove(i);
      }
      if(pendingMounts.size() >= MAX_PENDING_MOUNTS) pendingMounts.remove(0);
      pendingMounts.add(new PendingMount(packet));
   }

   private static boolean applyMount(EntityPlayer player, int aircraftId, int riderId, int seatId,
                                     UUID aircraftUUID, UUID riderUUID) {
      Entity aircraftEntity = player.worldObj.getEntityByID(aircraftId);
      Entity rider = player.worldObj.getEntityByID(riderId);
      if(rider == null || rider.isDead) return false;
      if(seatId < 0) {
         if(rider.ridingEntity != null) rider.mountEntity((Entity)null);
         return rider.ridingEntity == null;
      }
      if(!(aircraftEntity instanceof MCH_EntityBaseVehicle)) return false;
      MCH_EntityBaseVehicle aircraft = (MCH_EntityBaseVehicle)aircraftEntity;
      if(aircraft.isUAV() || aircraft.isNewUAV()) return true;
      Entity mount = seatId == 0 ? aircraft : aircraft.getSeat(seatId - 1);
      if(mount == null || mount.isDead) return false;
      if(mount instanceof MCH_EntitySeat && ((MCH_EntitySeat)mount).getParent() != aircraft) return false;
      if(rider.ridingEntity != mount) rider.mountEntity(mount);
      return rider.ridingEntity == mount && mount.riddenByEntity == rider;
   }

   public static void handleVehicleAccessLockToggle(EntityPlayer player, MCH_EntityBaseVehicle vehicle,
                                                     MCH_PacketPlayerControlBase control) {
      if(control.toggleVehicleAccessLock) {
         vehicle.requestVehicleAccessLockToggle(player);
      }
   }

   public static void handleRadarToggle(EntityPlayer player, MCH_EntityBaseVehicle vehicle,
                                        MCH_PacketPlayerControlBase control) {
      if(control.toggleRadar && vehicle != null && vehicle.hasRadar() && vehicle.isPilot(player)) {
         vehicle.toggleRadar(player);
      }
   }

   public static void onPacketIndRotation(EntityPlayer player, ByteArrayDataInput data) {
      if(player != null && !player.worldObj.isRemote) {
         MCH_PacketIndRotation req = new MCH_PacketIndRotation();
         req.readData(data);
         if(req.entityID_Ac > 0) {
            Entity e = player.worldObj.getEntityByID(req.entityID_Ac);
            if(e instanceof MCH_EntityBaseVehicle) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)e;
               ac.setRotRoll(req.roll);
               if(req.rollRev) {
                  //System.out.println("req.rollRev");
                  MCH_Lib.DbgLog(ac.worldObj, "onPacketIndRotation Error:req.rollRev y=%.2f, p=%.2f, r=%.2f", new Object[]{Float.valueOf(req.yaw), Float.valueOf(req.pitch), Float.valueOf(req.roll)});
                  if(ac.getRiddenByEntity() != null) {
                     ac.getRiddenByEntity().rotationYaw = req.yaw;
                     ac.getRiddenByEntity().prevRotationYaw = req.yaw;
                  }

                  for(int sid = 0; sid < ac.getSeatNum(); ++sid) {
                     Entity entity = ac.getEntityBySeatId(1 + sid);
                     if(entity != null) {
                        entity.rotationYaw += entity.rotationYaw <= 0.0F?180.0F:-180.0F;
                        //System.out.println("entity isn't null yaw applied");
                     }
                  }
               }

               ac.setRotYaw(req.yaw);
               //System.out.println("yaw changed");
               ac.setRotPitch(req.pitch);
               //System.out.println("pitch changed");
            }

         }
      }
   }

   public static void onPacketOnMountEntity(EntityPlayer player, ByteArrayDataInput data) {
      //System.out.println("player mount entity");
      if(player != null && player.worldObj.isRemote) {
         MCH_PacketNotifyOnMountEntity req = new MCH_PacketNotifyOnMountEntity();
         req.readData(data);
         MCH_Lib.DbgLog(player.worldObj, "onPacketOnMountEntity.rcv:%d, %d, %d, %d", new Object[]{Integer.valueOf(W_Entity.getEntityId(player)), Integer.valueOf(req.entityID_Ac), Integer.valueOf(req.entityID_rider), Integer.valueOf(req.seatID)});
         Integer riderKey = Integer.valueOf(req.entityID_rider);
         Integer lastSequence = lastMountSequences.get(riderKey);
         if(req.entityID_Ac > 0 && req.entityID_rider > 0
               && (lastSequence == null || req.sequence > lastSequence.intValue())) {
            if(lastMountSequences.size() >= MAX_PENDING_MOUNTS && !lastMountSequences.containsKey(riderKey)) {
               lastMountSequences.remove(lastMountSequences.keySet().iterator().next());
            }
            lastMountSequences.put(riderKey, Integer.valueOf(req.sequence));
            if(!applyMount(player, req.entityID_Ac, req.entityID_rider, req.seatID,
                  req.aircraftUUID, req.riderUUID) && req.seatID >= 0) {
               queueMount(player, req);
            }
         }
      }
   }

   public static void onPacketNotifyAmmoNum(EntityPlayer player, ByteArrayDataInput data) {
      if(player != null && player.worldObj.isRemote) {
         MCH_PacketNotifyAmmoNum status = new MCH_PacketNotifyAmmoNum();
         status.readData(data);
         if(status.entityID_Ac > 0) {
            Entity e = player.worldObj.getEntityByID(status.entityID_Ac);
            if(e instanceof MCH_EntityBaseVehicle) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)e;
               String msg = "onPacketNotifyAmmoNum:";
               msg = msg + (ac.getAcInfo() != null?ac.getAcInfo().displayName:"null") + ":";
               if(status.all) {
                  msg = msg + "All=true, Num=" + status.num;

                  for(int i = 0; i < ac.getWeaponNum() && i < status.num; ++i) {
                     ac.getWeapon(i).setAmmoNum(status.ammo[i]);
                     ac.getWeapon(i).setRestAllAmmoNum(status.restAmmo[i]);
                     msg = msg + ", [" + status.ammo[i] + "/" + status.restAmmo[i] + "]";
                  }

                  MCH_Lib.DbgLog(e.worldObj, msg, new Object[0]);
               } else if(status.weaponID < ac.getWeaponNum()) {
                  msg = msg + "All=false, WeaponID=" + status.weaponID + ", " + status.ammo[0] + ", " + status.restAmmo[0];
                  ac.getWeapon(status.weaponID).setAmmoNum(status.ammo[0]);
                  ac.getWeapon(status.weaponID).setRestAllAmmoNum(status.restAmmo[0]);
                  MCH_Lib.DbgLog(e.worldObj, msg, new Object[0]);
               } else {
                  MCH_Lib.DbgLog(e.worldObj, "Error:" + status.weaponID, new Object[0]);
               }
            }

         }
      }
   }

   public static void onPacketStatusRequest(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_PacketStatusRequest req = new MCH_PacketStatusRequest();
         req.readData(data);
         if(req.entityID_AC > 0) {
            Entity e = player.worldObj.getEntityByID(req.entityID_AC);

            if(e instanceof MCH_EntityBaseVehicle) {
               MCH_PacketStatusResponse.sendStatus((MCH_EntityBaseVehicle)e, player);
            }

         }
      }
   }

   public static void onPacketIndNotifyAmmoNum(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_PacketIndNotifyAmmoNum req = new MCH_PacketIndNotifyAmmoNum();
         req.readData(data);
         if(req.entityID_Ac > 0) {
            Entity e = player.worldObj.getEntityByID(req.entityID_Ac);
            if(e instanceof MCH_EntityBaseVehicle) {
               if(req.weaponID >= 0) {
                  MCH_PacketNotifyAmmoNum.sendAmmoNum((MCH_EntityBaseVehicle)e, player, req.weaponID);
               } else {
                  MCH_PacketNotifyAmmoNum.sendAllAmmoNum((MCH_EntityBaseVehicle)e, player);
               }
            }

         }
      }
   }

   public static void onPacketIndReload(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_PacketIndReload ind = new MCH_PacketIndReload();
         ind.readData(data);
         if(ind.entityID_Ac > 0) {
            Entity e = player.worldObj.getEntityByID(ind.entityID_Ac);
            if(e instanceof MCH_EntityBaseVehicle) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)e;
               MCH_Lib.DbgLog(e.worldObj, "onPacketIndReload :%s", new Object[]{ac.getAcInfo().displayName});
               ac.supplyAmmo(ind.weaponID);
            }

         }
      }
   }

   public static void onPacketStatusResponse(EntityPlayer player, ByteArrayDataInput data) {
      if(player.worldObj.isRemote) {
         MCH_PacketStatusResponse status = new MCH_PacketStatusResponse();
         status.readData(data);
         //System.out.println("reading data");
         String msg = "onPacketStatusResponse:";
         if(status.entityID_AC > 0) {
            msg = msg + "EID=" + status.entityID_AC + ":";
            Entity e = player.worldObj.getEntityByID(status.entityID_AC);
            //System.out.println("player is an object");
            if(e instanceof MCH_EntityBaseVehicle) {
              // System.out.println("is player MCH_EntityBaseVehicle, MCH_BaseVehiclePacketHandler");
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)e;
               if(status.seatNum > 0 && status.weaponIDs != null && status.weaponIDs.length == status.seatNum) {
                  msg = msg + "seatNum=" + status.seatNum + ":";

                  for(int i = 0; i < status.seatNum; ++i) {
                     ac.updateWeaponID(i, status.weaponIDs[i]);
                     msg = msg + "[" + i + "," + status.weaponIDs[i] + "]";
                  }
               } else {
                  msg = msg + "Error seatNum=" + status.seatNum;
               }
            }

            MCH_Lib.DbgLog(true, msg, new Object[0]);
         }
      }
   }

   public static void onPacketNotifyWeaponID(EntityPlayer player, ByteArrayDataInput data) {
      if(player.worldObj.isRemote) {
         MCH_PacketNotifyWeaponID status = new MCH_PacketNotifyWeaponID();
         status.readData(data);
         if(status.entityID_Ac > 0) {
            Entity e = player.worldObj.getEntityByID(status.entityID_Ac);
            if(e instanceof MCH_EntityBaseVehicle) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)e;
               if(ac.isValidSeatID(status.seatID)) {
                  ac.getWeapon(status.weaponID).setAmmoNum(status.ammo);
                  ac.getWeapon(status.weaponID).setRestAllAmmoNum(status.restAmmo);
                  MCH_Lib.DbgLog(true, "onPacketNotifyWeaponID:WeaponID=%d (%d / %d)", new Object[]{Integer.valueOf(status.weaponID), Short.valueOf(status.ammo), Short.valueOf(status.restAmmo)});
                  if(W_Lib.isClientPlayer(ac.getEntityBySeatId(status.seatID))) {
                     MCH_Lib.DbgLog(true, "onPacketNotifyWeaponID:#discard:SeatID=%d, WeaponID=%d", new Object[]{Integer.valueOf(status.seatID), Integer.valueOf(status.weaponID)});
                  } else {
                     MCH_Lib.DbgLog(true, "onPacketNotifyWeaponID:SeatID=%d, WeaponID=%d", new Object[]{Integer.valueOf(status.seatID), Integer.valueOf(status.weaponID)});
                     ac.updateWeaponID(status.seatID, status.weaponID);
                  }
               }
            }

         }
      }
   }

   public static void onPacketNotifyHitBullet(EntityPlayer player, ByteArrayDataInput data) {
      if(player.worldObj.isRemote) {
         MCH_PacketNotifyHitBullet status = new MCH_PacketNotifyHitBullet();
         status.readData(data);
         if(status.entityID_Ac <= 0) {
            MCH_MOD.proxy.hitBullet();
         } else {
            Entity e = player.worldObj.getEntityByID(status.entityID_Ac);
            if(e instanceof MCH_EntityBaseVehicle) {
               ((MCH_EntityBaseVehicle)e).hitBullet();
            }
         }

      }
   }

   public static void onPacketSeatListRequest(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_PacketSeatListRequest req = new MCH_PacketSeatListRequest();
         req.readData(data);
         if(req.entityID_AC > 0) {
            Entity e = player.worldObj.getEntityByID(req.entityID_AC);
            if(e instanceof MCH_EntityBaseVehicle) {
               MCH_Lib.DbgLog(player.worldObj, "[MCH-SYNC][SEAT-REQUEST-RECEIVE] aircraftId=%d player=%s playerUuid=%s",
                       new Object[]{Integer.valueOf(req.entityID_AC), player.getCommandSenderName(), player.getUniqueID()});
               MCH_PacketSeatListResponse.sendSeatList((MCH_EntityBaseVehicle)e, player);
            }

         }
      }
   }

   public static void onPacketNotifyTVMissileEntity(EntityPlayer player, ByteArrayDataInput data) {
      if(player.worldObj.isRemote) {
         MCH_PacketNotifyTVMissileEntity packet = new MCH_PacketNotifyTVMissileEntity();
         packet.readData(data);
         if(packet.entityID_Ac <= 0) {
            return;
         }

         if(packet.entityID_TVMissile <= 0) {
            return;
         }

         Entity e = player.worldObj.getEntityByID(packet.entityID_Ac);
         if(e == null || !(e instanceof MCH_EntityBaseVehicle)) {
            return;
         }

         MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)e;
         e = player.worldObj.getEntityByID(packet.entityID_TVMissile);
         if(e == null || !(e instanceof MCH_EntityTvMissile)) {
            return;
         }

         ((MCH_EntityTvMissile)e).shootingEntity = player;
         ac.setTVMissile((MCH_EntityTvMissile)e);
      }

   }

   public static void onPacketSeatListResponse(EntityPlayer player, ByteArrayDataInput data) {
      if(player.worldObj.isRemote) {
         MCH_PacketSeatListResponse seatList = new MCH_PacketSeatListResponse();
         seatList.readData(data);
         if(seatList.entityID_AC > 0) {
            Entity e = player.worldObj.getEntityByID(seatList.entityID_AC);
            if(e instanceof MCH_EntityBaseVehicle) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)e;
               MCH_Lib.DbgLog(player.worldObj, "[MCH-SYNC][SEAT-RESPONSE-RECEIVE] aircraftId=%d aircraftUuid=%s packetSeats=%d localSeats=%d",
                       new Object[]{Integer.valueOf(seatList.entityID_AC), ac.getUniqueID(), Integer.valueOf(seatList.seatNum), Integer.valueOf(ac.getSeats().length)});
               if(seatList.seatNum > 0 && seatList.seatNum == ac.getSeats().length && seatList.seatEntityID != null && seatList.seatEntityID.length == seatList.seatNum) {
                  for(int i = 0; i < seatList.seatNum; ++i) {
                     Entity entity = player.worldObj.getEntityByID(seatList.seatEntityID[i]);
                     if(entity instanceof MCH_EntitySeat) {
                        MCH_EntitySeat seat = (MCH_EntitySeat)entity;
                        MCH_Lib.DbgLog(player.worldObj, "[MCH-SYNC][SEAT-APPLY] aircraftId=%d index=%d seatId=%d seatUuid=%s",
                                new Object[]{Integer.valueOf(seatList.entityID_AC), Integer.valueOf(i), Integer.valueOf(seat.getEntityId()), seat.getUniqueID()});
                        seat.seatID = i;
                        seat.setParent(ac);
                        seat.parentUniqueID = ac.getCommonUniqueId();
                        if(seatList.riderEntityID == null || seatList.riderEntityID.length <= i || seatList.riderEntityID[i] <= 0) {
                           seat.riddenByEntity = null;
                        } else {
                           Entity rider = player.worldObj.getEntityByID(seatList.riderEntityID[i]);
                           if(rider != null) {
                              seat.riddenByEntity = rider;
                              rider.ridingEntity = seat;
                           }
                        }
                        ac.setSeat(i, seat);
                     } else {
                        ac.setSeat(i, null);
                        MCH_Lib.DbgLog(player.worldObj, "[MCH-SYNC][SEAT-APPLY-FAIL] reason=seat_entity_missing_or_wrong_type aircraftId=%d index=%d requestedSeatId=%d resolved=%s",
                                new Object[]{Integer.valueOf(seatList.entityID_AC), Integer.valueOf(i), Integer.valueOf(seatList.seatEntityID[i]), entity});
                     }
                  }
                  ac.debugVehicleState("SEAT-RESPONSE-APPLIED", player);
               } else {
                  MCH_Lib.DbgLog(player.worldObj, "[MCH-SYNC][SEAT-APPLY-FAIL] reason=count_mismatch aircraftId=%d packetSeats=%d localSeats=%d",
                          new Object[]{Integer.valueOf(seatList.entityID_AC), Integer.valueOf(seatList.seatNum), Integer.valueOf(ac.getSeats().length)});
               }
            }

         }
      }
   }

   public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_EntityBaseVehicle ac = null;
         if(player.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat pc = (MCH_EntitySeat)player.ridingEntity;
            ac = pc.getParent();
         } else {
            ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
         }

         if(ac != null) {
            MCH_PacketSeatPlayerControl pc1 = new MCH_PacketSeatPlayerControl();
            pc1.readData(data);
            if(pc1.isUnmount) {
               ac.unmountEntityFromSeat(player);
            } else if(pc1.switchSeat > 0) {
               if(pc1.switchSeat == 3) {
                  player.mountEntity((Entity)null);
                  ac.keepOnRideRotation = true;
                  ac.interactFirst(player, true);
               }

               if(pc1.switchSeat == 1) {
                  ac.switchNextSeat(player);
               }

               if(pc1.switchSeat == 2) {
                  ac.switchPrevSeat(player);
               }
            } else if(pc1.parachuting) {
               ac.unmount(player);
            }

         }
      }
   }

   public static void onPacket_ClientSetting(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_PacketNotifyClientSetting pc = new MCH_PacketNotifyClientSetting();
         pc.readData(data);
         MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
         if(ac != null) {
            int sid = ac.getSeatIdByEntity(player);
            if(sid == 0) {
               ac.cs_dismountAll = pc.dismountAll;
               ac.cs_heliAutoThrottleDown = pc.heliAutoThrottleDown;
               ac.cs_planeAutoThrottleDown = pc.planeAutoThrottleDown;
               ac.cs_shipAutoThrottleDown = pc.shipAutoThrottleDown;
               ac.cs_tankAutoThrottleDown = pc.tankAutoThrottleDown;
            }

            ac.camera.setShaderSupport(sid, Boolean.valueOf(pc.shaderSupport));
         }

      }
   }

   public static void onPacketNotifyInfoReloaded(EntityPlayer player, ByteArrayDataInput data) {
      MCH_PacketNotifyInfoReloaded pc = new MCH_PacketNotifyInfoReloaded();
      pc.readData(data);

      // Client-side: full info reload (sent by server after /mcheli reload)
      if(player.worldObj.isRemote && pc.type == 2) {
         MCH_MOD.proxy.scheduleClientInfoReload();
         return;
      }

      if(player.worldObj.isRemote && pc.type == 4) {
         MCH_MOD.proxy.scheduleTargetedVehicleReload(pc.requestId, pc.entityId,
               pc.definition, pc.success, pc.reason);
         return;
      }

      if(!player.worldObj.isRemote) {
         MCH_EntityBaseVehicle ac;
         int i$;
         switch(pc.type) {
         case 0:
            // Legacy clients get the same authoritative, single-entity behavior.
            handleTargetedReload(player, pc, false);
            break;
         case 3:
            handleTargetedReload(player, pc, true);
            break;
         case 1:
            MCH_WeaponInfoManager.reload();
            WorldServer[] arr$ = MinecraftServer.getServer().worldServers;
            int len$ = arr$.length;

            for(i$ = 0; i$ < len$; ++i$) {
               WorldServer world = arr$[i$];
               List list = world.loadedEntityList;

               for(int i = 0; i < list.size(); ++i) {
                  if(list.get(i) instanceof MCH_EntityBaseVehicle) {
                     ac = (MCH_EntityBaseVehicle)list.get(i);
                     if(ac.getAcInfo() != null) {
                        ac.changeType(ac.getAcInfo().name);
                        ac.createSeats(UUID.randomUUID().toString());
                     }
                  }
               }
            }
         }

      }
   }

   private static void handleTargetedReload(EntityPlayer player,
         MCH_PacketNotifyInfoReloaded request, boolean validateIdentity) {
      MCH_EntityBaseVehicle vehicle = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      if(vehicle == null || vehicle.getAcInfo() == null) {
         MCH_PacketNotifyInfoReloaded.sendTargetedResult(player, request.requestId,
               request.entityId, "", false, "No controlled vehicle");
         return;
      }
      if(validateIdentity && request.entityId != vehicle.getEntityId()) {
         MCH_PacketNotifyInfoReloaded.sendTargetedResult(player, request.requestId,
               request.entityId, vehicle.getAcInfo().name, false, "Entity ID changed");
         return;
      }
      String name = vehicle.getAcInfo().name;
      MCH_InfoManagerBase manager = MCH_VehicleInfoReload.managerFor(vehicle);
      if(manager == null) {
         MCH_PacketNotifyInfoReloaded.sendTargetedResult(player, request.requestId, request.entityId, name, false,
               "Unsupported MCHeli vehicle type");
         return;
      }
      boolean success = manager.reloadEntry(name);
      String reason = manager.getLastReloadError();
      if(success) {
         MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)manager.getMap().get(name);
         success = vehicle.applyTargetedInfo(info);
         if(!success) reason = "Seat count changed; restart is required";
      }
      MCH_PacketNotifyInfoReloaded.sendTargetedResult(player, request.requestId, request.entityId, name, success,
            success ? "Reloaded vehicle definition and model" : reason);
   }

   public static void onPacketAircraftLocation(EntityPlayer entityPlayer, ByteArrayDataInput data) {
      if(entityPlayer.worldObj.isRemote) {
         MCH_PacketBaseVehicleLocation pc = new MCH_PacketBaseVehicleLocation();
         pc.readData(data);

         //Mk1Eyeball.getInstance().addContact(pc);
      }
   }
}
