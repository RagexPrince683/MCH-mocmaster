package mcheli.hud;

import mcheli.hud.MCH_HudItem;

public class MCH_HudItemExit extends MCH_HudItem {

   public MCH_HudItemExit(int fileLine) {
      super(fileLine);
   }

   public void execute() {
      super.parent.exit = true;
   }
}
