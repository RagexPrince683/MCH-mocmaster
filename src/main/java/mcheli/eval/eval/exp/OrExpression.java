package mcheli.eval.eval.exp;

public class OrExpression extends Col2OpeExpression {

   public OrExpression() {
      this.setOperator("||");
   }

   protected OrExpression(OrExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new OrExpression(this, s);
   }

   public long evalLong() {
      long val = super.expl.evalLong();
      return val != 0L?val:super.expr.evalLong();
   }

   public double evalDouble() {
      double val = super.expl.evalDouble();
      return val != 0.0D?val:super.expr.evalDouble();
   }

   public Object evalObject() {
      Object val = super.expl.evalObject();
      return super.share.oper.bool(val)?val:super.expr.evalObject();
   }
}
