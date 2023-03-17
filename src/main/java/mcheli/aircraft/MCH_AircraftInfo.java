package mcheli.aircraft;

import mcheli.MCH_BaseInfo;
import mcheli.MCH_MOD;
import mcheli.hud.MCH_Hud;
import mcheli.hud.MCH_HudManager;
import mcheli.weapon.MCH_WeaponInfoManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.IModelCustom;

import java.util.*;

public abstract class MCH_AircraftInfo extends MCH_BaseInfo {

   public final String name;
   public String displayName;
   public HashMap displayNameLang;
   public int itemID;
   public List recipeString;
   public List recipe;
   public boolean isShapedRecipe;
   public String category;
   public boolean isEnableGunnerMode;
   public int cameraZoom;
   public boolean isEnableConcurrentGunnerMode;
   public boolean isEnableNightVision;
   public boolean isEnableEntityRadar;
   public boolean isEnableEjectionSeat;
   public boolean isEnableParachuting;
   public MCH_AircraftInfo.Flare flare;
   public float bodyHeight;
   public float bodyWidth;
   public boolean isFloat;
   public float floatOffset;
   public float gravity;
   public float gravityInWater;
   public int maxHp;
   public float armorMinDamage;
   public float armorMaxDamage;
   public float armorDamageFactor;
   public boolean enableBack;
   public int inventorySize;
   public boolean isUAV;
   public boolean isSmallUAV;
   public boolean isTargetDrone;
   public float autoPilotRot;
   public float onGroundPitch;
   public boolean canMoveOnGround;
   public boolean canRotOnGround;
   public List weaponSetList;
   public List seatList;
   public List exclusionSeatList;
   public List hudList;
   public MCH_Hud hudTvMissile;
   public float damageFactor;
   public float submergedDamageHeight;
   public boolean regeneration;
   public List extraBoundingBox;
   public List wheels;
   public int maxFuel;
   public float fuelConsumption;
   public float fuelSupplyRange;
   public float ammoSupplyRange;
   public float repairOtherVehiclesRange;
   public int repairOtherVehiclesValue;
   public float stealth;
   public boolean canRide;
   public float entityWidth;
   public float entityHeight;
   public float entityPitch;
   public float entityRoll;
   public float stepHeight;
   public List entityRackList;
   public int mobSeatNum;
   public int entityRackNum;
   public MCH_MobDropOption mobDropOption;
   public List repellingHooks;
   public List rideRacks;
   public List particleSplashs;
   public List searchLights;
   public float rotorSpeed;
   public boolean enableSeaSurfaceParticle;
   public float pivotTurnThrottle;
   public float trackRollerRot;
   public float partWheelRot;
   public float onGroundPitchFactor;
   public float onGroundRollFactor;
   public Vec3 turretPosition;
   public boolean defaultFreelook;
   public Vec3 unmountPosition;
   public float markerWidth;
   public float markerHeight;
   public float bbZmin;
   public float bbZmax;
   public float bbZ;
   public boolean alwaysCameraView;
   public List cameraPosition;
   public float cameraRotationSpeed;
   public float speed;
   public float motionFactor;
   public float mobilityYaw;
   public float mobilityPitch;
   public float mobilityRoll;
   public float mobilityYawOnGround;
   public float minRotationPitch;
   public float maxRotationPitch;
   public float minRotationRoll;
   public float maxRotationRoll;
   public boolean limitRotation;
   public float throttleUpDown;
   public float throttleUpDownOnEntity;
   private List textureNameList;
   public int textureCount;
   public float particlesScale;
   public boolean hideEntity;
   public boolean smoothShading;
   public String soundMove;
   public float soundRange;
   public float soundVolume;
   public float soundPitch;
   public IModelCustom model;
   public List hatchList;
   public List cameraList;
   public List partWeapon;
   public List partWeaponBay;
   public List canopyList;
   public List landingGear;
   public List partThrottle;
   public List partRotPart;
   public List partCrawlerTrack;
   public List partTrackRoller;
   public List partWheel;
   public List partSteeringWheel;
   public List lightHatchList;
   private String lastWeaponType = "";
   private int lastWeaponIndex = -1;
   private MCH_AircraftInfo.PartWeapon lastWeaponPart;
   public int radarPower=0;
   public int radarMax = 0;
   public List<MCH_Hardpoint> hardpointList = new ArrayList<MCH_Hardpoint>();
   public int surfaceSearch=0;
   public double esmPower = 0.0f;
   public boolean isNaval = false;

   Hashtable<String, Integer>fuelDict = new Hashtable<String, Integer>();

   public abstract Item getItem();

   public ItemStack getItemStack() {
      return new ItemStack(this.getItem());
   }

   public abstract String getDirectoryName();

   public abstract String getKindName();

   public MCH_AircraftInfo(String s) {
      this.name = s;
      this.displayName = this.name;
      this.displayNameLang = new HashMap();
      this.itemID = 0;
      this.recipeString = new ArrayList();
      this.recipe = new ArrayList();
      this.isShapedRecipe = true;
      this.category = "zzz";
      this.isEnableGunnerMode = false;
      this.isEnableConcurrentGunnerMode = false;
      this.isEnableNightVision = false;
      this.isEnableEntityRadar = false;
      this.isEnableEjectionSeat = false;
      this.isEnableParachuting = false;
      this.flare = new MCH_AircraftInfo.Flare();
      this.weaponSetList = new ArrayList();
      this.seatList = new ArrayList();
      this.exclusionSeatList = new ArrayList();
      this.hudList = new ArrayList();
      this.hudTvMissile = null;
      this.bodyHeight = 0.7F;
      this.bodyWidth = 2.0F;
      this.isFloat = false;
      this.floatOffset = 0.0F;
      this.gravity = -0.04F;
      this.gravityInWater = -0.04F;
      this.maxHp = 10;
      this.damageFactor = 0.2F;
      this.submergedDamageHeight = 0.0F;
      this.inventorySize = 0;
      this.armorDamageFactor = 0.0F;
      this.armorMaxDamage = 0.0F;
      this.armorMinDamage = 0.0F;
      this.enableBack = false;
      this.isUAV = false;
      this.isSmallUAV = false;
      this.isTargetDrone = false;
      this.autoPilotRot = -0.6F;
      this.regeneration = false;
      this.onGroundPitch = 0.0F;
      this.canMoveOnGround = true;
      this.canRotOnGround = true;
      this.cameraZoom = this.getDefaultMaxZoom();
      this.extraBoundingBox = new ArrayList();
      this.maxFuel = 0;
      this.fuelConsumption = 1.0F;
      this.fuelSupplyRange = 0.0F;
      this.ammoSupplyRange = 0.0F;
      this.repairOtherVehiclesRange = 0.0F;
      this.repairOtherVehiclesValue = 10;
      this.stealth = 0.0F;
      this.canRide = true;
      this.entityWidth = 1.0F;
      this.entityHeight = 1.0F;
      this.entityPitch = 0.0F;
      this.entityRoll = 0.0F;
      this.stepHeight = this.getDefaultStepHeight();
      this.entityRackList = new ArrayList();
      this.mobSeatNum = 0;
      this.entityRackNum = 0;
      this.mobDropOption = new MCH_MobDropOption();
      this.repellingHooks = new ArrayList();
      this.rideRacks = new ArrayList();
      this.particleSplashs = new ArrayList();
      this.searchLights = new ArrayList();
      this.markerHeight = 1.0F;
      this.markerWidth = 2.0F;
      this.bbZmax = 1.0F;
      this.bbZmin = -1.0F;
      this.rotorSpeed = this.getDefaultRotorSpeed();
      this.wheels = this.getDefaultWheelList();
      this.onGroundPitchFactor = 0.0F;
      this.onGroundRollFactor = 0.0F;
      this.turretPosition = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      this.defaultFreelook = false;
      this.unmountPosition = null;
      this.cameraPosition = new ArrayList();
      this.alwaysCameraView = false;
      this.cameraRotationSpeed = 1000.0F;
      this.speed = 0.1F;
      this.motionFactor = 0.96F;
      this.mobilityYaw = 1.0F;
      this.mobilityPitch = 1.0F;
      this.mobilityRoll = 1.0F;
      this.mobilityYawOnGround = 1.0F;
      this.minRotationPitch = this.getMinRotationPitch();
      this.maxRotationPitch = this.getMaxRotationPitch();
      this.minRotationRoll = this.getMinRotationPitch();
      this.maxRotationRoll = this.getMaxRotationPitch();
      this.limitRotation = false;
      this.throttleUpDown = 1.0F;
      this.throttleUpDownOnEntity = 2.0F;
      this.pivotTurnThrottle = 0.0F;
      this.trackRollerRot = 30.0F;
      this.partWheelRot = 30.0F;
      this.textureNameList = new ArrayList();
      this.textureNameList.add(this.name);
      this.textureCount = 0;
      this.particlesScale = 1.0F;
      this.enableSeaSurfaceParticle = false;
      this.hideEntity = false;
      this.smoothShading = true;
      this.soundMove = "";
      this.soundPitch = 1.0F;
      this.soundVolume = 1.0F;
      this.soundRange = this.getDefaultSoundRange();
      this.model = null;
      this.hatchList = new ArrayList();
      this.cameraList = new ArrayList();
      this.partWeapon = new ArrayList();
      this.lastWeaponPart = null;
      this.partWeaponBay = new ArrayList();
      this.canopyList = new ArrayList();
      this.landingGear = new ArrayList();
      this.partThrottle = new ArrayList();
      this.partRotPart = new ArrayList();
      this.partCrawlerTrack = new ArrayList();
      this.partTrackRoller = new ArrayList();
      this.partWheel = new ArrayList();
      this.partSteeringWheel = new ArrayList();
      this.lightHatchList = new ArrayList();
      this.radarPower = 0;

   }

