package mcheli.helicopter;

import java.util.ArrayList;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.helicopter.MCH_ItemHeli;
import net.minecraft.item.Item;

public class MCH_HeliInfo extends MCH_AircraftInfo {

   public MCH_ItemHeli item = null;
   public boolean isEnableFoldBlade;
   public List rotorList;


   public MCH_HeliInfo(String name) {
      super(name);
      super.isEnableGunnerMode = false;
      this.isEnableFoldBlade = false;
      this.rotorList = new ArrayList();
      super.minRotationPitch = -20.0F;
      super.maxRotationPitch = 20.0F;
   }

   public boolean isValidData() throws Exception {
      double var10001 = (double)super.speed;
      MCH_Config var10002 = MCH_MOD.config;
      super.speed = (float)(var10001 * MCH_Config.AllHeliSpeed.prmDouble);
      return super.isValidData();
   }

   public float getDefaultSoundRange() {
      return 80.0F;
   }

   public float getDefaultRotorSpeed() {
      return 79.99F;
   }

   public int getDefaultMaxZoom() {
      return 8;
   }

   public Item getItem() {
      return this.item;
   }

   public String getDefaultHudName(int seatId) {
      return seatId <= 0?"heli":(seatId == 1?"heli_gnr":"gunner");
   }

   public void loadItemData(String item, String data) {
      super.loadItemData(item, data);
      if(item.compareTo("enablefoldblade") == 0) {
         this.isEnableFoldBlade = this.toBool(data);
      } else if(item.compareTo("addrotor") == 0 || item.compareTo("addrotorold") == 0) {
         String[] s = data.split("\\s*,\\s*");
         if(s.length == 8 || s.length == 9) {
            boolean cfb = s.length == 9 && this.toBool(s[8]);
            MCH_HeliInfo.Rotor e = new MCH_HeliInfo.Rotor(this.toInt(s[0]), this.toInt(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), this.toFloat(s[7]), "blade" + this.rotorList.size(), cfb, item.compareTo("addrotorold") == 0);
            this.rotorList.add(e);
         }
      }

      MCH_AircraftInfo.allAircraftInfo.put(name, this);
   }

   public String getDirectoryName() {
      return "helicopters";
   }

   public String getKindName() {
      return "helicopter";
   }

   public void preReload() {
      super.preReload();
      this.rotorList.clear();
   }

   public void postReload() {
      MCH_MOD.proxy.registerModelsHeli(super.name, true);
   }

   public class Rotor extends MCH_AircraftInfo.DrawnPart {

      public final int bladeNum;
      public final int bladeRot;
      public final boolean haveFoldFunc;
      public final boolean oldRenderMethod;


      public Rotor(int b, int br, float x, float y, float z, float rx, float ry, float rz, String model, boolean hf, boolean old) {
         super(x, y, z, rx, ry, rz, model);
         this.bladeNum = b;
         this.bladeRot = br;
         this.haveFoldFunc = hf;
         this.oldRenderMethod = old;
      }
   }
}
