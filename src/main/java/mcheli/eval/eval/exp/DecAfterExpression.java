package mcheli.eval.eval.exp;

public class DecAfterExpression extends Col1AfterExpression {

   public DecAfterExpression() {
      this.setOperator("--");
   }

   protected DecAfterExpression(DecAfterExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new DecAfterExpression(this, s);
   }

   protected long operateLong(long val) {
      super.exp.let(val - 1L, super.pos);
      return val;
   }

   protected double operateDouble(double val) {
      super.exp.let(val - 1.0D, super.pos);
      return val;
   }

   public Object evalObject() {
      Object val = super.exp.evalObject();
      super.exp.let(super.share.oper.inc(val, -1), super.pos);
      return val;
   }
}
