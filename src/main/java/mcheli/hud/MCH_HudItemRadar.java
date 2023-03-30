package mcheli.hud;

import java.util.ArrayList;
import java.util.Iterator;
import mcheli.MCH_Lib;
import mcheli.MCH_Vector2;
import mcheli.hud.MCH_HudItem;

public class MCH_HudItemRadar extends MCH_HudItem {

   private final String rot;
   private final String left;
   private final String top;
   private final String width;
   private final String height;
   private final boolean isEntityRadar;


   public MCH_HudItemRadar(int fileLine, boolean isEntityRadar, String rot, String left, String top, String width, String height) {
      super(fileLine);
      this.isEntityRadar = isEntityRadar;
      this.rot = toFormula(rot);
      this.left = toFormula(left);
      this.top = toFormula(top);
      this.width = toFormula(width);
      this.height = toFormula(height);
   }

   public void execute() {
      if(this.isEntityRadar) {
         if(MCH_HudItem.EntityList != null && MCH_HudItem.EntityList.size() > 0) {
            this.drawEntityList(MCH_HudItem.EntityList, (float)calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height));
         }
      } else if(MCH_HudItem.EnemyList != null && MCH_HudItem.EnemyList.size() > 0) {
         this.drawEntityList(MCH_HudItem.EnemyList, (float)calc(this.rot), MCH_HudItem.centerX + calc(this.left), MCH_HudItem.centerY + calc(this.top), calc(this.width), calc(this.height));
      }

   }

   protected void drawEntityList(ArrayList src, float r, double left, double top, double w, double h) {
      double w1 = -w / 2.0D;
      double w2 = w / 2.0D;
      double h1 = -h / 2.0D;
      double h2 = h / 2.0D;
      double w_factor = w / 64.0D;
      double h_factor = h / 64.0D;
      double[] list = new double[src.size() * 2];
      int idx = 0;

      for(Iterator drawList = src.iterator(); drawList.hasNext(); idx += 2) {
         MCH_Vector2 i = (MCH_Vector2)drawList.next();
         list[idx + 0] = i.x / 2.0D * w_factor;
         list[idx + 1] = i.y / 2.0D * h_factor;
      }

      MCH_Lib.rotatePoints(list, r);
      ArrayList drawList1 = new ArrayList();

      for(int i1 = 0; i1 + 1 < list.length; i1 += 2) {
         if(list[i1 + 0] > w1 && list[i1 + 0] < w2 && list[i1 + 1] > h1 && list[i1 + 1] < h2) {
            drawList1.add(Double.valueOf(list[i1 + 0] + left + w / 2.0D));
            drawList1.add(Double.valueOf(list[i1 + 1] + top + h / 2.0D));
         }
      }

      this.drawPoints(drawList1, MCH_HudItem.colorSetting, MCH_HudItem.scaleFactor * 2);
   }
}
