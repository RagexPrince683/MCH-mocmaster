package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.Tessellator;

import java.util.ArrayList;
import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class W_GroupObject {

   public String name;
   public ArrayList faces;
   public int glDrawingMode;


   public W_GroupObject() {
      this("");
   }

   public W_GroupObject(String name) {
      this(name, -1);
   }

   public W_GroupObject(String name, int glDrawingMode) {
      this.faces = new ArrayList();
      this.name = name;
      this.glDrawingMode = glDrawingMode;
   }

   public void render() {
      if(this.faces.size() > 0) {
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawing(this.glDrawingMode);
         this.render(tessellator);
         tessellator.draw();
      }

   }

   public void render(Tessellator tessellator) {
      if(this.faces.size() > 0) {
         Iterator i$ = this.faces.iterator();

         while(i$.hasNext()) {
            W_Face face = (W_Face)i$.next();
            face.addFaceForRender(tessellator);
         }
      }

   }
}
