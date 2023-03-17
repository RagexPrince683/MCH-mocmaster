package mcheli.eval.eval.exp;

import java.util.List;

public class FuncArgExpression extends Col2OpeExpression {

   public FuncArgExpression() {
      this.setOperator(",");
   }

   protected FuncArgExpression(FuncArgExpression from, ShareExpValue s) {
      super(from, s);
   }

   public AbstractExpression dup(ShareExpValue s) {
      return new FuncArgExpression(this, s);
   }

   protected void evalArgsLong(List args) {
      super.expl.evalArgsLong(args);
      super.expr.evalArgsLong(args);
   }

   protected void evalArgsDouble(List args) {
      super.expl.evalArgsDouble(args);
      super.expr.evalArgsDouble(args);
   }

   protected void evalArgsObject(List args) {
      super.expl.evalArgsObject(args);
      super.expr.evalArgsObject(args);
   }

   protected String toStringLeftSpace() {
      return "";
   }
}
