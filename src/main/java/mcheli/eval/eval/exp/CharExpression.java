package mcheli.eval.eval.exp;

import mcheli.eval.eval.EvalException;
import mcheli.eval.eval.lex.Lex;
import mcheli.eval.util.CharUtil;

public class CharExpression extends WordExpression {

   public static AbstractExpression create(Lex lex, int prio) {
      String str = lex.getWord();
      str = CharUtil.escapeString(str, 1, str.length() - 2);
      CharExpression exp = new CharExpression(str);
      exp.setPos(lex.getString(), lex.getPos());
      exp.setPriority(prio);
      exp.share = lex.getShare();
      return exp;
   }

   public CharExpression(String str) {
      super(str);
      this.setOperator("\'");
      this.setEndOperator("\'");
   }

   protected CharExpression(CharExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new CharExpression(this, s);
   }

   public static CharExpression create(AbstractExpression from, String word) {
      CharExpression n = new CharExpression(word);
      n.string = from.string;
      n.pos = from.pos;
      n.prio = from.prio;
      n.share = from.share;
      return n;
   }

   public long evalLong() {
      try {
         return (long)super.word.charAt(0);
      } catch (Exception var2) {
         throw new EvalException(2003, super.word, super.string, super.pos, var2);
      }
   }

   public double evalDouble() {
      try {
         return (double)super.word.charAt(0);
      } catch (Exception var2) {
         throw new EvalException(2003, super.word, super.string, super.pos, var2);
      }
   }

   public Object evalObject() {
      return new Character(super.word.charAt(0));
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(this.getOperator());
      sb.append(super.word);
      sb.append(this.getEndOperator());
      return sb.toString();
   }
}
