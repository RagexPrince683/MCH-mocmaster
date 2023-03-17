package mcheli;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_OStream extends ByteArrayOutputStream {

   public int index = 0;
   public static final int SIZE = 30720;


   public void write(DataOutputStream dos) {
      try {
         int e;
         if(this.index + 30720 <= this.size()) {
            e = 30720;
         } else {
            e = this.size() - this.index;
         }

         dos.writeInt(this.index);
         dos.writeInt(e);
         dos.writeInt(this.size());
         dos.write(this.buf, this.index, e);
         this.index += e;
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public boolean isDataEnd() {
      return this.index >= this.size();
   }
}
