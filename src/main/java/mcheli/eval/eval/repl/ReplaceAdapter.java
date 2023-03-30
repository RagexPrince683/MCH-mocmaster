package mcheli.eval.eval.repl;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col1Expression;
import mcheli.eval.eval.exp.Col2Expression;
import mcheli.eval.eval.exp.Col2OpeExpression;
import mcheli.eval.eval.exp.Col3Expression;
import mcheli.eval.eval.exp.FunctionExpression;
import mcheli.eval.eval.exp.WordExpression;
import mcheli.eval.eval.repl.Replace;

public class ReplaceAdapter implements Replace {

   public AbstractExpression replace0(WordExpression exp) {
      return exp;
   }

   public AbstractExpression replace1(Col1Expression exp) {
      return exp;
   }

   public AbstractExpression replace2(Col2Expression exp) {
      return exp;
   }

   public AbstractExpression replace2(Col2OpeExpression exp) {
      return exp;
   }

   public AbstractExpression replace3(Col3Expression exp) {
      return exp;
   }

   public AbstractExpression replaceVar0(WordExpression exp) {
      return exp;
   }

   public AbstractExpression replaceVar1(Col1Expression exp) {
      return exp;
   }

   public AbstractExpression replaceVar2(Col2Expression exp) {
      return exp;
   }

   public AbstractExpression replaceVar2(Col2OpeExpression exp) {
      return exp;
   }

   public AbstractExpression replaceVar3(Col3Expression exp) {
      return exp;
   }

   public AbstractExpression replaceFunc(FunctionExpression exp) {
      return exp;
   }

   public AbstractExpression replaceLet(Col2Expression exp) {
      return exp;
   }
}
