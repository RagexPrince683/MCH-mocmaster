package mcheli.eval.eval.exp;

import mcheli.eval.eval.EvalException;
import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col2OpeExpression;
import mcheli.eval.eval.exp.ShareExpValue;

public class FieldExpression extends Col2OpeExpression {

   public FieldExpression() {
      this.setOperator(".");
   }

   protected FieldExpression(FieldExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new FieldExpression(this, s);
   }

   public long evalLong() {
      try {
         return super.share.var.evalLong(this.getVariable());
      } catch (EvalException evalException) {
         throw evalException;
      } catch (Exception exception) {
         throw new EvalException(2003, this.toString(), super.string, super.pos, exception);
      }
   }

   public double evalDouble() {
      try {
         return super.share.var.evalDouble(this.getVariable());
      } catch (EvalException evalException) {
         throw evalException;
      } catch (Exception exception) {
         throw new EvalException(2003, this.toString(), super.string, super.pos, exception);
      }
   }

   public Object evalObject() {
      return this.getVariable();
   }

   protected Object getVariable() {
      Object obj = super.expl.getVariable();
      if(obj == null) {
         throw new EvalException(2104, super.expl.toString(), super.string, super.pos, (Throwable)null);
      } else {
         String word = super.expr.getWord();

         try {
            return super.share.var.getObject(obj, word);
         } catch (EvalException evalException) {
            throw evalException;
         } catch (Exception exception) {
            throw new EvalException(2301, this.toString(), super.string, super.pos, exception);
         }
      }
   }

   protected void let(Object val, int pos) {
      Object obj = super.expl.getVariable();
      if(obj == null) {
         throw new EvalException(2104, super.expl.toString(), super.string, pos, (Throwable)null);
      } else {
         String word = super.expr.getWord();

         try {
            super.share.var.setValue(obj, word, val);
         } catch (EvalException evalException) {
            throw evalException;
         } catch (Exception exception) {
            throw new EvalException(2302, this.toString(), super.string, pos, exception);
         }
      }
   }

   protected AbstractExpression replace() {
      super.expl = super.expl.replaceVar();
      return super.share.repl.replace2((Col2OpeExpression)this);
   }

   protected AbstractExpression replaceVar() {
      super.expl = super.expl.replaceVar();
      return super.share.repl.replaceVar2((Col2OpeExpression)this);
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.expl.toString());
      sb.append('.');
      sb.append(super.expr.toString());
      return sb.toString();
   }
}
