package mcheli.command;

import com.google.common.collect.Lists;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.gui.MCH_Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SideOnly(Side.CLIENT)
public class MCH_GuiTitle extends MCH_Gui {

   private final List chatLines = new ArrayList();
   private int prevPlayerTick = 0;
   private int restShowTick = 0;
   private int showTick = 0;
   private float colorAlpha = 0.0F;
   private int position = 0;
   private static Minecraft s_minecraft;


   public MCH_GuiTitle(Minecraft minecraft) {
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
      if(this.restShowTick > 0 && this.chatLines.size() > 0 && player != null && player.worldObj != null) {
         if(this.prevPlayerTick != player.ticksExisted) {
            ++this.showTick;
            --this.restShowTick;
         }

         this.prevPlayerTick = player.ticksExisted;
      }

      return this.restShowTick > 0;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      GL11.glLineWidth((float)(MCH_Gui.scaleFactor * 2));
      GL11.glDisable(3042);
      if(MCH_Gui.scaleFactor <= 0) {
         MCH_Gui.scaleFactor = 1;
      }

      this.colorAlpha = 1.0F;
      if(this.restShowTick > 20 && this.showTick < 5) {
         this.colorAlpha = 0.2F * (float)this.showTick;
      }

      if(this.showTick > 0 && this.restShowTick < 5) {
         this.colorAlpha = 0.2F * (float)this.restShowTick;
      }

      this.drawChat();
   }

   private String func_146235_b(String s) {
      return Minecraft.getMinecraft().gameSettings.chatColours?s:EnumChatFormatting.getTextWithoutFormattingCodes(s);
   }

   private int func_146233_a() {
      short short1 = 320;
      byte b0 = 40;
      return MathHelper.floor_float(super.mc.gameSettings.chatWidth * (float)(short1 - b0) + (float)b0);
   }

   public void setupTitle(IChatComponent chatComponent, int showTime, int pos) {
      byte displayTime = 20;
      byte line = 0;
      this.chatLines.clear();
      this.position = pos;
      this.showTick = 0;
      this.restShowTick = showTime;
      int k = MathHelper.floor_float((float)this.func_146233_a() / super.mc.gameSettings.chatScale);
      int l = 0;
      ChatComponentText chatcomponenttext = new ChatComponentText("");
      ArrayList arraylist = Lists.newArrayList();
      ArrayList arraylist1 = Lists.newArrayList(chatComponent);

      for(int ichatcomponent2 = 0; ichatcomponent2 < arraylist1.size(); ++ichatcomponent2) {
         IChatComponent iterator = (IChatComponent)arraylist1.get(ichatcomponent2);
         String[] splitLine = (iterator.getUnformattedTextForChat() + "").split("\n");
         int lineCnt = 0;
         String[] arr$ = splitLine;
         int len$ = splitLine.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String sLine = arr$[i$];
            String s = this.func_146235_b(iterator.getChatStyle().getFormattingCode() + sLine);
            int j1 = super.mc.fontRenderer.getStringWidth(s);
            ChatComponentText chatcomponenttext1 = new ChatComponentText(s);
            chatcomponenttext1.setChatStyle(iterator.getChatStyle().createShallowCopy());
            boolean flag1 = false;
            if(l + j1 > k) {
               String s1 = super.mc.fontRenderer.trimStringToWidth(s, k - l, false);
               String s2 = s1.length() < s.length()?s.substring(s1.length()):null;
               if(s2 != null && s2.length() > 0) {
                  int k1 = s1.lastIndexOf(" ");
                  if(k1 >= 0 && super.mc.fontRenderer.getStringWidth(s.substring(0, k1)) > 0) {
                     s1 = s.substring(0, k1);
                     s2 = s.substring(k1);
                  }

                  ChatComponentText chatcomponenttext2 = new ChatComponentText(s2);
                  chatcomponenttext2.setChatStyle(iterator.getChatStyle().createShallowCopy());
                  arraylist1.add(ichatcomponent2 + 1, chatcomponenttext2);
               }

               j1 = super.mc.fontRenderer.getStringWidth(s1);
               chatcomponenttext1 = new ChatComponentText(s1);
               chatcomponenttext1.setChatStyle(iterator.getChatStyle().createShallowCopy());
               flag1 = true;
            }

            if(l + j1 <= k) {
               l += j1;
               chatcomponenttext.appendSibling(chatcomponenttext1);
            } else {
               flag1 = true;
            }

            if(flag1) {
               arraylist.add(chatcomponenttext);
               l = 0;
               chatcomponenttext = new ChatComponentText("");
            }

            ++lineCnt;
            if(lineCnt < splitLine.length) {
               arraylist.add(chatcomponenttext);
               l = 0;
               chatcomponenttext = new ChatComponentText("");
            }
         }
      }

      arraylist.add(chatcomponenttext);
      Iterator var28 = arraylist.iterator();

      while(var28.hasNext()) {
         IChatComponent var27 = (IChatComponent)var28.next();
         this.chatLines.add(new ChatLine(displayTime, var27, line));
      }

      while(this.chatLines.size() > 100) {
         this.chatLines.remove(this.chatLines.size() - 1);
      }

   }

   private int func_146243_b() {
      short short1 = 180;
      byte b0 = 20;
      return MathHelper.floor_float(super.mc.gameSettings.chatHeightFocused * (float)(short1 - b0) + (float)b0);
   }

   private void drawChat() {
      float charAlpha = super.mc.gameSettings.chatOpacity * 0.9F + 0.1F;
      float scale = super.mc.gameSettings.chatScale * 2.0F;
      GL11.glPushMatrix();
      float posY = 0.0F;
      switch(this.position) {
      case 0:
      default:
         posY = (float)(super.mc.displayHeight / 2 / MCH_Gui.scaleFactor) - (float)this.chatLines.size() / 2.0F * 9.0F * scale;
         break;
      case 1:
         posY = 0.0F;
         break;
      case 2:
         posY = (float)(super.mc.displayHeight / MCH_Gui.scaleFactor) - (float)this.chatLines.size() * 9.0F * scale;
         break;
      case 3:
         posY = (float)(super.mc.displayHeight / 3 / MCH_Gui.scaleFactor) - (float)this.chatLines.size() / 2.0F * 9.0F * scale;
         break;
      case 4:
         posY = (float)(super.mc.displayHeight * 2 / 3 / MCH_Gui.scaleFactor) - (float)this.chatLines.size() / 2.0F * 9.0F * scale;
      }

      GL11.glTranslatef(0.0F, posY, 0.0F);
      GL11.glScalef(scale, scale, 1.0F);

      for(int i = 0; i < this.chatLines.size(); ++i) {
         ChatLine chatline = (ChatLine)this.chatLines.get(i);
         if(chatline != null) {
            int alpha = (int)(255.0F * charAlpha * this.colorAlpha);
            int y = i * 9;
            drawRect(0, y + 9, super.mc.displayWidth, y, alpha / 2 << 24);
            GL11.glEnable(3042);
            String s = chatline.func_151461_a().getFormattedText();
            int sw = super.mc.displayWidth / 2 / MCH_Gui.scaleFactor - super.mc.fontRenderer.getStringWidth(s);
            sw = (int)((float)sw / scale);
            super.mc.fontRenderer.drawStringWithShadow(s, sw, y + 1, 16777215 + (alpha << 24));
            GL11.glDisable(3008);
         }
      }

      GL11.glTranslatef(-3.0F, 0.0F, 0.0F);
      GL11.glPopMatrix();
   }
}
