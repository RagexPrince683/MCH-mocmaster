package mcheli.multiplay;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import org.lwjgl.input.*;
import org.lwjgl.opengl.*;
import mcheli.wrapper.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;

public class MCH_GuiScoreboard extends W_GuiContainer implements MCH_IGuiScoreboard
{
   public final EntityPlayer thePlayer;
   private MCH_GuiScoreboard_Base.SCREEN_ID screenID;
   private Map<MCH_GuiScoreboard_Base.SCREEN_ID, MCH_GuiScoreboard_Base> listScreen;
   private int lastTeamNum;

   public MCH_GuiScoreboard(final EntityPlayer player) {
      super((Container)new MCH_ContainerScoreboard(player));
      this.lastTeamNum = 0;
      this.thePlayer = player;
   }

   public void initGui() {
      Keyboard.enableRepeatEvents(true);
      super.initGui();
      this.buttonList.clear();
      this.labelList.clear();
      this.guiLeft = 0;
      this.guiTop = 0;
      (this.listScreen = new HashMap<MCH_GuiScoreboard_Base.SCREEN_ID, MCH_GuiScoreboard_Base>()).put(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN, new MCH_GuiScoreboard_Main(this, this.thePlayer));
      this.listScreen.put(MCH_GuiScoreboard_Base.SCREEN_ID.CREATE_TEAM, new MCH_GuiScoreboard_CreateTeam(this, this.thePlayer));
      for (final MCH_GuiScoreboard_Base s : this.listScreen.values()) {
         s.initGui(this.buttonList, (GuiScreen)this);
      }
      this.lastTeamNum = this.mc.theWorld.getScoreboard().getTeams().size();
      this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
   }

   public void updateScreen() {
      super.updateScreen();
      final int nowTeamNum = this.mc.theWorld.getScoreboard().getTeams().size();
      if (this.lastTeamNum != nowTeamNum) {
         this.lastTeamNum = nowTeamNum;
         this.initGui();
      }
      for (final MCH_GuiScoreboard_Base s : this.listScreen.values()) {
         try {
            s.updateScreenButtons(this.buttonList);
            s.updateScreen();
         }
         catch (Exception ex) {}
      }
   }

   @Override
   public void switchScreen(final MCH_GuiScoreboard_Base.SCREEN_ID id) {
      for (final MCH_GuiScoreboard_Base b : this.listScreen.values()) {
         b.leaveScreen();
      }
      this.screenID = id;
      this.getCurrentScreen().onSwitchScreen();
   }

   private MCH_GuiScoreboard_Base getCurrentScreen() {
      return this.listScreen.get(this.screenID);
   }

   public static void setVisible(final Object g, final boolean v) {
      if (g instanceof GuiButton) {
         ((GuiButton)g).visible = v;
      }
      if (g instanceof GuiTextField) {
         ((GuiTextField)g).setVisible(v);
      }
   }

   protected void keyTyped(final char c, final int code) {
      this.getCurrentScreen().keyTypedScreen(c, code);
   }

   protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
      try {
         for (final MCH_GuiScoreboard_Base s : this.listScreen.values()) {
            s.mouseClickedScreen(mouseX, mouseY, mouseButton);
         }
         super.mouseClicked(mouseX, mouseY, mouseButton);
      }
      catch (Exception ex) {}
   }

   protected void actionPerformed(final GuiButton btn) {
      if (btn != null && btn.enabled) {
         this.getCurrentScreen().actionPerformedScreen(btn);
      }
   }

   public void drawDefaultBackground() {
   }

   public void drawBackground(final int tint) {
      GL11.glDisable(2896);
      GL11.glDisable(2912);
      GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
   }

   protected void drawGuiContainerForegroundLayer(final int x, final int y) {
      this.getCurrentScreen().drawGuiContainerForegroundLayerScreen(x, y);
      for (final Object o : this.buttonList) {
         if (o instanceof W_GuiButton) {
            final W_GuiButton btn = (W_GuiButton)o;
            if (btn.isOnMouseOver() && btn.hoverStringList != null) {
               this.drawHoveringText((List)btn.hoverStringList, x, y, this.fontRendererObj);
               break;
            }
            continue;
         }
      }
   }

   public static void drawList(final Minecraft mc, final FontRenderer fontRendererObj, final boolean mng) {
      MCH_GuiScoreboard_Base.drawList(mc, fontRendererObj, mng);
   }

   protected void drawGuiContainerBackgroundLayer(final float par1, final int par2, final int par3) {
      this.getCurrentScreen().drawGuiContainerBackgroundLayer(par1, par2, par3);
   }

   public void setWorldAndResolution(final Minecraft mc, final int width, final int height) {
      super.setWorldAndResolution(mc, width, height);
      for (final MCH_GuiScoreboard_Base s : this.listScreen.values()) {
         s.setWorldAndResolution(mc, width, height);
      }
   }
}