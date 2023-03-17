package mcheli.eval.util;


public class CharUtil {

   public static String escapeString(String str) {
      return escapeString(str, 0, str.length());
   }

   public static String escapeString(String str, int pos, int len) {
      StringBuffer sb = new StringBuffer(len);
      int end_pos = pos + len;

      for(int[] ret = new int[1]; pos < end_pos; pos += ret[0]) {
         char c = escapeChar(str, pos, end_pos, ret);
         if(ret[0] <= 0) {
            break;
         }

         sb.append(c);
      }

      return sb.toString();
   }

   public static char escapeChar(String str, int pos, int end_pos, int[] ret) {
      if(pos >= end_pos) {
         ret[0] = 0;
         return '\u0000';
      } else {
         char c = str.charAt(pos);
         if(c != 92) {
            ret[0] = 1;
            return c;
         } else {
            ++pos;
            if(pos >= end_pos) {
               ret[0] = 1;
               return c;
            } else {
               ret[0] = 2;
               c = str.charAt(pos);
               long code;
               int i;
               switch(c) {
               case 48:
               case 49:
               case 50:
               case 51:
               case 52:
               case 53:
               case 54:
               case 55:
                  code = (long)(c - 48);

                  for(i = 1; i < 3; ++i) {
                     ++pos;
                     if(pos >= end_pos) {
                        break;
                     }

                     c = str.charAt(pos);
                     if(c < 48 || c > 55) {
                        break;
                     }

                     ++ret[0];
                     code *= 8L;
                     code += (long)(c - 48);
                  }

                  return (char)((int)code);
               case 98:
                  return '\b';
               case 102:
                  return '\f';
               case 110:
                  return '\n';
               case 114:
                  return '\r';
               case 116:
                  return '\t';
               case 117:
                  code = 0L;

                  for(i = 0; i < 4; ++i) {
                     ++pos;
                     if(pos >= end_pos) {
                        break;
                     }

                     c = str.charAt(pos);
                     if(48 <= c && c <= 57) {
                        ++ret[0];
                        code *= 16L;
                        code += (long)(c - 48);
                     } else if(97 <= c && c <= 102) {
                        ++ret[0];
                        code *= 16L;
                        code += (long)(c - 97 + 10);
                     } else {
                        if(65 > c || c > 70) {
                           break;
                        }

                        ++ret[0];
                        code *= 16L;
                        code += (long)(c - 65 + 10);
                     }
                  }

                  return (char)((int)code);
               default:
                  return c;
               }
            }
         }
      }
   }
}
