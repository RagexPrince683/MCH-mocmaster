package mcheli.eval.util;


public class NumberUtil {

   public static long parseLong(String str) {
      if(str == null) {
         return 0L;
      } else {
         str = str.trim();
         int len = str.length();
         if(len <= 0) {
            return 0L;
         } else {
            switch(str.charAt(len - 1)) {
            case 46:
            case 76:
            case 108:
               --len;
            default:
               if(len >= 3 && str.charAt(0) == 48) {
                  switch(str.charAt(1)) {
                  case 66:
                  case 98:
                     return parseLongBin(str, 2, len - 2);
                  case 79:
                  case 111:
                     return parseLongOct(str, 2, len - 2);
                  case 88:
                  case 120:
                     return parseLongHex(str, 2, len - 2);
                  }
               }

               return parseLongDec(str, 0, len);
            }
         }
      }
   }

   public static long parseLongBin(String str) {
      return str == null?0L:parseLongBin(str, 0, str.length());
   }

   public static long parseLongBin(String str, int pos, int len) {
      long ret = 0L;
      int i = 0;

      while(i < len) {
         ret *= 2L;
         char c = str.charAt(pos++);
         switch(c) {
         case 49:
            ++ret;
         case 48:
            ++i;
            break;
         default:
            throw new NumberFormatException(str.substring(pos, len));
         }
      }

      return ret;
   }

   public static long parseLongOct(String str) {
      return str == null?0L:parseLongOct(str, 0, str.length());
   }

   public static long parseLongOct(String str, int pos, int len) {
      long ret = 0L;
      int i = 0;

      while(i < len) {
         ret *= 8L;
         char c = str.charAt(pos++);
         switch(c) {
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
            ret += (long)(c - 48);
         case 48:
            ++i;
            break;
         default:
            throw new NumberFormatException(str.substring(pos, len));
         }
      }

      return ret;
   }

   public static long parseLongDec(String str) {
      return str == null?0L:parseLongDec(str, 0, str.length());
   }

   public static long parseLongDec(String str, int pos, int len) {
      long ret = 0L;
      int i = 0;

      while(i < len) {
         ret *= 10L;
         char c = str.charAt(pos++);
         switch(c) {
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
            ret += (long)(c - 48);
         case 48:
            ++i;
            break;
         default:
            throw new NumberFormatException(str.substring(pos, len));
         }
      }

      return ret;
   }

   public static long parseLongHex(String str) {
      return str == null?0L:parseLongHex(str, 0, str.length());
   }

   public static long parseLongHex(String str, int pos, int len) {
      long ret = 0L;

      for(int i = 0; i < len; ++i) {
         ret *= 16L;
         char c = str.charAt(pos++);
         switch(c) {
         case 48:
            break;
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
            ret += (long)(c - 48);
            break;
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 64:
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 76:
         case 77:
         case 78:
         case 79:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         case 85:
         case 86:
         case 87:
         case 88:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         default:
            throw new NumberFormatException(str.substring(pos, len));
         case 65:
         case 66:
         case 67:
         case 68:
         case 69:
         case 70:
            ret += (long)(c - 65 + 10);
            break;
         case 97:
         case 98:
         case 99:
         case 100:
         case 101:
         case 102:
            ret += (long)(c - 97 + 10);
         }
      }

      return ret;
   }
}
