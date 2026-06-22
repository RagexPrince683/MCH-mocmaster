package mcheli.hud;

import mcheli.aircraft.MCH_HudShared;
import mcheli.hud.MCH_HudItem;

public class MCH_HudItemRect extends MCH_HudItem {

   private final String left;
   private final String top;
   private final String width;
   private final String height;


   public MCH_HudItemRect(int fileLine, String left, String top, String width, String height) {
      super(fileLine);
      this.left = toFormula(left);
      this.top = toFormula(top);
      this.width = toFormula(width);
      this.height = toFormula(height);
   }

   public void execute() {
      if(MCH_HudShared.isNewHeliPilotHudActive(MCH_HudItem.ac, MCH_HudItem.player)
            && (this.left.equals("146") && this.top.equals("107")
            || this.left.equals("-145") && this.top.equals("57")
            || this.left.equals("-144") && this.top.equals("58"))) {
         return;
      }
      double x2 = MCH_HudItem.centerX + calc(this.left);
      double y2 = MCH_HudItem.centerY + calc(this.top);
      double x1 = x2 + (double)((int)calc(this.width));
      double y1 = y2 + (double)((int)calc(this.height));
      drawRect(x1, y1, x2, y2, MCH_HudItem.colorSetting);
   }
}
