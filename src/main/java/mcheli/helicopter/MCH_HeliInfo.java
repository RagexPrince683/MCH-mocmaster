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
   /** Maximum sustained upward speed for new-model helicopters, in blocks per tick; parser caps normal configs at 0.22. */
   public float maxClimbRate;
   /** Horizontal air-drag coefficient for future speed damping. */
   public float parasiteDrag;
   /** Minecraft-scale multiplier for pitch-derived rotor thrust converted into forward/backward acceleration. */
   public float horizontalRotorThrustScale;
   /** Additional multiplier applied only to roll-derived lateral rotor thrust. */
   public float helicopterLateralThrustScale;
   /** Lateral velocity drag coefficient applied independently from forward parasite drag. */
   public float helicopterLateralDrag;
   /** Maximum lateral speed as a scale of the new-heli horizontal safety speed. */
   public float helicopterMaxLateralSpeedScale;
   /** Optional backward thrust scale; NaN inherits forward thrust behavior for compatibility. */
   public float helicopterBackwardThrustScale;
   /** Optional backward speed scale; NaN inherits forward speed behavior for compatibility. */
   public float helicopterMaxBackwardSpeedScale;
   /** Strength of new-model hover assistance; 0 disables assist, 1 is full configured assist. */
   public float hoverAssistStrength;
   /** Vertical-speed deadzone for new-heli hover hold, in motionY blocks per tick. */
   public float hoverVerticalSpeedDeadzone;
   /** Proportional strength for new-heli hover vertical-speed hold. */
   public float hoverVerticalStabilizerStrength;
   /** Proportional strength for new-heli hover attitude leveling. */
   public float hoverPitchStabilizerStrength;
   /** Maximum per-tick hover throttle bias change. */
   public float hoverVerticalCorrectionLimit;
   /** Maximum total hover throttle/collective bias. */
   public float hoverThrottleBiasLimit;
   /** Ticks to wait after each hover throttle adjustment before applying another. */
   public int hoverVerticalAdjustmentInterval;
   /** Minimum throttle held by new-heli hover mode to prevent unrecoverable sink. */
   public float hoverMinimumThrottle;
   /** Show compact player-power readout to pilots using the new helicopter flight model. */
   public boolean newHeliControlHudDisplay;
   public List rotorList;


   public MCH_HeliInfo(String name) {
      super(name);
      super.isEnableGunnerMode = false;
      this.isEnableFoldBlade = false;
      this.useNewHelicopterFlightModel = false;
      this.physicalMass = 1.0F;
      this.mainRotorMaxThrust = 0.12F;
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
      this.maxClimbRate = 0.16F;
      this.parasiteDrag = 0.01F;
      this.horizontalRotorThrustScale = 0.35F;
      this.helicopterLateralThrustScale = 0.45F;
      this.helicopterLateralDrag = 0.055F;
      this.helicopterMaxLateralSpeedScale = 0.45F;
      this.helicopterBackwardThrustScale = Float.NaN;
      this.helicopterMaxBackwardSpeedScale = Float.NaN;
      this.hoverAssistStrength = 0.75F;
      this.hoverVerticalSpeedDeadzone = 0.01F;
      this.hoverVerticalStabilizerStrength = 0.08F;
      this.hoverPitchStabilizerStrength = 0.045F;
      this.hoverVerticalCorrectionLimit = 0.0025F;
      this.hoverThrottleBiasLimit = 0.08F;
      this.hoverVerticalAdjustmentInterval = 40;
      this.hoverMinimumThrottle = 0.65F;
      this.newHeliControlHudDisplay = true;
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
      } else if(item.equalsIgnoreCase("TailRotorAuthority") || item.equalsIgnoreCase("YawAuthority")) {
         this.tailRotorAuthority = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("YawDamping")) {
         this.yawDamping = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("AngularInertia")) {
         this.angularInertia = this.toFloat(data, 0.01F, 100000.0F);
      } else if(item.equalsIgnoreCase("TranslationalLiftCoefficient")) {
         this.translationalLiftCoefficient = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("VerticalDrag")) {
         this.verticalDrag = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("MaxClimbRate") || item.equalsIgnoreCase("HelicopterMaxClimbRate") || item.equalsIgnoreCase("NewHelicopterMaxClimbRate")) {
         this.maxClimbRate = this.toFloat(data, 0.0F, 0.22F);
      } else if(item.equalsIgnoreCase("ParasiteDrag")) {
         this.parasiteDrag = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HorizontalRotorThrustScale") || item.equalsIgnoreCase("HelicopterHorizontalThrustScale")) {
         this.horizontalRotorThrustScale = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HelicopterLateralThrustScale")) {
         this.helicopterLateralThrustScale = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HelicopterLateralDrag")) {
         this.helicopterLateralDrag = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HelicopterMaxLateralSpeedScale")) {
         this.helicopterMaxLateralSpeedScale = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HelicopterBackwardThrustScale")) {
         this.helicopterBackwardThrustScale = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HelicopterMaxBackwardSpeedScale")) {
         this.helicopterMaxBackwardSpeedScale = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HoverAssistStrength")) {
         this.hoverAssistStrength = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("HoverVerticalSpeedDeadzone")) {
         this.hoverVerticalSpeedDeadzone = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("HoverVerticalStabilizerStrength")) {
         this.hoverVerticalStabilizerStrength = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HoverPitchStabilizerStrength") || item.equalsIgnoreCase("HoverAttitudeStabilizerStrength")) {
         this.hoverPitchStabilizerStrength = this.toFloat(data, 0.0F, 1000.0F);
      } else if(item.equalsIgnoreCase("HoverVerticalCorrectionLimit")) {
         this.hoverVerticalCorrectionLimit = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("HoverThrottleBiasLimit")) {
         this.hoverThrottleBiasLimit = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("HoverVerticalAdjustmentInterval")) {
         this.hoverVerticalAdjustmentInterval = this.toInt(data, 0, 200);
      } else if(item.equalsIgnoreCase("HoverMinimumThrottle")) {
         this.hoverMinimumThrottle = this.toFloat(data, 0.0F, 1.0F);
      } else if(item.equalsIgnoreCase("NewHeliControlHudDisplay") || item.equalsIgnoreCase("NewHelicopterControlHudDisplay")) {
         this.newHeliControlHudDisplay = this.toBool(data);
      } else if(item.compareTo("addrotor") == 0 || item.compareTo("addrotorold") == 0) {
         String[] s = data.split("\\s*,\\s*");
         if(s.length == 8 || s.length == 9) {
            boolean cfb = s.length == 9 && this.toBool(s[8]);
            MCH_HeliInfo.Rotor e = new MCH_HeliInfo.Rotor(this.toInt(s[0]), this.toInt(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), this.toFloat(s[7]), "blade" + this.rotorList.size(), cfb, item.compareTo("addrotorold") == 0);
            this.rotorList.add(e);
         }
      }
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
