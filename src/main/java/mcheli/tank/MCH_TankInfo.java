package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.EnumBoundingBoxType;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_BoundingBox;
import mcheli.tank.MCH_ItemTank;
import net.minecraft.item.Item;
import net.minecraft.util.Vec3;

public class MCH_TankInfo extends MCH_BaseVehicleInfo {

   public MCH_ItemTank item = null;
   public int weightType = 0;
   public float weightedCenterZ = 0.0F;
   public int trackMaxHP = 100;
   public boolean enableTurretPop = false;


   public Item getItem() {
      return this.item;
   }

   public MCH_TankInfo(String name) {
      super(name);
   }

   public List getDefaultWheelList() {
      ArrayList list = new ArrayList();
      list.add(new MCH_BaseVehicleInfo.Wheel(Vec3.createVectorHelper(1.5D, -0.24D, 2.0D)));
      list.add(new MCH_BaseVehicleInfo.Wheel(Vec3.createVectorHelper(1.5D, -0.24D, -2.0D)));
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
      return 4.0F;
      //does not affect tanks for some reason?
      //seems to make rot speed faster in third person, absolutely nothing else changes (1.15 max speed cap)
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
      //this is not item data wtf
      super.loadItemData(item, data);
      if(item.equalsIgnoreCase("WeightType")) {
         data = data.toLowerCase();
         this.weightType = data.equals("tank")?2:(data.equals("car")?1:0);
      } else if(item.equalsIgnoreCase("WeightedCenterZ")) {
         this.weightedCenterZ = this.toFloat(data, -1000.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("TrackMaxHP")) {
         this.trackMaxHP = this.toInt(data, 1, 1000000);
      } else if(item.equalsIgnoreCase("EnableTurretPop")) {
         this.enableTurretPop = this.toBool(data, false);
      } else if(item.equalsIgnoreCase("AddTrackHitBox")) {
         String[] s = data.split("\\s*,\\s*");
         if(s.length >= 5) {
            float depth = s.length >= 7?this.toFloat(s[5]):this.toFloat(s[3]);
            float df = s.length >= 7?this.toFloat(s[6]):(s.length >= 6?this.toFloat(s[5]):1.0F);
            MCH_BoundingBox bb = new MCH_BoundingBox((double)this.toFloat(s[0]), (double)this.toFloat(s[1]), (double)this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), depth, df);
            bb.boundingBoxType = EnumBoundingBoxType.TRACK;
            this.extraBoundingBox.add(bb);
         }
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
