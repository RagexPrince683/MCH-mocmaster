package mcheli.eval.eval.exp;

import mcheli.eval.eval.EvalException;
import mcheli.eval.eval.lex.Lex;
import mcheli.eval.util.NumberUtil;

public class NumberExpression extends WordExpression {

   public static AbstractExpression create(Lex lex, int prio) {
      NumberExpression exp = new NumberExpression(lex.getWord());
      exp.setPos(lex.getString(), lex.getPos());
      exp.setPriority(prio);
      exp.share = lex.getShare();
      return exp;
   }

   public NumberExpression(String str) {
      super(str);
   }

   protected NumberExpression(NumberExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new NumberExpression(this, s);
   }

   public static NumberExpression create(AbstractExpression from, String word) {
      NumberExpression n = new NumberExpression(word);
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
      try {
         return new Long(NumberUtil.parseLong(super.word));
      } catch (Exception var4) {
         try {
            return Long.valueOf(super.word);
         } catch (Exception var3) {
            try {
               return Double.valueOf(super.word);
            } catch (Exception var2) {
               throw new EvalException(2003, super.word, super.string, super.pos, var2);
            }
         }
      }
   }
}
