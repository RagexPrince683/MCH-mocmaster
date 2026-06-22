package mcheli.hud;

import mcheli.aircraft.MCH_HudShared;
import mcheli.hud.MCH_HudItem;
import mcheli.wrapper.W_TextureUtil;
import org.lwjgl.opengl.GL11;

public class MCH_HudItemTexture extends MCH_HudItem {

   private final String name;
   private final String left;
   private final String top;
   private final String width;
   private final String height;
   private final String uLeft;
   private final String vTop;
   private final String uWidth;
   private final String vHeight;
   private final String rot;
   private int textureWidth;
   private int textureHeight;


   public MCH_HudItemTexture(int fileLine, String name, String left, String top, String width, String height, String uLeft, String vTop, String uWidth, String vHeight, String rot) {
      super(fileLine);
      this.name = name;
      this.left = toFormula(left);
      this.top = toFormula(top);
      this.width = toFormula(width);
      this.height = toFormula(height);
      this.uLeft = toFormula(uLeft);
      this.vTop = toFormula(vTop);
      this.uWidth = toFormula(uWidth);
      this.vHeight = toFormula(vHeight);
      this.rot = toFormula(rot);
      this.textureWidth = this.textureHeight = 0;
   }

   public void execute() {
      if(this.shouldSuppressForNewHeliHud()) {
         return;
      }
      GL11.glEnable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if(this.textureWidth == 0 || this.textureHeight == 0) {
         int w = 0;
         int h = 0;
         W_TextureUtil.TextureParam prm = W_TextureUtil.getTextureInfo("mcheli", "textures/gui/" + this.name + ".png");
         if(prm != null) {
            w = prm.width;
            h = prm.height;
         }

         this.textureWidth = w > 0?w:256;
         this.textureHeight = h > 0?h:256;
      }

      this.drawTexture(this.name, MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height), calc(this.uLeft), calc(this.vTop), calc(this.uWidth), calc(this.vHeight), (float)calc(this.rot), this.textureWidth, this.textureHeight);
   }
   private boolean shouldSuppressForNewHeliHud() {
      if(!MCH_HudShared.isNewHeliPilotHudActive(MCH_HudItem.ac, MCH_HudItem.player)) {
         return false;
      }
      if(this.name.equalsIgnoreCase("heli_hud") && this.top.equals("21")) {
         return true;
      }
      if(!this.name.equalsIgnoreCase("hud")) {
         return false;
      }
      return this.left.equals("-207") && this.top.equals("83")
            || this.left.equals("-170") && this.top.equals("83")
            || this.left.equals("-170+15") && this.top.equals("83")
            || this.left.equals("-133") && this.top.equals("83")
            || this.left.equals("-133+8+stick_x*12") && this.top.equals("83+8+stick_y*12")
            || this.left.equals("144") && this.top.equals("95")
            || this.left.equals("-208") && (this.top.equals("57") || this.top.equals("68"));
   }

}
