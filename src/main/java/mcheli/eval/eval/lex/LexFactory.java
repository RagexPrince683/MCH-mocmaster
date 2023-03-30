package mcheli.eval.eval.lex;

import java.util.List;
import mcheli.eval.eval.exp.ShareExpValue;
import mcheli.eval.eval.lex.Lex;
import mcheli.eval.eval.rule.ShareRuleValue;

public class LexFactory {

   public Lex create(String str, List[] opeList, ShareRuleValue share, ShareExpValue exp) {
      return new Lex(str, opeList, share.paren, exp);
   }
}
