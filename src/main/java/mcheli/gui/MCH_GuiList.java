package mcheli.gui;

import mcheli.wrapper.*;
import net.minecraft.client.*;
import java.util.*;

public class MCH_GuiList extends W_GuiButton
{
   public List<MCH_GuiListItem> listItems;
   public MCH_GuiSliderVertical scrollBar;
   public final int maxRowNum;
   public MCH_GuiListItem lastPushItem;

   public MCH_GuiList(final int id, final int maxRow, final int posX, final int posY, final int w, final int h, final String name) {
      super(id, posX, posY, w, h, "");
      this.maxRowNum = ((maxRow > 0) ? maxRow : 1);
      this.listItems = new ArrayList<MCH_GuiListItem>();
      this.scrollBar = new MCH_GuiSliderVertical(0, posX + w - 20, posY, 20, h, name, 0.0f, 0.0f, 0.0f, 1.0f);
      this.lastPushItem = null;
   }

   public void drawButton(final Minecraft mc, final int x, final int y) {
      if (this.isVisible()) {
         drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, -2143272896);
         this.scrollBar.drawButton(mc, x, y);
         for (int i = 0; i < this.maxRowNum; ++i) {
            if (i + this.getStartRow() >= this.listItems.size()) {
               break;
            }
            final MCH_GuiListItem item = this.listItems.get(i + this.getStartRow());
            item.draw(mc, x, y, this.xPosition, this.yPosition + 5 + 20 * i);
         }
      }
   }

   public void addItem(final MCH_GuiListItem item) {
      this.listItems.add(item);
      final int listNum = this.listItems.size();
      this.scrollBar.valueMax = ((listNum > this.maxRowNum) ? ((float)(listNum - this.maxRowNum)) : 0.0f);
   }

   public MCH_GuiListItem getItem(final int i) {
      return (i < this.getItemNum()) ? this.listItems.get(i) : null;
   }

   public int getItemNum() {
      return this.listItems.size();
   }

   public void scrollUp(final float a) {
      if (this.isVisible()) {
         this.scrollBar.scrollUp(a);
      }
   }

   public void scrollDown(final float a) {
      if (this.isVisible()) {
         this.scrollBar.scrollDown(a);
      }
   }

   public int getStartRow() {
      final int startRow = (int)this.scrollBar.getSliderValue();
      return (startRow >= 0) ? startRow : 0;
   }

   protected void mouseDragged(final Minecraft mc, final int x, final int y) {
      if (this.isVisible()) {
         this.scrollBar.mouseDragged(mc, x, y);
      }
   }

   public boolean mousePressed(final Minecraft mc, final int x, final int y) {
      boolean b = false;
      if (this.isVisible()) {
         b |= this.scrollBar.mousePressed(mc, x, y);
         for (int i = 0; i < this.maxRowNum; ++i) {
            if (i + this.getStartRow() >= this.listItems.size()) {
               break;
            }
            final MCH_GuiListItem item = this.listItems.get(i + this.getStartRow());
            if (item.mousePressed(mc, x, y)) {
               this.lastPushItem = item;
               b = true;
            }
         }
      }
      return b;
   }

   public void mouseReleased(final int x, final int y) {
      if (this.isVisible()) {
         this.scrollBar.mouseReleased(x, y);
         for (final MCH_GuiListItem item : this.listItems) {
            item.mouseReleased(x, y);
         }
      }
   }
}