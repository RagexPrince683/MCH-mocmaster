package mcheli.eval.eval.exp;

public class AndExpression extends Col2OpeExpression {

   public AndExpression() {
      this.setOperator("&&");
   }

   protected AndExpression(AndExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new AndExpression(this, s);
   }

   public long evalLong() {
      long val = super.expl.evalLong();
      return val == 0L?val:super.expr.evalLong();
   }

   public double evalDouble() {
      double val = super.expl.evalDouble();
      return val == 0.0D?val:super.expr.evalDouble();
   }

   public Object evalObject() {
      Object val = super.expl.evalObject();
      return !super.share.oper.bool(val)?val:super.expr.evalObject();
   }
}
