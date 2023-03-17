package mcheli.eval.eval.exp;

public class CommaExpression extends Col2OpeExpression {

   public CommaExpression() {
      this.setOperator(",");
   }

   protected CommaExpression(CommaExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new CommaExpression(this, s);
   }

   public long evalLong() {
      super.expl.evalLong();
      return super.expr.evalLong();
   }

   public double evalDouble() {
      super.expl.evalDouble();
      return super.expr.evalDouble();
   }

   public Object evalObject() {
      super.expl.evalObject();
      return super.expr.evalObject();
   }

   protected String toStringLeftSpace() {
      return "";
   }
}
