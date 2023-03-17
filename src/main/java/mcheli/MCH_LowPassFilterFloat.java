package mcheli;

public class MCH_LowPassFilterFloat {

   private MCH_Queue filter;


   public MCH_LowPassFilterFloat(int filterLength) {
      this.filter = new MCH_Queue(filterLength, Float.valueOf(0.0F));
   }

   public void clear() {
      this.filter.clear(Float.valueOf(0.0F));
   }

   public void put(float t) {
      this.filter.put(Float.valueOf(t));
   }

   public float getAvg() {
      float f = 0.0F;

      for(int i = 0; i < this.filter.size(); ++i) {
         f += ((Float)this.filter.get(i)).floatValue();
      }

      return f / (float)this.filter.size();
   }
}
