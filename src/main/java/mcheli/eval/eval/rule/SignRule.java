package mcheli.eval.eval.rule;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col1Expression;
import mcheli.eval.eval.lex.Lex;

public class SignRule extends AbstractRule {

   public SignRule(ShareRuleValue share) {
      super(share);
   }

   public AbstractExpression parse(Lex lex) {
      switch(lex.getType()) {
      case 2147483634:
         String ope = lex.getOperator();
         if(this.isMyOperator(ope)) {
            int pos = lex.getPos();
            return Col1Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, this.parse(lex.next()));
         }

         return super.nextRule.parse(lex);
      default:
         return super.nextRule.parse(lex);
      }
   }
}
