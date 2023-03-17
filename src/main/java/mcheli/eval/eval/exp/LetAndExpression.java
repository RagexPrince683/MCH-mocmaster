package mcheli.eval.eval.exp;

public final class LetAndExpression extends BitAndExpression {

   public LetAndExpression() {
      this.setOperator("&=");
   }

   protected LetAndExpression(LetAndExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new LetAndExpression(this, s);
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
