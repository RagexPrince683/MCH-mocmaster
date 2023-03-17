package mcheli.eval.eval.exp;

public class DecBeforeExpression extends Col1Expression {

   public DecBeforeExpression() {
      this.setOperator("--");
   }

   protected DecBeforeExpression(DecBeforeExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new DecBeforeExpression(this, s);
   }

   protected long operateLong(long val) {
      --val;
      super.exp.let(val, super.pos);
      return val;
   }

   protected double operateDouble(double val) {
      --val;
      super.exp.let(val, super.pos);
      return val;
   }

   public Object evalObject() {
      Object val = super.exp.evalObject();
      val = super.share.oper.inc(val, -1);
      super.exp.let(val, super.pos);
      return val;
   }

   protected AbstractExpression replace() {
      super.exp = super.exp.replaceVar();
      return super.share.repl.replaceVar1(this);
   }
}
