package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.nio.FloatBuffer;
import net.minecraft.client.renderer.Tessellator;

@SideOnly(Side.CLIENT)
public class W_Face {

   public int[] verticesID;
   public W_Vertex[] vertices;
   public W_Vertex[] vertexNormals;
   public W_Vertex faceNormal;
   public W_TextureCoordinate[] textureCoordinates;

   private float[] packedData;
   private int packedStride;
   private int packedTextureOffset = -1;
   private int packedNormalOffset = -1;
   private int packedVertexCount;
   private float faceNormalX;
   private float faceNormalY;
   private float faceNormalZ;
   private boolean hasPackedFaceNormal;
   private float[] repairedTextureCoordinates;

   public W_Face copy() {
      W_Face f = new W_Face();
      return f;
   }

   /**
    * Converts this face from object-heavy loader data into a single primitive array used by the renderer.
    * Once this is called the original wrapper vertex/UV/normal arrays are released for GC.
    */
   public void compact() {
      if(this.packedData == null && this.vertices != null) {
         this.packedVertexCount = this.vertices.length;
         boolean hasTextures = this.textureCoordinates != null && this.textureCoordinates.length > 0;
         boolean hasNormals = this.vertexNormals != null && this.vertexNormals.length > 0;
         this.packedStride = 3 + (hasTextures ? 2 : 0) + (hasNormals ? 3 : 0);
         this.packedTextureOffset = hasTextures ? 3 : -1;
         this.packedNormalOffset = hasNormals ? 3 + (hasTextures ? 2 : 0) : -1;
         this.packedData = new float[this.packedVertexCount * this.packedStride];

         for(int i = 0; i < this.packedVertexCount; ++i) {
            W_Vertex v = this.vertices[i];
            int offset = i * this.packedStride;
            this.packedData[offset] = v.x;
            this.packedData[offset + 1] = v.y;
            this.packedData[offset + 2] = v.z;

            if(hasTextures) {
               W_TextureCoordinate t = this.textureCoordinates[i];
               this.packedData[offset + this.packedTextureOffset] = t.u;
               this.packedData[offset + this.packedTextureOffset + 1] = t.v;
            }

            if(hasNormals) {
               W_Vertex n = this.vertexNormals[i];
               this.packedData[offset + this.packedNormalOffset] = n.x;
               this.packedData[offset + this.packedNormalOffset + 1] = n.y;
               this.packedData[offset + this.packedNormalOffset + 2] = n.z;
            }
         }
      }

      W_Vertex normal = this.faceNormal != null ? this.faceNormal : this.calculateFaceNormal();
      this.faceNormalX = normal.x;
      this.faceNormalY = normal.y;
      this.faceNormalZ = normal.z;
      this.hasPackedFaceNormal = true;

      this.verticesID = null;
      this.vertices = null;
      this.vertexNormals = null;
      this.faceNormal = null;
      this.textureCoordinates = null;
   }

   public int getVertexCount() {
      if(this.packedData != null) {
         return this.packedVertexCount;
      }

      return this.vertices != null ? this.vertices.length : 0;
   }

   public float getVertexX(int index) {
      return this.packedData != null ? this.packedData[index * this.packedStride] : this.vertices[index].x;
   }

   public float getVertexY(int index) {
      return this.packedData != null ? this.packedData[index * this.packedStride + 1] : this.vertices[index].y;
   }

   public float getVertexZ(int index) {
      return this.packedData != null ? this.packedData[index * this.packedStride + 2] : this.vertices[index].z;
   }

   /**
    * Writes this face in the fixed-function T2F_N3F_V3F layout used by model VBOs.
    */
   public void appendInterleaved(FloatBuffer buffer) {
      if(!this.hasPackedFaceNormal) {
         W_Vertex normal = this.faceNormal != null ? this.faceNormal : this.calculateFaceNormal();
         this.faceNormalX = normal.x;
         this.faceNormalY = normal.y;
         this.faceNormalZ = normal.z;
         this.hasPackedFaceNormal = true;
      }

      int vertexCount = this.getVertexCount();
      int textureCount = this.getTextureCoordinateCount();
      for(int i = 0; i < vertexCount; ++i) {
         buffer.put(textureCount > i ? this.getTextureU(i) : 0.0F);
         buffer.put(textureCount > i ? this.getTextureV(i) : 0.0F);
         buffer.put(this.hasVertexNormal(i) ? this.getNormalX(i) : this.faceNormalX);
         buffer.put(this.hasVertexNormal(i) ? this.getNormalY(i) : this.faceNormalY);
         buffer.put(this.hasVertexNormal(i) ? this.getNormalZ(i) : this.faceNormalZ);
         buffer.put(this.getVertexX(i));
         buffer.put(this.getVertexY(i));
         buffer.put(this.getVertexZ(i));
      }
   }

