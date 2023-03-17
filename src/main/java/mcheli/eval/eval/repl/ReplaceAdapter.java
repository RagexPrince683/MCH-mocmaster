package mcheli.eval.eval.repl;

import mcheli.eval.eval.exp.*;

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