   public float getDefaultSoundRange() {
      return 100.0F;
   }

   public List getDefaultWheelList() {
      return new ArrayList();
   }

   public float getDefaultRotorSpeed() {
      return 0.0F;
   }

   private float getDefaultStepHeight() {
      return 0.0F;
   }

   public boolean haveRepellingHook() {
      return this.repellingHooks.size() > 0;
   }

   public boolean haveFlare() {
      return this.flare.types.length > 0;
   }

   public boolean haveCanopy() {
      return this.canopyList.size() > 0;
   }

   public boolean haveLandingGear() {
      return this.landingGear.size() > 0;
   }

   public abstract String getDefaultHudName(int var1);

   public boolean isValidData() throws Exception {
      if(this.cameraPosition.size() <= 0) {
         this.cameraPosition.add(new MCH_AircraftInfo.CameraPosition());
      }

      this.bbZ = (this.bbZmax + this.bbZmin) / 2.0F;
      if(this.isTargetDrone) {
         this.isUAV = true;
      }

      if(this.isEnableParachuting && this.repellingHooks.size() > 0) {
         this.isEnableParachuting = false;
         this.repellingHooks.clear();
      }

      if(this.isUAV) {
         this.alwaysCameraView = true;
         if(this.seatList.size() == 0) {
            MCH_SeatInfo i = new MCH_SeatInfo(Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), false);
            this.seatList.add(i);
         }
      }

