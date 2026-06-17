package mcheli.flare;

import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.wrapper.W_McClient;
import net.minecraft.world.World;

public class MCH_Maintenance {

    //Cooldown duration 0means cooldown ended
    public int tick;
    //Active duration 0means use ended
    public int useTick;
    //Maintenance system active time
    public int useTime;
    //Maintenance system wait time
    public int waitTime;

    public World worldObj;

    public MCH_EntityBaseVehicle aircraft;

    public MCH_Maintenance(World w, MCH_EntityBaseVehicle ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse() {
        boolean result = false;
        System.out.println("MCH_Maintenance.onUse");
        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = waitTime;
                useTick = useTime;
                result = true;
                W_McClient.MOD_playSoundFX("wrench", 10.0F, 1.0F);
            }
        } else {
            result = true;
            tick = waitTime;
            useTick = useTime;
            aircraft.getEntityData().setBoolean("MaintenanceUsing", true);
        }
        return result;
    }

    public void onUpdate() {
        if (this.aircraft != null && !this.aircraft.isDead) {
            if (this.tick > 0) {
                --this.tick;
            }
            if (this.useTick > 0) {
                --this.useTick;
            }
            if(this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("MaintenanceUsing")) {
                this.aircraft.getEntityData().setBoolean("MaintenanceUsing", false);
            }
        }
    }

    private void onUsing() {
        if(!aircraft.isDead){
            aircraft.repair(aircraft.getMaxHP() / 100);
        }
    }


    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
