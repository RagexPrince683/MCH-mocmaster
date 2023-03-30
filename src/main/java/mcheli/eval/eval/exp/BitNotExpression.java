package mcheli.eval.eval.exp;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col1Expression;
import mcheli.eval.eval.exp.ShareExpValue;

public class BitNotExpression extends Col1Expression {

   public BitNotExpression() {
      this.setOperator("~");
   }

   protected BitNotExpression(BitNotExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new BitNotExpression(this, s);
   }

   protected long operateLong(long val) {
      return ~val;
   }

   protected double operateDouble(double val) {
      return (double)(~((long)val));
   }

   public Object evalObject() {
      return super.share.oper.bitNot(super.exp.evalObject());
   }
}
