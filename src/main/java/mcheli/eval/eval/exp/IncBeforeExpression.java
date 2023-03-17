package mcheli.eval.eval.exp;

public class IncBeforeExpression extends Col1Expression {

   public IncBeforeExpression() {
      this.setOperator("++");
   }

   protected IncBeforeExpression(IncBeforeExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new IncBeforeExpression(this, s);
   }

   protected long operateLong(long val) {
      ++val;
      super.exp.let(val, super.pos);
      return val;
   }

   protected double operateDouble(double val) {
      ++val;
      super.exp.let(val, super.pos);
      return val;
   }

   public Object evalObject() {
      Object val = super.exp.evalObject();
      val = super.share.oper.inc(val, 1);
      super.exp.let(val, super.pos);
      return val;
   }

   protected AbstractExpression replace() {
      super.exp = super.exp.replaceVar();
      return super.share.repl.replaceVar1(this);
   }
}
