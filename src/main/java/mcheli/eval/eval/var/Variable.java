package mcheli.eval.eval.var;


public interface Variable {

   void setValue(Object result1, Object result2);

   Object getObject(Object result1);

   long evalLong(Object result1);

   double evalDouble(Object result1);

   Object getObject(Object result1, int result2);

   void setValue(Object result1, int result2, Object result3);

   Object getObject(Object result1, String result2);

   void setValue(Object result1, String result2, Object result3);
}
