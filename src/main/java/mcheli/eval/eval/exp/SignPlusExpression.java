package mcheli.eval.eval.exp;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col1Expression;
import mcheli.eval.eval.exp.ShareExpValue;

public class SignPlusExpression extends Col1Expression {

   public SignPlusExpression() {
      this.setOperator("+");
   }

   protected SignPlusExpression(SignPlusExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new SignPlusExpression(this, s);
   }

   protected long operateLong(long val) {
      return val;
   }

   protected double operateDouble(double val) {
      return val;
   }

   public Object evalObject() {
      return super.share.oper.signPlus(super.exp.evalObject());
   }
}
