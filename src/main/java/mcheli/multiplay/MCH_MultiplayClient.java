package mcheli.multiplay;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.CoreModManager;
import mcheli.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

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
      } catch (Exception var8) {
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
         String arr$ = mc[files];
         MCH_Lib.DbgLog(true, "java.class.path=" + arr$, new Object[0]);
         if(arr$.length() > 1) {
            File len$ = new File(arr$);
            if(len$.getAbsolutePath().toLowerCase().indexOf("versions") >= 0) {
               modList.add(EnumChatFormatting.AQUA + "# Client class=" + len$.getName() + " : file size= " + len$.length());
            }
         }
      }

      modList.add(EnumChatFormatting.YELLOW + "=== ActiveModList ===");
      Iterator var20 = Loader.instance().getActiveModList().iterator();

      while(var20.hasNext()) {
         ModContainer var21 = (ModContainer)var20.next();
         modList.add("" + var21 + "  [" + var21.getModId() + "]  " + var21.getName() + "[" + var21.getDisplayVersion() + "]  " + var21.getSource().getName());
      }

      String var22;
      if(CoreModManager.getAccessTransformers().size() > 0) {
         modList.add(EnumChatFormatting.YELLOW + "=== AccessTransformers ===");
         var20 = CoreModManager.getAccessTransformers().iterator();

         while(var20.hasNext()) {
            var22 = (String)var20.next();
            modList.add(var22);
         }
      }

      if(CoreModManager.getLoadedCoremods().size() > 0) {
         modList.add(EnumChatFormatting.YELLOW + "=== LoadedCoremods ===");
         var20 = CoreModManager.getLoadedCoremods().iterator();

         while(var20.hasNext()) {
            var22 = (String)var20.next();
            modList.add(var22);
         }
      }

      if(CoreModManager.getReparseableCoremods().size() > 0) {
         modList.add(EnumChatFormatting.YELLOW + "=== ReparseableCoremods ===");
         var20 = CoreModManager.getReparseableCoremods().iterator();

         while(var20.hasNext()) {
            var22 = (String)var20.next();
            modList.add(var22);
         }
      }

      Minecraft var23 = Minecraft.getMinecraft();
      MCH_FileSearch var24 = new MCH_FileSearch();
      File[] var25 = var24.listFiles((new File(var23.mcDataDir, "mods")).getAbsolutePath(), "*.jar");
      modList.add(EnumChatFormatting.YELLOW + "=== Manifest ===");
      File[] var26 = var25;
      int var27 = var25.length;

      int i$;
      File file;
      String e;
      JarFile jarFile;
      Enumeration jarEntries;
      String litemod_json;
      ZipEntry zipEntry;
      for(i$ = 0; i$ < var27; ++i$) {
         file = var26[i$];

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
         } catch (Exception var19) {
            modList.add(file.getName() + " : Read Manifest failed.");
         }
      }

      var24 = new MCH_FileSearch();
      var25 = var24.listFiles((new File(var23.mcDataDir, "mods")).getAbsolutePath(), "*.litemod");
      modList.add(EnumChatFormatting.LIGHT_PURPLE + "=== LiteLoader ===");
      var26 = var25;
      var27 = var25.length;

      for(i$ = 0; i$ < var27; ++i$) {
         file = var26[i$];

         try {
            e = file.getCanonicalPath();
            jarFile = new JarFile(e);
            jarEntries = jarFile.entries();
            litemod_json = "";

            while(jarEntries.hasMoreElements()) {
               zipEntry = (ZipEntry)jarEntries.nextElement();
               String var28 = zipEntry.getName().toLowerCase();
               if(!zipEntry.isDirectory()) {
                  if(!var28.equals("litemod.json")) {
                     int var30 = var28.lastIndexOf("/");
                     if(var30 >= 0) {
                        var28 = var28.substring(var30 + 1);
                     }

                     if(var28.indexOf("litemod") >= 0 && var28.endsWith("class")) {
                        var28 = zipEntry.getName();
                        if(var30 >= 0) {
                           var28 = var28.substring(var30 + 1);
                        }

                        litemod_json = litemod_json + " [" + var28 + "]";
                     }
                  } else {
                     InputStream var29 = jarFile.getInputStream(zipEntry);
                     BufferedReader var31 = new BufferedReader(new InputStreamReader(var29));

                     for(String line = var31.readLine(); line != null; line = var31.readLine()) {
                        line = line.replace(" ", "").trim();
                        if(line.toLowerCase().indexOf("name") >= 0) {
                           litemod_json = litemod_json + " [" + line + "]";
                           break;
                        }
                     }

                     var29.close();
                  }
               }
            }

            jarFile.close();
            if(!litemod_json.isEmpty()) {
               modList.add(file.getName() + litemod_json);
            }
         } catch (Exception var18) {
            modList.add(file.getName() + " : Read LiteLoader failed.");
         }
      }

   }

   public static void sendModsInfo(String playerName, int id) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.DebugLog) {
         modList.clear();
         readModList(playerName);
      }

      MCH_PacketModList.send(modList, id);
   }

}
