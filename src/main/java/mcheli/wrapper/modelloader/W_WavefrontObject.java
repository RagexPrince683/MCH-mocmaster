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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class W_WavefrontObject extends W_ModelCustom {

   private static Pattern vertexPattern = Pattern.compile("(v( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(v( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
   private static Pattern vertexNormalPattern = Pattern.compile("(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
   private static Pattern textureCoordinatePattern = Pattern.compile("(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *\\n)|(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *$)");
   private static Pattern face_V_VT_VN_Pattern = Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)");
   private static Pattern face_V_VT_Pattern = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)");
   private static Pattern face_V_VN_Pattern = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)");
   private static Pattern face_V_Pattern = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)");
   private static Pattern groupObjectPattern = Pattern.compile("([go]( [-\\$\\w\\d]+) *\\n)|([go]( [-\\$\\w\\d]+) *$)");
   //DELETE
   //private static Matcher vertexMatcher;
   //private static Matcher vertexNormalMatcher;
   //private static Matcher textureCoordinateMatcher;
   //private static Matcher face_V_VT_VN_Matcher;
  // private static Matcher face_V_VT_Matcher;
  // private static Matcher face_V_VN_Matcher;
  // private static Matcher face_V_Matcher;
  // private static Matcher groupObjectMatcher;
   //END DELETE
   public ArrayList vertices = new ArrayList();
   public ArrayList vertexNormals = new ArrayList();
   public ArrayList textureCoordinates = new ArrayList();
   public ArrayList groupObjects = new ArrayList();
   private int vertexNum = 0;
   private int faceNum = 0;
   private W_GroupObject currentGroupObject;
   private String fileName;


   public W_WavefrontObject(ResourceLocation resource) throws ModelFormatException {
      this.fileName = resource.toString();

      // Use the shared resolver first so editable files beat stale resource packs.
      String assetPath = "assets/" + resource.getResourceDomain() + "/" + resource.getResourcePath();
      InputStream is = mcheli.MCH_ResourceHelper.openResourceStream(assetPath);
      if (is != null) {
         try {
            this.loadObjModel(is);
            return;
         } catch (Exception exception) {
            throw new ModelFormatException("IO Exception reading model format", exception);
         }
      }

      try {
         IResource e = Minecraft.getMinecraft().getResourceManager().getResource(resource);
         this.loadObjModel(e.getInputStream());
         return;
      } catch (IOException ignored) {
         // Fall through to MCH_ResourceHelper for addon directory files
      }

      throw new ModelFormatException("IO Exception reading model format:" + this.fileName);
   }

   public W_WavefrontObject(String fileName, URL resource) throws ModelFormatException {
      this.fileName = fileName;

      try {
         this.loadObjModel(resource.openStream());
      } catch (IOException oException) {
         throw new ModelFormatException("IO Exception reading model format", oException);
      }
   }

   public W_WavefrontObject(String filename, InputStream inputStream) throws ModelFormatException {
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
            if(!currentLine.startsWith("#") && currentLine.length() != 0) {
               W_Vertex e;
               if(currentLine.startsWith("v ")) {
                  e = this.parseVertex(currentLine, lineCount);
                  if(e != null) {
                     this.checkMinMax(e);
                     this.vertices.add(e);
                  }
               } else if(currentLine.startsWith("vn ")) {
                  e = this.parseVertexNormal(currentLine, lineCount);
                  if(e != null) {
                     this.vertexNormals.add(e);
                  }
               } else if(currentLine.startsWith("vt ")) {
                  W_TextureCoordinate textureCoordinate2 = this.parseTextureCoordinate(currentLine, lineCount);
                  if(textureCoordinate2 != null) {
                     this.textureCoordinates.add(textureCoordinate2);
                  }
               } else if(currentLine.startsWith("f ")) {
                  if(this.currentGroupObject == null) {
                     this.currentGroupObject = new W_GroupObject("Default");
                  }

                  W_Face result = this.parseFace(currentLine, lineCount);
                  if(result != null) {
                     this.currentGroupObject.faces.add(result);
                  }
               } else if(currentLine.startsWith("g ") | currentLine.startsWith("o ") && currentLine.charAt(2) == 36) {
                  W_GroupObject groupObject2 = this.parseGroupObject(currentLine, lineCount);
                  if(groupObject2 != null && this.currentGroupObject != null) {
                     this.groupObjects.add(this.currentGroupObject);
                  }

                  this.currentGroupObject = groupObject2;
               }
            }
         }

         this.groupObjects.add(this.currentGroupObject);
         this.vertexNum = this.vertices.size();
         this.faceNum = 0;
         this.compactFaces();
         this.groupObjects.trimToSize();
         this.releaseLoaderScratch();
      } catch (IOException oException2) {
         throw new ModelFormatException("IO Exception reading model format", oException2);
      } finally {
         this.checkMinMaxFinal();

         try {
            reader.close();
         } catch (IOException oException3) {
            ;
         }

         try {
            inputStream.close();
         } catch (IOException oException4) {
            ;
         }

      }

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

   private void compactFaces() {
      for(Object object : this.groupObjects) {
         W_GroupObject group = (W_GroupObject)object;
         this.faceNum += group.faces.size();
         group.finalizeGeometry();
      }
   }

   private void releaseLoaderScratch() {
      if(this.vertices != null) {
         this.vertices.clear();
         this.vertices = null;
      }

      if(this.vertexNormals != null) {
         this.vertexNormals.clear();
         this.vertexNormals = null;
      }

      if(this.textureCoordinates != null) {
         this.textureCoordinates.clear();
         this.textureCoordinates = null;
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
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         if(partName.equalsIgnoreCase(groupObject.name)) {
            groupObject.render();
         }
      }

   }

   public void renderPartTransformed(String partName) {
      Iterator iteratedValueIndex = this.groupObjects.iterator();

      while(iteratedValueIndex.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)iteratedValueIndex.next();
         if(partName.equalsIgnoreCase(groupObject.name)) {
            groupObject.renderTransformed();
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

   private W_Vertex parseVertex(String line, int lineCount) throws ModelFormatException {
      Object vertex = null;
      if(isValidVertexLine(line)) {
         line = line.substring(line.indexOf(" ") + 1);
         String[] tokens = line.split(" ");

         try {
            return (W_Vertex)(tokens.length == 2?new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1])):(tokens.length == 3?new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])):vertex));
         } catch (NumberFormatException numberFormatException) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), numberFormatException);
         }
      } else {
         throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Incorrect format");
      }
   }

   private W_Vertex parseVertexNormal(String line, int lineCount) throws ModelFormatException {
      Object vertexNormal = null;
      if(isValidVertexNormalLine(line)) {
         line = line.substring(line.indexOf(" ") + 1);
         String[] tokens = line.split(" ");

         try {
            return (W_Vertex)(tokens.length == 3?new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])):vertexNormal);
         } catch (NumberFormatException numberFormatException) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), numberFormatException);
         }
      } else {
         throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Incorrect format");
      }
   }

   private W_TextureCoordinate parseTextureCoordinate(String line, int lineCount) throws ModelFormatException {
      Object textureCoordinate = null;
      if(isValidTextureCoordinateLine(line)) {
         line = line.substring(line.indexOf(" ") + 1);
         String[] tokens = line.split(" ");

         try {
            return (W_TextureCoordinate)(tokens.length == 2?new W_TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1])):(tokens.length == 3?new W_TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])):textureCoordinate));
         } catch (NumberFormatException numberFormatException) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), numberFormatException);
         }
      } else {
         throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Incorrect format");
      }
   }

   private W_Face parseFace(String line, int lineCount) throws ModelFormatException {
      W_Face face = null;
      if(isValidFaceLine(line)) {
         face = new W_Face();
         String trimmedLine = line.substring(line.indexOf(" ") + 1);
         String[] tokens = trimmedLine.split(" ");
         String[] subTokens = null;
         if(tokens.length == 3) {
            if(this.currentGroupObject.glDrawingMode == -1) {
               this.currentGroupObject.glDrawingMode = 4;
            } else if(this.currentGroupObject.glDrawingMode != 4) {
               throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Invalid number of points for face (expected 4, found " + tokens.length + ")");
            }
         } else if(tokens.length == 4) {
            if(this.currentGroupObject.glDrawingMode == -1) {
               this.currentGroupObject.glDrawingMode = 7;
            } else if(this.currentGroupObject.glDrawingMode != 7) {
               throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Invalid number of points for face (expected 3, found " + tokens.length + ")");
            }
         }

         int i;
         if(isValidFace_V_VT_VN_Line(line)) {
            face.vertices = new W_Vertex[tokens.length];
            face.textureCoordinates = new W_TextureCoordinate[tokens.length];
            face.vertexNormals = new W_Vertex[tokens.length];

            for(i = 0; i < tokens.length; ++i) {
               subTokens = tokens[i].split("/");
               face.vertices[i] = (W_Vertex)this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
               face.textureCoordinates[i] = (W_TextureCoordinate)this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
               face.vertexNormals[i] = (W_Vertex)this.vertexNormals.get(Integer.parseInt(subTokens[2]) - 1);
            }

            face.faceNormal = face.calculateFaceNormal();
         } else if(isValidFace_V_VT_Line(line)) {
            face.vertices = new W_Vertex[tokens.length];
            face.textureCoordinates = new W_TextureCoordinate[tokens.length];

            for(i = 0; i < tokens.length; ++i) {
               subTokens = tokens[i].split("/");
               face.vertices[i] = (W_Vertex)this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
               face.textureCoordinates[i] = (W_TextureCoordinate)this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
            }

            face.faceNormal = face.calculateFaceNormal();
         } else if(isValidFace_V_VN_Line(line)) {
            face.vertices = new W_Vertex[tokens.length];
            face.vertexNormals = new W_Vertex[tokens.length];

            for(i = 0; i < tokens.length; ++i) {
               subTokens = tokens[i].split("//");
               face.vertices[i] = (W_Vertex)this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
               face.vertexNormals[i] = (W_Vertex)this.vertexNormals.get(Integer.parseInt(subTokens[1]) - 1);
            }

            face.faceNormal = face.calculateFaceNormal();
         } else {
            if(!isValidFace_V_Line(line)) {
               throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Incorrect format");
            }

            face.vertices = new W_Vertex[tokens.length];

            for(i = 0; i < tokens.length; ++i) {
               face.vertices[i] = (W_Vertex)this.vertices.get(Integer.parseInt(tokens[i]) - 1);
            }

            face.faceNormal = face.calculateFaceNormal();
         }

         return face;
      } else {
         throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Incorrect format");
      }
   }

   private W_GroupObject parseGroupObject(String line, int lineCount) throws ModelFormatException {
      W_GroupObject group = null;
      if(isValidGroupObjectLine(line)) {
         String trimmedLine = line.substring(line.indexOf(" ") + 1);
         if(trimmedLine.length() > 0) {
            group = new W_GroupObject(trimmedLine);
         }

         return group;
      } else {
         throw new ModelFormatException("Error parsing entry (\'" + line + "\'" + ", line " + lineCount + ") in file \'" + this.fileName + "\' - Incorrect format");
      }
   }

   private static boolean isValidVertexLine(String line) {
      return vertexPattern.matcher(line).matches();
   }

   private static boolean isValidVertexNormalLine(String line) {
      return vertexNormalPattern.matcher(line).matches();
   }

   private static boolean isValidTextureCoordinateLine(String line) {
      return textureCoordinatePattern.matcher(line).matches();
   }

   private static boolean isValidFace_V_VT_VN_Line(String line) {
      return face_V_VT_VN_Pattern.matcher(line).matches();
   }

   private static boolean isValidFace_V_VT_Line(String line) {
      return face_V_VT_Pattern.matcher(line).matches();
   }

   private static boolean isValidFace_V_VN_Line(String line) {
      return face_V_VN_Pattern.matcher(line).matches();
   }

   private static boolean isValidFace_V_Line(String line) {
      return face_V_Pattern.matcher(line).matches();
   }

   private static boolean isValidFaceLine(String line) {
      return isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(line);
   }

   private static boolean isValidGroupObjectLine(String line) {
      return groupObjectPattern.matcher(line).matches();
   }

   public String getType() {
      return "obj";
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
      return this.vertices != null ? this.vertices.size() : this.vertexNum;
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
