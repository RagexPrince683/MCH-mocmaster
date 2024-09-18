package mcheli.eval.eval.exp;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col2Expression;
import mcheli.eval.eval.exp.ShareExpValue;

public abstract class Col2OpeExpression extends Col2Expression {

   protected Col2OpeExpression() {}

   protected Col2OpeExpression(Col2Expression from, ShareExpValue s) {
      super(from, s);
   }

   protected final long operateLong(long vl, long vr) {
      throw new RuntimeException("This function must not be called");
   }

   protected final double operateDouble(double vl, double vr) {
      throw new RuntimeException("This function must not be called");
   }

   protected final Object operateObject(Object vl, Object vr) {
      throw new RuntimeException("This function must not be called");
   }

   protected AbstractExpression replace() {
      super.expl = super.expl.replace();
      super.expr = super.expr.replace();
      return super.share.repl.replace2(this);
   }

   protected AbstractExpression replaceVar() {
      super.expl = super.expl.replaceVar();
      super.expr = super.expr.replaceVar();
      return super.share.repl.replaceVar2(this);
   }
}
