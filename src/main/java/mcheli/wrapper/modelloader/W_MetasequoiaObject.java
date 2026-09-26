package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import mcheli.wrapper.modelloader.W_Face;
import mcheli.wrapper.modelloader.W_GroupObject;
import mcheli.wrapper.modelloader.W_ModelCustom;
import mcheli.wrapper.modelloader.W_TextureCoordinate;
import mcheli.wrapper.modelloader.W_Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFormatException;

@SideOnly(Side.CLIENT)
public class W_MetasequoiaObject extends W_ModelCustom {

   public ArrayList vertices = new ArrayList();
   public ArrayList groupObjects = new ArrayList();
   private W_GroupObject currentGroupObject = null;
   private String fileName;
   private int vertexNum = 0;
   private int faceNum = 0;

   /** Half-open range in the ordered MQO object list. */
   public static final class GroupRange {
      public final int start;
      public final int end;

      private GroupRange(int start, int end) {
         this.start = start;
         this.end = end;
      }
   }

   private W_MetasequoiaObject(W_MetasequoiaObject source, ArrayList selected) {
      this.fileName = source.fileName + "#view";
      // Geometry is immutable after parsing.  A view owns its list, but deliberately
      // shares the groups (and their VBOs) so the source cache is never mutated.
      this.groupObjects = selected;
      this.vertices = null;
      this.vertexNum = source.vertexNum;
      this.faceNum = 0;
      this.min = source.min; this.minX = source.minX; this.minY = source.minY; this.minZ = source.minZ;
      this.max = source.max; this.maxX = source.maxX; this.maxY = source.maxY; this.maxZ = source.maxZ;
      this.size = source.size; this.sizeX = source.sizeX; this.sizeY = source.sizeY; this.sizeZ = source.sizeZ;
      for(Object object : selected) this.faceNum += ((W_GroupObject)object).getFaceCount();
   }

   /**
    * MQO '$' groups are section markers: the section includes every following
    * unprefixed group and ends immediately before the next '$' marker.
    */
   public GroupRange resolveGroupRange(String sectionName) {
      for(int i = 0; i < this.groupObjects.size(); ++i) {
         W_GroupObject group = (W_GroupObject)this.groupObjects.get(i);
         if(sectionName.equalsIgnoreCase(group.name)) {
            int end = i + 1;
            while(end < this.groupObjects.size()) {
               String name = ((W_GroupObject)this.groupObjects.get(end)).name;
               if(name != null && name.startsWith("$")) break;
               ++end;
            }
            return new GroupRange(i, end);
         }
      }
      return null;
   }

   public W_MetasequoiaObject createView(Collection ranges) {
      ArrayList selected = new ArrayList();
      boolean[] included = new boolean[this.groupObjects.size()];
      for(Object object : ranges) {
         GroupRange range = (GroupRange)object;
         for(int i = range.start; i < range.end; ++i) included[i] = true;
      }
      for(int i = 0; i < included.length; ++i) if(included[i]) selected.add(this.groupObjects.get(i));
      return new W_MetasequoiaObject(this, selected);
   }

   public W_MetasequoiaObject createViewExcluding(Collection ranges) {
      ArrayList selected = new ArrayList(this.groupObjects);
      boolean[] excluded = new boolean[this.groupObjects.size()];
      for(Object object : ranges) {
         GroupRange range = (GroupRange)object;
         for(int i = range.start; i < range.end; ++i) excluded[i] = true;
      }
      selected.clear();
      for(int i = 0; i < excluded.length; ++i) if(!excluded[i]) selected.add(this.groupObjects.get(i));
      return new W_MetasequoiaObject(this, selected);
   }

   public int getGroupCount() {
      return this.groupObjects.size();
   }

   public java.util.List getPartNames() {
      ArrayList names = new ArrayList();
      for(Object object : this.groupObjects) names.add(((W_GroupObject)object).name);
      return names;
   }


   public W_MetasequoiaObject(ResourceLocation resource) throws ModelFormatException {
      this.fileName = resource.toString();

      // Use the shared resolver first so editable files beat stale resource packs.
      String assetPath = "assets/" + resource.getResourceDomain() + "/" + resource.getResourcePath();
      InputStream is = mcheli.MCH_ResourceHelper.openResourceStream(assetPath);
      if (is != null) {
         try {
            this.loadObjModel(is);
            return;
         } catch (Exception exception) {
            throw new ModelFormatException("IO Exception reading model format:" + this.fileName, exception);
         }
      }

      try {
         IResource e = Minecraft.getMinecraft().getResourceManager().getResource(resource);
         this.loadObjModel(e.getInputStream());
         return;
      } catch (IOException ignored) {
         // Fall through to MCH_ResourceHelper for addon filesystem resources
      }

      throw new ModelFormatException("IO Exception reading model format:" + this.fileName);
   }

