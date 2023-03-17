package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Vec3;

@SideOnly(Side.CLIENT)
public class W_Face {

   public int[] verticesID;
   public W_Vertex[] vertices;
   public W_Vertex[] vertexNormals;
   public W_Vertex faceNormal;
   public W_TextureCoordinate[] textureCoordinates;


   public W_Face copy() {
      W_Face f = new W_Face();
      return f;
   }

   public void addFaceForRender(Tessellator tessellator) {
      this.addFaceForRender(tessellator, 0.0F);
   }

   public void addFaceForRender(Tessellator tessellator, float textureOffset) {
      if(this.faceNormal == null) {
         this.faceNormal = this.calculateFaceNormal();
      }

      tessellator.setNormal(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z);
      float averageU = 0.0F;
      float averageV = 0.0F;
      if(this.textureCoordinates != null && this.textureCoordinates.length > 0) {
         for(int offsetU = 0; offsetU < this.textureCoordinates.length; ++offsetU) {
            averageU += this.textureCoordinates[offsetU].u;
            averageV += this.textureCoordinates[offsetU].v;
         }

         averageU /= (float)this.textureCoordinates.length;
         averageV /= (float)this.textureCoordinates.length;
      }

      for(int i = 0; i < this.vertices.length; ++i) {
         if(this.textureCoordinates != null && this.textureCoordinates.length > 0) {
            float var8 = textureOffset;
            float offsetV = textureOffset;
            if(this.textureCoordinates[i].u > averageU) {
               var8 = -textureOffset;
            }

            if(this.textureCoordinates[i].v > averageV) {
               offsetV = -textureOffset;
            }

            if(this.vertexNormals != null && i < this.vertexNormals.length) {
               tessellator.setNormal(this.vertexNormals[i].x, this.vertexNormals[i].y, this.vertexNormals[i].z);
            }

            tessellator.addVertexWithUV((double)this.vertices[i].x, (double)this.vertices[i].y, (double)this.vertices[i].z, (double)(this.textureCoordinates[i].u + var8), (double)(this.textureCoordinates[i].v + offsetV));
         } else {
            tessellator.addVertex((double)this.vertices[i].x, (double)this.vertices[i].y, (double)this.vertices[i].z);
         }
      }

   }

   public W_Vertex calculateFaceNormal() {
      Vec3 v1 = Vec3.createVectorHelper((double)(this.vertices[1].x - this.vertices[0].x), (double)(this.vertices[1].y - this.vertices[0].y), (double)(this.vertices[1].z - this.vertices[0].z));
      Vec3 v2 = Vec3.createVectorHelper((double)(this.vertices[2].x - this.vertices[0].x), (double)(this.vertices[2].y - this.vertices[0].y), (double)(this.vertices[2].z - this.vertices[0].z));
      Vec3 normalVector = null;
      normalVector = v1.crossProduct(v2).normalize();
      return new W_Vertex((float)normalVector.xCoord, (float)normalVector.yCoord, (float)normalVector.zCoord);
   }
}
