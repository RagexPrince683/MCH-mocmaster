package mcheli.tank;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.item.Item;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MCH_TankInfo extends MCH_AircraftInfo {

   public MCH_ItemTank item = null;
   public int weightType = 0;
   public float weightedCenterZ = 0.0F;


   public Item getItem() {
      return this.item;
   }

   public MCH_TankInfo(String name) {
      super(name);
   }

   public List getDefaultWheelList() {
      ArrayList list = new ArrayList();
      list.add(new MCH_AircraftInfo.Wheel(Vec3.createVectorHelper(1.5D, -0.24D, 2.0D)));
      list.add(new MCH_AircraftInfo.Wheel(Vec3.createVectorHelper(1.5D, -0.24D, -2.0D)));
      return list;
   }

   public float getDefaultSoundRange() {
      return 50.0F;
   }

   public float getDefaultRotorSpeed() {
      return 47.94F;
   }

   private float getDefaultStepHeight() {
      return 0.6F;
   }

   public float getMaxSpeed() {
      return 1.8F;
   }

   public int getDefaultMaxZoom() {
      return 8;
   }

   public String getDefaultHudName(int seatId) {
      return seatId <= 0?"tank":(seatId == 1?"tank":"gunner");
   }

   public boolean isValidData() throws Exception {
      double var10001 = (double)super.speed;
      MCH_Config var10002 = MCH_MOD.config;
      super.speed = (float)(var10001 * MCH_Config.AllTankSpeed.prmDouble);
      return super.isValidData();
   }

   public void loadItemData(String item, String data) {
      super.loadItemData(item, data);
      if(item.equalsIgnoreCase("WeightType")) {
         data = data.toLowerCase();
         this.weightType = data.equals("tank")?2:(data.equals("car")?1:0);
      } else if(item.equalsIgnoreCase("WeightedCenterZ")) {
         this.weightedCenterZ = this.toFloat(data, -1000.0F, 1000.0F);
      }

   }

   public String getDirectoryName() {
      return "tanks";
   }

   public String getKindName() {
      return "tank";
   }

   public void preReload() {
      super.preReload();
   }

   public void postReload() {
      MCH_MOD.proxy.registerModelsTank(super.name, true);
   }
}
