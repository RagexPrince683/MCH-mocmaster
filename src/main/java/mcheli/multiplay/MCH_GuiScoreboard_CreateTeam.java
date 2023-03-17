package mcheli.multiplay;

import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_ScaledResolution;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

public class MCH_GuiScoreboard_CreateTeam extends MCH_GuiScoreboard_Base {

   private GuiButton buttonCreateTeamOK;
   private GuiButton buttonCreateTeamFF;
   private GuiTextField editCreateTeamName;
   private static boolean friendlyFire = true;
   private int lastTeamColor = 0;
   private static final String[] colorNames = new String[]{"RESET", "BLACK", "DARK_BLUE", "DARK_GREEN", "DARK_AQUA", "DARK_RED", "DARK_PURPLE", "GOLD", "GRAY", "DARK_GRAY", "BLUE", "GREEN", "AQUA", "RED", "LIGHT_PURPLE", "YELLOW"};


   public MCH_GuiScoreboard_CreateTeam(MCH_IGuiScoreboard switcher, EntityPlayer player) {
      super(switcher, player);
   }

   public void initGui() {
      super.initGui();
      W_ScaledResolution sr = new W_ScaledResolution(super.mc, super.mc.displayWidth, super.mc.displayHeight);
      int factor = sr.getScaleFactor() > 0?sr.getScaleFactor():1;
      super.guiLeft = 0;
      super.guiTop = 0;
      int x = super.mc.displayWidth / 2 / factor;
      int y = super.mc.displayHeight / 2 / factor;
      GuiButton buttonCTNextC = new GuiButton(576, x + 40, y - 20, 40, 20, ">");
      GuiButton buttonCTPrevC = new GuiButton(577, x - 80, y - 20, 40, 20, "<");
      this.buttonCreateTeamFF = new GuiButton(560, x - 80, y + 20, 160, 20, "");
      this.buttonCreateTeamOK = new GuiButton(528, x - 80, y + 60, 80, 20, "OK");
      GuiButton buttonCTCancel = new GuiButton(544, x + 0, y + 60, 80, 20, "Cancel");
      this.editCreateTeamName = new GuiTextField(super.fontRendererObj, x - 80, y - 55, 160, 20);
      this.editCreateTeamName.setText("");
      this.editCreateTeamName.setTextColor(-1);
      this.editCreateTeamName.setMaxStringLength(16);
      this.editCreateTeamName.setFocused(true);
      super.listGui.add(buttonCTNextC);
      super.listGui.add(buttonCTPrevC);
      super.listGui.add(this.buttonCreateTeamFF);
      super.listGui.add(this.buttonCreateTeamOK);
      super.listGui.add(buttonCTCancel);
      super.listGui.add(this.editCreateTeamName);
   }

   public void updateScreen() {
      String teamName = this.editCreateTeamName.getText();
      this.buttonCreateTeamOK.enabled = teamName.length() > 0 && teamName.length() <= 16;
      this.editCreateTeamName.updateCursorCounter();
      this.buttonCreateTeamFF.displayString = "Friendly Fire : " + (friendlyFire?"ON":"OFF");
   }

   public void acviveScreen() {
      this.editCreateTeamName.setText("");
      this.editCreateTeamName.setFocused(true);
   }

   protected void keyTyped(char c, int code) {
      if(code == 1) {
         this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
      } else {
         this.editCreateTeamName.textboxKeyTyped(c, code);
      }

   }

   protected void mouseClicked(int p_73864_1_, int p_73864_2_, int p_73864_3_) {
      this.editCreateTeamName.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
      super.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
   }

   protected void actionPerformed(GuiButton btn) {
      if(btn != null && btn.enabled) {
         switch(btn.id) {
         case 528:
            String teamName = this.editCreateTeamName.getText();
            if(teamName.length() > 0 && teamName.length() <= 16) {
               MCH_PacketIndMultiplayCommand.send(768, "scoreboard teams add " + teamName);
               MCH_PacketIndMultiplayCommand.send(768, "scoreboard teams option " + teamName + " color " + colorNames[this.lastTeamColor]);
               MCH_PacketIndMultiplayCommand.send(768, "scoreboard teams option " + teamName + " friendlyfire " + friendlyFire);
            }

            this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
            break;
         case 544:
            this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
            break;
         case 560:
            friendlyFire = !friendlyFire;
            break;
         case 576:
            ++this.lastTeamColor;
            if(this.lastTeamColor >= colorNames.length) {
               this.lastTeamColor = 0;
            }
            break;
         case 577:
            --this.lastTeamColor;
            if(this.lastTeamColor < 0) {
               this.lastTeamColor = colorNames.length - 1;
            }
         }
      }

   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      drawList(super.mc, super.fontRendererObj, true);
      W_ScaledResolution sr = new W_ScaledResolution(super.mc, super.mc.displayWidth, super.mc.displayHeight);
      int factor = sr.getScaleFactor() > 0?sr.getScaleFactor():1;
      W_McClient.MOD_bindTexture("textures/gui/mp_new_team.png");
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      int x = (super.mc.displayWidth / factor - 222) / 2;
      int y = (super.mc.displayHeight / factor - 200) / 2;
      this.drawTexturedModalRect(x, y, 0, 0, 222, 200);
      x = super.mc.displayWidth / 2 / factor;
      y = super.mc.displayHeight / 2 / factor;
      this.drawCenteredString("Create team", x, y - 85, -1);
      this.drawCenteredString("Team name", x, y - 70, -1);
      EnumChatFormatting ecf = EnumChatFormatting.getValueByName(colorNames[this.lastTeamColor]);
      this.drawCenteredString(ecf + "Team Color" + ecf, x, y - 13, -1);
      this.editCreateTeamName.drawTextBox();
   }

}
