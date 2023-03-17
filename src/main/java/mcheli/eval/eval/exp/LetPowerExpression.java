package mcheli.eval.eval.exp;

public class LetPowerExpression extends PowerExpression {

   public LetPowerExpression() {
      this.setOperator("**=");
   }

   protected LetPowerExpression(LetPowerExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new LetPowerExpression(this, s);
   }

   public long evalLong() {
      long val = super.evalLong();
      super.expl.let(val, super.pos);
      return val;
   }

   public double evalDouble() {
      double val = super.evalDouble();
      super.expl.let(val, super.pos);
      return val;
   }

   public Object evalObject() {
      Object val = super.evalObject();
      super.expl.let(val, super.pos);
      return val;
   }

   protected AbstractExpression replace() {
      super.expl = super.expl.replaceVar();
      super.expr = super.expr.replace();
      return super.share.repl.replaceLet(this);
   }
}
