package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

/** A named model section. Faces exist only while a parser is building the section. */
@SideOnly(Side.CLIENT)
public class W_GroupObject {

   private static final int FLOATS_PER_VERTEX = 8;
   private static final int U_OFFSET = 0;
   private static final int V_OFFSET = 1;
   private static final int NORMAL_OFFSET = 2;
   private static final int POSITION_OFFSET = 5;

   public String name;
   public ArrayList faces;
   public int glDrawingMode;

   private float[] geometry;
   private int[] faceVertexOffsets;
   private int vertexBufferId;
   private boolean vboUnavailable;
   private boolean vboDirty;

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

   /** Finalizes all faces into two group-sized primitive arrays and releases every face object. */
   public void finalizeGeometry() {
      if(this.faces == null) {
         return;
      }

      int faceCount = this.faces.size();
      int vertexCount = 0;
      for(Object object : this.faces) {
         vertexCount += ((W_Face)object).getVertexCount();
      }

      this.geometry = new float[vertexCount * FLOATS_PER_VERTEX];
      this.faceVertexOffsets = new int[faceCount + 1];
      FloatBuffer output = FloatBuffer.wrap(this.geometry);
      int faceIndex = 0;
      int offset = 0;
      for(Object object : this.faces) {
         W_Face face = (W_Face)object;
         this.faceVertexOffsets[faceIndex++] = offset;
         face.appendInterleaved(output);
         offset += face.getVertexCount();
      }
      this.faceVertexOffsets[faceCount] = offset;
      this.faces.clear();
      this.faces = null;
   }

   public int getFaceCount() {
      return this.faceVertexOffsets != null ? this.faceVertexOffsets.length - 1
            : this.faces != null ? this.faces.size() : 0;
   }

   public int getVertexCount() {
      if(this.geometry != null) {
         return this.geometry.length / FLOATS_PER_VERTEX;
      }
      int vertexCount = 0;
      if(this.faces != null) {
         for(Object object : this.faces) {
            vertexCount += ((W_Face)object).getVertexCount();
         }
      }
      return vertexCount;
   }

   public long getRetainedGeometryBytes() {
      long geometryBytes = this.geometry != null ? (long)this.geometry.length * 4L : 0L;
      long offsetBytes = this.faceVertexOffsets != null ? (long)this.faceVertexOffsets.length * 4L : 0L;
      return geometryBytes + offsetBytes;
   }

   public int getFaceVertexCount(int face) {
      return this.faceVertexOffsets[face + 1] - this.faceVertexOffsets[face];
   }

   public int getFaceFirstVertex(int face) {
      return this.faceVertexOffsets[face];
   }

   public float getVertexX(int vertex) { return this.geometry[vertex * FLOATS_PER_VERTEX + POSITION_OFFSET]; }
   public float getVertexY(int vertex) { return this.geometry[vertex * FLOATS_PER_VERTEX + POSITION_OFFSET + 1]; }
   public float getVertexZ(int vertex) { return this.geometry[vertex * FLOATS_PER_VERTEX + POSITION_OFFSET + 2]; }
   public float getTextureU(int vertex) { return this.geometry[vertex * FLOATS_PER_VERTEX + U_OFFSET]; }
   public float getTextureV(int vertex) { return this.geometry[vertex * FLOATS_PER_VERTEX + V_OFFSET]; }

   public void setTextureCoordinates(int vertex, float u, float v) {
      int base = vertex * FLOATS_PER_VERTEX;
      this.geometry[base + U_OFFSET] = u;
      this.geometry[base + V_OFFSET] = v;
      this.vboDirty = true;
   }

   public void render() {
      if(this.getVertexCount() == 0) {
         return;
      }
      if(this.renderVbo()) {
         return;
      }
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawing(this.glDrawingMode);
      this.render(tessellator);
      tessellator.draw();
   }

   public void renderTransformed() {
      if(this.getVertexCount() > 0) {
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
      int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
      boolean textureArrayEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_COORD_ARRAY);
      boolean normalArrayEnabled = GL11.glIsEnabled(GL11.GL_NORMAL_ARRAY);
      boolean vertexArrayEnabled = GL11.glIsEnabled(GL11.GL_VERTEX_ARRAY);
      boolean rendered = false;
      try {
         if(this.vboDirty && this.vertexBufferId != 0) {
            GL15.glDeleteBuffers(this.vertexBufferId);
            this.vertexBufferId = 0;
            this.vboDirty = false;
         }
         if(this.vertexBufferId == 0) {
            this.createVbo();
         }
         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
         GL11.glInterleavedArrays(GL11.GL_T2F_N3F_V3F, 0, 0L);
         GL11.glDrawArrays(this.glDrawingMode, 0, this.getVertexCount());
         rendered = true;
      } catch(RuntimeException failure) {
         this.vboUnavailable = true;
      } finally {
         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
         restoreClientState(GL11.GL_TEXTURE_COORD_ARRAY, textureArrayEnabled);
         restoreClientState(GL11.GL_NORMAL_ARRAY, normalArrayEnabled);
         restoreClientState(GL11.GL_VERTEX_ARRAY, vertexArrayEnabled);
      }
      return rendered;
   }

   private static void restoreClientState(int state, boolean enabled) {
      if(enabled) {
         GL11.glEnableClientState(state);
      } else {
         GL11.glDisableClientState(state);
      }
   }

   private void createVbo() {
      if(this.geometry == null) {
         this.finalizeGeometry();
      }
      if(this.geometry == null || this.geometry.length == 0) {
         return;
      }
      FloatBuffer upload = BufferUtils.createFloatBuffer(this.geometry.length);
      upload.put(this.geometry).flip();
      this.vertexBufferId = GL15.glGenBuffers();
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
      GL15.glBufferData(GL15.GL_ARRAY_BUFFER, upload, GL15.GL_STATIC_DRAW);
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
   }

   public void render(Tessellator tessellator) {
      this.renderFaces(tessellator, 0, this.getFaceCount() - 1);
   }

   public void renderFaces(Tessellator tessellator, int firstFace, int lastFace) {
      int from = Math.max(0, firstFace);
      int to = Math.min(lastFace, this.getFaceCount() - 1);
      if(this.faceVertexOffsets == null || this.geometry == null) {
         for(int face = from; face <= to; ++face) {
            ((W_Face)this.faces.get(face)).addFaceForRender(tessellator);
         }
         return;
      }
      for(int face = from; face <= to; ++face) {
         int firstVertex = this.faceVertexOffsets[face];
         int endVertex = this.faceVertexOffsets[face + 1];
         for(int vertex = firstVertex; vertex < endVertex; ++vertex) {
            int base = vertex * FLOATS_PER_VERTEX;
            tessellator.setNormal(this.geometry[base + NORMAL_OFFSET], this.geometry[base + NORMAL_OFFSET + 1], this.geometry[base + NORMAL_OFFSET + 2]);
            tessellator.addVertexWithUV(this.geometry[base + POSITION_OFFSET], this.geometry[base + POSITION_OFFSET + 1], this.geometry[base + POSITION_OFFSET + 2], this.geometry[base + U_OFFSET], this.geometry[base + V_OFFSET]);
         }
      }
   }
}
