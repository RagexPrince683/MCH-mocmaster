package mcheli.eval.eval.func;

import java.lang.reflect.Method;

public class MathFunction implements Function {

   public long evalLong(Object object, String name, Long[] args) throws Throwable {
      Class[] types = new Class[args.length];

      for(int m = 0; m < types.length; ++m) {
         types[m] = Long.TYPE;
      }

      Method var7 = Math.class.getMethod(name, types);
      Object ret = var7.invoke((Object)null, (Object[])args);
      return ((Long)ret).longValue();
   }

   public double evalDouble(Object object, String name, Double[] args) throws Throwable {
      Class[] types = new Class[args.length];

      for(int m = 0; m < types.length; ++m) {
         types[m] = Double.TYPE;
      }

      Method var7 = Math.class.getMethod(name, types);
      Object ret = var7.invoke((Object)null, (Object[])args);
      return ((Double)ret).doubleValue();
   }

   public Object evalObject(Object object, String name, Object[] args) throws Throwable {
      Class[] types = new Class[args.length];

      for(int m = 0; m < types.length; ++m) {
         Class c = args[m].getClass();
         if(Double.class.isAssignableFrom(c)) {
            c = Double.TYPE;
         } else if(Float.class.isAssignableFrom(c)) {
            c = Float.TYPE;
         } else if(Integer.class.isAssignableFrom(c)) {
            c = Integer.TYPE;
         } else if(Number.class.isAssignableFrom(c)) {
            c = Long.TYPE;
         }

         types[m] = c;
      }

      Method var7 = Math.class.getMethod(name, types);
      return var7.invoke((Object)null, args);
   }
}
