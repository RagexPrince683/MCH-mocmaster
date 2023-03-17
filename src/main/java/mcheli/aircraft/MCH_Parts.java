package mcheli.aircraft;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;

public class MCH_Parts {

   public final Entity parent;
   public final DataWatcher dataWatcher;
   public final int shift;
   public final int dataIndex;
   public final String partName;
   public float prevRotation = 0.0F;
   public float rotation = 0.0F;
   public float rotationMax = 90.0F;
   public float rotationInv = 1.0F;
   public MCH_Parts.Sound soundStartSwichOn = new MCH_Parts.Sound();
   public MCH_Parts.Sound soundEndSwichOn = new MCH_Parts.Sound();
   public MCH_Parts.Sound soundSwitching = new MCH_Parts.Sound();
   public MCH_Parts.Sound soundStartSwichOff = new MCH_Parts.Sound();
   public MCH_Parts.Sound soundEndSwichOff = new MCH_Parts.Sound();
   private boolean status = false;


   public MCH_Parts(Entity parent, int shiftBit, int dataWatcherIndex, String name) {
      this.parent = parent;
      this.dataWatcher = parent.getDataWatcher();
      this.shift = shiftBit;
      this.dataIndex = dataWatcherIndex;
      this.status = (this.getDataWatcherValue() & 1 << this.shift) != 0;
      this.partName = name;
   }

   public int getDataWatcherValue() {
      return this.dataWatcher.getWatchableObjectInt(this.dataIndex);
   }

   public void setStatusServer(boolean stat) {
      this.setStatusServer(stat, true);
   }

   public void setStatusServer(boolean stat, boolean playSound) {
      if(!this.parent.worldObj.isRemote && this.getStatus() != stat) {
         MCH_Lib.DbgLog(false, "setStatusServer(ID=%d %s :%s -> %s)", new Object[]{Integer.valueOf(this.shift), this.partName, this.getStatus()?"ON":"OFF", stat?"ON":"OFF"});
         this.updateDataWatcher(stat);
         this.playSound(this.soundSwitching);
         if(!stat) {
            this.playSound(this.soundStartSwichOff);
         } else {
            this.playSound(this.soundStartSwichOn);
         }

         this.update();
      }

   }

   protected void updateDataWatcher(boolean stat) {
      int currentStatus = this.dataWatcher.getWatchableObjectInt(this.dataIndex);
      int mask = 1 << this.shift;
      if(!stat) {
         this.dataWatcher.updateObject(this.dataIndex, Integer.valueOf(currentStatus & ~mask));
      } else {
         this.dataWatcher.updateObject(this.dataIndex, Integer.valueOf(currentStatus | mask));
      }

      this.status = stat;
   }

   public boolean getStatus() {
      return this.status;
   }

   public boolean isOFF() {
      return !this.status && this.rotation <= 0.02F;
   }

   public boolean isON() {
      return this.status && this.rotation >= this.rotationMax - 0.02F;
   }

   public void updateStatusClient(int statFromDataWatcher) {
      if(this.parent.worldObj.isRemote) {
         this.status = (statFromDataWatcher & 1 << this.shift) != 0;
      }

   }

   public void update() {
      this.prevRotation = this.rotation;
      if(this.getStatus()) {
         if(this.rotation < this.rotationMax) {
            this.rotation += this.rotationInv;
            if(this.rotation >= this.rotationMax) {
               this.playSound(this.soundEndSwichOn);
            }
         }
      } else if(this.rotation > 0.0F) {
         this.rotation -= this.rotationInv;
         if(this.rotation <= 0.0F) {
            this.playSound(this.soundEndSwichOff);
         }
      }

   }

   public void forceSwitch(boolean onoff) {
      this.updateDataWatcher(onoff);
      this.rotation = this.prevRotation = this.rotationMax;
   }

   public float getFactor() {
      return this.rotationMax > 0.0F?this.rotation / this.rotationMax:0.0F;
   }

   public void playSound(MCH_Parts.Sound snd) {
      if(!snd.name.isEmpty() && !this.parent.worldObj.isRemote) {
         W_WorldFunc.MOD_playSoundAtEntity(this.parent, snd.name, snd.volume, snd.pitch);
      }

   }

   public class Sound {

      public String name = "";
      public float volume = 1.0F;
      public float pitch = 1.0F;


      public void setPrm(String n, float v, float p) {
         this.name = n;
         this.volume = v;
         this.pitch = p;
      }
   }
}
