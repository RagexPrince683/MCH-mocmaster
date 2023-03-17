package mcheli.eval.eval.exp;

import mcheli.eval.eval.EvalException;
import mcheli.eval.eval.lex.Lex;
import mcheli.eval.util.CharUtil;
import mcheli.eval.util.NumberUtil;

public class StringExpression extends WordExpression {

   public static AbstractExpression create(Lex lex, int prio) {
      String str = lex.getWord();
      str = CharUtil.escapeString(str, 1, str.length() - 2);
      StringExpression exp = new StringExpression(str);
      exp.setPos(lex.getString(), lex.getPos());
      exp.setPriority(prio);
      exp.share = lex.getShare();
      return exp;
   }

   public StringExpression(String str) {
      super(str);
      this.setOperator("\"");
      this.setEndOperator("\"");
   }

   protected StringExpression(StringExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new StringExpression(this, s);
   }

   public static StringExpression create(AbstractExpression from, String word) {
      StringExpression n = new StringExpression(word);
      n.string = from.string;
      n.pos = from.pos;
      n.prio = from.prio;
      n.share = from.share;
      return n;
   }

   public long evalLong() {
      try {
         return NumberUtil.parseLong(super.word);
      } catch (Exception var4) {
         try {
            return Long.parseLong(super.word);
         } catch (Exception var3) {
            try {
               return (long)Double.parseDouble(super.word);
            } catch (Exception var2) {
               throw new EvalException(2003, super.word, super.string, super.pos, var2);
            }
         }
      }
   }

   public double evalDouble() {
      try {
         return Double.parseDouble(super.word);
      } catch (Exception var4) {
         try {
            return (double)NumberUtil.parseLong(super.word);
         } catch (Exception var3) {
            throw new EvalException(2003, super.word, super.string, super.pos, var4);
         }
      }
   }

   public Object evalObject() {
      return super.word;
   }

   public boolean equals(Object obj) {
      if(obj instanceof StringExpression) {
         StringExpression e = (StringExpression)obj;
         return super.word.equals(e.word);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return super.word.hashCode();
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(this.getOperator());
      sb.append(super.word);
      sb.append(this.getEndOperator());
      return sb.toString();
   }
}
