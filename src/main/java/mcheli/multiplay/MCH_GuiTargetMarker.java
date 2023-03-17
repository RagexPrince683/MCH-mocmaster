package mcheli.multiplay;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.MCH_MarkEntityPos;
import mcheli.MCH_ServerSettings;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.gui.MCH_Gui;
import mcheli.particles.MCH_ParticlesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class MCH_GuiTargetMarker extends MCH_Gui {

   private static FloatBuffer matModel = BufferUtils.createFloatBuffer(16);
   private static FloatBuffer matProjection = BufferUtils.createFloatBuffer(16);
   private static IntBuffer matViewport = BufferUtils.createIntBuffer(16);
   private static ArrayList entityPos = new ArrayList();
   private static HashMap spotedEntity = new HashMap();
   private static Minecraft s_minecraft;
   private static int spotedEntityCountdown = 0;


   public MCH_GuiTargetMarker(Minecraft minecraft) {
      super(minecraft);
      s_minecraft = minecraft;
   }

   public void initGui() {
      super.initGui();
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public boolean isDrawGui(EntityPlayer player) {
      return player != null && player.worldObj != null;
   }

   public static void onClientTick() {
      if(!Minecraft.getMinecraft().isGamePaused()) {
         ++spotedEntityCountdown;
      }

      if(spotedEntityCountdown >= 20) {
         spotedEntityCountdown = 0;
         Iterator i = spotedEntity.keySet().iterator();

         while(i.hasNext()) {
            Integer key = (Integer)i.next();
            int count = ((Integer)spotedEntity.get(key)).intValue();
            if(count > 0) {
               spotedEntity.put(key, Integer.valueOf(count - 1));
            }
         }

         i = spotedEntity.values().iterator();

         while(i.hasNext()) {
            if(((Integer)i.next()).intValue() <= 0) {
               i.remove();
            }
         }
      }

   }

   public static boolean isSpotedEntity(Entity entity) {
      int entityId = entity.getEntityId();
      Iterator i$ = spotedEntity.keySet().iterator();

      int key;
      do {
         if(!i$.hasNext()) {
            return false;
         }

         key = ((Integer)i$.next()).intValue();
      } while(key != entityId);

      return true;
   }

   public static void addSpotedEntity(int entityId, int count) {
      if(spotedEntity.containsKey(Integer.valueOf(entityId))) {
         int now = ((Integer)spotedEntity.get(Integer.valueOf(entityId))).intValue();
         if(count > now) {
            spotedEntity.put(Integer.valueOf(entityId), Integer.valueOf(count));
         }
      } else {
         spotedEntity.put(Integer.valueOf(entityId), Integer.valueOf(count));
      }

   }

   public static void addMarkEntityPos(int reserve, Entity entity, double x, double y, double z) {
      addMarkEntityPos(reserve, entity, x, y, z, false);
   }

   public static void addMarkEntityPos(int reserve, Entity entity, double x, double y, double z, boolean nazo) {
      if(isEnableEntityMarker()) {
         MCH_TargetType spotType = MCH_TargetType.NONE;
         EntityClientPlayerMP clientPlayer = s_minecraft.thePlayer;
         if(entity instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft e = (MCH_EntityAircraft)entity;
            if(e.isMountedEntity(clientPlayer)) {
               return;
            }

            if(e.isMountedSameTeamEntity(clientPlayer)) {
               spotType = MCH_TargetType.SAME_TEAM_PLAYER;
            }
         } else if(entity instanceof EntityPlayer) {
            if(entity == clientPlayer || entity.ridingEntity instanceof MCH_EntitySeat || entity.ridingEntity instanceof MCH_EntityAircraft) {
               return;
            }

            if(clientPlayer.getTeam() != null && clientPlayer.isOnSameTeam((EntityLivingBase)entity)) {
               spotType = MCH_TargetType.SAME_TEAM_PLAYER;
            }
         }

         if(spotType == MCH_TargetType.NONE && isSpotedEntity(entity)) {
            spotType = MCH_Multiplay.canSpotEntity(clientPlayer, clientPlayer.posX, clientPlayer.posY + (double)clientPlayer.getEyeHeight(), clientPlayer.posZ, entity, false);
         }

         if(reserve == 100) {
            spotType = MCH_TargetType.POINT;
         }

         if(spotType != MCH_TargetType.NONE) {
            MCH_MarkEntityPos e1 = new MCH_MarkEntityPos(spotType.ordinal(), entity);
            GL11.glGetFloat(2982, matModel);
            GL11.glGetFloat(2983, matProjection);
            GL11.glGetInteger(2978, matViewport);
            if(nazo) {
               GLU.gluProject((float)z, (float)y, (float)x, matModel, matProjection, matViewport, e1.pos);
               float yy = e1.pos.get(1);
               GLU.gluProject((float)x, (float)y, (float)z, matModel, matProjection, matViewport, e1.pos);
               e1.pos.put(1, yy);
            } else {
               GLU.gluProject((float)x, (float)y, (float)z, matModel, matProjection, matViewport, e1.pos);
            }

            entityPos.add(e1);
         }

      }
   }

   public static void clearMarkEntityPos() {
      entityPos.clear();
   }

   public static boolean isEnableEntityMarker() {
      MCH_Config var10000 = MCH_MOD.config;
      boolean var0;
      if(MCH_Config.DisplayEntityMarker.prmBool && (Minecraft.getMinecraft().isSingleplayer() || MCH_ServerSettings.enableEntityMarker)) {
         var10000 = MCH_MOD.config;
         if(MCH_Config.EntityMarkerSize.prmDouble > 0.0D) {
            var0 = true;
            return var0;
         }
      }

      var0 = false;
      return var0;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      GL11.glLineWidth((float)(MCH_Gui.scaleFactor * 2));
      if(this.isDrawGui(player)) {
         GL11.glDisable(3042);
         if(isEnableEntityMarker()) {
            this.drawMark();
         }

      }
   }

   void drawMark() {
      int[] COLOR_TABLE = new int[]{0, -808464433, -805371904, -805306624, -822018049, -805351649, -65536, 0};
      int scale = MCH_Gui.scaleFactor > 0?MCH_Gui.scaleFactor:2;
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4b((byte)-1, (byte)-1, (byte)-1, (byte)-1);
      GL11.glDepthMask(false);
      int DW = super.mc.displayWidth;
      int DH = super.mc.displayHeight;
      int DSW = super.mc.displayWidth / scale;
      int DSH = super.mc.displayHeight / scale;
      double x = 9999.0D;
      double z = 9999.0D;
      double y = 9999.0D;
      Tessellator tessellator = Tessellator.instance;

      for(int i = 0; i < 2; ++i) {
         if(i == 0) {
            tessellator.startDrawing(i == 0?4:1);
         }

         Iterator i$ = entityPos.iterator();

         while(i$.hasNext()) {
            MCH_MarkEntityPos e = (MCH_MarkEntityPos)i$.next();
            int color = COLOR_TABLE[e.type];
            x = (double)(e.pos.get(0) / (float)scale);
            z = (double)e.pos.get(2);
            y = (double)(e.pos.get(1) / (float)scale);
            if(z < 1.0D) {
               y = (double)DSH - y;
            } else if(x < (double)(DW / 2)) {
               x = 10000.0D;
            } else if(x >= (double)(DW / 2)) {
               x = -10000.0D;
            }

            MCH_Config var10000;
            double MARK_SIZE;
            if(i == 0) {
               var10000 = MCH_MOD.config;
               MARK_SIZE = MCH_Config.EntityMarkerSize.prmDouble;
               if(e.type < MCH_TargetType.POINT.ordinal() && z < 1.0D && x >= 0.0D && x <= (double)DSW && y >= 0.0D && y <= (double)DSH) {
                  this.drawTriangle1(tessellator, x, y, MARK_SIZE, color);
               }
            } else if(e.type == MCH_TargetType.POINT.ordinal() && e.entity != null) {
               var10000 = MCH_MOD.config;
               MARK_SIZE = MCH_Config.BlockMarkerSize.prmDouble;
               double S;
               if(z < 1.0D && x >= 0.0D && x <= (double)(DSW - 20) && y >= 0.0D && y <= (double)(DSH - 40)) {
                  S = (double)super.mc.thePlayer.getDistanceToEntity(e.entity);
                  GL11.glEnable(3553);
                  this.drawCenteredString(String.format("%.0fm", new Object[]{Double.valueOf(S)}), (int)x, (int)(y + MARK_SIZE * 1.1D + 16.0D), color);
                  if(x >= (double)(DSW / 2 - 20) && x <= (double)(DSW / 2 + 20) && y >= (double)(DSH / 2 - 20) && y <= (double)(DSH / 2 + 20)) {
                     this.drawString(String.format("x : %.0f", new Object[]{Double.valueOf(e.entity.posX)}), (int)(x + MARK_SIZE + 18.0D), (int)y - 12, color);
                     this.drawString(String.format("y : %.0f", new Object[]{Double.valueOf(e.entity.posY)}), (int)(x + MARK_SIZE + 18.0D), (int)y - 4, color);
                     this.drawString(String.format("z : %.0f", new Object[]{Double.valueOf(e.entity.posZ)}), (int)(x + MARK_SIZE + 18.0D), (int)y + 4, color);
                  }

                  GL11.glDisable(3553);
                  tessellator.startDrawing(1);
                  drawRhombus(tessellator, 15, x, y, (double)super.zLevel, MARK_SIZE, color);
               } else {
                  tessellator.startDrawing(1);
                  S = 30.0D;
                  if(x < S) {
                     drawRhombus(tessellator, 1, S, (double)(DSH / 2), (double)super.zLevel, MARK_SIZE, color);
                  } else if(x > (double)DSW - S) {
                     drawRhombus(tessellator, 4, (double)DSW - S, (double)(DSH / 2), (double)super.zLevel, MARK_SIZE, color);
                  }

                  if(y < S) {
                     drawRhombus(tessellator, 8, (double)(DSW / 2), S, (double)super.zLevel, MARK_SIZE, color);
                  } else if(y > (double)DSH - S * 2.0D) {
                     drawRhombus(tessellator, 2, (double)(DSW / 2), (double)DSH - S * 2.0D, (double)super.zLevel, MARK_SIZE, color);
                  }
               }

               tessellator.draw();
            }
         }

         if(i == 0) {
            tessellator.draw();
         }
      }

      GL11.glDepthMask(true);
      GL11.glEnable(3553);
      GL11.glDisable(3042);
   }

   public static void drawRhombus(Tessellator tessellator, int dir, double x, double y, double z, double size, int color) {
      size *= 2.0D;
      tessellator.setColorRGBA_I(16777215 & color, color >> 24 & 255);
      double M = size / 3.0D;
      if((dir & 1) != 0) {
         tessellator.addVertex(x - size, y, z);
         tessellator.addVertex(x - size + M, y - M, z);
         tessellator.addVertex(x - size, y, z);
         tessellator.addVertex(x - size + M, y + M, z);
      }

      if((dir & 4) != 0) {
         tessellator.addVertex(x + size, y, z);
         tessellator.addVertex(x + size - M, y - M, z);
         tessellator.addVertex(x + size, y, z);
         tessellator.addVertex(x + size - M, y + M, z);
      }

      if((dir & 8) != 0) {
         tessellator.addVertex(x, y - size, z);
         tessellator.addVertex(x + M, y - size + M, z);
         tessellator.addVertex(x, y - size, z);
         tessellator.addVertex(x - M, y - size + M, z);
      }

      if((dir & 2) != 0) {
         tessellator.addVertex(x, y + size, z);
         tessellator.addVertex(x + M, y + size - M, z);
         tessellator.addVertex(x, y + size, z);
         tessellator.addVertex(x - M, y + size - M, z);
      }

   }

   public void drawTriangle1(Tessellator tessellator, double x, double y, double size, int color) {
      tessellator.setColorRGBA_I(16777215 & color, color >> 24 & 255);
      tessellator.addVertex(x + size / 2.0D, y - 10.0D - size, (double)super.zLevel);
      tessellator.addVertex(x - size / 2.0D, y - 10.0D - size, (double)super.zLevel);
      tessellator.addVertex(x + 0.0D, y - 10.0D, (double)super.zLevel);
   }

   public void drawTriangle2(Tessellator tessellator, double x, double y, double size, int color) {
      tessellator.setColorRGBA_I(8355711 & color, color >> 24 & 255);
      tessellator.addVertex(x + size / 2.0D, y - 10.0D - size, (double)super.zLevel);
      tessellator.addVertex(x - size / 2.0D, y - 10.0D - size, (double)super.zLevel);
      tessellator.addVertex(x - size / 2.0D, y - 10.0D - size, (double)super.zLevel);
      tessellator.addVertex(x + 0.0D, y - 10.0D, (double)super.zLevel);
      tessellator.addVertex(x + 0.0D, y - 10.0D, (double)super.zLevel);
      tessellator.addVertex(x + size / 2.0D, y - 10.0D - size, (double)super.zLevel);
   }

   public static void markPoint(int px, int py, int pz) {
      EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
      if(player != null && player.worldObj != null) {
         if(py < 1000) {
            MCH_ParticlesUtil.spawnMarkPoint(player, 0.5D + (double)px, 1.0D + (double)py, 0.5D + (double)pz);
         } else {
            MCH_ParticlesUtil.clearMarkPoint();
         }
      }

   }

}
