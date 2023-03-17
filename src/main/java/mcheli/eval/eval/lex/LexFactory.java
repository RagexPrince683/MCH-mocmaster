package mcheli.eval.eval.lex;

import mcheli.eval.eval.exp.ShareExpValue;
import mcheli.eval.eval.rule.ShareRuleValue;

import java.util.List;

public class LexFactory {

   public Lex create(String str, List[] opeList, ShareRuleValue share, ShareExpValue exp) {
      return new Lex(str, opeList, share.paren, exp);
   }
}
