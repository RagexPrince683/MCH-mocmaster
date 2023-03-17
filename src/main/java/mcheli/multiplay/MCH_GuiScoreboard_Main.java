package mcheli.multiplay;

import mcheli.MCH_ServerSettings;
import mcheli.wrapper.W_GuiButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Iterator;
import java.util.List;

public class MCH_GuiScoreboard_Main extends MCH_GuiScoreboard_Base {

   private W_GuiButton buttonSwitchPVP;


   public MCH_GuiScoreboard_Main(MCH_IGuiScoreboard switcher, EntityPlayer player) {
      super(switcher, player);
   }

   public void initGui() {
      super.initGui();
      if(this.buttonSwitchPVP == null) {
         super.guiLeft = 0;
         super.guiTop = 0;
         int WIDTH = getScoreboradWidth(super.mc) * 3 / 4;
         if(WIDTH < 80) {
            WIDTH = 80;
         }

         int LEFT = getScoreBoardLeft(super.mc, this.getTeamNum() + 1, 0) / 4;
         this.buttonSwitchPVP = new W_GuiButton(1024, LEFT, 80, WIDTH, 20, "");
         super.listGui.add(this.buttonSwitchPVP);
         W_GuiButton btn = new W_GuiButton(256, LEFT, 100, WIDTH, 20, "Team shuffle");
         btn.addHoverString("Shuffle all players.");
         super.listGui.add(btn);
         super.listGui.add(new W_GuiButton(512, LEFT, 120, WIDTH, 20, "New team"));
         btn = new W_GuiButton(768, LEFT, 140, WIDTH, 20, "Jump spawn pos");
         btn.addHoverString("Teleport all players -> spawn point.");
         super.listGui.add(btn);
         btn = new W_GuiButton(1280, LEFT, 160, WIDTH, 20, "Destroy All");
         btn.addHoverString("Destroy all aircraft and vehicle.");
         super.listGui.add(btn);
      }
   }

   protected void keyTyped(char c, int code) {
      if(code == 1) {
         super.mc.thePlayer.closeScreen();
      }

   }

   public void updateScreenButtons(List list) {
      Iterator i$ = list.iterator();

      while(i$.hasNext()) {
         Object o = i$.next();
         GuiButton button = (GuiButton)o;
         if(button.id == 1024) {
            button.displayString = "PVP : " + (MCH_ServerSettings.enablePVP?"ON":"OFF");
         }
      }

   }

   protected void actionPerformed(GuiButton btn) {
      if(btn != null && btn.enabled) {
         switch(btn.id) {
         case 256:
            MCH_PacketIndMultiplayCommand.send(256, "");
            break;
         case 512:
            this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.CREATE_TEAM);
            break;
         case 768:
            MCH_PacketIndMultiplayCommand.send(512, "");
            break;
         case 1024:
            MCH_PacketIndMultiplayCommand.send(1024, "");
            break;
         case 1280:
            MCH_PacketIndMultiplayCommand.send(1280, "");
         }
      }

   }

   public void drawGuiContainerForegroundLayerScreen(int x, int y) {
      super.drawGuiContainerForegroundLayerScreen(x, y);
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      drawList(super.mc, super.fontRendererObj, true);
   }
}
