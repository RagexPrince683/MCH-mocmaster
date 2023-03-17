package mcheli.eval.eval.rule;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col2Expression;
import mcheli.eval.eval.lex.Lex;

public class Col2Rule extends AbstractRule {

   public Col2Rule(ShareRuleValue share) {
      super(share);
   }

   protected AbstractExpression parse(Lex lex) {
      AbstractExpression x = super.nextRule.parse(lex);

      while(true) {
         switch(lex.getType()) {
         case 2147483634:
            String ope = lex.getOperator();
            if(!this.isMyOperator(ope)) {
               return x;
            }

            int pos = lex.getPos();
            AbstractExpression y = super.nextRule.parse(lex.next());
            x = Col2Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, x, y);
            break;
         default:
            return x;
         }
      }
   }
}
