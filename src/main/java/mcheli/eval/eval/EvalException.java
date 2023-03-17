package mcheli.eval.eval;

import mcheli.eval.eval.lex.Lex;

public class EvalException extends RuntimeException {

   private static final long serialVersionUID = 4174576689426433715L;
   public static final int PARSE_NOT_FOUND_END_OP = 1001;
   public static final int PARSE_INVALID_OP = 1002;
   public static final int PARSE_INVALID_CHAR = 1003;
   public static final int PARSE_END_OF_STR = 1004;
   public static final int PARSE_STILL_EXIST = 1005;
   public static final int PARSE_NOT_FUNC = 1101;
   public static final int EXP_FORBIDDEN_CALL = 2001;
   public static final int EXP_NOT_VARIABLE = 2002;
   public static final int EXP_NOT_NUMBER = 2003;
   public static final int EXP_NOT_LET = 2004;
   public static final int EXP_NOT_VAR_VALUE = 2101;
   public static final int EXP_NOT_LET_VAR = 2102;
   public static final int EXP_NOT_DEF_VAR = 2103;
   public static final int EXP_NOT_DEF_OBJ = 2104;
   public static final int EXP_NOT_ARR_VALUE = 2201;
   public static final int EXP_NOT_LET_ARR = 2202;
   public static final int EXP_NOT_FLD_VALUE = 2301;
   public static final int EXP_NOT_LET_FIELD = 2302;
   public static final int EXP_FUNC_CALL_ERROR = 2401;
   protected int msg_code;
   protected String[] msg_opt;
   protected String string;
   protected int pos;
   protected String word;


   public EvalException(int msg, Lex lex) {
      this(msg, (String[])null, lex);
   }

   public EvalException(int msg, String[] opt, Lex lex) {
      this.pos = -1;
      this.msg_code = msg;
      this.msg_opt = opt;
      if(lex != null) {
         this.string = lex.getString();
         this.pos = lex.getPos();
         this.word = lex.getWord();
      }

   }

   public EvalException(int msg, String word, String string, int pos, Throwable e) {
      for(this.pos = -1; e != null && e.getClass() == RuntimeException.class && e.getCause() != null; e = e.getCause()) {
         ;
      }

      if(e != null) {
         super.initCause(e);
      }

      this.msg_code = msg;
      this.string = string;
      this.pos = pos;
      this.word = word;
   }

   public int getErrorCode() {
      return this.msg_code;
   }

   public String[] getOption() {
      return this.msg_opt;
   }

   public String getWord() {
      return this.word;
   }

   public String getString() {
      return this.string;
   }

   public int getPos() {
      return this.pos;
   }

   public static String getErrCodeMessage(int code) {

      return "shits fucked bruh sry";
   }

   public String getDefaultFormat(String msgFmt) {
      StringBuffer fmt = new StringBuffer(128);
      fmt.append(msgFmt);
      boolean bWord = false;
      if(this.word != null && this.word.length() > 0) {
         bWord = true;
         if(this.word.equals(this.string)) {
            bWord = false;
         }
      }

      if(bWord) {
         fmt.append(" word=%w");
      }

      if(this.pos >= 0) {
         fmt.append(" pos=%p");
      }

      if(this.string != null) {
         fmt.append(" string=%s");
      }

      if(this.getCause() != null) {
         fmt.append(" cause by %e");
      }

      return fmt.toString();
   }

   public String toString() {
      String msg = getErrCodeMessage(this.msg_code);
      String fmt = this.getDefaultFormat(msg);
      return this.toString(fmt);
   }

   public String toString(String fmt) {
      StringBuffer sb = new StringBuffer(256);
      int len = fmt.length();

      for(int i = 0; i < len; ++i) {
         char c = fmt.charAt(i);
         if(c != 37) {
            sb.append(c);
         } else {
            if(i + 1 >= len) {
               sb.append(c);
               break;
            }

            ++i;
            c = fmt.charAt(i);
            switch(c) {
            case 37:
               sb.append('%');
               break;
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
               int n = c - 48;
               if(this.msg_opt != null && n < this.msg_opt.length) {
                  sb.append(this.msg_opt[n]);
               }
               break;
            case 99:
               sb.append(this.msg_code);
               break;
            case 101:
               sb.append(this.getCause());
               break;
            case 112:
               sb.append(this.pos);
               break;
            case 115:
               sb.append(this.string);
               break;
            case 119:
               sb.append(this.word);
               break;
            default:
               sb.append('%');
               sb.append(c);
            }
         }
      }

      return sb.toString();
   }
}
