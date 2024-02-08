package mcheli;

import org.lwjgl.input.*;
import net.minecraft.client.settings.*;
import mcheli.wrapper.*;

public class MCH_Key
{
   public int key;
   private boolean isPress;
   private boolean isBeforePress;

   public MCH_Key(final int k) {
      this.key = k;
      //womp womp, not the problem
      this.isPress = false;
      this.isBeforePress = false;
   }

   public boolean isKeyDown() {
      return !this.isBeforePress && this.isPress;
   }

   public boolean isKeyPress() {
      return this.isPress;
   }

   public boolean isKeyUp() {
      return this.isBeforePress && !this.isPress;
   }

   public void update() {
      if (this.key == 0) {
         return;
      }
      this.isBeforePress = this.isPress;
      if (this.key >= 0) {
         this.isPress = Keyboard.isKeyDown(this.key);
      }
      else {
         this.isPress = Mouse.isButtonDown(this.key + 100);
      }
   }

   public static boolean isKeyDown(final int key) {
      if (key > 0) {
         return Keyboard.isKeyDown(key);
      }
      return key < 0 && Mouse.isButtonDown(key + 100);
   }

   public static boolean isKeyDown(final KeyBinding keyBind) {
      return isKeyDown(W_KeyBinding.getKeyCode(keyBind));
   }
}