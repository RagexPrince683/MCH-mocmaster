package mcheli.eval.eval.exp;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col1Expression;
import mcheli.eval.eval.exp.ShareExpValue;

public class SignMinusExpression extends Col1Expression {

   public SignMinusExpression() {
      this.setOperator("-");
   }

   protected SignMinusExpression(SignMinusExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new SignMinusExpression(this, s);
   }

   protected long operateLong(long val) {
      return -val;
   }

   protected double operateDouble(double val) {
      return -val;
   }

   public Object evalObject() {
      return super.share.oper.signMinus(super.exp.evalObject());
   }
}
