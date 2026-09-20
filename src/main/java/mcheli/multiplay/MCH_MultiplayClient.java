package mcheli.multiplay;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.CoreModManager;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;
import mcheli.MCH_Config;
import mcheli.MCH_FileSearch;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_OStream;
import mcheli.multiplay.MCH_PacketLargeData;
import mcheli.multiplay.MCH_PacketModList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class MCH_MultiplayClient {

   private static IntBuffer pixelBuffer;
   private static int[] pixelValues;
   private static MCH_OStream dataOutputStream;
   private static List modList = new ArrayList();


   public static void startSendImageData() {
      Minecraft mc = Minecraft.getMinecraft();
      sendScreenShot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
   }

   public static void sendScreenShot(int displayWidth, int displayHeight, Framebuffer framebufferMc) {
      //you WILL give me your screenshots and you will LIKE IT
      try {
         if(OpenGlHelper.isFramebufferEnabled()) {
            displayWidth = framebufferMc.framebufferTextureWidth;
            displayHeight = framebufferMc.framebufferTextureHeight;
         }

         int exception = displayWidth * displayHeight;
         if(pixelBuffer == null || pixelBuffer.capacity() < exception) {
            pixelBuffer = BufferUtils.createIntBuffer(exception);
            pixelValues = new int[exception];
         }

         GL11.glPixelStorei(3333, 1);
         GL11.glPixelStorei(3317, 1);
         pixelBuffer.clear();
         if(OpenGlHelper.isFramebufferEnabled()) {
            GL11.glBindTexture(3553, framebufferMc.framebufferTexture);
            GL11.glGetTexImage(3553, 0, '\u80e1', '\u8367', pixelBuffer);
         } else {
            GL11.glReadPixels(0, 0, displayWidth, displayHeight, '\u80e1', '\u8367', pixelBuffer);
         }

         pixelBuffer.get(pixelValues);
         TextureUtil.func_147953_a(pixelValues, displayWidth, displayHeight);
         BufferedImage bufferedimage = null;
         if(OpenGlHelper.isFramebufferEnabled()) {
            bufferedimage = new BufferedImage(framebufferMc.framebufferWidth, framebufferMc.framebufferHeight, 1);
            int l = framebufferMc.framebufferTextureHeight - framebufferMc.framebufferHeight;

            for(int i1 = l; i1 < framebufferMc.framebufferTextureHeight; ++i1) {
               for(int j1 = 0; j1 < framebufferMc.framebufferWidth; ++j1) {
                  bufferedimage.setRGB(j1, i1 - l, pixelValues[i1 * framebufferMc.framebufferTextureWidth + j1]);
               }
            }
         } else {
            bufferedimage = new BufferedImage(displayWidth, displayHeight, 1);
            bufferedimage.setRGB(0, 0, displayWidth, displayHeight, pixelValues, 0, displayWidth);
         }

         dataOutputStream = new MCH_OStream();
         ImageIO.write(bufferedimage, "png", dataOutputStream);
      } catch (Exception exception2) {
         ;
      }

   }

   public static void readImageData(DataOutputStream dos) throws IOException {
      dataOutputStream.write(dos);
   }

   public static void sendImageData() {
      if(dataOutputStream != null) {
         MCH_PacketLargeData.send();
         if(dataOutputStream.isDataEnd()) {
            dataOutputStream = null;
         }
      }

   }

   public static double getPerData() {
      return dataOutputStream == null?-1.0D:(double)dataOutputStream.index / (double)dataOutputStream.size();
   }

   public static void readModList(String playerName) {
      modList = new ArrayList();
      modList.add(EnumChatFormatting.RED + "###### " + playerName + " ######");
      String[] classFileNameList = System.getProperty("java.class.path").split(File.pathSeparator);
      String[] mc = classFileNameList;
      int search = classFileNameList.length;

      for(int files = 0; files < search; ++files) {
         String iteratedValues = mc[files];
         MCH_Lib.DbgLog(true, "java.class.path=" + iteratedValues, new Object[0]);
         if(iteratedValues.length() > 1) {
            File iteratedValueCount = new File(iteratedValues);
            if(iteratedValueCount.getAbsolutePath().toLowerCase().indexOf("versions") >= 0) {
               modList.add(EnumChatFormatting.AQUA + "# Client class=" + iteratedValueCount.getName() + " : file size= " + iteratedValueCount.length());
            }
         }
      }

      modList.add(EnumChatFormatting.YELLOW + "=== ActiveModList ===");
      Iterator iterator2 = Loader.instance().getActiveModList().iterator();

      while(iterator2.hasNext()) {
         ModContainer modContainer = (ModContainer)iterator2.next();
         modList.add("" + modContainer + "  [" + modContainer.getModId() + "]  " + modContainer.getName() + "[" + modContainer.getDisplayVersion() + "]  " + modContainer.getSource().getName());
      }

      String result;
      if(CoreModManager.getAccessTransformers().size() > 0) {
         modList.add(EnumChatFormatting.YELLOW + "=== AccessTransformers ===");
         iterator2 = CoreModManager.getAccessTransformers().iterator();

         while(iterator2.hasNext()) {
            result = (String)iterator2.next();
            modList.add(result);
         }
      }

      if(CoreModManager.getLoadedCoremods().size() > 0) {
         modList.add(EnumChatFormatting.YELLOW + "=== LoadedCoremods ===");
         iterator2 = CoreModManager.getLoadedCoremods().iterator();

         while(iterator2.hasNext()) {
            result = (String)iterator2.next();
            modList.add(result);
         }
      }

      if(CoreModManager.getReparseableCoremods().size() > 0) {
         modList.add(EnumChatFormatting.YELLOW + "=== ReparseableCoremods ===");
         iterator2 = CoreModManager.getReparseableCoremods().iterator();

         while(iterator2.hasNext()) {
            result = (String)iterator2.next();
            modList.add(result);
         }
      }

      Minecraft result2 = Minecraft.getMinecraft();
      MCH_FileSearch fileSearch = new MCH_FileSearch();
      File[] file2 = fileSearch.listFiles((new File(result2.mcDataDir, "mods")).getAbsolutePath(), "*.jar");
      modList.add(EnumChatFormatting.YELLOW + "=== Manifest ===");
      File[] file3 = file2;
      int index2 = file2.length;

      int iteratedValueIndex;
      File file;
      String e;
      JarFile jarFile;
      Enumeration jarEntries;
      String litemod_json;
      ZipEntry zipEntry;
      for(iteratedValueIndex = 0; iteratedValueIndex < index2; ++iteratedValueIndex) {
         file = file3[iteratedValueIndex];

         try {
            e = file.getCanonicalPath();
            jarFile = new JarFile(e);
            jarEntries = jarFile.entries();
            litemod_json = "";

            while(jarEntries.hasMoreElements()) {
               zipEntry = (ZipEntry)jarEntries.nextElement();
               if(zipEntry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF") && !zipEntry.isDirectory()) {
                  InputStream fname = jarFile.getInputStream(zipEntry);
                  BufferedReader index = new BufferedReader(new InputStreamReader(fname));

                  for(String br = index.readLine(); br != null; br = index.readLine()) {
                     br = br.replace(" ", "").trim();
                     if(!br.isEmpty()) {
                        litemod_json = litemod_json + " [" + br + "]";
                     }
                  }

                  fname.close();
                  break;
               }
            }

            jarFile.close();
            if(!litemod_json.isEmpty()) {
               modList.add(file.getName() + litemod_json);
            }
         } catch (Exception exception) {
            modList.add(file.getName() + " : Read Manifest failed.");
         }
      }

      fileSearch = new MCH_FileSearch();
      file2 = fileSearch.listFiles((new File(result2.mcDataDir, "mods")).getAbsolutePath(), "*.litemod");
      modList.add(EnumChatFormatting.LIGHT_PURPLE + "=== LiteLoader ===");
      file3 = file2;
      index2 = file2.length;

      for(iteratedValueIndex = 0; iteratedValueIndex < index2; ++iteratedValueIndex) {
         file = file3[iteratedValueIndex];

         try {
            e = file.getCanonicalPath();
            jarFile = new JarFile(e);
            jarEntries = jarFile.entries();
            litemod_json = "";

            while(jarEntries.hasMoreElements()) {
               zipEntry = (ZipEntry)jarEntries.nextElement();
               String name = zipEntry.getName().toLowerCase();
               if(!zipEntry.isDirectory()) {
                  if(!name.equals("litemod.json")) {
                     int result3 = name.lastIndexOf("/");
                     if(result3 >= 0) {
                        name = name.substring(result3 + 1);
                     }

                     if(name.indexOf("litemod") >= 0 && name.endsWith("class")) {
                        name = zipEntry.getName();
                        if(result3 >= 0) {
                           name = name.substring(result3 + 1);
                        }

                        litemod_json = litemod_json + " [" + name + "]";
                     }
                  } else {
                     InputStream inputStream = jarFile.getInputStream(zipEntry);
                     BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                     for(String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                        line = line.replace(" ", "").trim();
                        if(line.toLowerCase().indexOf("name") >= 0) {
                           litemod_json = litemod_json + " [" + line + "]";
                           break;
                        }
                     }

                     inputStream.close();
                  }
               }
            }

            jarFile.close();
            if(!litemod_json.isEmpty()) {
               modList.add(file.getName() + litemod_json);
            }
         } catch (Exception exception2) {
            modList.add(file.getName() + " : Read LiteLoader failed.");
         }
      }

   }

   public static void sendModsInfo(String playerName, int id) {
      MCH_Config configuration = MCH_MOD.config;
      if(MCH_Config.EnableMCHLibDebugLog.prmBool) {
         modList.clear();
         readModList(playerName);
      }

      MCH_PacketModList.send(modList, id);
   }

}
