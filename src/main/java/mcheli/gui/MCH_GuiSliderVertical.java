package mcheli.gui;

import mcheli.MCH_Key;
import mcheli.wrapper.W_GuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class MCH_GuiSliderVertical extends W_GuiButton {

   private float currentSlider;
   private boolean isMousePress;
   public float valueMin = 0.0F;
   public float valueMax = 1.0F;
   public float valueStep = 0.1F;


   public MCH_GuiSliderVertical(int gui_id, int posX, int posY, int sliderWidth, int sliderHeight, String string, float defaultSliderPos, float minVal, float maxVal, float step) {
      super(gui_id, posX, posY, sliderWidth, sliderHeight, string);
      this.valueMin = minVal;
      this.valueMax = maxVal;
      this.valueStep = step;
      this.setSliderValue(defaultSliderPos);
   }

   public int getHoverState(boolean p_146114_1_) {
      return 0;
   }

   public void scrollUp(float a) {
      if(this.isVisible() && !this.isMousePress) {
         this.setSliderValue(this.getSliderValue() + this.valueStep * a);
      }

   }

   public void scrollDown(float a) {
      if(this.isVisible() && !this.isMousePress) {
         this.setSliderValue(this.getSliderValue() - this.valueStep * a);
      }

   }

   protected void mouseDragged(Minecraft mc, int x, int y) {
      if(this.isVisible()) {
         if(this.isMousePress) {
            this.currentSlider = (float)(y - (super.yPosition + 4)) / (float)(super.height - 8);
            if(this.currentSlider < 0.0F) {
               this.currentSlider = 0.0F;
            }

            if(this.currentSlider > 1.0F) {
               this.currentSlider = 1.0F;
            }

            this.currentSlider = this.normalizeValue(this.denormalizeValue(this.currentSlider));
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         this.drawTexturedModalRect(super.xPosition, super.yPosition + (int)(this.currentSlider * (float)(super.height - 8)), 66, 0, 20, 4);
         this.drawTexturedModalRect(super.xPosition, super.yPosition + (int)(this.currentSlider * (float)(super.height - 8)) + 4, 66, 196, 20, 4);
         if(!MCH_Key.isKeyDown(-100)) {
            this.mouseReleased(x, y);
         }
      }

   }

   public void setSliderValue(float f) {
      this.currentSlider = this.normalizeValue(f);
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
         this.currentSlider = (float)(y - (super.yPosition + 4)) / (float)(super.height - 8);
         if(this.currentSlider < 0.0F) {
            this.currentSlider = 0.0F;
         }

         if(this.currentSlider > 1.0F) {
            this.currentSlider = 1.0F;
         }

         this.isMousePress = true;
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased(int p_146118_1_, int p_146118_2_) {
      this.isMousePress = false;
   }

   public void drawButton(Minecraft mc, int x, int y) {
      if(this.isVisible()) {
         FontRenderer fontrenderer = mc.fontRenderer;
         mc.getTextureManager().bindTexture(new ResourceLocation("mcheli", "textures/gui/widgets.png"));
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         this.setOnMouseOver(x >= super.xPosition && y >= super.yPosition && x < super.xPosition + super.width && y < super.yPosition + super.height);
         int k = this.getHoverState(this.isOnMouseOver());
         this.enableBlend();
         this.drawTexturedModalRect(super.xPosition, super.yPosition, 46 + k * 20, 0, super.width, super.height / 2);
         this.drawTexturedModalRect(super.xPosition, super.yPosition + super.height / 2, 46 + k * 20, 200 - super.height / 2, super.width, super.height / 2);
         this.mouseDragged(mc, x, y);
         int l = 14737632;
         if(super.packedFGColour != 0) {
            l = super.packedFGColour;
         } else if(!super.enabled) {
            l = 10526880;
         } else if(this.isOnMouseOver()) {
            l = 16777120;
         }

         this.drawCenteredString(fontrenderer, super.displayString, super.xPosition + super.width / 2, super.yPosition + (super.height - 8) / 2, l);
         mc.getTextureManager().bindTexture(GuiButton.buttonTextures);
      }

   }
}
