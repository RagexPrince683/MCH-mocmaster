package mcheli.helicopter;

import java.util.ArrayList;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.helicopter.MCH_ItemHeli;
import net.minecraft.item.Item;

public class MCH_HeliInfo extends MCH_BaseVehicleInfo {

   public MCH_ItemHeli item = null;
   public boolean isEnableFoldBlade;
   /** Opt-in gate for the isolated helicopter flight-model rewrite. Default false keeps legacy lift/motion behavior. */
   public boolean useNewHelicopterFlightModel;
   /** Relative vehicle mass used by future helicopter physics. 1.0 matches current legacy scale. */
   public float physicalMass;
   /** Maximum upward rotor force for future physics, in legacy motion units per tick at full collective. */
   public float mainRotorMaxThrust;
   /** Rotor acceleration resistance for future spool calculations; larger values change RPM more slowly. */
   public float rotorInertia;
   /** Normalized rotor RPM increase per tick while spooling up in the future model. */
   public float rotorSpoolUpRate;
   /** Normalized rotor RPM decrease per tick while spooling down in the future model. */
   public float rotorSpoolDownRate;
   /** Collective input smoothing rate for future lift control, normalized per tick. */
   public float collectiveResponse;
   /** Pitch/roll cyclic control authority multiplier for future helicopter physics. */
   public float cyclicAuthority;
   /** Tail rotor yaw authority multiplier for future helicopter physics. */
   public float tailRotorAuthority;
   /** Yaw damping multiplier for future angular stabilization. */
   public float yawDamping;
   /** Relative rotational inertia used by future angular acceleration calculations. */
   public float angularInertia;
   /** Forward-speed lift bonus coefficient for future translational-lift calculations. */
   public float translationalLiftCoefficient;
   /** Vertical drag coefficient for future climb/descent damping. */
   public float verticalDrag;
   /** Horizontal air-drag coefficient for future speed damping. */
   public float parasiteDrag;
   /** Strength of future hover assistance; 0 disables assist, 1 is full configured assist. */
   public float hoverAssistStrength;
   public List rotorList;


   public MCH_HeliInfo(String name) {
      super(name);
      super.isEnableGunnerMode = false;
      this.isEnableFoldBlade = false;
      this.useNewHelicopterFlightModel = false;
      this.physicalMass = 1.0F;
      this.mainRotorMaxThrust = 0.06F;
      this.rotorInertia = 1.0F;
      this.rotorSpoolUpRate = 0.02F;
      this.rotorSpoolDownRate = 0.03F;
      this.collectiveResponse = 0.08F;
      this.cyclicAuthority = 1.0F;
      this.tailRotorAuthority = 1.0F;
      this.yawDamping = 0.15F;
      this.angularInertia = 1.0F;
      this.translationalLiftCoefficient = 0.004F;
      this.verticalDrag = 0.02F;
      this.parasiteDrag = 0.01F;
      this.hoverAssistStrength = 0.0F;
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
      } else if(item.equalsIgnoreCase("UseNewHelicopterFlightModel") || item.equalsIgnoreCase("EnableNewHelicopterFlightModel")) {
         this.useNewHelicopterFlightModel = this.toBool(data);
      } else if(item.equalsIgnoreCase("PhysicalMass")) {
         this.physicalMass = this.toFloat(data, 0.01F, 100000.0F);
      } else if(item.equalsIgnoreCase("MainRotorMaxThrust")) {
         this.mainRotorMaxThrust = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("RotorInertia")) {
         this.rotorInertia = this.toFloat(data, 0.01F, 100000.0F);
      } else if(item.equalsIgnoreCase("RotorSpoolUpRate")) {
         this.rotorSpoolUpRate = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("RotorSpoolDownRate")) {
         this.rotorSpoolDownRate = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("CollectiveResponse")) {
         this.collectiveResponse = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("CyclicAuthority")) {
         this.cyclicAuthority = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("TailRotorAuthority")) {
         this.tailRotorAuthority = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("YawDamping")) {
         this.yawDamping = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("AngularInertia")) {
         this.angularInertia = this.toFloat(data, 0.01F, 100000.0F);
      } else if(item.equalsIgnoreCase("TranslationalLiftCoefficient")) {
         this.translationalLiftCoefficient = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("VerticalDrag")) {
         this.verticalDrag = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("ParasiteDrag")) {
         this.parasiteDrag = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HoverAssistStrength")) {
         this.hoverAssistStrength = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.compareTo("addrotor") == 0 || item.compareTo("addrotorold") == 0) {
         String[] s = data.split("\\s*,\\s*");
         if(s.length == 8 || s.length == 9) {
            boolean cfb = s.length == 9 && this.toBool(s[8]);
            MCH_HeliInfo.Rotor e = new MCH_HeliInfo.Rotor(this.toInt(s[0]), this.toInt(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), this.toFloat(s[7]), "blade" + this.rotorList.size(), cfb, item.compareTo("addrotorold") == 0);
            this.rotorList.add(e);
         }
      }

      MCH_BaseVehicleInfo.allBaseVehicleInfo.put(name, this);
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

   public class Rotor extends MCH_BaseVehicleInfo.DrawnPart {

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
