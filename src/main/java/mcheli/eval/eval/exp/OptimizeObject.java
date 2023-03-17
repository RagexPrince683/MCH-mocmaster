package mcheli.eval.eval.exp;

import mcheli.eval.eval.repl.ReplaceAdapter;

public class OptimizeObject extends ReplaceAdapter {

   protected boolean isConst(AbstractExpression x) {
      return x instanceof NumberExpression || x instanceof StringExpression || x instanceof CharExpression;
   }

   protected boolean isTrue(AbstractExpression x) {
      return x.evalLong() != 0L;
   }

   protected AbstractExpression toConst(AbstractExpression exp) {
      try {
         Object e = exp.evalObject();
         return (AbstractExpression)(e instanceof String?StringExpression.create(exp, (String)e):(e instanceof Character?CharExpression.create(exp, e.toString()):NumberExpression.create(exp, e.toString())));
      } catch (Exception var3) {
         return exp;
      }
   }

   public AbstractExpression replace0(WordExpression exp) {
      return (AbstractExpression)(exp instanceof VariableExpression?this.toConst(exp):exp);
   }

   public AbstractExpression replace1(Col1Expression exp) {
      return (AbstractExpression)(exp instanceof ParenExpression?exp.exp:(exp instanceof SignPlusExpression?exp.exp:(this.isConst(exp.exp)?this.toConst(exp):exp)));
   }

   public AbstractExpression replace2(Col2Expression exp) {
      boolean const_l = this.isConst(exp.expl);
      boolean const_r = this.isConst(exp.expr);
      return (AbstractExpression)(const_l && const_r?this.toConst(exp):exp);
   }

   public AbstractExpression replace2(Col2OpeExpression exp) {
      if(exp instanceof ArrayExpression) {
         return (AbstractExpression)(this.isConst(exp.expr)?this.toConst(exp):exp);
      } else if(exp instanceof FieldExpression) {
         return this.toConst(exp);
      } else {
         boolean const_l = this.isConst(exp.expl);
         return (AbstractExpression)(exp instanceof AndExpression?(const_l?(this.isTrue(exp.expl)?exp.expr:exp.expl):exp):(exp instanceof OrExpression?(const_l?(this.isTrue(exp.expl)?exp.expl:exp.expr):exp):(exp instanceof CommaExpression?(const_l?exp.expr:exp):exp)));
      }
   }

   public AbstractExpression replace3(Col3Expression exp) {
      return (AbstractExpression)(this.isConst(exp.exp1)?(this.isTrue(exp.exp1)?exp.exp2:exp.exp3):exp);
   }
}
