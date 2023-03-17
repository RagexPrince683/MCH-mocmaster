package mcheli.eval.eval.rule;

import mcheli.eval.eval.EvalException;
import mcheli.eval.eval.exp.*;
import mcheli.eval.eval.lex.Lex;

public class PrimaryRule extends AbstractRule {

   public PrimaryRule(ShareRuleValue share) {
      super(share);
   }

   public final AbstractExpression parse(Lex lex) {
      switch(lex.getType()) {
      case 2147483632:
         AbstractExpression w = VariableExpression.create(lex, super.prio);
         lex.next();
         return w;
      case 2147483633:
         AbstractExpression n = NumberExpression.create(lex, super.prio);
         lex.next();
         return n;
      case 2147483634:
         String ope = lex.getOperator();
         int pos = lex.getPos();
         if(this.isMyOperator(ope)) {
            if(ope.equals(super.share.paren.getOperator())) {
               return this.parseParen(lex, ope, pos);
            }

            return Col1Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, this.parse(lex.next()));
         }

         throw new EvalException(1002, lex);
      case 2147483635:
         AbstractExpression s = StringExpression.create(lex, super.prio);
         lex.next();
         return s;
      case 2147483636:
         AbstractExpression c = CharExpression.create(lex, super.prio);
         lex.next();
         return c;
      case 2147483637:
      case 2147483638:
      case 2147483639:
      case 2147483640:
      case 2147483641:
      case 2147483642:
      case 2147483643:
      case 2147483644:
      case 2147483645:
      case 2147483646:
      default:
         throw new EvalException(1003, lex);
      case Integer.MAX_VALUE:
         throw new EvalException(1004, lex);
      }
   }

   protected AbstractExpression parseParen(Lex lex, String ope, int pos) {
      AbstractExpression s = super.share.topRule.parse(lex.next());
      if(!lex.isOperator(super.share.paren.getEndOperator())) {
         throw new EvalException(1001, new String[]{super.share.paren.getEndOperator()}, lex);
      } else {
         lex.next();
         return Col1Expression.create(this.newExpression(ope, lex.getShare()), lex.getString(), pos, s);
      }
   }
}