   public W_MetasequoiaObject(String fileName, URL resource) throws ModelFormatException {
      this.fileName = fileName;

      try {
         this.loadObjModel(resource.openStream());
      } catch (IOException iterator2) {
         throw new ModelFormatException("IO Exception reading model format:" + this.fileName, iterator2);
      }
   }

   public W_MetasequoiaObject(String filename, InputStream inputStream) throws ModelFormatException {
      this.fileName = filename;
      this.loadObjModel(inputStream);
   }

   public boolean containsPart(String partName) {
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      W_GroupObject groupObject;
      do {
         if(!iteratedValueIndex.hasNext()) {
            return false;
         }

         groupObject = (W_GroupObject)iteratedValueIndex.next();
      } while(!partName.equalsIgnoreCase(groupObject.name));

      return true;
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

                     int result = 0;

                     while((currentLine = reader.readLine()) != null) {
                        ++lineCount;
                        currentLine = currentLine.replaceAll("\\s+", " ").trim();
                        if(isValidFaceLine(currentLine)) {
                           result = Integer.valueOf(currentLine.split(" ")[1]).intValue();
                           break;
                        }
                     }

                     if(result > 0) {
                        while((currentLine = reader.readLine()) != null) {
                           ++lineCount;
                           currentLine = currentLine.replaceAll("\\s+", " ").trim();
                           String[] formattedText = currentLine.split(" ");
                           if(formattedText.length <= 2) {
                              if(formattedText.length > 2 && Integer.valueOf(formattedText[0]).intValue() != 3) {
                                 throw new ModelFormatException("found face is not triangle : " + this.fileName + " : line=" + lineCount);
                              }
                           } else {
                              if(Integer.valueOf(formattedText[0]).intValue() >= 3) {
                                 W_Face[] faces = this.parseFace(currentLine, lineCount, mirror);
                                 W_Face[] iteratedValues = faces;
                                 int iteratedValueCount = faces.length;

                                 for(int iteratedValueIndex = 0; iteratedValueIndex < iteratedValueCount; ++iteratedValueIndex) {
                                    W_Face face = iteratedValues[iteratedValueIndex];
                                    e.faces.add(face);
                                 }
                              }

                              --result;
                              if(result <= 0) {
                                 break;
                              }
                           }
                        }

                        this.calcVerticesNormal(e, shading, facet);
                        this.compactFaces(e);
                     }
                  }

