package mcheli.eval.eval.exp;

import mcheli.eval.eval.Expression;
import mcheli.eval.eval.Rule;
import mcheli.eval.eval.func.InvokeFunction;
import mcheli.eval.eval.oper.JavaExOperator;
import mcheli.eval.eval.oper.Operator;
import mcheli.eval.eval.ref.Refactor;
import mcheli.eval.eval.repl.Replace;
import mcheli.eval.eval.srch.Search;
import mcheli.eval.eval.var.MapVariable;
import mcheli.eval.eval.var.Variable;

public class ShareExpValue extends Expression {

   public AbstractExpression paren;


   public void setAbstractExpression(AbstractExpression ae) {
      super.ae = ae;
   }

   public void initVar() {
      if(super.var == null) {
         super.var = new MapVariable();
      }

   }

   public void initOper() {
      if(super.oper == null) {
         super.oper = new JavaExOperator();
      }

   }

   public void initFunc() {
      if(super.func == null) {
         super.func = new InvokeFunction();
      }

   }

   public long evalLong() {
      this.initVar();
      this.initFunc();
      return super.ae.evalLong();
   }

   public double evalDouble() {
      this.initVar();
      this.initFunc();
      return super.ae.evalDouble();
   }

   public Object eval() {
      this.initVar();
      this.initOper();
      this.initFunc();
      return super.ae.evalObject();
   }

   public void optimizeLong(Variable var) {
      this.optimize(var, (Replace)(new OptimizeLong()));
   }

   public void optimizeDouble(Variable var) {
      this.optimize(var, (Replace)(new OptimizeDouble()));
   }

   public void optimize(Variable var, Operator oper) {
      Operator bak = super.oper;
      super.oper = oper;

      try {
         this.optimize(var, (Replace)(new OptimizeObject()));
      } finally {
         super.oper = bak;
      }

   }

   protected void optimize(Variable var, Replace repl) {
      Variable bak = super.var;
      if(var == null) {
         var = new MapVariable();
      }

      super.var = (Variable)var;
      super.repl = repl;

      try {
         super.ae = super.ae.replace();
      } finally {
         super.var = bak;
      }

   }

   public void search(Search srch) {
      if(srch == null) {
         throw new NullPointerException();
      } else {
         super.srch = srch;
         super.ae.search();
      }
   }

   public void refactorName(Refactor ref) {
      if(ref == null) {
         throw new NullPointerException();
      } else {
         super.srch = new Search4RefactorName(ref);
         super.ae.search();
      }
   }

   public void refactorFunc(Refactor ref, Rule rule) {
      if(ref == null) {
         throw new NullPointerException();
      } else {
         super.repl = new Replace4RefactorGetter(ref, rule);
         super.ae.replace();
      }
   }

   public boolean same(Expression obj) {
      if(!(obj instanceof ShareExpValue)) {
         return false;
      } else {
         AbstractExpression p = ((ShareExpValue)obj).paren;
         return this.paren.same(p) && super.same(obj);
      }
   }

   public Expression dup() {
      ShareExpValue n = new ShareExpValue();
      n.ae = super.ae.dup(n);
      n.paren = this.paren.dup(n);
      return n;
   }
}
