package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFormatException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class W_MetasequoiaObject extends W_ModelCustom {

   public ArrayList vertices = new ArrayList();
   public ArrayList groupObjects = new ArrayList();
   private W_GroupObject currentGroupObject = null;
   private String fileName;
   private int vertexNum = 0;
   private int faceNum = 0;


   public W_MetasequoiaObject(ResourceLocation resource) throws ModelFormatException {
      this.fileName = resource.toString();

      try {
         IResource e = Minecraft.getMinecraft().getResourceManager().getResource(resource);
         this.loadObjModel(e.getInputStream());
      } catch (IOException var3) {
         throw new ModelFormatException("IO Exception reading model format:" + this.fileName, var3);
      }
   }

   public W_MetasequoiaObject(String fileName, URL resource) throws ModelFormatException {
      this.fileName = fileName;

      try {
         this.loadObjModel(resource.openStream());
      } catch (IOException var4) {
         throw new ModelFormatException("IO Exception reading model format:" + this.fileName, var4);
      }
   }

   public W_MetasequoiaObject(String filename, InputStream inputStream) throws ModelFormatException {
      this.fileName = filename;
      this.loadObjModel(inputStream);
   }

   public boolean containsPart(String partName) {
      Iterator i$ = this.groupObjects.iterator();

      W_GroupObject groupObject;
      do {
         if(!i$.hasNext()) {
            return false;
         }

         groupObject = (W_GroupObject)i$.next();
      } while(!partName.equalsIgnoreCase(groupObject.name));

      return true;
   }
   
   public W_GroupObject getPart(String partName) {
	      Iterator i$ = this.groupObjects.iterator();

	      W_GroupObject groupObject;
	      do {
	         if(!i$.hasNext()) {
	            return null;
	         }

	         groupObject = (W_GroupObject)i$.next();
	      } while(!partName.equalsIgnoreCase(groupObject.name));

	      return groupObject;
	   }

   private void loadObjModel(InputStream inputStream) throws ModelFormatException {
      BufferedReader reader = null;
      String currentLine = null;
      int lineCount = 0;

      try {
         reader = new BufferedReader(new InputStreamReader(inputStream));

         while((currentLine = reader.readLine()) != null) {
            ++lineCount;
            currentLine = currentLine.replaceAll("\\s+", " ").trim();
            if(isValidGroupObjectLine(currentLine)) {
               W_GroupObject e = this.parseGroupObject(currentLine, lineCount);
               if(e != null) {
                  e.glDrawingMode = 4;
                  this.vertices.clear();
                  int vertexNum = 0;
                  boolean mirror = false;
                  double facet = Math.cos(0.785398163375D);
                  boolean shading = false;

                  String[] faceNum;
                  while((currentLine = reader.readLine()) != null) {
                     ++lineCount;
                     currentLine = currentLine.replaceAll("\\s+", " ").trim();
                     if(currentLine.equalsIgnoreCase("mirror 1")) {
                        mirror = true;
                     }

                     if(currentLine.equalsIgnoreCase("shading 1")) {
                        shading = true;
                     }

                     faceNum = currentLine.split(" ");
                     if(faceNum.length == 2 && faceNum[0].equalsIgnoreCase("facet")) {
                        facet = Math.cos(Double.parseDouble(faceNum[1]) * 3.1415926535D / 180.0D);
                     }

                     if(isValidVertexLine(currentLine)) {
                        vertexNum = Integer.valueOf(currentLine.split(" ")[1]).intValue();
                        break;
                     }
                  }

                  if(vertexNum > 0) {
                     while((currentLine = reader.readLine()) != null) {
                        ++lineCount;
                        currentLine = currentLine.replaceAll("\\s+", " ").trim();
                        faceNum = currentLine.split(" ");
                        if(faceNum.length == 3) {
                           W_Vertex s = new W_Vertex(Float.valueOf(faceNum[0]).floatValue() / 100.0F, Float.valueOf(faceNum[1]).floatValue() / 100.0F, Float.valueOf(faceNum[2]).floatValue() / 100.0F);
                           this.checkMinMax(s);
                           this.vertices.add(s);
                           --vertexNum;
                           if(vertexNum <= 0) {
                              break;
                           }
                        } else if(faceNum.length > 0) {
                           throw new ModelFormatException("format error : " + this.fileName + " : line=" + lineCount);
                        }
                     }

                     int var30 = 0;

                     while((currentLine = reader.readLine()) != null) {
                        ++lineCount;
                        currentLine = currentLine.replaceAll("\\s+", " ").trim();
                        if(isValidFaceLine(currentLine)) {
                           var30 = Integer.valueOf(currentLine.split(" ")[1]).intValue();
                           break;
                        }
                     }

                     if(var30 > 0) {
                        while((currentLine = reader.readLine()) != null) {
                           ++lineCount;
                           currentLine = currentLine.replaceAll("\\s+", " ").trim();
                           String[] var31 = currentLine.split(" ");
                           if(var31.length <= 2) {
                              if(var31.length > 2 && Integer.valueOf(var31[0]).intValue() != 3) {
                                 throw new ModelFormatException("found face is not triangle : " + this.fileName + " : line=" + lineCount);
                              }
                           } else {
                              if(Integer.valueOf(var31[0]).intValue() >= 3) {
                                 W_Face[] faces = this.parseFace(currentLine, lineCount, mirror);
                                 W_Face[] arr$ = faces;
                                 int len$ = faces.length;

                                 for(int i$ = 0; i$ < len$; ++i$) {
                                    W_Face face = arr$[i$];
                                    e.faces.add(face);
                                 }
                              }

                              --var30;
                              if(var30 <= 0) {
                                 break;
                              }
                           }
                        }

                        this.calcVerticesNormal(e, shading, facet);
                     }
                  }

                  this.vertexNum += this.vertices.size();
                  this.faceNum += e.faces.size();
                  this.vertices.clear();
                  this.groupObjects.add(e);
               }
            }
         }
      } catch (IOException var28) {
         throw new ModelFormatException("IO Exception reading model format : " + this.fileName, var28);
      } finally {
         this.checkMinMaxFinal();
         this.vertices = null;

         try {
            reader.close();
         } catch (IOException var27) {
            ;
         }

         try {
            inputStream.close();
         } catch (IOException var26) {
            ;
         }

      }

   }

   public void calcVerticesNormal(W_GroupObject group, boolean shading, double facet) {
      Iterator i$ = group.faces.iterator();

      while(i$.hasNext()) {
         W_Face f = (W_Face)i$.next();
         f.vertexNormals = new W_Vertex[f.verticesID.length];

         for(int i = 0; i < f.verticesID.length; ++i) {
            W_Vertex vn = this.getVerticesNormalFromFace(f.faceNormal, f.verticesID[i], group, (float)facet);
            vn.normalize();
            if(shading) {
               if((double)(f.faceNormal.x * vn.x + f.faceNormal.y * vn.y + f.faceNormal.z * vn.z) >= facet) {
                  f.vertexNormals[i] = vn;
               } else {
                  f.vertexNormals[i] = f.faceNormal;
               }
            } else {
               f.vertexNormals[i] = f.faceNormal;
            }
         }
      }

   }

   public W_Vertex getVerticesNormalFromFace(W_Vertex faceNormal, int verticesID, W_GroupObject group, float facet) {
      W_Vertex v = new W_Vertex(0.0F, 0.0F, 0.0F);
      Iterator i$ = group.faces.iterator();

      while(i$.hasNext()) {
         W_Face f = (W_Face)i$.next();
         int[] arr$ = f.verticesID;
         int len$ = arr$.length;

         for(int i$1 = 0; i$1 < len$; ++i$1) {
            int id = arr$[i$1];
            if(id == verticesID) {
               if(f.faceNormal.x * faceNormal.x + f.faceNormal.y * faceNormal.y + f.faceNormal.z * faceNormal.z >= facet) {
                  v.add(f.faceNormal);
               }
               break;
            }
         }
      }

      v.normalize();
      return v;
   }

   public void renderAll() {
      Tessellator tessellator = Tessellator.instance;
      if(this.currentGroupObject != null) {
         tessellator.startDrawing(this.currentGroupObject.glDrawingMode);
      } else {
         tessellator.startDrawing(4);
      }

      this.tessellateAll(tessellator);
      tessellator.draw();
   }

   public void tessellateAll(Tessellator tessellator) {
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         groupObject.render(tessellator);
      }

   }

   public void renderOnly(String ... groupNames) {
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         String[] arr$ = groupNames;
         int len$ = groupNames.length;

         for(int i$1 = 0; i$1 < len$; ++i$1) {
            String groupName = arr$[i$1];
            if(groupName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render();
            }
         }
      }

   }

   public void tessellateOnly(Tessellator tessellator, String ... groupNames) {
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         String[] arr$ = groupNames;
         int len$ = groupNames.length;

         for(int i$1 = 0; i$1 < len$; ++i$1) {
            String groupName = arr$[i$1];
            if(groupName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render(tessellator);
            }
         }
      }

   }

   public void renderPart(String partName) {
      W_GroupObject groupObject;
      if(partName.charAt(0) == 36) {
         for(int i$ = 0; i$ < this.groupObjects.size(); ++i$) {
            groupObject = (W_GroupObject)this.groupObjects.get(i$);
            if(partName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render();
               ++i$;

               while(i$ < this.groupObjects.size()) {
                  groupObject = (W_GroupObject)this.groupObjects.get(i$);
                  if(groupObject.name.charAt(0) == 36) {
                     break;
                  }

                  groupObject.render();
                  ++i$;
               }
            }
         }
      } else {
         Iterator var4 = this.groupObjects.iterator();

         while(var4.hasNext()) {
            groupObject = (W_GroupObject)var4.next();
            if(partName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render();
            }
         }
      }

   }

   public void tessellatePart(Tessellator tessellator, String partName) {
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         if(partName.equalsIgnoreCase(groupObject.name)) {
            groupObject.render(tessellator);
         }
      }

   }

   public void renderAllExcept(String ... excludedGroupNames) {
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         boolean skipPart = false;
         String[] arr$ = excludedGroupNames;
         int len$ = excludedGroupNames.length;

         for(int i$1 = 0; i$1 < len$; ++i$1) {
            String excludedGroupName = arr$[i$1];
            if(excludedGroupName.equalsIgnoreCase(groupObject.name)) {
               skipPart = true;
            }
         }

         if(!skipPart) {
            groupObject.render();
         }
      }

   }

   public void tessellateAllExcept(Tessellator tessellator, String ... excludedGroupNames) {
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         boolean exclude = false;
         String[] arr$ = excludedGroupNames;
         int len$ = excludedGroupNames.length;

         for(int i$1 = 0; i$1 < len$; ++i$1) {
            String excludedGroupName = arr$[i$1];
            if(excludedGroupName.equalsIgnoreCase(groupObject.name)) {
               exclude = true;
            }
         }

         if(!exclude) {
            groupObject.render(tessellator);
         }
      }

   }

   private W_Face[] parseFace(String line, int lineCount, boolean mirror) {
      String[] s = line.split("[ VU)(M]+");
      int vnum = Integer.valueOf(s[0]).intValue();
      if(vnum != 3 && vnum != 4) {
         return new W_Face[0];
      } else {
         W_Face face1;
         if(vnum == 3) {
            face1 = new W_Face();
            face1.verticesID = new int[]{Integer.valueOf(s[3]).intValue(), Integer.valueOf(s[2]).intValue(), Integer.valueOf(s[1]).intValue()};
            face1.vertices = new W_Vertex[]{(W_Vertex)this.vertices.get(face1.verticesID[0]), (W_Vertex)this.vertices.get(face1.verticesID[1]), (W_Vertex)this.vertices.get(face1.verticesID[2])};
            if(s.length >= 11) {
               face1.textureCoordinates = new W_TextureCoordinate[]{new W_TextureCoordinate(Float.valueOf(s[9]).floatValue(), Float.valueOf(s[10]).floatValue()), new W_TextureCoordinate(Float.valueOf(s[7]).floatValue(), Float.valueOf(s[8]).floatValue()), new W_TextureCoordinate(Float.valueOf(s[5]).floatValue(), Float.valueOf(s[6]).floatValue())};
            } else {
               face1.textureCoordinates = new W_TextureCoordinate[]{new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F)};
            }

            face1.faceNormal = face1.calculateFaceNormal();
            return new W_Face[]{face1};
         } else {
            face1 = new W_Face();
            face1.verticesID = new int[]{Integer.valueOf(s[3]).intValue(), Integer.valueOf(s[2]).intValue(), Integer.valueOf(s[1]).intValue()};
            face1.vertices = new W_Vertex[]{(W_Vertex)this.vertices.get(face1.verticesID[0]), (W_Vertex)this.vertices.get(face1.verticesID[1]), (W_Vertex)this.vertices.get(face1.verticesID[2])};
            if(s.length >= 12) {
               face1.textureCoordinates = new W_TextureCoordinate[]{new W_TextureCoordinate(Float.valueOf(s[10]).floatValue(), Float.valueOf(s[11]).floatValue()), new W_TextureCoordinate(Float.valueOf(s[8]).floatValue(), Float.valueOf(s[9]).floatValue()), new W_TextureCoordinate(Float.valueOf(s[6]).floatValue(), Float.valueOf(s[7]).floatValue())};
            } else {
               face1.textureCoordinates = new W_TextureCoordinate[]{new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F)};
            }

            face1.faceNormal = face1.calculateFaceNormal();
            W_Face face2 = new W_Face();
            face2.verticesID = new int[]{Integer.valueOf(s[4]).intValue(), Integer.valueOf(s[3]).intValue(), Integer.valueOf(s[1]).intValue()};
            face2.vertices = new W_Vertex[]{(W_Vertex)this.vertices.get(face2.verticesID[0]), (W_Vertex)this.vertices.get(face2.verticesID[1]), (W_Vertex)this.vertices.get(face2.verticesID[2])};
            if(s.length >= 14) {
               face2.textureCoordinates = new W_TextureCoordinate[]{new W_TextureCoordinate(Float.valueOf(s[12]).floatValue(), Float.valueOf(s[13]).floatValue()), new W_TextureCoordinate(Float.valueOf(s[10]).floatValue(), Float.valueOf(s[11]).floatValue()), new W_TextureCoordinate(Float.valueOf(s[6]).floatValue(), Float.valueOf(s[7]).floatValue())};
            } else {
               face2.textureCoordinates = new W_TextureCoordinate[]{new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F)};
            }

            face2.faceNormal = face2.calculateFaceNormal();
            return new W_Face[]{face1, face2};
         }
      }
   }

   private static boolean isValidGroupObjectLine(String line) {
      String[] s = line.split(" ");
      return s.length >= 2 && s[0].equals("Object")?s[1].length() >= 4 && s[1].charAt(0) == 34:false;
   }

   private W_GroupObject parseGroupObject(String line, int lineCount) throws ModelFormatException {
      W_GroupObject group = null;
      if(isValidGroupObjectLine(line)) {
         String[] s = line.split(" ");
         String trimmedLine = s[1].substring(1, s[1].length() - 1);
         if(trimmedLine.length() > 0) {
            group = new W_GroupObject(trimmedLine);
         }

         return group;
      } else {
         throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Incorrect format");
      }
   }

   private static boolean isValidVertexLine(String line) {
      String[] s = line.split(" ");
      return s[0].equals("vertex");
   }

   private static boolean isValidFaceLine(String line) {
      String[] s = line.split(" ");
      return s[0].equals("face");
   }

   public String getType() {
      return "mqo";
   }

   public void renderAllLine(int startLine, int maxLine) {
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawing(1);
      this.renderAllLine(tessellator, startLine, maxLine);
      tessellator.draw();
   }

   public void renderAllLine(Tessellator tessellator, int startLine, int maxLine) {
      int lineCnt = 0;
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         if(groupObject.faces.size() > 0) {
            Iterator i$1 = groupObject.faces.iterator();

            while(i$1.hasNext()) {
               W_Face face = (W_Face)i$1.next();

               for(int i = 0; i < face.vertices.length / 3; ++i) {
                  W_Vertex v1 = face.vertices[i * 3 + 0];
                  W_Vertex v2 = face.vertices[i * 3 + 1];
                  W_Vertex v3 = face.vertices[i * 3 + 2];
                  ++lineCnt;
                  if(lineCnt > maxLine) {
                     return;
                  }

                  tessellator.addVertex((double)v1.x, (double)v1.y, (double)v1.z);
                  tessellator.addVertex((double)v2.x, (double)v2.y, (double)v2.z);
                  ++lineCnt;
                  if(lineCnt > maxLine) {
                     return;
                  }

                  tessellator.addVertex((double)v2.x, (double)v2.y, (double)v2.z);
                  tessellator.addVertex((double)v3.x, (double)v3.y, (double)v3.z);
                  ++lineCnt;
                  if(lineCnt > maxLine) {
                     return;
                  }

                  tessellator.addVertex((double)v3.x, (double)v3.y, (double)v3.z);
                  tessellator.addVertex((double)v1.x, (double)v1.y, (double)v1.z);
               }
            }
         }
      }

   }

   public int getVertexNum() {
      return this.vertexNum;
   }

   public int getFaceNum() {
      return this.faceNum;
   }

   public void renderAll(int startFace, int maxFace) {
      if(startFace < 0) {
         startFace = 0;
      }

      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawing(4);
      this.renderAll(tessellator, startFace, maxFace);
      tessellator.draw();
   }

   public void renderAll(Tessellator tessellator, int startFace, int maxLine) {
      int faceCnt = 0;
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         if(groupObject.faces.size() > 0) {
            Iterator i$1 = groupObject.faces.iterator();

            while(i$1.hasNext()) {
               W_Face face = (W_Face)i$1.next();
               ++faceCnt;
               if(faceCnt >= startFace) {
                  if(faceCnt > maxLine) {
                     return;
                  }

                  face.addFaceForRender(tessellator);
               }
            }
         }
      }

   }
}
