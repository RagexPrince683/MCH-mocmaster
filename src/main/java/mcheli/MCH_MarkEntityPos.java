package mcheli;

import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class MCH_MarkEntityPos {

   public FloatBuffer pos;
   public int type;
   public Entity entity;


   public MCH_MarkEntityPos(int type, Entity entity) {
      this.type = type;
      this.pos = BufferUtils.createFloatBuffer(3);
      this.entity = entity;
   }

   public MCH_MarkEntityPos(int type) {
      this(type, (Entity)null);
   }
}