      this.mobSeatNum = this.seatList.size();
      this.entityRackNum = this.entityRackList.size();
      if(this.getNumSeat() < 1) {
         throw new Exception();
      } else {
         int var10;
         if(this.getNumHud() < this.getNumSeat()) {
            for(var10 = this.getNumHud(); var10 < this.getNumSeat(); ++var10) {
               this.hudList.add(MCH_HudManager.get(this.getDefaultHudName(var10)));
            }
         }

         if(this.getNumSeat() == 1 && this.getNumHud() == 1) {
            this.hudList.add(MCH_HudManager.get(this.getDefaultHudName(1)));
         }

         Iterator var11 = this.entityRackList.iterator();

         while(var11.hasNext()) {
            MCH_SeatRackInfo wb = (MCH_SeatRackInfo)var11.next();
            this.seatList.add(wb);
         }

         this.entityRackList.clear();
         if(this.hudTvMissile == null) {
            this.hudTvMissile = MCH_HudManager.get("tv_missile");
         }

         if(this.textureNameList.size() < 1) {
            throw new Exception();
         } else {
            if(this.itemID <= 0) {
               ;
            }

            for(var10 = 0; var10 < this.partWeaponBay.size(); ++var10) {
               MCH_AircraftInfo.WeaponBay var12 = (MCH_AircraftInfo.WeaponBay)this.partWeaponBay.get(var10);
               String[] weaponNames = var12.weaponName.split("\\s*/\\s*");
               if(weaponNames.length <= 0) {
                  this.partWeaponBay.remove(var10);
               } else {
                  ArrayList list = new ArrayList();
                  String[] arr$ = weaponNames;
                  int len$ = weaponNames.length;

                  for(int i$ = 0; i$ < len$; ++i$) {
                     String s = arr$[i$];
                     int id = this.getWeaponIdByName(s);
                     if(id >= 0) {
                        list.add(Integer.valueOf(id));
                     }
                  }

                  if(list.size() <= 0) {
                     this.partWeaponBay.remove(var10);
                  } else {
                     ((MCH_AircraftInfo.WeaponBay)this.partWeaponBay.get(var10)).weaponIds = (Integer[])list.toArray(new Integer[0]);
                  }
               }
            }

            return true;
         }
      }
   }

   public int getInfo_MaxSeatNum() {
      return 30;
   }

   public int getNumSeatAndRack() {
      return this.seatList.size();
   }

   public int getNumSeat() {
      return this.mobSeatNum;
   }

   public int getNumRack() {
      return this.entityRackNum;
   }

   public int getNumHud() {
      return this.hudList.size();
   }

   public float getMaxSpeed() {
      return 0.8F;
   }

   public float getMinRotationPitch() {
      return -89.9F;
   }

   public float getMaxRotationPitch() {
      return 80.0F;
   }

   public float getMinRotationRoll() {
      return -80.0F;
   }

   public float getMaxRotationRoll() {
      return 80.0F;
   }

   public int getDefaultMaxZoom() {
      return 1;
   }

   public boolean haveHatch() {
      return this.hatchList.size() > 0;
   }

   public boolean havePartCamera() {
      return this.cameraList.size() > 0;
   }

   public boolean havePartThrottle() {
      return this.partThrottle.size() > 0;
   }

   public MCH_AircraftInfo.WeaponSet getWeaponSetById(int id) {
      return id >= 0 && id < this.weaponSetList.size()?(MCH_AircraftInfo.WeaponSet)this.weaponSetList.get(id):null;
   }

   public MCH_AircraftInfo.Weapon getWeaponById(int id) {
      MCH_AircraftInfo.WeaponSet ws = this.getWeaponSetById(id);
      return ws != null?(MCH_AircraftInfo.Weapon)ws.weapons.get(0):null;
   }

   public int getWeaponIdByName(String s) {
      for(int i = 0; i < this.weaponSetList.size(); ++i) {
         if(((MCH_AircraftInfo.WeaponSet)this.weaponSetList.get(i)).type.equalsIgnoreCase(s)) {
            return i;
         }
      }

      return -1;
   }

   public MCH_AircraftInfo.Weapon getWeaponByName(String s) {
      for(int i = 0; i < this.weaponSetList.size(); ++i) {
         if(((MCH_AircraftInfo.WeaponSet)this.weaponSetList.get(i)).type.equalsIgnoreCase(s)) {
            return this.getWeaponById(i);
         }
      }

      return null;
   }

   public int getWeaponNum() {
      return this.weaponSetList.size();
   }

   public void loadItemData(String item, String data) {
      if(item.compareTo("displayname") == 0) {
         this.displayName = data.trim();
      } else {
         String[] s;
         if(item.compareTo("adddisplayname") == 0) {
            s = data.split("\\s*,\\s*");
            if(s != null && s.length == 2) {
               this.displayNameLang.put(s[0].trim(), s[1].trim());
            }
         } else if(item.equalsIgnoreCase("radarPower")) {
				this.radarPower = toInt(data);	
         } else if(item.equalsIgnoreCase("surfaceSearch")) {
				this.surfaceSearch = toInt(data);
		}else if(item.equalsIgnoreCase("radarMax")){
        	 this.radarMax = toInt(data);
         }else if(item.equalsIgnoreCase("addFuel")){
            String[] data2 = data.split(" ");
            try{
               String fuelName = data2[0];
               int fuelVal = Integer.parseInt(data2[1]);
               this.fuelDict.put(fuelName, fuelVal);
            }catch(Exception e){}
         }else if(item.compareTo("addhardpoint") == 0) {
 			s = data.split("\\s*,\\s*");
 			String[] types = s[0].split(";");
 			float y = (s.length >= 5) ? toFloat(s[4]) : 0.0F;
 			float p = (s.length >= 6) ? toFloat(s[5]) : 0.0F;
 			boolean canUsePilot = (s.length >= 7) ? toBool(s[6]) : true;
 			int seatID = (s.length >= 8) ? (toInt(s[7], 1, getInfo_MaxSeatNum()) - 1) : 0;
 			if (seatID <= 0) canUsePilot = true; 
 			float dfy = (s.length >= 9) ? toFloat(s[8]) : 0.0F;
 			dfy = MathHelper.wrapAngleTo180_float(dfy);
 			float mny = (s.length >= 10) ? toFloat(s[9]) : 0.0F;
 			float mxy = (s.length >= 11) ? toFloat(s[10]) : 0.0F;
 			float mnp = (s.length >= 12) ? toFloat(s[11]) : 0.0F;
 			float mxp = (s.length >= 13) ? toFloat(s[12]) : 0.0F;
 			//System.out.println("HARDPOINT " + s[0]);
 			MCH_Hardpoint hardpoint = new MCH_Hardpoint(toFloat(s[1]), toFloat(s[2]), toFloat(s[3]),y,p,canUsePilot,seatID,dfy,mny,mxy,mnp,mxp);

 			for(String t : types){
               String[] x = t.split(":");
               int qty = toInt(x[1]);
               hardpoint.addWeaponData(x[0], qty);
            }
 			this.hardpointList.add(hardpoint);
 	 	}else  if(item.equalsIgnoreCase("Category")) {
            this.category = data.toUpperCase().replaceAll("[,;:]", ".").replaceAll("[ \t]", "");
         } else if(item.equalsIgnoreCase("CanRide")) {
            this.canRide = this.toBool(data, true);
         } else if(item.equalsIgnoreCase("isNaval")) {
            this.isNaval = this.toBool(data, false);
         } else if(item.equalsIgnoreCase("MaxFuel")) {
            this.maxFuel = this.toInt(data, 0, 100000000);
         } else if(item.equalsIgnoreCase("ESM")) {
             this.esmPower = this.toFloat(data, 0.0F, 10.0F);
          }else if(item.equalsIgnoreCase("FuelConsumption")) {
            this.fuelConsumption = this.toFloat(data, 0.0F, 10000.0F);
         } else if(item.equalsIgnoreCase("FuelSupplyRange")) {
            this.fuelSupplyRange = this.toFloat(data, 0.0F, 1000.0F);
         } else if(item.equalsIgnoreCase("AmmoSupplyRange")) {
            this.ammoSupplyRange = this.toFloat(data, 0.0F, 1000.0F);
         } else if(item.equalsIgnoreCase("RepairOtherVehicles")) {
            s = this.splitParam(data);
            if(s.length >= 1) {
               this.repairOtherVehiclesRange = this.toFloat(s[0], 0.0F, 1000.0F);
               if(s.length >= 2) {
                  this.repairOtherVehiclesValue = this.toInt(s[1], 0, 10000000);
               }
            }
         } else if(item.compareTo("itemid") == 0) {
            this.itemID = this.toInt(data, 0, '\uffff');
         } else if(item.compareTo("addtexture") == 0) {
            this.textureNameList.add(data.toLowerCase());
         } else if(item.compareTo("particlesscale") == 0) {
            this.particlesScale = this.toFloat(data, 0.0F, 50.0F);
         } else if(item.equalsIgnoreCase("EnableSeaSurfaceParticle")) {
            this.enableSeaSurfaceParticle = this.toBool(data);
         } else {
            Vec3 df;
            int c;
            float ry;
            float rz;
            int px;
            float py;
            float pz;
            if(item.equalsIgnoreCase("AddParticleSplash")) {
               s = this.splitParam(data);
               if(s.length >= 3) {
                  df = this.toVec3(s[0], s[1], s[2]);
                  c = s.length >= 4?this.toInt(s[3], 1, 100):2;
                  ry = s.length >= 5?this.toFloat(s[4]):2.0F;
                  rz = s.length >= 6?this.toFloat(s[5]):1.0F;
                  px = s.length >= 7?this.toInt(s[6], 1, 100000):80;
                  py = s.length >= 8?this.toFloat(s[7]):0.01F;
                  pz = s.length >= 9?this.toFloat(s[8]):0.0F;
                  this.particleSplashs.add(new MCH_AircraftInfo.ParticleSplash(df, c, ry, rz, px, py, pz));
               }
            } else {
               float w;
               int var22;
               float var26;
               if(!item.equalsIgnoreCase("AddSearchLight") && !item.equalsIgnoreCase("AddFixedSearchLight") && !item.equalsIgnoreCase("AddSteeringSearchLight")) {
                  float var15;
                  if(item.equalsIgnoreCase("AddPartLightHatch")) {
                     s = this.splitParam(data);
                     if(s.length >= 6) {
                        var15 = s.length >= 7?this.toFloat(s[6], -1800.0F, 1800.0F):90.0F;
                        this.lightHatchList.add(new MCH_AircraftInfo.Hatch(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), var15, "light_hatch" + this.lightHatchList.size(), false));
                     }
                  } else {
                     int var16;
                     if(item.equalsIgnoreCase("AddRepellingHook")) {
                        s = this.splitParam(data);
                        if(s != null && s.length >= 3) {
                           var16 = s.length >= 4?this.toInt(s[3], 1, 100000):10;
                           this.repellingHooks.add(new MCH_AircraftInfo.RepellingHook(this.toVec3(s[0], s[1], s[2]), var16));
                        }
                     } else {
                        String[] var17;
                        float var18;
                        boolean var31;
                        if(item.equalsIgnoreCase("AddRack")) {
                           s = data.toLowerCase().split("\\s*,\\s*");
                           if(s != null && s.length >= 7) {
                              var17 = s[0].split("\\s*/\\s*");
                              var18 = s.length >= 8?this.toFloat(s[7]):6.0F;
                              ry = s.length >= 9?this.toFloat(s[8], 0.0F, 1000000.0F):20.0F;
                              rz = s.length >= 10?this.toFloat(s[9]):0.0F;
                              var26 = s.length >= 11?this.toFloat(s[10]):0.0F;
                              var31 = s.length >= 12?this.toBool(s[11]):false;
                              this.entityRackList.add(new MCH_SeatRackInfo(var17, this.toDouble(s[1]), this.toDouble(s[2]), this.toDouble(s[3]), new MCH_AircraftInfo.CameraPosition(this.toVec3(s[4], s[5], s[6]).addVector(0.0D, 1.5D, 0.0D)), var18, ry, rz, var26, var31));
                           }
                        } else if(item.equalsIgnoreCase("RideRack")) {
                           s = this.splitParam(data);
                           if(s.length >= 2) {
                              MCH_AircraftInfo.RideRack var19 = new MCH_AircraftInfo.RideRack(s[0].trim().toLowerCase(), this.toInt(s[1], 1, 10000));
                              this.rideRacks.add(var19);
                           }
                        } else {
                           Vec3 var20;
                           boolean var25;
                           boolean var34;
                           boolean var37;
                           if(!item.equalsIgnoreCase("AddSeat") && !item.equalsIgnoreCase("AddGunnerSeat") && !item.equalsIgnoreCase("AddFixRotSeat")) {
                              if(item.equalsIgnoreCase("SetWheelPos")) {
                                 s = this.splitParam(data);
                                 if(s.length >= 4) {
                                    var15 = Math.abs(this.toFloat(s[0]));
                                    var18 = this.toFloat(s[1]);
                                    this.wheels.clear();

                                    for(var22 = 2; var22 < s.length; ++var22) {
                                       this.wheels.add(new MCH_AircraftInfo.Wheel(Vec3.createVectorHelper((double)var15, (double)var18, (double)this.toFloat(s[var22]))));
                                    }

                                    Collections.sort(this.wheels, new Comparator<Wheel>(){
                                       public int compare(MCH_AircraftInfo.Wheel arg0, MCH_AircraftInfo.Wheel arg1) {
                                          return arg0.pos.zCoord > arg1.pos.zCoord?-1:1;
                                       }
                                    });
                                 }
                              } else if(item.equalsIgnoreCase("ExclusionSeat")) {
                                 s = this.splitParam(data);
                                 if(s.length >= 2) {
                                    Integer[] var27 = new Integer[s.length];

                                    for(c = 0; c < var27.length; ++c) {
                                       var27[c] = Integer.valueOf(this.toInt(s[c], 1, 10000) - 1);
                                    }

                                    this.exclusionSeatList.add(var27);
                                 }
                              } else if(MCH_MOD.proxy.isRemote() && item.equalsIgnoreCase("HUD")) {
                                 this.hudList.clear();
                                 s = data.split("\\s*,\\s*");
                                 var17 = s;
                                 c = s.length;

                                 for(var22 = 0; var22 < c; ++var22) {
                                    String var43 = var17[var22];
                                    MCH_Hud var38 = MCH_HudManager.get(var43);
                                    if(var38 == null) {
                                       var38 = MCH_Hud.NoDisp;
                                    }

                                    this.hudList.add(var38);
                                 }
                              } else if(item.compareTo("enablenightvision") == 0) {
                                 this.isEnableNightVision = this.toBool(data);
                              } else if(item.compareTo("enableentityradar") == 0) {
                                 this.isEnableEntityRadar = this.toBool(data);
                              } else if(item.equalsIgnoreCase("EnableEjectionSeat")) {
                                 this.isEnableEjectionSeat = this.toBool(data);
                              } else if(item.equalsIgnoreCase("EnableParachuting")) {
                                 this.isEnableParachuting = this.toBool(data);
                              } else if(item.equalsIgnoreCase("MobDropOption")) {
                                 s = this.splitParam(data);
                                 if(s.length >= 3) {
                                    this.mobDropOption.pos = this.toVec3(s[0], s[1], s[2]);
                                    this.mobDropOption.interval = s.length >= 4?this.toInt(s[3]):12;
                                 }
                              } else if(item.equalsIgnoreCase("Width")) {
                                 this.bodyWidth = this.toFloat(data, 0.1F, 1000.0F);
                              } else if(item.equalsIgnoreCase("Height")) {
                                 this.bodyHeight = this.toFloat(data, 0.1F, 1000.0F);
                              } else if(item.compareTo("float") == 0) {
                                 this.isFloat = this.toBool(data);
                              } else if(item.compareTo("floatoffset") == 0) {
                                 this.floatOffset = -this.toFloat(data);
                              } else if(item.compareTo("gravity") == 0) {
                                 this.gravity = this.toFloat(data, -50.0F, 50.0F);
                              } else if(item.compareTo("gravityinwater") == 0) {
                                 this.gravityInWater = this.toFloat(data, -50.0F, 50.0F);
                              } else {
                                 boolean var28;
                                 if(item.compareTo("cameraposition") == 0) {
                                    s = data.split("\\s*,\\s*");
                                    if(s.length >= 3) {
                                       this.alwaysCameraView = s.length >= 4?this.toBool(s[3]):false;
                                       var28 = s.length >= 5;
                                       var18 = s.length >= 5?this.toFloat(s[4]):0.0F;
                                       ry = s.length >= 6?this.toFloat(s[5]):0.0F;
                                       this.cameraPosition.add(new MCH_AircraftInfo.CameraPosition(this.toVec3(s[0], s[1], s[2]), var28, var18, ry));
                                    }
                                 } else if(item.equalsIgnoreCase("UnmountPosition")) {
                                    s = data.split("\\s*,\\s*");
                                    if(s.length >= 3) {
                                       this.unmountPosition = this.toVec3(s[0], s[1], s[2]);
                                    }
                                 } else if(item.equalsIgnoreCase("TurretPosition")) {
                                    s = data.split("\\s*,\\s*");
                                    if(s.length >= 3) {
                                       this.turretPosition = this.toVec3(s[0], s[1], s[2]);
                                    }
                                 } else if(item.equalsIgnoreCase("CameraRotationSpeed")) {
                                    this.cameraRotationSpeed = this.toFloat(data, 0.0F, 10000.0F);
                                 } else if(item.compareTo("regeneration") == 0) {
                                    this.regeneration = this.toBool(data);
                                 } else if(item.compareTo("speed") == 0) {
                                    this.speed = this.toFloat(data, 0.0F, this.getMaxSpeed());
                                 } else if(item.equalsIgnoreCase("EnableBack")) {
                                    this.enableBack = this.toBool(data);
                                 } else if(item.equalsIgnoreCase("MotionFactor")) {
                                    this.motionFactor = this.toFloat(data, 0.0F, 1.0F);
                                 } else if(item.equalsIgnoreCase("MobilityYawOnGround")) {
                                    this.mobilityYawOnGround = this.toFloat(data, 0.0F, 100.0F);
                                 } else if(item.equalsIgnoreCase("MobilityYaw")) {
                                    this.mobilityYaw = this.toFloat(data, 0.0F, 100.0F);
                                 } else if(item.equalsIgnoreCase("MobilityPitch")) {
                                    this.mobilityPitch = this.toFloat(data, 0.0F, 100.0F);
                                 } else if(item.equalsIgnoreCase("MobilityRoll")) {
                                    this.mobilityRoll = this.toFloat(data, 0.0F, 100.0F);
                                 } else if(item.equalsIgnoreCase("MinRotationPitch")) {
                                    this.limitRotation = true;
                                    this.minRotationPitch = this.toFloat(data, this.getMinRotationPitch(), 0.0F);
                                 } else if(item.equalsIgnoreCase("MaxRotationPitch")) {
                                    this.limitRotation = true;
                                    this.maxRotationPitch = this.toFloat(data, 0.0F, this.getMaxRotationPitch());
                                 } else if(item.equalsIgnoreCase("MinRotationRoll")) {
                                    this.limitRotation = true;
                                    this.minRotationRoll = this.toFloat(data, this.getMinRotationRoll(), 0.0F);
                                 } else if(item.equalsIgnoreCase("MaxRotationRoll")) {
                                    this.limitRotation = true;
                                    this.maxRotationRoll = this.toFloat(data, 0.0F, this.getMaxRotationRoll());
                                 } else if(item.compareTo("throttleupdown") == 0) {
                                    this.throttleUpDown = this.toFloat(data, 0.0F, 3.0F);
                                 } else if(item.equalsIgnoreCase("ThrottleUpDownOnEntity")) {
                                    this.throttleUpDownOnEntity = this.toFloat(data, 0.0F, 100000.0F);
                                 } else if(item.equalsIgnoreCase("Stealth")) {
                                    this.stealth = this.toFloat(data, 0.0F, 1000.0F);
                                 } else if(item.equalsIgnoreCase("EntityWidth")) {
                                    this.entityWidth = this.toFloat(data, -100.0F, 100.0F);
                                 } else if(item.equalsIgnoreCase("EntityHeight")) {
                                    this.entityHeight = this.toFloat(data, -100.0F, 100.0F);
                                 } else if(item.equalsIgnoreCase("EntityPitch")) {
                                    this.entityPitch = this.toFloat(data, -360.0F, 360.0F);
                                 } else if(item.equalsIgnoreCase("EntityRoll")) {
                                    this.entityRoll = this.toFloat(data, -360.0F, 360.0F);
                                 } else if(item.equalsIgnoreCase("StepHeight")) {
                                    this.stepHeight = this.toFloat(data, 0.0F, 1000.0F);
                                 } else if(item.equalsIgnoreCase("CanMoveOnGround")) {
                                    this.canMoveOnGround = this.toBool(data);
                                 } else if(item.equalsIgnoreCase("CanRotOnGround")) {
                                    this.canRotOnGround = this.toBool(data);
                                 } else if(!item.equalsIgnoreCase("AddWeapon") && !item.equalsIgnoreCase("AddTurretWeapon")) {
                                    if(!item.equalsIgnoreCase("AddPartWeapon") && !item.equalsIgnoreCase("AddPartRotWeapon") && !item.equalsIgnoreCase("AddPartTurretWeapon") && !item.equalsIgnoreCase("AddPartTurretRotWeapon") && !item.equalsIgnoreCase("AddPartWeaponMissile")) {
                                       if(item.equalsIgnoreCase("AddPartWeaponChild")) {
                                          s = data.split("\\s*,\\s*");
                                          if(s.length >= 5 && this.lastWeaponPart != null) {
                                             var15 = s.length >= 6?this.toFloat(s[5]):0.0F;
                                             MCH_AircraftInfo.PartWeaponChild var30 = new MCH_AircraftInfo.PartWeaponChild(this.lastWeaponPart.name, this.toBool(s[0]), this.toBool(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.lastWeaponPart.modelName + "_" + this.lastWeaponPart.child.size(), 0.0F, 0.0F, 0.0F, var15);
                                             this.lastWeaponPart.child.add(var30);
                                          }
                                       } else if(item.compareTo("addrecipe") != 0 && item.compareTo("addshapelessrecipe") != 0) {
                                          if(item.compareTo("maxhp") == 0) {
                                            // this.maxHp = this.toInt(data, 1, 100000);
                                          } else if(item.compareTo("inventorysize") == 0) {
                                             this.inventorySize = this.toInt(data, 0, 54);
                                          } else if(item.compareTo("damagefactor") == 0) {
                                             this.damageFactor = this.toFloat(data, 0.0F, 1.0F);
                                          } else if(item.equalsIgnoreCase("SubmergedDamageHeight")) {
                                             this.submergedDamageHeight = this.toFloat(data, -1000.0F, 1000.0F);
                                          } else if(item.equalsIgnoreCase("ArmorDamageFactor")) {
                                             this.armorDamageFactor = this.toFloat(data, 0.0F, 10000.0F);
                                          } else if(item.equalsIgnoreCase("ArmorMinDamage")) {
                                             this.armorMinDamage = this.toFloat(data, 0.0F, 1000000.0F);
                                          } else if(item.equalsIgnoreCase("ArmorMaxDamage")) {
                                             this.armorMaxDamage = this.toFloat(data, 0.0F, 1000000.0F);
                                          } else if(item.equalsIgnoreCase("FlareType")) {
                                             s = data.split("\\s*,\\s*");
                                             this.flare.types = new int[s.length];

                                             for(var16 = 0; var16 < s.length; ++var16) {
                                                this.flare.types[var16] = this.toInt(s[var16], 1, 10);
                                             }
                                          } else if(item.equalsIgnoreCase("FlareOption")) {
                                             s = this.splitParam(data);
                                             if(s.length >= 3) {
                                                this.flare.pos = this.toVec3(s[0], s[1], s[2]);
                                             }
                                          } else if(item.equalsIgnoreCase("Sound")) {
                                             this.soundMove = data.toLowerCase();
                                          } else if(item.equalsIgnoreCase("SoundRange")) {
                                             this.soundRange = this.toFloat(data, 1.0F, 1000.0F);
                                          } else if(item.equalsIgnoreCase("SoundVolume")) {
                                             this.soundVolume = this.toFloat(data, 0.0F, 10.0F);
                                          } else if(item.equalsIgnoreCase("SoundPitch")) {
                                             this.soundPitch = this.toFloat(data, 0.0F, 10.0F);
                                          } else if(item.equalsIgnoreCase("UAV")) {
                                             this.isUAV = this.toBool(data);
                                             this.isSmallUAV = false;
                                          } else if(item.equalsIgnoreCase("SmallUAV")) {
                                             this.isUAV = this.toBool(data);
                                             this.isSmallUAV = true;
                                          } else if(item.equalsIgnoreCase("TargetDrone")) {
                                             this.isTargetDrone = this.toBool(data);
                                          } else if(item.compareTo("autopilotrot") == 0) {
                                             this.autoPilotRot = this.toFloat(data, -5.0F, 5.0F);
                                          } else if(item.compareTo("ongroundpitch") == 0) {
                                             this.onGroundPitch = -this.toFloat(data, -90.0F, 90.0F);
                                          } else if(item.compareTo("enablegunnermode") == 0) {
                                             this.isEnableGunnerMode = this.toBool(data);
                                          } else if(item.compareTo("hideentity") == 0) {
                                             this.hideEntity = this.toBool(data);
                                          } else if(item.equalsIgnoreCase("SmoothShading")) {
                                             this.smoothShading = this.toBool(data);
                                          } else if(item.compareTo("concurrentgunnermode") == 0) {
                                             this.isEnableConcurrentGunnerMode = this.toBool(data);
                                          } else {
                                             boolean var32;
                                             if(!item.equalsIgnoreCase("AddPartWeaponBay") && !item.equalsIgnoreCase("AddPartSlideWeaponBay")) {
                                                if(item.compareTo("addparthatch") != 0 && item.compareTo("addpartslidehatch") != 0) {
                                                   if(item.compareTo("addpartcanopy") != 0 && item.compareTo("addpartslidecanopy") != 0) {
                                                      if(!item.equalsIgnoreCase("AddPartLG") && !item.equalsIgnoreCase("AddPartSlideRotLG") && !item.equalsIgnoreCase("AddPartLGRev") && !item.equalsIgnoreCase("AddPartLGHatch")) {
                                                         if(item.equalsIgnoreCase("AddPartThrottle")) {
                                                            s = data.split("\\s*,\\s*");
                                                            if(s.length >= 7) {
                                                               var15 = s.length >= 8?this.toFloat(s[7]):0.0F;
                                                               var18 = s.length >= 9?this.toFloat(s[8]):0.0F;
                                                               ry = s.length >= 10?this.toFloat(s[9]):0.0F;
                                                               MCH_AircraftInfo.Throttle var40 = new MCH_AircraftInfo.Throttle(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), "throttle" + this.partThrottle.size(), var15, var18, ry);
                                                               this.partThrottle.add(var40);
                                                            }
                                                         } else if(item.equalsIgnoreCase("AddPartRotation")) {
                                                            s = data.split("\\s*,\\s*");
                                                            if(s.length >= 7) {
                                                               var28 = s.length >= 8?this.toBool(s[7]):true;
                                                               MCH_AircraftInfo.RotPart var46 = new MCH_AircraftInfo.RotPart(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), var28, "rotpart" + this.partThrottle.size());
                                                               this.partRotPart.add(var46);
                                                            }
                                                         } else if(item.compareTo("addpartcamera") == 0) {
                                                            s = data.split("\\s*,\\s*");
                                                            if(s.length >= 3) {
                                                               var28 = s.length >= 4?this.toBool(s[3]):true;
                                                               boolean var48 = s.length >= 5?this.toBool(s[4]):false;
                                                               MCH_AircraftInfo.Camera var45 = new MCH_AircraftInfo.Camera(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), 0.0F, -1.0F, 0.0F, "camera" + this.cameraList.size(), var28, var48);
                                                               this.cameraList.add(var45);
                                                            }
                                                         } else if(item.equalsIgnoreCase("AddPartWheel")) {
                                                            s = this.splitParam(data);
                                                            if(s.length >= 3) {
                                                               var15 = s.length >= 4?this.toFloat(s[3], -1800.0F, 1800.0F):0.0F;
                                                               var18 = s.length >= 7?this.toFloat(s[4]):0.0F;
                                                               ry = s.length >= 7?this.toFloat(s[5]):1.0F;
                                                               rz = s.length >= 7?this.toFloat(s[6]):0.0F;
                                                               var26 = s.length >= 10?this.toFloat(s[7]):this.toFloat(s[0]);
                                                               py = s.length >= 10?this.toFloat(s[8]):this.toFloat(s[1]);
                                                               pz = s.length >= 10?this.toFloat(s[9]):this.toFloat(s[2]);
                                                               this.partWheel.add(new MCH_AircraftInfo.PartWheel(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), var18, ry, rz, var15, var26, py, pz, "wheel" + this.partWheel.size()));
                                                            }
                                                         } else if(item.equalsIgnoreCase("AddPartSteeringWheel")) {
                                                            s = this.splitParam(data);
                                                            if(s.length >= 7) {
                                                               this.partSteeringWheel.add(new MCH_AircraftInfo.PartWheel(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), "steering_wheel" + this.partSteeringWheel.size()));
                                                            }
                                                         } else if(item.equalsIgnoreCase("AddTrackRoller")) {
                                                            s = this.splitParam(data);
                                                            if(s.length >= 3) {
                                                               this.partTrackRoller.add(new MCH_AircraftInfo.TrackRoller(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), "track_roller" + this.partTrackRoller.size()));
                                                            }
                                                         } else if(item.equalsIgnoreCase("AddCrawlerTrack")) {
                                                            this.partCrawlerTrack.add(this.createCrawlerTrack(data, "crawler_track" + this.partCrawlerTrack.size()));
                                                         } else if(item.equalsIgnoreCase("PivotTurnThrottle")) {
                                                            this.pivotTurnThrottle = this.toFloat(data, 0.0F, 1.0F);
                                                         } else if(item.equalsIgnoreCase("TrackRollerRot")) {
                                                            this.trackRollerRot = this.toFloat(data, -10000.0F, 10000.0F);
                                                         } else if(item.equalsIgnoreCase("PartWheelRot")) {
                                                            this.partWheelRot = this.toFloat(data, -10000.0F, 10000.0F);
                                                         } else if(item.compareTo("camerazoom") == 0) {
                                                            this.cameraZoom = this.toInt(data, 1, 10);
                                                         } else if(item.equalsIgnoreCase("DefaultFreelook")) {
                                                            this.defaultFreelook = this.toBool(data);
                                                         } else if(item.equalsIgnoreCase("BoundingBox")) {
                                                            s = data.split("\\s*,\\s*");
                                                            if(s.length >= 5) {
                                                               var15 = s.length >= 6?this.toFloat(s[5]):1.0F;
                                                               if(s.length >= 7) {
                                                            	   System.out.println("MM: " + s[6]);
                                                               }
                                                               int mm = s.length >= 7?this.toInt(s[6]):0;
                                                               MCH_BoundingBox var49 = new MCH_BoundingBox((double)this.toFloat(s[0]), (double)this.toFloat(s[1]), (double)this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), var15, mm);
                                                               this.extraBoundingBox.add(var49);
                                                               if(var49.boundingBox.maxY > (double)this.markerHeight) {
                                                                  this.markerHeight = (float)var49.boundingBox.maxY;
                                                               }

                                                               this.markerWidth = (float)Math.max((double)this.markerWidth, Math.abs(var49.boundingBox.maxX) / 2.0D);
                                                               this.markerWidth = (float)Math.max((double)this.markerWidth, Math.abs(var49.boundingBox.minX) / 2.0D);
                                                               this.markerWidth = (float)Math.max((double)this.markerWidth, Math.abs(var49.boundingBox.maxZ) / 2.0D);
                                                               this.markerWidth = (float)Math.max((double)this.markerWidth, Math.abs(var49.boundingBox.minZ) / 2.0D);
                                                               this.bbZmin = (float)Math.min((double)this.bbZmin, var49.boundingBox.minZ);
                                                               this.bbZmax = (float)Math.min((double)this.bbZmax, var49.boundingBox.maxZ);
                                                            }
                                                         } else if(item.equalsIgnoreCase("RotorSpeed")) {
                                                            this.rotorSpeed = this.toFloat(data, -10000.0F, 10000.0F);
                                                            if((double)this.rotorSpeed > 0.01D) {
                                                               this.rotorSpeed = (float)((double)this.rotorSpeed - 0.01D);
                                                            }

                                                            if((double)this.rotorSpeed < -0.01D) {
                                                               this.rotorSpeed = (float)((double)this.rotorSpeed + 0.01D);
                                                            }
                                                         } else if(item.equalsIgnoreCase("OnGroundPitchFactor")) {
                                                            this.onGroundPitchFactor = this.toFloat(data, 0.0F, 180.0F);
                                                         } else if(item.equalsIgnoreCase("OnGroundRollFactor")) {
                                                            this.onGroundRollFactor = this.toFloat(data, 0.0F, 180.0F);
                                                         }
                                                      } else {
                                                         s = data.split("\\s*,\\s*");
                                                         MCH_AircraftInfo.LandingGear var42;
                                                         if(!item.equalsIgnoreCase("AddPartSlideRotLG") && s.length >= 6) {
                                                            var15 = s.length >= 7?this.toFloat(s[6], -180.0F, 180.0F):90.0F;
                                                            var15 /= 90.0F;
                                                            var42 = new MCH_AircraftInfo.LandingGear(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), "lg" + this.landingGear.size(), var15, item.equalsIgnoreCase("AddPartLgRev"), item.equalsIgnoreCase("AddPartLGHatch"));
                                                            if(s.length >= 8) {
                                                               var42.enableRot2 = true;
                                                               var42.maxRotFactor2 = s.length >= 11?this.toFloat(s[10], -180.0F, 180.0F):90.0F;
                                                               var42.maxRotFactor2 /= 90.0F;
                                                               var42.rot2 = Vec3.createVectorHelper((double)this.toFloat(s[7]), (double)this.toFloat(s[8]), (double)this.toFloat(s[9]));
                                                            }

                                                            this.landingGear.add(var42);
                                                         }

                                                         if(item.equalsIgnoreCase("AddPartSlideRotLG") && s.length >= 9) {
                                                            var15 = s.length >= 10?this.toFloat(s[9], -180.0F, 180.0F):90.0F;
                                                            var15 /= 90.0F;
                                                            var42 = new MCH_AircraftInfo.LandingGear(this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), this.toFloat(s[7]), this.toFloat(s[8]), "lg" + this.landingGear.size(), var15, false, false);
                                                            var42.slide = Vec3.createVectorHelper((double)this.toFloat(s[0]), (double)this.toFloat(s[1]), (double)this.toFloat(s[2]));
                                                            this.landingGear.add(var42);
                                                         }
                                                      }
                                                   } else {
                                                      s = data.split("\\s*,\\s*");
                                                      var28 = item.compareTo("addpartslidecanopy") == 0;
                                                      var22 = this.canopyList.size();
                                                      if(var22 > 0) {
                                                         --var22;
                                                      }

                                                      MCH_AircraftInfo.Canopy var35;
                                                      if(var28) {
                                                         if(s.length >= 3) {
                                                            var35 = new MCH_AircraftInfo.Canopy(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), 0.0F, 0.0F, 0.0F, 90.0F, "canopy" + var22, var28);
                                                            this.canopyList.add(var35);
                                                            if(var22 == 0) {
                                                               var35 = new MCH_AircraftInfo.Canopy(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), 0.0F, 0.0F, 0.0F, 90.0F, "canopy", var28);
                                                               this.canopyList.add(var35);
                                                            }
                                                         }
                                                      } else if(s.length >= 6) {
                                                         var18 = s.length >= 7?this.toFloat(s[6], -180.0F, 180.0F):90.0F;
                                                         var18 /= 90.0F;
                                                         var35 = new MCH_AircraftInfo.Canopy(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), var18, "canopy" + var22, var28);
                                                         this.canopyList.add(var35);
                                                         if(var22 == 0) {
                                                            var35 = new MCH_AircraftInfo.Canopy(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), var18, "canopy", var28);
                                                            this.canopyList.add(var35);
                                                         }
                                                      }
                                                   }
                                                } else {
                                                   var32 = item.compareTo("addpartslidehatch") == 0;
                                                   var17 = data.split("\\s*,\\s*");
                                                   var20 = null;
                                                   MCH_AircraftInfo.Hatch var36;
                                                   if(var32) {
                                                      if(var17.length >= 3) {
                                                         var36 = new MCH_AircraftInfo.Hatch(this.toFloat(var17[0]), this.toFloat(var17[1]), this.toFloat(var17[2]), 0.0F, 0.0F, 0.0F, 90.0F, "hatch" + this.hatchList.size(), var32);
                                                         this.hatchList.add(var36);
                                                      }
                                                   } else if(var17.length >= 6) {
                                                      ry = var17.length >= 7?this.toFloat(var17[6], -180.0F, 180.0F):90.0F;
                                                      var36 = new MCH_AircraftInfo.Hatch(this.toFloat(var17[0]), this.toFloat(var17[1]), this.toFloat(var17[2]), this.toFloat(var17[3]), this.toFloat(var17[4]), this.toFloat(var17[5]), ry, "hatch" + this.hatchList.size(), var32);
                                                      this.hatchList.add(var36);
                                                   }
                                                }
                                             } else {
                                                var32 = item.equalsIgnoreCase("AddPartSlideWeaponBay");
                                                var17 = data.split("\\s*,\\s*");
                                                var20 = null;
                                                MCH_AircraftInfo.WeaponBay var33;
                                                if(var32) {
                                                   if(var17.length >= 4) {
                                                      var33 = new MCH_AircraftInfo.WeaponBay(var17[0].trim().toLowerCase(), this.toFloat(var17[1]), this.toFloat(var17[2]), this.toFloat(var17[3]), 0.0F, 0.0F, 0.0F, 90.0F, "wb" + this.partWeaponBay.size(), var32);
                                                      this.partWeaponBay.add(var33);
                                                   }
                                                } else if(var17.length >= 7) {
                                                   ry = var17.length >= 8?this.toFloat(var17[7], -180.0F, 180.0F):90.0F;
                                                   var33 = new MCH_AircraftInfo.WeaponBay(var17[0].trim().toLowerCase(), this.toFloat(var17[1]), this.toFloat(var17[2]), this.toFloat(var17[3]), this.toFloat(var17[4]), this.toFloat(var17[5]), this.toFloat(var17[6]), ry / 90.0F, "wb" + this.partWeaponBay.size(), var32);
                                                   this.partWeaponBay.add(var33);
                                                }
                                             }
                                          }
                                       } else {
                                         // this.isShapedRecipe = item.compareTo("addrecipe") == 0;
                                          //this.recipeString.add(data.toUpperCase());
                                       }
                                    } else {
                                       s = data.split("\\s*,\\s*");
                                       if(s.length >= 7) {
                                          var15 = 0.0F;
                                          var18 = 0.0F;
                                          ry = 0.0F;
                                          rz = 0.0F;
                                          var34 = item.equalsIgnoreCase("AddPartRotWeapon") || item.equalsIgnoreCase("AddPartTurretRotWeapon");
                                          var31 = item.equalsIgnoreCase("AddPartWeaponMissile");
                                          var37 = item.equalsIgnoreCase("AddPartTurretWeapon") || item.equalsIgnoreCase("AddPartTurretRotWeapon");
                                          if(var34) {
                                             var15 = s.length >= 10?this.toFloat(s[7]):0.0F;
                                             var18 = s.length >= 10?this.toFloat(s[8]):0.0F;
                                             ry = s.length >= 10?this.toFloat(s[9]):-1.0F;
                                          } else {
                                             rz = s.length >= 8?this.toFloat(s[7]):0.0F;
                                          }

                                          MCH_AircraftInfo.PartWeapon var41 = new MCH_AircraftInfo.PartWeapon(this.splitParamSlash(s[0].toLowerCase().trim()), var34, var31, this.toBool(s[1]), this.toBool(s[2]), this.toBool(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), "weapon" + this.partWeapon.size(), var15, var18, ry, rz, var37);
                                          this.lastWeaponPart = var41;
                                          this.partWeapon.add(var41);
                                       }
                                    }
                                 } else {
                                    s = data.split("\\s*,\\s*");
                                    String var29 = s[0].toLowerCase();
                                    if(s.length >= 4 && MCH_WeaponInfoManager.contains(var29)) {
                                       var18 = s.length >= 5?this.toFloat(s[4]):0.0F;
                                       ry = s.length >= 6?this.toFloat(s[5]):0.0F;
                                       var25 = s.length >= 7?this.toBool(s[6]):true;
                                       px = s.length >= 8?this.toInt(s[7], 1, this.getInfo_MaxSeatNum()) - 1:0;
                                       if(px <= 0) {
                                          var25 = true;
                                       }

                                       py = s.length >= 9?this.toFloat(s[8]):0.0F;
                                       py = MathHelper.wrapAngleTo180_float(py);
                                       pz = s.length >= 10?this.toFloat(s[9]):0.0F;
                                       w = s.length >= 11?this.toFloat(s[10]):0.0F;
                                       float var44 = s.length >= 12?this.toFloat(s[11]):0.0F;
                                       float var47 = s.length >= 13?this.toFloat(s[12]):0.0F;
                                       MCH_AircraftInfo.Weapon e = new MCH_AircraftInfo.Weapon(this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), var18, ry, var25, px, py, pz, w, var44, var47, item.equalsIgnoreCase("AddTurretWeapon"));
                                       if(var29.compareTo(this.lastWeaponType) != 0) {
                                          this.weaponSetList.add(new MCH_AircraftInfo.WeaponSet(var29));
                                          ++this.lastWeaponIndex;
                                          this.lastWeaponType = var29;
                                       }

                                       ((MCH_AircraftInfo.WeaponSet)this.weaponSetList.get(this.lastWeaponIndex)).weapons.add(e);
                                    }
                                 }
                              }
                           } else {
                              if(this.seatList.size() >= this.getInfo_MaxSeatNum()) {
                                 return;
                              }

                              s = this.splitParam(data);
                              if(s.length < 3) {
                                 return;
                              }

                              var20 = this.toVec3(s[0], s[1], s[2]);
                              MCH_SeatInfo var21;
                              if(item.equalsIgnoreCase("AddSeat")) {
                                 boolean var23 = s.length >= 4?this.toBool(s[3]):false;
                                 var21 = new MCH_SeatInfo(var20, var23);
                                 this.seatList.add(var21);
                              } else {
                                 if(s.length >= 6) {
                                    MCH_AircraftInfo.CameraPosition var24 = new MCH_AircraftInfo.CameraPosition(this.toVec3(s[3], s[4], s[5]));
                                    var25 = s.length >= 7?this.toBool(s[6]):false;
                                    if(item.equalsIgnoreCase("AddGunnerSeat")) {
                                       if(s.length >= 9) {
                                          var26 = this.toFloat(s[7], -90.0F, 90.0F);
                                          py = this.toFloat(s[8], -90.0F, 90.0F);
                                          if(var26 > py) {
                                             pz = var26;
                                             var26 = py;
                                             py = pz;
                                          }

                                          var37 = s.length >= 10?this.toBool(s[9]):false;
                                          var21 = new MCH_SeatInfo(var20, true, var24, true, var25, false, 0.0F, 0.0F, var26, py, var37);
                                       } else {
                                          var21 = new MCH_SeatInfo(var20, true, var24, true, var25, false, 0.0F, 0.0F, false);
                                       }
                                    } else {
                                       var34 = s.length >= 9;
                                       py = var34?this.toFloat(s[7]):0.0F;
                                       pz = var34?this.toFloat(s[8]):0.0F;
                                       boolean var39 = s.length >= 10?this.toBool(s[9]):false;
                                       var21 = new MCH_SeatInfo(var20, true, var24, true, var25, var34, py, pz, var39);
                                    }
                                 } else {
                                    var21 = new MCH_SeatInfo(var20, true, new MCH_AircraftInfo.CameraPosition(), false, false, false, 0.0F, 0.0F, false);
                                 }

                                 this.seatList.add(var21);
                              }
                           }
                        }
                     }
                  }
               } else {
                  s = this.splitParam(data);
                  if(s.length >= 7) {
                     df = this.toVec3(s[0], s[1], s[2]);
                     c = this.hex2dec(s[3]);
                     var22 = this.hex2dec(s[4]);
                     rz = this.toFloat(s[5]);
                     var26 = this.toFloat(s[6]);
                     py = s.length >= 8?this.toFloat(s[7]):0.0F;
                     pz = s.length >= 9?this.toFloat(s[8]):0.0F;
                     w = s.length >= 10?this.toFloat(s[9]):0.0F;
                     boolean mnp = !item.equalsIgnoreCase("AddSearchLight");
                     boolean mxp = item.equalsIgnoreCase("AddSteeringSearchLight");
                     this.searchLights.add(new MCH_AircraftInfo.SearchLight(df, c, var22, rz, var26, mnp, py, pz, mxp, w));
                  }
               }
            }
         }
      }

   }

   public MCH_AircraftInfo.CrawlerTrack createCrawlerTrack(String data, String name) {
      String[] s = this.splitParam(data);
      int PC = s.length - 3;
      boolean REV = this.toBool(s[0]);
      float LEN = this.toFloat(s[1], 0.001F, 1000.0F) * 0.9F;
      float Z = this.toFloat(s[2]);
      if(PC < 4) {
         return null;
      } else {
         double[] cx = new double[PC];
         double[] cy = new double[PC];

         for(int lp = 0; lp < PC; ++lp) {
            int dist = !REV?lp:PC - lp - 1;
            String[] xy = this.splitParamSlash(s[3 + dist]);
            cx[lp] = (double)this.toFloat(xy[0]);
            cy[lp] = (double)this.toFloat(xy[1]);
         }

         ArrayList var21 = new ArrayList();
         var21.add(new MCH_AircraftInfo.CrawlerTrackPrm((float)cx[0], (float)cy[0]));
         double var22 = 0.0D;

         int c;
         for(c = 0; c < PC; ++c) {
            double pp = cx[(c + 1) % PC] - cx[c];
            double np = cy[(c + 1) % PC] - cy[c];
            var22 += Math.sqrt(pp * pp + np * np);
            double nr = var22;

            for(int nnr = 1; var22 >= (double)LEN; ++nnr) {
               var21.add(new MCH_AircraftInfo.CrawlerTrackPrm((float)(cx[c] + pp * ((double)(LEN * (float)nnr) / nr)), (float)(cy[c] + np * ((double)(LEN * (float)nnr) / nr))));
               var22 -= (double)LEN;
            }
         }

         for(c = 0; c < var21.size(); ++c) {
            MCH_AircraftInfo.CrawlerTrackPrm var24 = (MCH_AircraftInfo.CrawlerTrackPrm)var21.get((c + var21.size() - 1) % var21.size());
            MCH_AircraftInfo.CrawlerTrackPrm cp = (MCH_AircraftInfo.CrawlerTrackPrm)var21.get(c);
            MCH_AircraftInfo.CrawlerTrackPrm var25 = (MCH_AircraftInfo.CrawlerTrackPrm)var21.get((c + 1) % var21.size());
            float pr = (float)(Math.atan2((double)(var24.x - cp.x), (double)(var24.y - cp.y)) * 180.0D / 3.141592653589793D);
            float var26 = (float)(Math.atan2((double)(var25.x - cp.x), (double)(var25.y - cp.y)) * 180.0D / 3.141592653589793D);
            float ppr = (pr + 360.0F) % 360.0F;
            float var27 = var26 + 180.0F;
            if(((double)var27 < (double)ppr - 0.3D || (double)var27 > (double)ppr + 0.3D) && var27 - ppr < 100.0F && var27 - ppr > -100.0F) {
               var27 = (var27 + ppr) / 2.0F;
            }

            cp.r = var27;
         }

         MCH_AircraftInfo.CrawlerTrack var23 = new MCH_AircraftInfo.CrawlerTrack(name);
         var23.len = LEN;
         var23.cx = cx;
         var23.cy = cy;
         var23.lp = var21;
         var23.z = Z;
         var23.side = Z >= 0.0F?1:0;
         return var23;
      }
   }

   public String getTextureName() {
      String s = (String)this.textureNameList.get(this.textureCount);
      this.textureCount = (this.textureCount + 1) % this.textureNameList.size();
      return s;
   }

   public String getNextTextureName(String base) {
      if(this.textureNameList.size() >= 2) {
         for(int i = 0; i < this.textureNameList.size(); ++i) {
            String s = (String)this.textureNameList.get(i);
            if(s.equalsIgnoreCase(base)) {
               i = (i + 1) % this.textureNameList.size();
               return (String)this.textureNameList.get(i);
            }
         }
      }

      return base;
   }

   public void preReload() {
      this.textureNameList.clear();
      this.textureNameList.add(this.name);
      this.cameraList.clear();
      this.cameraPosition.clear();
      this.canopyList.clear();
      this.flare = new MCH_AircraftInfo.Flare();
      this.hatchList.clear();
      this.hudList.clear();
      this.landingGear.clear();
      this.particleSplashs.clear();
      this.searchLights.clear();
      this.partThrottle.clear();
      this.partRotPart.clear();
      this.partCrawlerTrack.clear();
      this.partTrackRoller.clear();
      this.partWheel.clear();
      this.partSteeringWheel.clear();
      this.lightHatchList.clear();
      this.partWeapon.clear();
      this.partWeaponBay.clear();
      this.repellingHooks.clear();
      this.rideRacks.clear();
      this.seatList.clear();
      this.exclusionSeatList.clear();
      this.entityRackList.clear();
      this.extraBoundingBox.clear();
      this.weaponSetList.clear();
      this.lastWeaponIndex = -1;
      this.lastWeaponType = "";
      this.lastWeaponPart = null;
      this.wheels.clear();
      this.unmountPosition = null;
   }

   public static String[] getCannotReloadItem() {
      return new String[]{"DisplayName", "AddDisplayName", "ItemID", "AddRecipe", "AddShapelessRecipe", "InventorySize", "Sound", "UAV", "SmallUAV", "TargetDrone", "Category"};
   }

   public boolean canReloadItem(String item) {
      String[] ignoreItems = getCannotReloadItem();
      String[] arr$ = ignoreItems;
      int len$ = ignoreItems.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String s = arr$[i$];
         if(s.equalsIgnoreCase(item)) {
            return false;
         }
      }

      return true;
   }

   public class RotPart extends MCH_AircraftInfo.DrawnPart {

      public final float rotSpeed;
      public final boolean rotAlways;


      public RotPart(float px, float py, float pz, float rx, float ry, float rz, float mr, boolean a, String name) {
         super(px, py, pz, rx, ry, rz, name);
         this.rotSpeed = mr;
         this.rotAlways = a;
      }
   }

   public class Hatch extends MCH_AircraftInfo.DrawnPart {

      public final float maxRotFactor;
      public final float maxRot;
      public final boolean isSlide;


      public Hatch(float px, float py, float pz, float rx, float ry, float rz, float mr, String name, boolean slide) {
         super(px, py, pz, rx, ry, rz, name);
         this.maxRot = mr;
         this.maxRotFactor = this.maxRot / 90.0F;
         this.isSlide = slide;
      }
   }

   public class TrackRoller extends MCH_AircraftInfo.DrawnPart {

      final int side;


      public TrackRoller(float px, float py, float pz, String name) {
         super(px, py, pz, 0.0F, 0.0F, 0.0F, name);
         this.side = px >= 0.0F?1:0;
      }
   }

   public class LandingGear extends MCH_AircraftInfo.DrawnPart {

      public Vec3 slide = null;
      public final float maxRotFactor;
      public boolean enableRot2;
      public Vec3 rot2;
      public float maxRotFactor2;
      public final boolean reverse;
      public final boolean hatch;


      public LandingGear(float x, float y, float z, float rx, float ry, float rz, String model, float maxRotF, boolean rev, boolean isHatch) {
         super(x, y, z, rx, ry, rz, model);
         this.maxRotFactor = maxRotF;
         this.enableRot2 = false;
         this.rot2 = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
         this.maxRotFactor2 = 0.0F;
         this.reverse = rev;
         this.hatch = isHatch;
      }
   }

   public class PartWeaponChild extends MCH_AircraftInfo.DrawnPart {

      public final String[] name;
      public final boolean yaw;
      public final boolean pitch;
      public final float recoilBuf;


      public PartWeaponChild(String[] name, boolean y, boolean p, float px, float py, float pz, String modelName, float rx, float ry, float rz, float rb) {
         super(px, py, pz, rx, ry, rz, modelName);
         this.name = name;
         this.yaw = y;
         this.pitch = p;
         this.recoilBuf = rb;
      }
   }

   public class Canopy extends MCH_AircraftInfo.DrawnPart {

      public final float maxRotFactor;
      public final boolean isSlide;


      public Canopy(float px, float py, float pz, float rx, float ry, float rz, float mr, String name, boolean slide) {
         super(px, py, pz, rx, ry, rz, name);
         this.maxRotFactor = mr;
         this.isSlide = slide;
      }
   }

   public class SearchLight {

      public final int colorStart;
      public final int colorEnd;
      public final Vec3 pos;
      public final float height;
      public final float width;
      public final float angle;
      public final boolean fixDir;
      public final float yaw;
      public final float pitch;
      public final boolean steering;
      public final float stRot;


      public SearchLight(Vec3 pos, int cs, int ce, float h, float w, boolean fix, float y, float p, boolean st, float stRot) {
         this.colorStart = cs;
         this.colorEnd = ce;
         this.pos = pos;
         this.height = h;
         this.width = w;
         this.angle = (float)(Math.atan2((double)(w / 2.0F), (double)h) * 180.0D / 3.141592653589793D);
         this.fixDir = fix;
         this.steering = st;
         this.yaw = y;
         this.pitch = p;
         this.stRot = stRot;
      }
   }

   public class DrawnPart {

      public final Vec3 pos;
      public final Vec3 rot;
      public final String modelName;
      public IModelCustom model;


      public DrawnPart(float px, float py, float pz, float rx, float ry, float rz, String name) {
         this.pos = Vec3.createVectorHelper((double)px, (double)py, (double)pz);
         this.rot = Vec3.createVectorHelper((double)rx, (double)ry, (double)rz);
         this.modelName = name;
         this.model = null;
      }
   }

   public class Throttle extends MCH_AircraftInfo.DrawnPart {

      public final Vec3 slide;
      public final float rot2;


      public Throttle(float px, float py, float pz, float rx, float ry, float rz, float rot, String name, float px2, float py2, float pz2) {
         super(px, py, pz, rx, ry, rz, name);
         this.rot2 = rot;
         this.slide = Vec3.createVectorHelper((double)px2, (double)py2, (double)pz2);
      }
   }

   public class PartWeapon extends MCH_AircraftInfo.DrawnPart {

      public final String[] name;
      public final boolean rotBarrel;
      public final boolean isMissile;
      public final boolean hideGM;
      public final boolean yaw;
      public final boolean pitch;
      public final float recoilBuf;
      public List child;
      public final boolean turret;


      public PartWeapon(String[] name, boolean rotBrl, boolean missile, boolean hgm, boolean y, boolean p, float px, float py, float pz, String modelName, float rx, float ry, float rz, float rb, boolean turret) {
         super(px, py, pz, rx, ry, rz, modelName);
         this.name = name;
         this.rotBarrel = rotBrl;
         this.isMissile = missile;
         this.hideGM = hgm;
         this.yaw = y;
         this.pitch = p;
         this.recoilBuf = rb;
         this.child = new ArrayList();
         this.turret = turret;
      }
   }

   public class PartWheel extends MCH_AircraftInfo.DrawnPart {

      final float rotDir;
      final Vec3 pos2;


      public PartWheel(float px, float py, float pz, float rx, float ry, float rz, float rd, float px2, float py2, float pz2, String name) {
         super(px, py, pz, rx, ry, rz, name);
         this.rotDir = rd;
         this.pos2 = Vec3.createVectorHelper((double)px2, (double)py2, (double)pz2);
      }

      public PartWheel(float px, float py, float pz, float rx, float ry, float rz, float rd, String name) {
         this(px, py, pz, rx, ry, rz, rd, px, py, pz, name);
      }
   }

   public class Camera extends MCH_AircraftInfo.DrawnPart {

      public final boolean yawSync;
      public final boolean pitchSync;


      public Camera(float px, float py, float pz, float rx, float ry, float rz, String name, boolean ys, boolean ps) {
         super(px, py, pz, rx, ry, rz, name);
         this.yawSync = ys;
         this.pitchSync = ps;
      }
   }

   public class Weapon {

      public final Vec3 pos;
      public final float yaw;
      public final float pitch;
      public final boolean canUsePilot;
      public final int seatID;
      public final float defaultYaw;
      public final float minYaw;
      public final float maxYaw;
      public final float minPitch;
      public final float maxPitch;
      public final boolean turret;


      public Weapon(float x, float y, float z, float yaw, float pitch, boolean canPirot, int seatId, float defy, float mny, float mxy, float mnp, float mxp, boolean turret) {
         this.pos = Vec3.createVectorHelper((double)x, (double)y, (double)z);
         this.yaw = yaw;
         this.pitch = pitch;
         this.canUsePilot = canPirot;
         this.seatID = seatId;
         this.defaultYaw = defy;
         this.minYaw = mny;
         this.maxYaw = mxy;
         this.minPitch = mnp;
         this.maxPitch = mxp;
         this.turret = turret;
      }
   }

   public class WeaponSet {

      public final String type;
      public ArrayList weapons;


      public WeaponSet(String t) {
         this.type = t;
         this.weapons = new ArrayList();
      }
   }

   public class RepellingHook {

      final Vec3 pos;
      final int interval;


      public RepellingHook(Vec3 pos, int inv) {
         this.pos = pos;
         this.interval = inv;
      }
   }

   public class Wheel {

      public final float size;
      public final Vec3 pos;


      public Wheel(Vec3 v, float sz) {
         this.pos = v;
         this.size = sz;
      }

      public Wheel(Vec3 v) {
         this(v, 1.0F);
      }
   }

   public class Flare {

      public int[] types = new int[0];
      public Vec3 pos = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);


   }

   public class RideRack {

      public final String name;
      public final int rackID;


      public RideRack(String n, int id) {
         this.name = n;
         this.rackID = id;
      }
   }

   public class CameraPosition {

      public final Vec3 pos;
      public final boolean fixRot;
      public final float yaw;
      public final float pitch;


      public CameraPosition(Vec3 vec3, boolean fixRot, float yaw, float pitch) {
         this.pos = vec3;
         this.fixRot = fixRot;
         this.yaw = yaw;
         this.pitch = pitch;
      }

      public CameraPosition(Vec3 vec3) {
         this(vec3, false, 0.0F, 0.0F);
      }

      public CameraPosition() {
         this(Vec3.createVectorHelper(0.0D, 0.0D, 0.0D));
      }
   }

   public class WeaponBay extends MCH_AircraftInfo.DrawnPart {

      public final float maxRotFactor;
      public final boolean isSlide;
      private final String weaponName;
      public Integer[] weaponIds;


      public WeaponBay(String wn, float px, float py, float pz, float rx, float ry, float rz, float mr, String name, boolean slide) {
         super(px, py, pz, rx, ry, rz, name);
         this.maxRotFactor = mr;
         this.isSlide = slide;
         this.weaponName = wn;
         this.weaponIds = new Integer[0];
      }
   }

   public class ParticleSplash {

      public final int num;
      public final float acceleration;
      public final float size;
      public final Vec3 pos;
      public final int age;
      public final float motionY;
      public final float gravity;


      public ParticleSplash(Vec3 v, int nm, float siz, float acc, int ag, float my, float gr) {
         this.num = nm;
         this.pos = v;
         this.size = siz;
         this.acceleration = acc;
         this.age = ag;
         this.motionY = my;
         this.gravity = gr;
      }
   }

   public class CrawlerTrack extends MCH_AircraftInfo.DrawnPart {

      public float len = 0.35F;
      public double[] cx;
      public double[] cy;
      public List lp;
      public float z;
      public int side;


      public CrawlerTrack(String name) {
         super(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, name);
      }
   }

   public class CrawlerTrackPrm {

      float x;
      float y;
      float nx;
      float ny;
      float r;


      public CrawlerTrackPrm(float x, float y) {
         this.x = x;
         this.y = y;
      }
   }
}
