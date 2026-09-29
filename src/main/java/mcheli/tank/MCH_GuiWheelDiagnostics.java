package mcheli.tank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.network.packets.PacketWheelDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Wrapped, paged server/client columns with a persistent snapshot identity/age header. */
@SideOnly(Side.CLIENT)
public final class MCH_GuiWheelDiagnostics {
   private static int page;
   private static boolean pageUp, pageDown;
   private MCH_GuiWheelDiagnostics() {}
   public static boolean visible(EntityPlayer player) {
      return MCH_Config.TestMode.prmBool && PacketWheelDiagnostics.isVisible(player);
   }
   public static void draw(Minecraft mc, int width, int height) {
      if(!visible(mc.thePlayer)) return;
      MCH_EntityTank car = MCH_WheelDiagnostics.riddenCar(mc.thePlayer);
      PacketWheelDiagnostics packet = PacketWheelDiagnostics.snapshot(mc.thePlayer);
      List<String> left = new ArrayList<String>();
      if(packet != null) left.addAll(packet.left);
      List<String> right = new ArrayList<String>();
      right.add("Wheel diagnostics: S=server, C=client");
      right.add("C body pitch=" + MCH_WheelDiagnostics.n(car.getRotPitch()) + " deg");
      right.add("C horizontal velocity=" + MCH_WheelDiagnostics.n(car.motionX) + "," + MCH_WheelDiagnostics.n(car.motionZ));
      if(packet != null) right.addAll(packet.right);
      else right.add("S wheels/contact/suspension=N/A");
      int leftWidth = textWidth(mc, left) + 12, rightWidth = textWidth(mc, right) + 12;
      int panelHeight = Math.max(left.size(), right.size()) * 10 + 12;
      float scale = Math.min(1.0F, Math.min((width - 8.0F) / (leftWidth + rightWidth + 8),
            (height - 8.0F) / panelHeight));
      // Prefer at least two physical pixels per font pixel. Wrap and page on
      // small windows instead of making every diagnostic illegible.
      scale = Math.max(Math.min(1.0F, 2.0F / mcheli.gui.MCH_Gui.scaleFactor), scale);
      int availableWidth = (int)((width - 8) / scale);
      List<String> header = wrap(mc, java.util.Collections.singletonList(PacketWheelDiagnostics.status(mc.thePlayer)), availableWidth - 12);
      int headerHeight = header.size() * 10;
      int columnWidth = (availableWidth - 8) / 2;
      left = wrap(mc, left, columnWidth - 12);
      right = wrap(mc, right, columnWidth - 12);
      int rows = Math.max(1, (int)((height - 8) / scale - 24 - headerHeight) / 10);
      int pages = Math.max(1, (Math.max(left.size(), right.size()) + rows - 1) / rows);
      boolean up = Keyboard.isKeyDown(Keyboard.KEY_PRIOR), down = Keyboard.isKeyDown(Keyboard.KEY_NEXT);
      if(up && !pageUp) --page;
      if(down && !pageDown) ++page;
      pageUp = up; pageDown = down;
      page = Math.max(0, Math.min(pages - 1, page));
      int start = page * rows;
      left = left.subList(Math.min(start, left.size()), Math.min(start + rows, left.size()));
      right = right.subList(Math.min(start, right.size()), Math.min(start + rows, right.size()));
      panelHeight = Math.max(left.size(), right.size()) * 10 + 24 + headerHeight;
      GL11.glPushMatrix();
      try {
         GL11.glTranslatef(4, 4, 0);
         GL11.glScalef(scale, scale, 1);
         Gui.drawRect(0, 0, availableWidth, panelHeight, 0xD0101010);
         column(mc, header, 6, 6, 0xFFFFFFFF);
         column(mc, left, 6, 6 + headerHeight, 0xFFFFFF80);
         column(mc, right, columnWidth + 14, 6 + headerHeight, 0xFF80FFFF);
         mc.fontRenderer.drawStringWithShadow("Page " + (page + 1) + "/" + pages + " (PgUp/PgDn)",
               6, panelHeight - 12, 0xFFFFFFFF);
      } finally {
         GL11.glPopMatrix();
      }
   }
   private static List<String> wrap(Minecraft mc, List<String> lines, int width) {
      List<String> result = new ArrayList<String>();
      for(String line : lines) result.addAll(mc.fontRenderer.listFormattedStringToWidth(line, Math.max(20, width)));
      return result;
   }
   private static int textWidth(Minecraft mc, List<String> lines) {
      int width = 0;
      for(String line : lines) width = Math.max(width, mc.fontRenderer.getStringWidth(line));
      return width;
   }
   private static void column(Minecraft mc, List<String> lines, int x, int y, int color) {
      for(int i = 0; i < lines.size(); ++i) mc.fontRenderer.drawStringWithShadow(lines.get(i), x, y + i * 10, color);
   }
}
