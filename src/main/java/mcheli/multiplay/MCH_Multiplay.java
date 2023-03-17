package mcheli.multiplay;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.command.server.CommandScoreboard;
import net.minecraft.command.server.CommandTeleport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import java.util.*;

public class MCH_Multiplay {

   public static final MCH_TargetType[][] ENTITY_SPOT_TABLE = new MCH_TargetType[][]{{MCH_TargetType.NONE, MCH_TargetType.NONE}, {MCH_TargetType.OTHER_MOB, MCH_TargetType.OTHER_MOB}, {MCH_TargetType.MONSTER, MCH_TargetType.MONSTER}, {MCH_TargetType.NONE, MCH_TargetType.NO_TEAM_PLAYER}, {MCH_TargetType.NONE, MCH_TargetType.SAME_TEAM_PLAYER}, {MCH_TargetType.NONE, MCH_TargetType.OTHER_TEAM_PLAYER}, {MCH_TargetType.NONE, MCH_TargetType.NONE}, {MCH_TargetType.NONE, MCH_TargetType.NO_TEAM_PLAYER}, {MCH_TargetType.NONE, MCH_TargetType.SAME_TEAM_PLAYER}, {MCH_TargetType.NONE, MCH_TargetType.OTHER_TEAM_PLAYER}};


   public static boolean canSpotEntityWithFilter(int filter, Entity entity) {
      return entity instanceof MCP_EntityPlane?(filter & 32) != 0:(entity instanceof MCH_EntityHeli?(filter & 16) != 0:(!(entity instanceof MCH_EntityVehicle) && !(entity instanceof MCH_EntityTank)?(entity instanceof EntityPlayer?(filter & 4) != 0:(entity instanceof EntityLivingBase?(isMonster(entity)?(filter & 2) != 0:(filter & 1) != 0):false)):(filter & 8) != 0));
   }

   public static boolean isMonster(Entity entity) {
      return entity.getClass().toString().toLowerCase().indexOf("monster") >= 0;
   }

