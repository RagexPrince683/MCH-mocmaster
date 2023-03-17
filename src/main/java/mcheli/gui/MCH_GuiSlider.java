package mcheli.gui;

import mcheli.MCH_Key;
import mcheli.wrapper.W_GuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class MCH_GuiSlider extends W_GuiButton {

   private float currentSlider;
   private boolean isMousePress;
   public String stringFormat;
   public float valueMin = 0.0F;
   public float valueMax = 1.0F;
   public float valueStep = 0.1F;


   public MCH_GuiSlider(int gui_id, int posX, int posY, int sliderWidth, int sliderHeight, String string_format, float defaultSliderPos, float minVal, float maxVal, float step) {
      super(gui_id, posX, posY, sliderWidth, sliderHeight, "");
      this.stringFormat = string_format;
      this.valueMin = minVal;
      this.valueMax = maxVal;
      this.valueStep = step;
      this.setSliderValue(defaultSliderPos);
   }

   public int getHoverState(boolean p_146114_1_) {
      return 0;
   }

   protected void mouseDragged(Minecraft mc, int x, int y) {
      if(this.isVisible()) {
         if(this.isMousePress) {
            this.currentSlider = (float)(x - (super.xPosition + 4)) / (float)(super.width - 8);
            if(this.currentSlider < 0.0F) {
               this.currentSlider = 0.0F;
            }

            if(this.currentSlider > 1.0F) {
               this.currentSlider = 1.0F;
            }

            this.currentSlider = this.normalizeValue(this.denormalizeValue(this.currentSlider));
            this.updateDisplayString();
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         this.drawTexturedModalRect(super.xPosition + (int)(this.currentSlider * (float)(super.width - 8)), super.yPosition, 0, 66, 4, 20);
         this.drawTexturedModalRect(super.xPosition + (int)(this.currentSlider * (float)(super.width - 8)) + 4, super.yPosition, 196, 66, 4, 20);
         if(!MCH_Key.isKeyDown(-100)) {
            this.mouseReleased(x, y);
         }
      }

   }

   public void updateDisplayString() {
      super.displayString = String.format(this.stringFormat, new Object[]{Float.valueOf(this.denormalizeValue(this.currentSlider))});
   }

   public void setSliderValue(float f) {
      this.currentSlider = this.normalizeValue(f);
      this.updateDisplayString();
   }

   public float getSliderValue() {
      return this.denormalizeValue(this.currentSlider);
   }

   public float getSliderValueInt(int digit) {
      int d;
      for(d = 1; digit > 0; --digit) {
         d *= 10;
      }

      int n = (int)(this.denormalizeValue(this.currentSlider) * (float)d);
      return (float)n / (float)d;
   }

   public float normalizeValue(float f) {
      return MathHelper.clamp_float((this.snapToStepClamp(f) - this.valueMin) / (this.valueMax - this.valueMin), 0.0F, 1.0F);
   }

   public float denormalizeValue(float f) {
      return this.snapToStepClamp(this.valueMin + (this.valueMax - this.valueMin) * MathHelper.clamp_float(f, 0.0F, 1.0F));
   }

   public float snapToStepClamp(float f) {
      f = this.snapToStep(f);
      return MathHelper.clamp_float(f, this.valueMin, this.valueMax);
   }

   protected float snapToStep(float f) {
      if(this.valueStep > 0.0F) {
         f = this.valueStep * (float)Math.round(f / this.valueStep);
      }

      return f;
   }

   public boolean mousePressed(Minecraft mc, int x, int y) {
      if(super.mousePressed(mc, x, y)) {
         this.currentSlider = (float)(x - (super.xPosition + 4)) / (float)(super.width - 8);
         if(this.currentSlider < 0.0F) {
            this.currentSlider = 0.0F;
         }

         if(this.currentSlider > 1.0F) {
            this.currentSlider = 1.0F;
         }

         this.updateDisplayString();
         this.isMousePress = true;
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased(int p_146118_1_, int p_146118_2_) {
      this.isMousePress = false;
   }
}
