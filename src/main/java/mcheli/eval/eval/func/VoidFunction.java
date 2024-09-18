package mcheli.eval.eval.func;

import mcheli.eval.eval.func.Function;

public class VoidFunction implements Function {

   public long evalLong(Object object, String name, Long[] args) throws Throwable {
      System.out.println(object + "." + name + "The function was called(long)");

      for(int i = 0; i < args.length; ++i) {
         System.out.println("arg[" + i + "] " + args[i]);
      }

      return 0L;
   }

   public double evalDouble(Object object, String name, Double[] args) throws Throwable {
      System.out.println(object + "." + name + "The function was called(double)");

      for(int i = 0; i < args.length; ++i) {
         System.out.println("arg[" + i + "] " + args[i]);
      }

      return 0.0D;
   }

   public Object evalObject(Object object, String name, Object[] args) throws Throwable {
      System.out.println(object + "." + name + "The function was called(Object)");

      for(int i = 0; i < args.length; ++i) {
         System.out.println("arg[" + i + "] " + args[i]);
      }

      return null;
   }
}
