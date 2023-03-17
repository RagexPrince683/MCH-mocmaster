package mcheli.weapon;

import mcheli.MCH_Lib;

public class MCH_Fuze {

    boolean detonateOnImpact = false;
    int proxAboveGround = 0;
    float proxLiving = 0.0f;
    float proxVehicle = 0.0f;
    int delayFromShot = 0;
    int delayFromImpact = 0;
    MCH_EntityBaseBullet weapon;

    public MCH_Fuze(MCH_EntityBaseBullet weapon){
        this.weapon = weapon;
        //weapon.getInfo is null, so fun
        //proxAboveGround = weapon.getInfo().explosionAltitude;
        delayFromShot = weapon.delayFuse;
        //delayFromImpact = weapon.getInfo().impactFuze;
    }

    public boolean shouldDetonate(){
        if(weapon.isCollided && detonateOnImpact){
            return true;
        }else if(proxAboveGround > 0 && MCH_Lib.getBlockIdY(weapon, 3, -proxAboveGround) != 0){
            return true;
        }else if(delayFromShot > weapon.getCountOnUpdate()){
            return true;
        }


        return false;
    }
}
