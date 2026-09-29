package mcheli.tank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.network.packets.PacketWheelDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

/** Two independently sized columns, with a contrasting background and explicit state labels. */
@SideOnly(Side.CLIENT)
public final class MCH_GuiWheelDiagnostics {
   private MCH_GuiWheelDiagnostics() {}
   public static boolean visible(EntityPlayer player) {
      return MCH_Config.TestMode.prmBool && PacketWheelDiagnostics.isVisible(player);
   }
   public static void draw(Minecraft mc, int width, int height) {
      if(!visible(mc.thePlayer)) return;
      MCH_EntityTank car = MCH_WheelDiagnostics.riddenCar(mc.thePlayer);
      PacketWheelDiagnostics packet = PacketWheelDiagnostics.snapshot(mc.thePlayer);
      List<String> left = packet != null ? packet.left : Collections.singletonList("S server snapshot=N/A (waiting/expired)");
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
      GL11.glPushMatrix();
      try {
         GL11.glTranslatef(4, 4, 0);
         GL11.glScalef(scale, scale, 1);
         Gui.drawRect(0, 0, leftWidth + rightWidth + 8, panelHeight, 0xD0101010);
         column(mc, left, 6, 0xFFFFFF80);
         column(mc, right, leftWidth + 14, 0xFF80FFFF);
      } finally {
         GL11.glPopMatrix();
      }
   }
   private static int textWidth(Minecraft mc, List<String> lines) {
      int width = 0;
      for(String line : lines) width = Math.max(width, mc.fontRenderer.getStringWidth(line));
      return width;
   }
   private static void column(Minecraft mc, List<String> lines, int x, int color) {
      for(int i = 0; i < lines.size(); ++i) mc.fontRenderer.drawStringWithShadow(lines.get(i), x, 6 + i * 10, color);
   }
}