                  this.vertexNum += this.vertices.size();
                  this.faceNum += e.getFaceCount();
                  this.vertices.clear();
                  this.groupObjects.add(e);
               }
            }
         }
      } catch (IOException oException) {
         throw new ModelFormatException("IO Exception reading model format : " + this.fileName, oException);
      } finally {
         this.checkMinMaxFinal();
         this.groupObjects.trimToSize();
         this.vertices = null;

         try {
            reader.close();
         } catch (IOException oException2) {
            ;
         }

         try {
            inputStream.close();
         } catch (IOException oException3) {
            ;
         }

      }

   }

   private void compactFaces(W_GroupObject group) {
      group.finalizeGeometry();
   }

   public void calcVerticesNormal(W_GroupObject group, boolean shading, double facet) {
      Iterator iteratedValueIndex = group.faces.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_Face f = (W_Face)iteratedValueIndex.next();
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
      Iterator iteratedValueIndex = group.faces.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_Face f = (W_Face)iteratedValueIndex.next();
         int[] iteratedValues = f.verticesID;
         int iteratedValueCount = iteratedValues.length;

         for(int iteratedValueIndex1 = 0; iteratedValueIndex1 < iteratedValueCount; ++iteratedValueIndex1) {
            int id = iteratedValues[iteratedValueIndex1];
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
      Iterator iteratedValueIndex = this.groupObjects.iterator();
      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         if(groupObject != null) {
            groupObject.render();
         }
      }
   }

   public void renderAllTransformed() {
      Iterator iteratedValueIndex = this.groupObjects.iterator();
      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         if(groupObject != null) {
            groupObject.renderTransformed();
         }
      }
   }

   public void tessellateAll(Tessellator tessellator) {
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         groupObject.render(tessellator);
      }

   }

   public void renderOnly(String ... groupNames) {
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         String[] iteratedValues = groupNames;
         int iteratedValueCount = groupNames.length;

         for(int iteratedValueIndex1 = 0; iteratedValueIndex1 < iteratedValueCount; ++iteratedValueIndex1) {
            String groupName = iteratedValues[iteratedValueIndex1];
            if(groupName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render();
            }
         }
      }

   }

   public void tessellateOnly(Tessellator tessellator, String ... groupNames) {
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         String[] iteratedValues = groupNames;
         int iteratedValueCount = groupNames.length;

         for(int iteratedValueIndex1 = 0; iteratedValueIndex1 < iteratedValueCount; ++iteratedValueIndex1) {
            String groupName = iteratedValues[iteratedValueIndex1];
            if(groupName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render(tessellator);
            }
         }
      }

   }

   public void renderPart(String partName) {
      W_GroupObject groupObject;
      if(partName.charAt(0) == 36) {
         for(int iteratedValueIndex = 0; iteratedValueIndex < this.groupObjects.size(); ++iteratedValueIndex) {
            groupObject = (W_GroupObject)this.groupObjects.get(iteratedValueIndex);
            if(partName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render();
               ++iteratedValueIndex;

               while(iteratedValueIndex < this.groupObjects.size()) {
                  groupObject = (W_GroupObject)this.groupObjects.get(iteratedValueIndex);
                  if(groupObject.name.charAt(0) == 36) {
                     break;
                  }

                  groupObject.render();
                  ++iteratedValueIndex;
               }
            }
         }
      } else {
         Iterator iterator2 = this.groupObjects.iterator();

         while(iterator2.hasNext()) {
            groupObject = (W_GroupObject)iterator2.next();
            if(partName.equalsIgnoreCase(groupObject.name)) {
               groupObject.render();
            }
         }
      }

   }

   public void renderPartTransformed(String partName) {
      W_GroupObject groupObject;
      if(partName.charAt(0) == 36) {
         for(int iteratedValueIndex = 0; iteratedValueIndex < this.groupObjects.size(); ++iteratedValueIndex) {
            groupObject = (W_GroupObject)this.groupObjects.get(iteratedValueIndex);
            if(partName.equalsIgnoreCase(groupObject.name)) {
               groupObject.renderTransformed();
               ++iteratedValueIndex;

               while(iteratedValueIndex < this.groupObjects.size()) {
                  groupObject = (W_GroupObject)this.groupObjects.get(iteratedValueIndex);
                  if(groupObject.name.charAt(0) == 36) {
                     break;
                  }

                  groupObject.renderTransformed();
                  ++iteratedValueIndex;
               }
            }
         }
      } else {
         Iterator iterator2 = this.groupObjects.iterator();

         while(iterator2.hasNext()) {
            groupObject = (W_GroupObject)iterator2.next();
            if(partName.equalsIgnoreCase(groupObject.name)) {
               groupObject.renderTransformed();
            }
         }
      }

   }

   public void tessellatePart(Tessellator tessellator, String partName) {
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         if(partName.equalsIgnoreCase(groupObject.name)) {
            groupObject.render(tessellator);
         }
      }

   }

   public void renderAllExcept(String ... excludedGroupNames) {
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         boolean skipPart = false;
         String[] iteratedValues = excludedGroupNames;
         int iteratedValueCount = excludedGroupNames.length;

         for(int iteratedValueIndex1 = 0; iteratedValueIndex1 < iteratedValueCount; ++iteratedValueIndex1) {
            String excludedGroupName = iteratedValues[iteratedValueIndex1];
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
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         boolean exclude = false;
         String[] iteratedValues = excludedGroupNames;
         int iteratedValueCount = excludedGroupNames.length;

         for(int iteratedValueIndex1 = 0; iteratedValueIndex1 < iteratedValueCount; ++iteratedValueIndex1) {
            String excludedGroupName = iteratedValues[iteratedValueIndex1];
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
      int lineCount = 0;
      for(Object object : this.groupObjects) {
         W_GroupObject group = (W_GroupObject)object;
         for(int face = 0; face < group.getFaceCount(); ++face) {
            int vertices = group.getFaceVertexCount(face);
            int firstVertex = group.getFaceFirstVertex(face);
            for(int triangle = 0; triangle < vertices / 3; ++triangle) {
               int vertex = firstVertex + triangle * 3;
               lineCount = this.addTriangleLines(tessellator, group, vertex, lineCount, maxLine);
               if(lineCount > maxLine) {
                  return;
               }
            }
         }
      }
   }

   private int addTriangleLines(Tessellator tessellator, W_GroupObject group, int vertex, int lineCount, int maxLine) {
      int[] from = new int[]{0, 1, 2};
      int[] to = new int[]{1, 2, 0};
      for(int edge = 0; edge < 3; ++edge) {
         ++lineCount;
         if(lineCount > maxLine) {
            return lineCount;
         }
         int a = vertex + from[edge];
         int b = vertex + to[edge];
         tessellator.addVertex(group.getVertexX(a), group.getVertexY(a), group.getVertexZ(a));
         tessellator.addVertex(group.getVertexX(b), group.getVertexY(b), group.getVertexZ(b));
      }
      return lineCount;
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

   public void renderAll(Tessellator tessellator, int startFace, int maxFace) {
      int faceCount = 0;
      for(Object object : this.groupObjects) {
         W_GroupObject group = (W_GroupObject)object;
         int first = Math.max(0, startFace - faceCount);
         int last = Math.min(group.getFaceCount() - 1, maxFace - faceCount);
         if(first <= last) {
            group.renderFaces(tessellator, first, last);
         }
         faceCount += group.getFaceCount();
         if(faceCount > maxFace) {
            return;
         }
      }
   }

}
