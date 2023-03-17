package mcheli.multiplay;

import mcheli.wrapper.W_GuiButton;
import mcheli.wrapper.W_GuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MCH_GuiScoreboard extends W_GuiContainer implements MCH_IGuiScoreboard {

   public final EntityPlayer thePlayer;
   private MCH_GuiScoreboard_Base.SCREEN_ID screenID;
   private Map listScreen;
   private int lastTeamNum = 0;


   public MCH_GuiScoreboard(EntityPlayer player) {
      super(new MCH_ContainerScoreboard(player));
      this.thePlayer = player;
   }

   public void initGui() {
      Keyboard.enableRepeatEvents(true);
      super.initGui();
      super.buttonList.clear();
      super.labelList.clear();
      super.guiLeft = 0;
      super.guiTop = 0;
      this.listScreen = new HashMap();
      this.listScreen.put(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN, new MCH_GuiScoreboard_Main(this, this.thePlayer));
      this.listScreen.put(MCH_GuiScoreboard_Base.SCREEN_ID.CREATE_TEAM, new MCH_GuiScoreboard_CreateTeam(this, this.thePlayer));
      Iterator i$ = this.listScreen.values().iterator();

      while(i$.hasNext()) {
         MCH_GuiScoreboard_Base s = (MCH_GuiScoreboard_Base)i$.next();
         s.initGui(super.buttonList, this);
      }

      this.lastTeamNum = super.mc.theWorld.getScoreboard().getTeams().size();
      this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
   }

   public void updateScreen() {
      super.updateScreen();
      int nowTeamNum = super.mc.theWorld.getScoreboard().getTeams().size();
      if(this.lastTeamNum != nowTeamNum) {
         this.lastTeamNum = nowTeamNum;
         this.initGui();
      }

      Iterator i$ = this.listScreen.values().iterator();

      while(i$.hasNext()) {
         MCH_GuiScoreboard_Base s = (MCH_GuiScoreboard_Base)i$.next();

         try {
            s.updateScreenButtons(super.buttonList);
            s.updateScreen();
         } catch (Exception var5) {
            ;
         }
      }

   }

   public void switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID id) {
      Iterator i$ = this.listScreen.values().iterator();

      while(i$.hasNext()) {
         MCH_GuiScoreboard_Base b = (MCH_GuiScoreboard_Base)i$.next();
         b.leaveScreen();
      }

      this.screenID = id;
      this.getCurrentScreen().onSwitchScreen();
   }

   private MCH_GuiScoreboard_Base getCurrentScreen() {
      return (MCH_GuiScoreboard_Base)this.listScreen.get(this.screenID);
   }

   public static void setVisible(Object g, boolean v) {
      if(g instanceof GuiButton) {
         ((GuiButton)g).visible = v;
      }

      if(g instanceof GuiTextField) {
         ((GuiTextField)g).setVisible(v);
      }

   }

   protected void keyTyped(char c, int code) {
      this.getCurrentScreen().keyTypedScreen(c, code);
   }

   protected void mouseClicked(int p_73864_1_, int p_73864_2_, int p_73864_3_) {
      try {
         Iterator e = this.listScreen.values().iterator();

         while(e.hasNext()) {
            MCH_GuiScoreboard_Base s = (MCH_GuiScoreboard_Base)e.next();
            s.mouseClickedScreen(p_73864_1_, p_73864_2_, p_73864_3_);
         }

         super.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
      } catch (Exception var6) {
         ;
      }

   }

   protected void actionPerformed(GuiButton btn) {
      if(btn != null && btn.enabled) {
         this.getCurrentScreen().actionPerformedScreen(btn);
      }

   }

   public void drawDefaultBackground() {}

   public void drawBackground(int p_146278_1_) {
      GL11.glDisable(2896);
      GL11.glDisable(2912);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected void drawGuiContainerForegroundLayer(int x, int y) {
      this.getCurrentScreen().drawGuiContainerForegroundLayerScreen(x, y);
      Iterator i$ = super.buttonList.iterator();

      while(i$.hasNext()) {
         Object o = i$.next();
         if(o instanceof W_GuiButton) {
            W_GuiButton btn = (W_GuiButton)o;
            if(btn.isOnMouseOver() && btn.hoverStringList != null) {
               this.drawHoveringText(btn.hoverStringList, x, y, super.fontRendererObj);
               break;
            }
         }
      }

   }

   public static void drawList(Minecraft mc, FontRenderer fontRendererObj, boolean mng) {
      MCH_GuiScoreboard_Base.drawList(mc, fontRendererObj, mng);
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      this.getCurrentScreen().drawGuiContainerBackgroundLayer(par1, par2, par3);
   }

   public void setWorldAndResolution(Minecraft p_146280_1_, int p_146280_2_, int p_146280_3_) {
      super.setWorldAndResolution(p_146280_1_, p_146280_2_, p_146280_3_);
      Iterator i$ = this.listScreen.values().iterator();

      while(i$.hasNext()) {
         MCH_GuiScoreboard_Base s = (MCH_GuiScoreboard_Base)i$.next();
         s.setWorldAndResolution(p_146280_1_, p_146280_2_, p_146280_3_);
      }

   }
}
