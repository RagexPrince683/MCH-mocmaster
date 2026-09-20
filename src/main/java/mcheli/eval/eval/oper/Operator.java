package mcheli.eval.eval.oper;


public interface Operator {

   Object power(Object result1, Object result2);

   Object signPlus(Object result1);

   Object signMinus(Object result1);

   Object plus(Object result1, Object result2);

   Object minus(Object result1, Object result2);

   Object mult(Object result1, Object result2);

   Object div(Object result1, Object result2);

   Object mod(Object result1, Object result2);

   Object bitNot(Object result1);

   Object shiftLeft(Object result1, Object result2);

   Object shiftRight(Object result1, Object result2);

   Object shiftRightLogical(Object result1, Object result2);

   Object bitAnd(Object result1, Object result2);

   Object bitOr(Object result1, Object result2);

   Object bitXor(Object result1, Object result2);

   Object not(Object result1);

   Object equal(Object result1, Object result2);

   Object notEqual(Object result1, Object result2);

   Object lessThan(Object result1, Object result2);

   Object lessEqual(Object result1, Object result2);

   Object greaterThan(Object result1, Object result2);

   Object greaterEqual(Object result1, Object result2);

   boolean bool(Object result1);

   Object inc(Object result1, int result2);
}
