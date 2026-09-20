package mcheli.eval.eval.func;


public interface Function {

   long evalLong(Object result1, String result2, Long[] result3) throws Throwable;

   double evalDouble(Object result1, String result2, Double[] result3) throws Throwable;

   Object evalObject(Object result1, String result2, Object[] result3) throws Throwable;
}
