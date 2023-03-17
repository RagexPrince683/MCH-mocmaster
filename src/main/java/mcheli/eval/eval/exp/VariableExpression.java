package mcheli.eval.eval.exp;

import mcheli.eval.eval.EvalException;
import mcheli.eval.eval.lex.Lex;

public class VariableExpression extends WordExpression {

   public static AbstractExpression create(Lex lex, int prio) {
      VariableExpression exp = new VariableExpression(lex.getWord());
      exp.setPos(lex.getString(), lex.getPos());
      exp.setPriority(prio);
      exp.share = lex.getShare();
      return exp;
   }

   public VariableExpression(String str) {
      super(str);
   }

   protected VariableExpression(VariableExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new VariableExpression(this, s);
   }

   public long evalLong() {
      try {
         return super.share.var.evalLong(this.getVarValue());
      } catch (EvalException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new EvalException(2003, super.word, super.string, super.pos, var3);
      }
   }

   public double evalDouble() {
      try {
         return super.share.var.evalDouble(this.getVarValue());
      } catch (EvalException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new EvalException(2003, super.word, super.string, super.pos, var3);
      }
   }

   public Object evalObject() {
      return this.getVarValue();
   }

   protected void let(Object val, int pos) {
      String name = this.getWord();

      try {
         super.share.var.setValue(name, val);
      } catch (EvalException var5) {
         throw var5;
      } catch (Exception var6) {
         throw new EvalException(2102, name, super.string, pos, var6);
      }
   }

   private Object getVarValue() {
      String word = this.getWord();

      Object val;
      try {
         val = super.share.var.getObject(word);
      } catch (EvalException var4) {
         throw var4;
      } catch (Exception var5) {
         throw new EvalException(2101, word, super.string, super.pos, var5);
      }

      if(val == null) {
         throw new EvalException(2103, word, super.string, super.pos, (Throwable)null);
      } else {
         return val;
      }
   }

   protected Object getVariable() {
      try {
         return super.share.var.getObject(super.word);
      } catch (EvalException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new EvalException(2002, super.word, super.string, super.pos, (Throwable)null);
      }
   }
}
