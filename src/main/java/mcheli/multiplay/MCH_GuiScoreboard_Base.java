package mcheli.multiplay;

import mcheli.wrapper.W_GuiContainer;
import mcheli.wrapper.W_ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.util.*;

public abstract class MCH_GuiScoreboard_Base extends W_GuiContainer {

   public List listGui;
   public static final int BUTTON_ID_SHUFFLE = 256;
   public static final int BUTTON_ID_CREATE_TEAM = 512;
   public static final int BUTTON_ID_CREATE_TEAM_OK = 528;
   public static final int BUTTON_ID_CREATE_TEAM_CANCEL = 544;
   public static final int BUTTON_ID_CREATE_TEAM_FF = 560;
   public static final int BUTTON_ID_CREATE_TEAM_NEXT_C = 576;
   public static final int BUTTON_ID_CREATE_TEAM_PREV_C = 577;
   public static final int BUTTON_ID_JUMP_SPAWN_POINT = 768;
   public static final int BUTTON_ID_SWITCH_PVP = 1024;
   public static final int BUTTON_ID_DESTORY_ALL = 1280;
   private MCH_IGuiScoreboard screen_switcher;


   public MCH_GuiScoreboard_Base(MCH_IGuiScoreboard switcher, EntityPlayer player) {
      super(new MCH_ContainerScoreboard(player));
      this.screen_switcher = switcher;
      super.mc = Minecraft.getMinecraft();
   }

   public void initGui() {}

   public void initGui(List buttonList, GuiScreen parents) {
      this.listGui = new ArrayList();
      super.mc = Minecraft.getMinecraft();
      super.fontRendererObj = super.mc.fontRenderer;
      super.width = parents.width;
      super.height = parents.height;
      this.initGui();
      Iterator i$ = this.listGui.iterator();

      while(i$.hasNext()) {
         Gui b = (Gui)i$.next();
         if(b instanceof GuiButton) {
            buttonList.add(b);
         }
      }

      super.buttonList.clear();
   }

   public static void setVisible(Object g, boolean v) {
      if(g instanceof GuiButton) {
         ((GuiButton)g).visible = v;
      }

      if(g instanceof GuiTextField) {
         ((GuiTextField)g).setVisible(v);
      }

   }

   public void updateScreenButtons(List list) {}

   protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {}

   public int getTeamNum() {
      return super.mc.theWorld.getScoreboard().getTeams().size();
   }

   protected void acviveScreen() {}

   public void onSwitchScreen() {
      Iterator i$ = this.listGui.iterator();

      while(i$.hasNext()) {
         Gui b = (Gui)i$.next();
         setVisible(b, true);
      }

      this.acviveScreen();
   }

   public void leaveScreen() {
      Iterator i$ = this.listGui.iterator();

      while(i$.hasNext()) {
         Gui b = (Gui)i$.next();
         setVisible(b, false);
      }

   }

   public void keyTypedScreen(char c, int code) {
      this.keyTyped(c, code);
   }

   public void mouseClickedScreen(int p_73864_1_, int p_73864_2_, int p_73864_3_) {
      try {
         this.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
      } catch (Exception var7) {
         if(p_73864_3_ == 0) {
            for(int l = 0; l < super.buttonList.size(); ++l) {
               GuiButton guibutton = (GuiButton)super.buttonList.get(l);
               if(guibutton.mousePressed(super.mc, p_73864_1_, p_73864_2_)) {
                  guibutton.func_146113_a(super.mc.getSoundHandler());
                  this.actionPerformed(guibutton);
               }
            }
         }
      }

   }

   public void drawGuiContainerForegroundLayerScreen(int param1, int param2) {
      this.drawGuiContainerForegroundLayer(param1, param2);
   }

   protected void actionPerformedScreen(GuiButton btn) {
      this.actionPerformed(btn);
   }