   public void addFaceForRender(Tessellator tessellator) {
      this.addFaceForRender(tessellator, 0.0F);
   }

   public void addFaceForRender(Tessellator tessellator, float textureOffset) {
      if(!this.hasPackedFaceNormal) {
         W_Vertex normal = this.faceNormal != null ? this.faceNormal : this.calculateFaceNormal();
         this.faceNormalX = normal.x;
         this.faceNormalY = normal.y;
         this.faceNormalZ = normal.z;
         this.hasPackedFaceNormal = true;
      }

      tessellator.setNormal(this.faceNormalX, this.faceNormalY, this.faceNormalZ);
      int vertexCount = this.getVertexCount();
      int textureCount = this.getTextureCoordinateCount();
      boolean hasTextureCoordinates = textureCount > 0;
      float averageU = 0.0F;
      float averageV = 0.0F;
      if(hasTextureCoordinates) {
         for(int offsetU = 0; offsetU < textureCount; ++offsetU) {
            averageU += this.getTextureU(offsetU);
            averageV += this.getTextureV(offsetU);
         }

         averageU /= (float)textureCount;
         averageV /= (float)textureCount;
      }

      for(int i = 0; i < vertexCount; ++i) {
         if(hasTextureCoordinates) {
            float offsetU = textureOffset;
            float offsetV = textureOffset;
            float textureU = this.getTextureU(i);
            float textureV = this.getTextureV(i);
            if(textureU > averageU) {
               offsetU = -textureOffset;
            }

            if(textureV > averageV) {
               offsetV = -textureOffset;
            }

            if(this.hasVertexNormal(i)) {
               tessellator.setNormal(this.getNormalX(i), this.getNormalY(i), this.getNormalZ(i));
            }

            tessellator.addVertexWithUV((double)this.getVertexX(i), (double)this.getVertexY(i), (double)this.getVertexZ(i), (double)(textureU + offsetU), (double)(textureV + offsetV));
         } else {
            tessellator.addVertex((double)this.getVertexX(i), (double)this.getVertexY(i), (double)this.getVertexZ(i));
         }
      }

   }

   public W_Vertex calculateFaceNormal() {
      float v1x = this.getVertexX(1) - this.getVertexX(0);
      float v1y = this.getVertexY(1) - this.getVertexY(0);
      float v1z = this.getVertexZ(1) - this.getVertexZ(0);
      float v2x = this.getVertexX(2) - this.getVertexX(0);
      float v2y = this.getVertexY(2) - this.getVertexY(0);
      float v2z = this.getVertexZ(2) - this.getVertexZ(0);
      float normalX = v1y * v2z - v1z * v2y;
      float normalY = v1z * v2x - v1x * v2z;
      float normalZ = v1x * v2y - v1y * v2x;
      float length = (float)Math.sqrt((double)(normalX * normalX + normalY * normalY + normalZ * normalZ));
      if(length > 0.0F) {
         normalX /= length;
         normalY /= length;
         normalZ /= length;
      }

      return new W_Vertex(normalX, normalY, normalZ);
   }

   public int getTextureCoordinateCount() {
      if(this.packedData != null) {
         return this.packedTextureOffset >= 0 ? this.packedVertexCount : 0;
      }

      return this.textureCoordinates != null ? this.textureCoordinates.length : 0;
   }

   public float getTextureU(int index) {
      return this.repairedTextureCoordinates != null ? this.repairedTextureCoordinates[index * 2]
         : this.packedData != null ? this.packedData[index * this.packedStride + this.packedTextureOffset] : this.textureCoordinates[index].u;
   }

   public float getTextureV(int index) {
      return this.repairedTextureCoordinates != null ? this.repairedTextureCoordinates[index * 2 + 1]
         : this.packedData != null ? this.packedData[index * this.packedStride + this.packedTextureOffset + 1] : this.textureCoordinates[index].v;
   }

   public void setRepairedTextureCoordinates(float[] coordinates) {
      this.repairedTextureCoordinates = coordinates;
   }

   private boolean hasVertexNormal(int index) {
      if(this.packedData != null) {
         return this.packedNormalOffset >= 0 && index < this.packedVertexCount;
      }

      return this.vertexNormals != null && index < this.vertexNormals.length;
   }

   private float getNormalX(int index) {
      return this.packedData != null ? this.packedData[index * this.packedStride + this.packedNormalOffset] : this.vertexNormals[index].x;
   }

   private float getNormalY(int index) {
      return this.packedData != null ? this.packedData[index * this.packedStride + this.packedNormalOffset + 1] : this.vertexNormals[index].y;
   }

   private float getNormalZ(int index) {
      return this.packedData != null ? this.packedData[index * this.packedStride + this.packedNormalOffset + 2] : this.vertexNormals[index].z;
   }
}