   public static MCH_TargetType canSpotEntity(Entity user, double posX, double posY, double posZ, Entity target, boolean checkSee) {
      if(!(user instanceof EntityLivingBase)) {
         return MCH_TargetType.NONE;
      } else {
         EntityLivingBase spotter = (EntityLivingBase)user;
         int col = spotter.getTeam() == null?0:1;
         byte row = 0;
         if(target instanceof EntityLivingBase) {
            if(!isMonster(target)) {
               row = 1;
            } else {
               row = 2;
            }
         }

         if(spotter.getTeam() != null) {
            if(target instanceof EntityPlayer) {
               EntityPlayer ret = (EntityPlayer)target;
               if(ret.getTeam() == null) {
                  row = 3;
               } else if(spotter.isOnSameTeam(ret)) {
                  row = 4;
               } else {
                  row = 5;
               }
            } else if(target instanceof MCH_EntityAircraft) {
               MCH_EntityAircraft ret1 = (MCH_EntityAircraft)target;
               EntityPlayer vs = ret1.getFirstMountPlayer();
               if(vs == null) {
                  row = 6;
               } else if(vs.getTeam() == null) {
                  row = 7;
               } else if(spotter.isOnSameTeam(vs)) {
                  row = 8;
               } else {
                  row = 9;
               }
            }
         } else if(target instanceof EntityPlayer || target instanceof MCH_EntityAircraft) {
            row = 0;
         }

         MCH_TargetType ret2 = ENTITY_SPOT_TABLE[row][col];
         if(checkSee && ret2 != MCH_TargetType.NONE) {
            Vec3 vs1 = Vec3.createVectorHelper(posX, posY, posZ);
            Vec3 ve = Vec3.createVectorHelper(target.posX, target.posY + (double)target.getEyeHeight(), target.posZ);
            MovingObjectPosition mop = target.worldObj.rayTraceBlocks(vs1, ve);
            if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
               ret2 = MCH_TargetType.NONE;
            }
         }

         return ret2;
      }
   }

   public static boolean canAttackEntity(DamageSource ds, Entity target) {
      return canAttackEntity(ds.getEntity(), target);
   }

   public static boolean canAttackEntity(Entity attacker, Entity target) {
      if(attacker != null && target != null) {
         EntityPlayer attackPlayer = null;
         EntityPlayer targetPlayer = null;
         if(attacker instanceof EntityPlayer) {
            attackPlayer = (EntityPlayer)attacker;
         }

         if(target instanceof EntityPlayer) {
            targetPlayer = (EntityPlayer)target;
         } else if(target.riddenByEntity instanceof EntityPlayer) {
            targetPlayer = (EntityPlayer)target.riddenByEntity;
         }

         if(target instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft)target;
            if(ac.getRiddenByEntity() instanceof EntityPlayer) {
               targetPlayer = (EntityPlayer)ac.getRiddenByEntity();
            }
         }

         if(attackPlayer != null && targetPlayer != null && !attackPlayer.canAttackPlayer(targetPlayer)) {
            return false;
         }
      }

      return true;
   }

   public static void jumpSpawnPoint(EntityPlayer player) {
      MCH_Lib.DbgLog(false, "JumpSpawnPoint", new Object[0]);
      CommandTeleport cmd = new CommandTeleport();
      if(cmd.canCommandSenderUseCommand(player)) {
         MinecraftServer minecraftServer = MinecraftServer.getServer();
         String[] arr$ = minecraftServer.getConfigurationManager().getAllUsernames();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String playerName = arr$[i$];
            EntityPlayerMP jumpPlayer = CommandTeleport.getPlayer(player, playerName);
            ChunkCoordinates cc = null;
            if(jumpPlayer != null && jumpPlayer.dimension == player.dimension) {
               cc = jumpPlayer.getBedLocation(jumpPlayer.dimension);
               if(cc != null) {
                  cc = EntityPlayer.verifyRespawnCoordinates(minecraftServer.worldServerForDimension(jumpPlayer.dimension), cc, true);
               }

               if(cc == null) {
                  cc = jumpPlayer.worldObj.provider.getRandomizedSpawnPoint();
               }
            }

            if(cc != null) {
               String[] cmdStr = new String[]{playerName, String.format("%.1f", new Object[]{Double.valueOf((double)cc.posX + 0.5D)}), String.format("%.1f", new Object[]{Double.valueOf((double)cc.posY + 0.1D)}), String.format("%.1f", new Object[]{Double.valueOf((double)cc.posZ + 0.5D)})};
               cmd.processCommand(player, cmdStr);
            }
         }
      }

   }

   public static void shuffleTeam(EntityPlayer player) {
      Collection teams = player.worldObj.getScoreboard().getTeams();
      int teamNum = teams.size();
      MCH_Lib.DbgLog(false, "ShuffleTeam:%d teams ----------", new Object[]{Integer.valueOf(teamNum)});
      if(teamNum > 0) {
         CommandScoreboard cmd = new CommandScoreboard();
         if(cmd.canCommandSenderUseCommand(player)) {
            List list = Arrays.asList(MinecraftServer.getServer().getConfigurationManager().getAllUsernames());
            Collections.shuffle(list);
            ArrayList listTeam = new ArrayList();
            Iterator j = teams.iterator();

            while(j.hasNext()) {
               Object exe_cmd = j.next();
               ScorePlayerTeam process_cmd = (ScorePlayerTeam)exe_cmd;
               listTeam.add(process_cmd.getRegisteredName());
            }

            Collections.shuffle(listTeam);
            int var9 = 0;

            for(int var10 = 0; var9 < list.size(); ++var9) {
               listTeam.set(var10, (String)listTeam.get(var10) + " " + (String)list.get(var9));
               ++var10;
               if(var10 >= teamNum) {
                  var10 = 0;
               }
            }

            for(var9 = 0; var9 < listTeam.size(); ++var9) {
               String var11 = "teams join " + (String)listTeam.get(var9);
               String[] var12 = var11.split(" ");
               if(var12.length > 3) {
                  MCH_Lib.DbgLog(false, "ShuffleTeam:" + var11, new Object[0]);
                  cmd.processCommand(player, var12);
               }
            }
         }
      }

   }

   public static boolean spotEntity(EntityPlayer player, MCH_EntityAircraft ac, double posX, double posY, double posZ, int targetFilter, float spotLength, int markTime, float angle) {
      boolean ret = false;
      if(!player.worldObj.isRemote) {
         float acYaw = 0.0F;
         float acPitch = 0.0F;
         float acRoll = 0.0F;
         if(ac != null) {
            acYaw = ac.getRotYaw();
            acPitch = ac.getRotPitch();
            acRoll = ac.getRotRoll();
         }

         Vec3 vv = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -player.rotationYaw, -player.rotationPitch, -acRoll);
         double tx = vv.xCoord;
         double tz = vv.zCoord;
         List list = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand((double)spotLength, (double)spotLength, (double)spotLength));
         ArrayList entityList = new ArrayList();
         Vec3 pos = Vec3.createVectorHelper(posX, posY, posZ);

         for(int entityId = 0; entityId < list.size(); ++entityId) {
            Entity i = (Entity)list.get(entityId);
            if(canSpotEntityWithFilter(targetFilter, i)) {
               MCH_TargetType stopType = canSpotEntity(player, posX, posY, posZ, i, true);
               if(stopType != MCH_TargetType.NONE && stopType != MCH_TargetType.SAME_TEAM_PLAYER) {
                  double dist = i.getDistanceSq(pos.xCoord, pos.yCoord, pos.zCoord);
                  if(dist > 1.0D && dist < (double)(spotLength * spotLength)) {
                     double cx = i.posX - pos.xCoord;
                     double cy = i.posY - pos.yCoord;
                     double cz = i.posZ - pos.zCoord;
                     double h = (double)MCH_Lib.getPosAngle(tx, tz, cx, cz);
                     double v = Math.atan2(cy, Math.sqrt(cx * cx + cz * cz)) * 180.0D / 3.141592653589793D;
                     v = Math.abs(v + (double)player.rotationPitch);
                     if(h < (double)(angle * 2.0F) && v < (double)(angle * 2.0F)) {
                        entityList.add(Integer.valueOf(i.getEntityId()));
                     }
                  }
               }
            }
         }

         if(entityList.size() > 0) {
            int[] var39 = new int[entityList.size()];

            for(int var40 = 0; var40 < var39.length; ++var40) {
               var39[var40] = ((Integer)entityList.get(var40)).intValue();
            }

            sendSpotedEntityListToSameTeam(player, markTime, var39);
            ret = true;
         } else {
            ret = false;
         }
      }

      return ret;
   }

   public static void sendSpotedEntityListToSameTeam(EntityPlayer player, int count, int[] entityId) {
      ServerConfigurationManager svCnf = MinecraftServer.getServer().getConfigurationManager();
      Iterator i$ = svCnf.playerEntityList.iterator();

      while(i$.hasNext()) {
         EntityPlayerMP notifyPlayer = (EntityPlayerMP)i$.next();
         if(player == notifyPlayer || player.isOnSameTeam(notifyPlayer)) {
            MCH_PacketNotifySpotedEntity.send(notifyPlayer, count, entityId);
         }
      }

   }

   public static boolean markPoint(EntityPlayer player, double posX, double posY, double posZ) {
      Vec3 vs = Vec3.createVectorHelper(posX, posY, posZ);
      Vec3 ve = MCH_Lib.Rot2Vec3(player.rotationYaw, player.rotationPitch);
      ve = vs.addVector(ve.xCoord * 1000.0D, ve.yCoord * 1000.0D, ve.zCoord * 1000.0D);
      MovingObjectPosition mop = player.worldObj.rayTraceBlocks(vs, ve, true);
      if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
         sendMarkPointToSameTeam(player, mop.blockX, mop.blockY + 2, mop.blockZ);
         return true;
      } else {
         sendMarkPointToSameTeam(player, 0, 1000, 0);
         return false;
      }
   }

   public static void sendMarkPointToSameTeam(EntityPlayer player, int x, int y, int z) {
      ServerConfigurationManager svCnf = MinecraftServer.getServer().getConfigurationManager();
      Iterator i$ = svCnf.playerEntityList.iterator();

      while(i$.hasNext()) {
         EntityPlayerMP notifyPlayer = (EntityPlayerMP)i$.next();
         if(player == notifyPlayer || player.isOnSameTeam(notifyPlayer)) {
            MCH_PacketNotifyMarkPoint.send(notifyPlayer, x, y, z);
         }
      }

   }

}
