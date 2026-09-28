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
   public boolean civilianCarGrip = false;
   public boolean civilianCarDrivetrain = false;
   public float carThrottleResponse = 0.25F;
   public float carIdleRpm = 800.0F;
   public float carRedlineRpm = 6500.0F;
   public int carForwardGears = 5;
   public int carShiftTicks = 8;
   public float carServiceBrake = 0.025F;
   public float carHandbrake = 0.015F;
   public float[] carGearRatios = null;
   public float carReverseGearRatio = 3.2F;
   public float carFinalDrive = 4.0F;
   public float carWheelRadius = 0.32F;
   public float carDriveForce = 0.0015F;
   public float carDrag = 0.0015F;
   public float carLongitudinalGrip = 0.03F;
   private float configuredSpeed = -1.0F;
   /** Explicit axle selection; null is legacy propulsion or axle-neutral with the engine opt-in. */
   public DriveType driveType = null;

   public enum DriveType {
      FWD, RWD, AWD;

      public static DriveType parse(String value) {
         if(value != null) for(DriveType type : values()) {
            if(type.name().equalsIgnoreCase(value.trim())) return type;
         }
         return null;
      }
   }
   /** Explicit civilian reverse opt-in; zero retains legacy behavior. Blocks/tick, absolute. */
   public float civilianCarReverseSpeed = 0.0F;
   public boolean enableBrakeLights = false;
   public boolean carGripDiagnostics = false;
   public MCH_CarTireGrip.TireSize frontTireSize = null;
   public MCH_CarTireGrip.TireSize rearTireSize = null;
   public float carLateralGrip = MCH_CarTireGrip.DEFAULT_GRIP;
   public float carMinimumSteering = 0.0F;
   /** Suspension acceleration at full compression, in blocks/tick squared. */
   public float suspensionSpring = 0.055F;
   /** Damper acceleration per block/tick while the tire is compressing. */
   public float suspensionCompressionDamping = 0.035F;
   /** Damper acceleration per block/tick while the tire is rebounding. */
   public float suspensionReboundDamping = 0.050F;
   /** Maximum vertical wheel travel in blocks. */
   public float suspensionTravel = 0.45F;


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
      return this.civilianCarDrivetrain ? 8.0F : 4.0F;
   }

   public int getDefaultMaxZoom() {
      return 8;
   }

   public String getDefaultHudName(int seatId) {
      return seatId <= 0?"tank":(seatId == 1?"tank":"gunner");
   }

   public boolean isValidData() throws Exception {
      // Resolve after all keys: a car opt-in may appear after Speed in a content pack.
      double result = this.configuredSpeed >= 0 ? Math.min(this.configuredSpeed, this.getMaxSpeed()) : super.speed;
      if(this.carGearRatios == null || this.carGearRatios.length != this.carForwardGears) {
         this.carGearRatios = new float[this.carForwardGears];
         for(int i = 0; i < this.carForwardGears; ++i) {
            this.carGearRatios[i] = this.carForwardGears == 1 ? 1.0F
                  : (float)(3.5D * Math.pow(0.22D, (double)i / (this.carForwardGears - 1)));
         }
      }
      MCH_Config configuration = MCH_MOD.config;
      super.speed = (float)(result * MCH_Config.AllTankSpeed.prmDouble);
      return super.isValidData();
   }

   public void loadItemData(String item, String data) {
      // Handle car-only keys before the shared parser's client HUD/model branch.
      if(item.equalsIgnoreCase("CivilianCarDrivetrain")) {
         this.civilianCarDrivetrain = this.toBool(data, false);
         return;
      } else if(item.equalsIgnoreCase("CarThrottleResponse")) {
         this.carThrottleResponse = carValue(data, 0.25F, 0.05F, 1.0F);
         return;
      } else if(item.equalsIgnoreCase("CarIdleRPM")) {
         this.carIdleRpm = carValue(data, 800, 500, 2000);
         return;
      } else if(item.equalsIgnoreCase("CarRedlineRPM")) {
         this.carRedlineRpm = carValue(data, 6500, 3000, 12000);
         return;
      } else if(item.equalsIgnoreCase("CarForwardGears")) {
         this.carForwardGears = (int)carValue(data, 5, 1, 8);
         return;
      } else if(item.equalsIgnoreCase("CarShiftTicks")) {
         this.carShiftTicks = (int)carValue(data, 8, 1, 40);
         return;
      } else if(item.equalsIgnoreCase("CarGearRatios")) {
         this.carGearRatios = parseCarRatios(data);
         return;
      } else if(item.equalsIgnoreCase("CarReverseGearRatio")) {
         this.carReverseGearRatio = carValue(data, 3.2F, 0.2F, 6.0F);
         return;
      } else if(item.equalsIgnoreCase("CarFinalDrive")) {
         this.carFinalDrive = carValue(data, 4.0F, 1.0F, 8.0F);
         return;
      } else if(item.equalsIgnoreCase("CarWheelRadius")) {
         this.carWheelRadius = carValue(data, 0.32F, 0.2F, 0.6F);
         return;
      } else if(item.equalsIgnoreCase("CarDriveForce")) {
         this.carDriveForce = carValue(data, 0.0015F, 0.0001F, 0.02F);
         return;
      } else if(item.equalsIgnoreCase("CarDrag")) {
         this.carDrag = carValue(data, 0.0015F, 0.00001F, 0.1F);
         return;
      } else if(item.equalsIgnoreCase("CarLongitudinalGrip")) {
         this.carLongitudinalGrip = carValue(data, 0.03F, 0.005F, 0.24F);
         return;
      } else if(item.equalsIgnoreCase("CarServiceBrake")) {
         this.carServiceBrake = carValue(data, 0.025F, 0.001F, 0.5F);
         return;
      } else if(item.equalsIgnoreCase("CarHandbrake")) {
         this.carHandbrake = carValue(data, 0.015F, 0.001F, 0.5F);
         return;
      } else if(item.equalsIgnoreCase("Speed")) {
         this.configuredSpeed = this.toFloat(data, 0.0F, 8.0F);
         super.speed = Math.min(this.configuredSpeed, 4.0F);
         return;
      } else if(item.equalsIgnoreCase("CivilianCarGrip")) {
         this.civilianCarGrip = this.toBool(data, false);
         return;
      } else if(item.equalsIgnoreCase("DriveType")) {
         this.driveType = DriveType.parse(data);
         return;
      } else if(item.equalsIgnoreCase("CivilianCarReverseSpeed")) {
         this.civilianCarReverseSpeed = MCH_CarReverseControl.parseSpeed(data);
         return;
      } else if(item.equalsIgnoreCase("EnableBrakeLights")) {
         this.enableBrakeLights = this.toBool(data, false);
         return;
      } else if(item.equalsIgnoreCase("CarGripDiagnostics")) {
         this.carGripDiagnostics = this.toBool(data, false);
         return;
      } else if(item.equalsIgnoreCase("FrontTireSize")) {
         this.frontTireSize = MCH_CarTireGrip.parseTireSize(data);
         return;
      } else if(item.equalsIgnoreCase("RearTireSize")) {
         this.rearTireSize = MCH_CarTireGrip.parseTireSize(data);
         return;
      } else if(item.equalsIgnoreCase("CarLateralGrip")) {
         float grip;
         try {
            grip = this.toFloat(data);
         } catch(NumberFormatException ex) {
            grip = MCH_CarTireGrip.DEFAULT_GRIP;
         }
         this.carLateralGrip = Float.isNaN(grip) || Float.isInfinite(grip) ? MCH_CarTireGrip.DEFAULT_GRIP
                 : Math.max(0.0F, Math.min(MCH_CarTireGrip.MAX_GRIP, grip));
         return;
      } else if(item.equalsIgnoreCase("CarMinimumSteering")) {
         float steering;
         try {
            steering = this.toFloat(data);
         } catch(NumberFormatException ex) {
            steering = 0.0F;
         }
         this.carMinimumSteering = Float.isNaN(steering) || Float.isInfinite(steering) ? 0.0F
                 : Math.max(0.0F, Math.min(10.0F, steering));
         return;
      } else if(item.equalsIgnoreCase("SuspensionSpring")) {
         this.suspensionSpring = this.toFloat(data, 0.0F, 0.25F);
         return;
      } else if(item.equalsIgnoreCase("SuspensionCompressionDamping")) {
         this.suspensionCompressionDamping = this.toFloat(data, 0.0F, 0.25F);
         return;
      } else if(item.equalsIgnoreCase("SuspensionReboundDamping")) {
         this.suspensionReboundDamping = this.toFloat(data, 0.0F, 0.25F);
         return;
      } else if(item.equalsIgnoreCase("SuspensionTravel")) {
         this.suspensionTravel = this.toFloat(data, 0.05F, 1.5F);
         return;
      }
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

   private static float[] parseCarRatios(String data) {
      String[] values = data.split(",", -1);
      if(values.length < 1 || values.length > 8) return null;
      float[] ratios = new float[values.length];
      try {
         for(int i = 0; i < values.length; ++i) {
            float ratio = Float.parseFloat(values[i].trim());
            if(Float.isNaN(ratio) || Float.isInfinite(ratio) || ratio < 0.2F || ratio > 6.0F
                  || i > 0 && ratio >= ratios[i - 1]) return null;
            ratios[i] = ratio;
         }
      } catch(NumberFormatException ex) {
         return null;
      }
      return ratios;
   }

   private static float carValue(String data, float fallback, float min, float max) {
      try {
         float value = Float.parseFloat(data.trim());
         return Float.isNaN(value) || Float.isInfinite(value) ? fallback : Math.max(min, Math.min(max, value));
      } catch(NumberFormatException ex) {
         return fallback;
      }
   }

   public String getKindName() {
      return "tank";
   }

   public void preReload() {
      super.preReload();
      this.civilianCarDrivetrain = false;
      this.carThrottleResponse = 0.25F;
      this.carIdleRpm = 800.0F;
      this.carRedlineRpm = 6500.0F;
      this.carForwardGears = 5;
      this.carShiftTicks = 8;
      this.carServiceBrake = 0.025F;
      this.carHandbrake = 0.015F;
      this.carGearRatios = null;
      this.carReverseGearRatio = 3.2F;
      this.carFinalDrive = 4.0F;
      this.carWheelRadius = 0.32F;
      this.carDriveForce = 0.0015F;
      this.carDrag = 0.0015F;
      this.carLongitudinalGrip = 0.03F;
      this.configuredSpeed = -1.0F;
      this.civilianCarGrip = false;
      this.driveType = null;
      this.civilianCarReverseSpeed = 0.0F;
      this.enableBrakeLights = false;
      this.carGripDiagnostics = false;
      this.frontTireSize = null;
      this.rearTireSize = null;
      this.carLateralGrip = MCH_CarTireGrip.DEFAULT_GRIP;
      this.carMinimumSteering = 0.0F;
      this.suspensionSpring = 0.055F;
      this.suspensionCompressionDamping = 0.035F;
      this.suspensionReboundDamping = 0.050F;
      this.suspensionTravel = 0.45F;
   }

   public void postReload() {
      MCH_MOD.proxy.registerModelsTank(super.name, true);
   }
}
