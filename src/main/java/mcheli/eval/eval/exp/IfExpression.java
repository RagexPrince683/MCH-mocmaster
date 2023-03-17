package mcheli.eval.eval.exp;

public class IfExpression extends Col3Expression {

   public IfExpression() {
      this.setOperator("?");
      this.setEndOperator(":");
   }

   protected IfExpression(IfExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new IfExpression(this, s);
   }

   public long evalLong() {
      return super.exp1.evalLong() != 0L?super.exp2.evalLong():super.exp3.evalLong();
   }

   public double evalDouble() {
      return super.exp1.evalDouble() != 0.0D?super.exp2.evalDouble():super.exp3.evalDouble();
   }

   public Object evalObject() {
      return super.share.oper.bool(super.exp1.evalObject())?super.exp2.evalObject():super.exp3.evalObject();
   }
}
