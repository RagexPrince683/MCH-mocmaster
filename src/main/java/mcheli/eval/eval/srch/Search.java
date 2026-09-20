package mcheli.eval.eval.srch;

import mcheli.eval.eval.exp.AbstractExpression;
import mcheli.eval.eval.exp.Col1Expression;
import mcheli.eval.eval.exp.Col2Expression;
import mcheli.eval.eval.exp.Col3Expression;
import mcheli.eval.eval.exp.FunctionExpression;
import mcheli.eval.eval.exp.WordExpression;

public interface Search {

   boolean end();

   void search(AbstractExpression result1);

   void search0(WordExpression result1);

   boolean search1_begin(Col1Expression result1);

   void search1_end(Col1Expression result1);

   boolean search2_begin(Col2Expression result1);

   boolean search2_2(Col2Expression result1);

   void search2_end(Col2Expression result1);

   boolean search3_begin(Col3Expression result1);

   boolean search3_2(Col3Expression result1);

   boolean search3_3(Col3Expression result1);

   void search3_end(Col3Expression result1);

   boolean searchFunc_begin(FunctionExpression result1);

   boolean searchFunc_2(FunctionExpression result1);

   void searchFunc_end(FunctionExpression result1);
}
