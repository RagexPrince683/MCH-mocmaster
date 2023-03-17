package mcheli.eval.eval.exp;

import mcheli.eval.eval.EvalException;

import java.util.ArrayList;
import java.util.List;

public class FunctionExpression extends Col1Expression {

   protected AbstractExpression target;
   String name;


   public static AbstractExpression create(AbstractExpression x, AbstractExpression args, int prio, ShareExpValue share) {
      AbstractExpression obj;
      if(x instanceof VariableExpression) {
         obj = null;
      } else {
         if(!(x instanceof FieldExpression)) {
            throw new EvalException(1101, x.toString(), x.string, x.pos, (Throwable)null);
         }

         FieldExpression name = (FieldExpression)x;
         obj = name.expl;
         x = name.expr;
      }

      String name1 = x.getWord();
      FunctionExpression f = new FunctionExpression(obj, name1);
      f.setExpression(args);
      f.setPos(x.string, x.pos);
      f.setPriority(prio);
      f.share = share;
      return f;
   }

   public FunctionExpression() {
      this.setOperator("(");
      this.setEndOperator(")");
   }

   public FunctionExpression(AbstractExpression obj, String word) {
      this();
      this.target = obj;
      this.name = word;
   }

   protected FunctionExpression(FunctionExpression from, ShareExpValue s) {
      super(from, s);
      if(from.target != null) {
         this.target = from.target.dup(s);
      }

      this.name = from.name;
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new FunctionExpression(this, s);
   }

   public long evalLong() {
      Object obj = null;
      if(this.target != null) {
         obj = this.target.getVariable();
      }

      List args = this.evalArgsLong();

      try {
         Long[] e = new Long[args.size()];
         return super.share.func.evalLong(obj, this.name, (Long[])((Long[])args.toArray(e)));
      } catch (EvalException var4) {
         throw var4;
      } catch (Throwable var5) {
         throw new EvalException(2401, this.name, super.string, super.pos, var5);
      }
   }

   public double evalDouble() {
      Object obj = null;
      if(this.target != null) {
         obj = this.target.getVariable();
      }

      List args = this.evalArgsDouble();

      try {
         Double[] e = new Double[args.size()];
         return super.share.func.evalDouble(obj, this.name, (Double[])((Double[])args.toArray(e)));
      } catch (EvalException var4) {
         throw var4;
      } catch (Throwable var5) {
         throw new EvalException(2401, this.name, super.string, super.pos, var5);
      }
   }

   public Object evalObject() {
      Object obj = null;
      if(this.target != null) {
         obj = this.target.getVariable();
      }

      List args = this.evalArgsObject();

      try {
         Object[] e = new Object[args.size()];
         return super.share.func.evalObject(obj, this.name, args.toArray(e));
      } catch (EvalException var4) {
         throw var4;
      } catch (Throwable var5) {
         throw new EvalException(2401, this.name, super.string, super.pos, var5);
      }
   }

   private List evalArgsLong() {
      ArrayList args = new ArrayList();
      if(super.exp != null) {
         super.exp.evalArgsLong(args);
      }

      return args;
   }

   private List evalArgsDouble() {
      ArrayList args = new ArrayList();
      if(super.exp != null) {
         super.exp.evalArgsDouble(args);
      }

      return args;
   }

   private List evalArgsObject() {
      ArrayList args = new ArrayList();
      if(super.exp != null) {
         super.exp.evalArgsObject(args);
      }

      return args;
   }

   protected Object getVariable() {
      return this.evalObject();
   }

   protected long operateLong(long val) {
      throw new RuntimeException("");
   }

   protected double operateDouble(double val) {
      throw new RuntimeException("");
   }

   protected void search() {
      super.share.srch.search(this);
      if(!super.share.srch.end()) {
         if(!super.share.srch.searchFunc_begin(this)) {
            if(!super.share.srch.end()) {
               if(this.target != null) {
                  this.target.search();
                  if(super.share.srch.end()) {
                     return;
                  }
               }

               if(!super.share.srch.searchFunc_2(this)) {
                  if(!super.share.srch.end()) {
                     if(super.exp != null) {
                        super.exp.search();
                        if(super.share.srch.end()) {
                           return;
                        }
                     }

                     super.share.srch.searchFunc_end(this);
                  }
               }
            }
         }
      }
   }

   protected AbstractExpression replace() {
      if(this.target != null) {
         this.target = this.target.replace();
      }

      if(super.exp != null) {
         super.exp = super.exp.replace();
      }

      return super.share.repl.replaceFunc(this);
   }

   public boolean equals(Object obj) {
      if(!(obj instanceof FunctionExpression)) {
         return false;
      } else {
         FunctionExpression e = (FunctionExpression)obj;
         return this.name.equals(e.name) && equals(this.target, e.target) && equals(super.exp, e.exp);
      }
   }

   private static boolean equals(AbstractExpression e1, AbstractExpression e2) {
      return e1 == null?e2 == null:(e2 == null?false:e1.equals(e2));
   }

   public int hashCode() {
      int t = this.target != null?this.target.hashCode():0;
      int a = super.exp != null?super.exp.hashCode():0;
      return this.name.hashCode() ^ t ^ a * 2;
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      if(this.target != null) {
         sb.append(this.target.toString());
         sb.append('.');
      }

      sb.append(this.name);
      sb.append('(');
      if(super.exp != null) {
         sb.append(super.exp.toString());
      }

      sb.append(')');
      return sb.toString();
   }
}
