package mcheli.eval.eval.exp;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col2Expression;
import mcheli.eval.eval.exp.ShareExpValue;

public class ShiftRightLogicalExpression extends Col2Expression {

   public ShiftRightLogicalExpression() {
      this.setOperator(">>>");
   }

   protected ShiftRightLogicalExpression(ShiftRightLogicalExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new ShiftRightLogicalExpression(this, s);
   }

   protected long operateLong(long vl, long vr) {
      return vl >>> (int)vr;
   }

   protected double operateDouble(double vl, double vr) {
      if(vl < 0.0D) {
         vl = -vl;
      }

      return vl / Math.pow(2.0D, vr);
   }

   protected Object operateObject(Object vl, Object vr) {
      return super.share.oper.shiftRightLogical(vl, vr);
   }
}
