package mcheli.eval.eval.exp;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.NumberExpression;
import mcheli.eval.eval.exp.OptimizeObject;

public class OptimizeDouble extends OptimizeObject {

   protected boolean isTrue(AbstractExpression x) {
      return x.evalDouble() != 0.0D;
   }

   protected AbstractExpression toConst(AbstractExpression exp) {
      try {
         double e = exp.evalDouble();
         return NumberExpression.create(exp, Double.toString(e));
      } catch (Exception var4) {
         return exp;
      }
   }
}
