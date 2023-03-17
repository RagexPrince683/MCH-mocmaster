package mcheli.hud;

public class MCH_HudItemColor extends MCH_HudItem {

   private final String updateColor;


   public MCH_HudItemColor(int fileLine, String newColor) {
      super(fileLine);
      this.updateColor = newColor;
   }

   public static MCH_HudItemColor createByParams(int fileLine, String[] prm) {
      return prm.length == 1?new MCH_HudItemColor(fileLine, toFormula(prm[0])):(prm.length == 4?new MCH_HudItemColor(fileLine, "((" + toFormula(prm[0]) + ")<<24)|" + "((" + toFormula(prm[1]) + ")<<16)|" + "((" + toFormula(prm[2]) + ")<<8 )|" + "((" + toFormula(prm[3]) + ")<<0 )"):null);
   }

   public void execute() {
      double d = calc(this.updateColor);
      long l = (long)d;
      MCH_HudItem.colorSetting = (int)l;
      updateVarMapItem("color", getColor());
   }
}
