package mcheli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MCH_InputFile {

   public File file = null;
   public BufferedReader br = null;


   public boolean open(String path) {
      this.close();
      this.file = new File(path);
      String filePath = this.file.getAbsolutePath();

      try {
         this.br = new BufferedReader(new FileReader(this.file));
         return true;
      } catch (FileNotFoundException var4) {
         MCH_Lib.DbgLog(true, "FILE open failed MCH_InputFile.open:" + filePath, new Object[0]);
         var4.printStackTrace();
         return false;
      }
   }

   public boolean openUTF8(File file) {
      return this.openUTF8(file.getPath());
   }

   public boolean openUTF8(String path) {
      this.close();
      this.file = new File(path);

      try {
         this.br = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), "UTF-8"));
         return true;
      } catch (Exception var3) {
         var3.printStackTrace();
         return false;
      }
   }

   /**
    * Opens a classpath resource for reading (UTF-8).
    * The resourcePath should be like "/assets/mcheli/helicopters/ah-64.txt"
    * or "assets/mcheli/helicopters/ah-64.txt" (leading / is optional).
    */
   public boolean openClasspath(String resourcePath) {
      this.close();
      this.file = null;

      try {
         this.br = MCH_ResourceHelper.openResource(resourcePath);
         return this.br != null;
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }

   public String readLine() {
      try {
         return this.br != null?this.br.readLine():null;
      } catch (IOException var2) {
         return null;
      }
   }

   public void close() {
      try {
         if(this.br != null) {
            this.br.close();
         }
      } catch (IOException var2) {
         ;
      }

      this.br = null;
   }
}
