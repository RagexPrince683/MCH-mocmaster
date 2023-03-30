package mcheli.eval.eval.exp;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.NumberExpression;
import mcheli.eval.eval.exp.OptimizeObject;

public class OptimizeLong extends OptimizeObject {

   protected boolean isTrue(AbstractExpression x) {
      return x.evalLong() != 0L;
   }

   protected AbstractExpression toConst(AbstractExpression exp) {
      try {
         long e = exp.evalLong();
         return NumberExpression.create(exp, Long.toString(e));
      } catch (Exception var4) {
         return exp;
      }
   }
}
