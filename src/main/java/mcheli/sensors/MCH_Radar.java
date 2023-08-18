package mcheli.sensors;

import mcheli.MCH_Vector2;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MCH_Radar {

   private World worldObj;
   private ArrayList entityList = new ArrayList();
   private ArrayList enemyList = new ArrayList();


   public ArrayList getEntityList() {
      return this.entityList;
   }

   public ArrayList getEnemyList() {
      return this.enemyList;
   }

   public MCH_Radar(World world) {
      this.worldObj = world;
   }

   public void clear() {
      this.entityList.clear();
      this.enemyList.clear();
   }

   public void updateXZ(Entity centerEntity, int range) {
      if(this.worldObj.isRemote) {
         this.clear();
         List list = centerEntity.worldObj.getEntitiesWithinAABBExcludingEntity(centerEntity, centerEntity.boundingBox.expand((double)range, (double)range, (double)range));

         for(int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity)list.get(i);
            if(entity instanceof EntityLiving) {
               double x = entity.posX - centerEntity.posX;
               double z = entity.posZ - centerEntity.posZ;
               if(x * x + z * z < (double)(range * range)) {
                  int y = 1 + (int)entity.posY;
                  if(y < 0) {
                     y = 1;
                  }

                  int blockCnt;
                  for(blockCnt = 0; y < 200; ++y) {
                     if(W_WorldFunc.getBlockId(this.worldObj, (int)entity.posX, y, (int)entity.posZ) != 0) {
                        ++blockCnt;
                        if(blockCnt >= 5) {
                           break;
                        }
                     }
                  }

                  if(blockCnt < 5) {
                     if(entity instanceof EntityMob) {
                        this.enemyList.add(new MCH_Vector2(x, z));
                     } else {
                        this.entityList.add(new MCH_Vector2(x, z));
                     }
                  }
               }
            }
         }

      }
   }
}
