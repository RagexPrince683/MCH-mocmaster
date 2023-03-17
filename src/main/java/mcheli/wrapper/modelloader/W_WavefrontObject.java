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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
   private static Matcher vertexMatcher;
   private static Matcher vertexNormalMatcher;
   private static Matcher textureCoordinateMatcher;
   private static Matcher face_V_VT_VN_Matcher;
   private static Matcher face_V_VT_Matcher;
   private static Matcher face_V_VN_Matcher;
   private static Matcher face_V_Matcher;
   private static Matcher groupObjectMatcher;
   public ArrayList vertices = new ArrayList();
   public ArrayList vertexNormals = new ArrayList();
   public ArrayList textureCoordinates = new ArrayList();
   public ArrayList groupObjects = new ArrayList();
   private W_GroupObject currentGroupObject;
   private String fileName;


   public W_WavefrontObject(ResourceLocation resource) throws ModelFormatException {
      this.fileName = resource.toString();

      try {
         IResource e = Minecraft.getMinecraft().getResourceManager().getResource(resource);
         this.loadObjModel(e.getInputStream());
      } catch (IOException var3) {
         throw new ModelFormatException("IO Exception reading model format", var3);
      }
   }

   public W_WavefrontObject(String fileName, URL resource) throws ModelFormatException {
      this.fileName = fileName;

      try {
         this.loadObjModel(resource.openStream());
      } catch (IOException var4) {
         throw new ModelFormatException("IO Exception reading model format", var4);
      }
   }

   public W_WavefrontObject(String filename, InputStream inputStream) throws ModelFormatException {
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
                  W_TextureCoordinate var18 = this.parseTextureCoordinate(currentLine, lineCount);
                  if(var18 != null) {
                     this.textureCoordinates.add(var18);
                  }
               } else if(currentLine.startsWith("f ")) {
                  if(this.currentGroupObject == null) {
                     this.currentGroupObject = new W_GroupObject("Default");
                  }

                  W_Face var19 = this.parseFace(currentLine, lineCount);
                  if(var19 != null) {
                     this.currentGroupObject.faces.add(var19);
                  }
               } else if(currentLine.startsWith("g ") | currentLine.startsWith("o ") && currentLine.charAt(2) == 36) {
                  W_GroupObject var20 = this.parseGroupObject(currentLine, lineCount);
                  if(var20 != null && this.currentGroupObject != null) {
                     this.groupObjects.add(this.currentGroupObject);
                  }

                  this.currentGroupObject = var20;
               }
            }
         }

         this.groupObjects.add(this.currentGroupObject);
      } catch (IOException var16) {
         throw new ModelFormatException("IO Exception reading model format", var16);
      } finally {
         this.checkMinMaxFinal();

         try {
            reader.close();
         } catch (IOException var15) {
            ;
         }

         try {
            inputStream.close();
         } catch (IOException var14) {
            ;
         }

      }

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
      Iterator i$ = this.groupObjects.iterator();

      while(i$.hasNext()) {
         W_GroupObject groupObject = (W_GroupObject)i$.next();
         if(partName.equalsIgnoreCase(groupObject.name)) {
            groupObject.render();
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

   private W_Vertex parseVertex(String line, int lineCount) throws ModelFormatException {
      Object vertex = null;
      if(isValidVertexLine(line)) {
         line = line.substring(line.indexOf(" ") + 1);
         String[] tokens = line.split(" ");

         try {
            return (W_Vertex)(tokens.length == 2?new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1])):(tokens.length == 3?new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])):vertex));
         } catch (NumberFormatException var6) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), var6);
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
         } catch (NumberFormatException var6) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), var6);
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
         } catch (NumberFormatException var6) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), var6);
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
      if(vertexMatcher != null) {
         vertexMatcher.reset();
      }

      vertexMatcher = vertexPattern.matcher(line);
      return vertexMatcher.matches();
   }

   private static boolean isValidVertexNormalLine(String line) {
      if(vertexNormalMatcher != null) {
         vertexNormalMatcher.reset();
      }

      vertexNormalMatcher = vertexNormalPattern.matcher(line);
      return vertexNormalMatcher.matches();
   }

   private static boolean isValidTextureCoordinateLine(String line) {
      if(textureCoordinateMatcher != null) {
         textureCoordinateMatcher.reset();
      }

      textureCoordinateMatcher = textureCoordinatePattern.matcher(line);
      return textureCoordinateMatcher.matches();
   }

   private static boolean isValidFace_V_VT_VN_Line(String line) {
      if(face_V_VT_VN_Matcher != null) {
         face_V_VT_VN_Matcher.reset();
      }

      face_V_VT_VN_Matcher = face_V_VT_VN_Pattern.matcher(line);
      return face_V_VT_VN_Matcher.matches();
   }

   private static boolean isValidFace_V_VT_Line(String line) {
      if(face_V_VT_Matcher != null) {
         face_V_VT_Matcher.reset();
      }

      face_V_VT_Matcher = face_V_VT_Pattern.matcher(line);
      return face_V_VT_Matcher.matches();
   }

   private static boolean isValidFace_V_VN_Line(String line) {
      if(face_V_VN_Matcher != null) {
         face_V_VN_Matcher.reset();
      }

      face_V_VN_Matcher = face_V_VN_Pattern.matcher(line);
      return face_V_VN_Matcher.matches();
   }

   private static boolean isValidFace_V_Line(String line) {
      if(face_V_Matcher != null) {
         face_V_Matcher.reset();
      }

      face_V_Matcher = face_V_Pattern.matcher(line);
      return face_V_Matcher.matches();
   }

   private static boolean isValidFaceLine(String line) {
      return isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(line);
   }

   private static boolean isValidGroupObjectLine(String line) {
      if(groupObjectMatcher != null) {
         groupObjectMatcher.reset();
      }

      groupObjectMatcher = groupObjectPattern.matcher(line);
      return groupObjectMatcher.matches();
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
      return this.vertices.size();
   }

   public int getFaceNum() {
      return this.getVertexNum() / 3;
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
