package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

@SideOnly(Side.CLIENT)
public class W_GroupObject {

   private static final int VBO_FLOATS_PER_VERTEX = 8;
   public String name;
   public ArrayList faces;
   public int glDrawingMode;
   private int vertexBufferId;
   private int vertexCount;
   private boolean vboUnavailable;

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
         if(this.renderVbo()) {
            return;
         }

         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawing(this.glDrawingMode);
         this.render(tessellator);
         tessellator.draw();
      }
   }

   public void renderTransformed() {
      if(this.faces.size() > 0) {
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawing(this.glDrawingMode);
         this.render(tessellator);
         tessellator.draw();
      }
   }

   private boolean renderVbo() {
      if(this.vboUnavailable || !GLContext.getCapabilities().OpenGL15) {
         return false;
      }

      try {
         if(this.vertexBufferId == 0) {
            this.createVbo();
         }

         if(this.vertexBufferId == 0 || this.vertexCount == 0) {
            return false;
         }

         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
         GL11.glInterleavedArrays(GL11.GL_T2F_N3F_V3F, 0, 0L);
         GL11.glDrawArrays(this.glDrawingMode, 0, this.vertexCount);
         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
         GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
         GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
         GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
         return true;
      } catch (RuntimeException ignored) {
         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
         GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
         GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
         GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
         this.vboUnavailable = true;
         return false;
      }
   }

   private void createVbo() {
      this.vertexCount = 0;
      Iterator i$ = this.faces.iterator();
      while(i$.hasNext()) {
         this.vertexCount += ((W_Face)i$.next()).getVertexCount();
      }

      if(this.vertexCount <= 0) {
         return;
      }

      FloatBuffer data = BufferUtils.createFloatBuffer(this.vertexCount * VBO_FLOATS_PER_VERTEX);
      i$ = this.faces.iterator();
      while(i$.hasNext()) {
         ((W_Face)i$.next()).appendInterleaved(data);
      }

      data.flip();
      this.vertexBufferId = GL15.glGenBuffers();
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
      GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
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
