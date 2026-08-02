package mcheli.vehicle;

import java.util.ArrayList;
import java.util.List;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.vehicle.MCH_ItemTurret;
import net.minecraft.item.Item;

//TURRET info
public class MCH_TurretInfo extends MCH_BaseVehicleInfo {

   public MCH_ItemTurret item = null;
   public boolean isEnableMove = false;
   public boolean isEnableRot = false;
   public int trackMaxHP = 100;
   public List partList = new ArrayList();


   public float getMinRotationPitch() {
      return -90.0F;
   }

   public float getMaxRotationPitch() {
      return 90.0F;
   }

   public Item getItem() {
      return this.item;
   }

   public MCH_TurretInfo(String name) {
      super(name);
      this.defaultFreelook = true;
   }

   public boolean isValidData() throws Exception {
      return super.isValidData();
   }

   public String getDefaultHudName(int seatId) {
      return "turret";
   }

   public void loadItemData(String item, String data) {
      super.loadItemData(item, data);
      if(item.compareTo("canmove") == 0) {
         this.isEnableMove = this.toBool(data);
      } else if(item.compareTo("canrotation") == 0) {
         this.isEnableRot = this.toBool(data);
      } else if(item.compareTo("rotationpitchmin") == 0) {
         super.loadItemData("minrotationpitch", data);
      } else if(item.compareTo("rotationpitchmax") == 0) {
         super.loadItemData("maxrotationpitch", data);
      } else {
         String[] s;
         float rb;
         MCH_TurretInfo.VPart p;
         if(item.compareTo("addpart") == 0) {
            s = data.split("\\s*,\\s*");
            if(s.length >= 7) {
               rb = s.length >= 8?this.toFloat(s[7]):0.0F;
               p = new MCH_TurretInfo.VPart(this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), "part" + this.partList.size(), this.toBool(s[0]), this.toBool(s[1]), this.toBool(s[2]), this.toInt(s[3]), rb);
               this.partList.add(p);
            }
         } else if(item.compareTo("addchildpart") == 0 && this.partList.size() > 0) {
            s = data.split("\\s*,\\s*");
            if(s.length >= 7) {
               rb = s.length >= 8?this.toFloat(s[7]):0.0F;
               p = (MCH_TurretInfo.VPart)this.partList.get(this.partList.size() - 1);
               if(p.child == null) {
                  p.child = new ArrayList();
               }

               MCH_TurretInfo.VPart n = new MCH_TurretInfo.VPart(this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), p.modelName + "_" + p.child.size(), this.toBool(s[0]), this.toBool(s[1]), this.toBool(s[2]), this.toInt(s[3]), rb);
               p.child.add(n);
            }
         }
      }
   }

   public String getDirectoryName() {
      return this.isTurretAssetDirectory() ? "turrets" : "vehicles";
   }

   public boolean isTurretCategory(String category) {
      if(category == null) {
         return false;
      }

      String normalized = category.trim().toLowerCase();
      return normalized.equals("turret") || normalized.equals("turrets") || normalized.endsWith(".turret") || normalized.endsWith(".turrets");
   }

   /**
    * Turrets historically lived under assets/mcheli/vehicles.  New packs may
    * place the same turret info files under assets/mcheli/turrets after the
    * vehicle-to-turret reclassification.  Keep both locations valid without
    * changing legacy registry/type names.
    */
   public boolean isTurretAssetDirectory() {
      if(super.filePath == null) {
         return isTurretCategory(super.category);
      }

      String path = super.filePath.replace('\\', '/').toLowerCase();
      return path.indexOf("/turrets/") >= 0;
   }

   public String getKindName() {
      return "turret";
   }

   public void preReload() {
      super.preReload();
      this.defaultFreelook = true;
      this.partList.clear();
   }

   public void postReload() {
      MCH_MOD.proxy.registerModelsVehicle(super.name, true);
   }

   public class VPart extends MCH_BaseVehicleInfo.DrawnPart {

      public final boolean rotPitch;
      public final boolean rotYaw;
      public final int type;
      public List child;
      public final boolean drawFP;
      public final float recoilBuf;


      public VPart(float x, float y, float z, String model, boolean drawfp, boolean roty, boolean rotp, int type, float rb) {
         super(x, y, z, 0.0F, 0.0F, 0.0F, model);
         this.rotYaw = roty;
         this.rotPitch = rotp;
         this.type = type;
         this.child = null;
         this.drawFP = drawfp;
         this.recoilBuf = rb;
      }
   }
}
