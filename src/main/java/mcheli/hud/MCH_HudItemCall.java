package mcheli.hud;

import mcheli.hud.MCH_Hud;
import mcheli.hud.MCH_HudItem;
import mcheli.hud.MCH_HudManager;

public class MCH_HudItemCall extends MCH_HudItem {

   private final String hudName;


   public MCH_HudItemCall(int fileLine, String name) {
      super(fileLine);
      this.hudName = name;
   }

   public void execute() {
      MCH_Hud hud = MCH_HudManager.get(this.hudName);
      if(hud != null) {
         hud.drawItems();
      }

   }
}
