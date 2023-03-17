package mcheli.eval.eval.exp;

import mcheli.eval.eval.EvalException;

public class ArrayExpression extends Col2OpeExpression {

   public ArrayExpression() {
      this.setOperator("[");
      this.setEndOperator("]");
   }

   protected ArrayExpression(ArrayExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new ArrayExpression(this, s);
   }

   public long evalLong() {
      try {
         return super.share.var.evalLong(this.getVariable());
      } catch (EvalException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new EvalException(2201, this.toString(), super.string, super.pos, var3);
      }
   }

   public double evalDouble() {
      try {
         return super.share.var.evalDouble(this.getVariable());
      } catch (EvalException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new EvalException(2201, this.toString(), super.string, super.pos, var3);
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
         int index = (int)super.expr.evalLong();

         try {
            return super.share.var.getObject(obj, index);
         } catch (EvalException var4) {
            throw var4;
         } catch (Exception var5) {
            throw new EvalException(2201, this.toString(), super.string, super.pos, var5);
         }
      }
   }

   protected void let(Object val, int pos) {
      Object obj = super.expl.getVariable();
      if(obj == null) {
         throw new EvalException(2104, super.expl.toString(), super.string, pos, (Throwable)null);
      } else {
         int index = (int)super.expr.evalLong();

         try {
            super.share.var.setValue(obj, index, val);
         } catch (EvalException var6) {
            throw var6;
         } catch (Exception var7) {
            throw new EvalException(2202, this.toString(), super.string, pos, var7);
         }
      }
   }

   protected AbstractExpression replaceVar() {
      super.expl = super.expl.replaceVar();
      super.expr = super.expr.replace();
      return super.share.repl.replaceVar2((Col2OpeExpression)this);
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.expl.toString());
      sb.append('[');
      sb.append(super.expr.toString());
      sb.append(']');
      return sb.toString();
   }
}