   public void switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID id) {
      this.screen_switcher.switchScreen(id);
   }

   public static int getScoreboradWidth(Minecraft mc) {
      W_ScaledResolution scaledresolution = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
      int ScaledWidth = scaledresolution.getScaledWidth() - 40;
      int width = ScaledWidth * 3 / 4 / (mc.theWorld.getScoreboard().getTeams().size() + 1);
      if(width > 150) {
         width = 150;
      }

      return width;
   }

   public static int getScoreBoardLeft(Minecraft mc, int teamNum, int teamIndex) {
      W_ScaledResolution scaledresolution = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
      int ScaledWidth = scaledresolution.getScaledWidth();
      return (int)((double)(ScaledWidth / 2) + (double)(getScoreboradWidth(mc) + 10) * (-((double)teamNum) / 2.0D + (double)teamIndex));
   }

   public static void drawList(Minecraft mc, FontRenderer fontRendererObj, boolean mng) {
      ArrayList teamList = new ArrayList();
      teamList.add((Object)null);
      Iterator i = mc.theWorld.getScoreboard().getTeams().iterator();

      while(i.hasNext()) {
         Object team = i.next();
         teamList.add((ScorePlayerTeam)team);
      }


      Collections.sort(teamList, new Comparator<ScorePlayerTeam>(){
         public int compare(ScorePlayerTeam o1, ScorePlayerTeam o2) {
            return o1 == null && o2 == null?0:(o1 == null?-1:(o2 == null?1:o1.getRegisteredName().compareTo(o2.getRegisteredName())));
         }
      });

      for(int var6 = 0; var6 < teamList.size(); ++var6) {
         if(mng) {
            drawPlayersList(mc, fontRendererObj, (ScorePlayerTeam)teamList.get(var6), 1 + var6, 1 + teamList.size());
         } else {
            drawPlayersList(mc, fontRendererObj, (ScorePlayerTeam)teamList.get(var6), var6, teamList.size());
         }
      }

   }

   public static void drawPlayersList(Minecraft mc, FontRenderer fontRendererObj, ScorePlayerTeam team, int teamIndex, int teamNum) {
      W_ScaledResolution scaledresolution = new W_ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
      int ScaledWidth = scaledresolution.getScaledWidth();
      int ScaledHeight = scaledresolution.getScaledHeight();
      ScoreObjective scoreobjective = mc.theWorld.getScoreboard().func_96539_a(0);
      NetHandlerPlayClient nethandlerplayclient = mc.thePlayer.sendQueue;
      List list = nethandlerplayclient.playerInfoList;
      int MaxPlayers = (list.size() / 5 + 1) * 5;
      MaxPlayers = MaxPlayers < 10?10:MaxPlayers;
      if(MaxPlayers > nethandlerplayclient.currentServerMaxPlayers) {
         MaxPlayers = nethandlerplayclient.currentServerMaxPlayers;
      }

      int width = getScoreboradWidth(mc);
      int listLeft = getScoreBoardLeft(mc, teamNum, teamIndex);
      int listTop = ScaledHeight / 2 - (MaxPlayers * 9 + 10) / 2;
      drawRect(listLeft - 1, listTop - 1 - 18, listLeft + width, listTop + 9 * MaxPlayers, Integer.MIN_VALUE);
      String teamName = ScorePlayerTeam.formatPlayerName(team, team == null?"No team":team.getRegisteredName());
      int teamNameX = listLeft + width / 2 - fontRendererObj.getStringWidth(teamName) / 2;
      fontRendererObj.drawStringWithShadow(teamName, teamNameX, listTop - 18, -1);
      String ff_onoff = "FriendlyFire : " + (team == null?"ON":(team.getAllowFriendlyFire()?"ON":"OFF"));
      int ff_onoffX = listLeft + width / 2 - fontRendererObj.getStringWidth(ff_onoff) / 2;
      fontRendererObj.drawStringWithShadow(ff_onoff, ff_onoffX, listTop - 9, -1);
      int drawY = 0;

      for(int i = 0; i < MaxPlayers; ++i) {
         int y = listTop + drawY * 9;
         int rectY = listTop + i * 9;
         drawRect(listLeft, rectY, listLeft + width - 1, rectY + 8, 553648127);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glEnable(3008);
         if(i < list.size()) {
            GuiPlayerInfo guiplayerinfo = (GuiPlayerInfo)list.get(i);
            String playerName = guiplayerinfo.name;
            ScorePlayerTeam steam = mc.theWorld.getScoreboard().getPlayersTeam(playerName);
            if(steam == null && team == null || steam != null && team != null && steam.isSameTeam(team)) {
               ++drawY;
               fontRendererObj.drawStringWithShadow(playerName, listLeft, y, -1);
               if(scoreobjective != null) {
                  int j4 = listLeft + fontRendererObj.getStringWidth(playerName) + 5;
                  int k4 = listLeft + width - 12 - 5;
                  if(k4 - j4 > 5) {
                     Score score = scoreobjective.getScoreboard().func_96529_a(guiplayerinfo.name, scoreobjective);
                     String s1 = EnumChatFormatting.YELLOW + "" + score.getScorePoints();
                     fontRendererObj.drawStringWithShadow(s1, k4 - fontRendererObj.getStringWidth(s1), y, 16777215);
                  }
               }

               drawResponseTime(listLeft + width - 12, y, guiplayerinfo.responseTime);
            }
         }
      }

   }

   public static void drawResponseTime(int x, int y, int responseTime) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
      byte b2;
      if(responseTime < 0) {
         b2 = 5;
      } else if(responseTime < 150) {
         b2 = 0;
      } else if(responseTime < 300) {
         b2 = 1;
      } else if(responseTime < 600) {
         b2 = 2;
      } else if(responseTime < 1000) {
         b2 = 3;
      } else {
         b2 = 4;
      }

      static_drawTexturedModalRect(x, y, 0, 176 + b2 * 8, 10, 8, 0.0D);
   }

   public static void static_drawTexturedModalRect(int x, int y, int x2, int y2, int x3, int y3, double zLevel) {
      float f = 0.00390625F;
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawingQuads();
      tessellator.addVertexWithUV((double)(x + 0), (double)(y + y3), zLevel, (double)((float)(x2 + 0) * 0.00390625F), (double)((float)(y2 + y3) * 0.00390625F));
      tessellator.addVertexWithUV((double)(x + x3), (double)(y + y3), zLevel, (double)((float)(x2 + x3) * 0.00390625F), (double)((float)(y2 + y3) * 0.00390625F));
      tessellator.addVertexWithUV((double)(x + x3), (double)(y + 0), zLevel, (double)((float)(x2 + x3) * 0.00390625F), (double)((float)(y2 + 0) * 0.00390625F));
      tessellator.addVertexWithUV((double)(x + 0), (double)(y + 0), zLevel, (double)((float)(x2 + 0) * 0.00390625F), (double)((float)(y2 + 0) * 0.00390625F));
      tessellator.draw();
   }

   protected static enum SCREEN_ID {

      MAIN("MAIN", 0),
      CREATE_TEAM("CREATE_TEAM", 1);
      // $FF: synthetic field
      private static final MCH_GuiScoreboard_Base.SCREEN_ID[] $VALUES = new MCH_GuiScoreboard_Base.SCREEN_ID[]{MAIN, CREATE_TEAM};


      private SCREEN_ID(String var1, int var2) {}

   }
}
