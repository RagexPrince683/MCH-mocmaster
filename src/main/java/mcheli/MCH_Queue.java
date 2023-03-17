package mcheli;

import java.util.ArrayList;
import java.util.List;

public class MCH_Queue {

   private int current;
   private List list;


   public MCH_Queue(int filterLength, Object initVal) {
      if(filterLength <= 0) {
         filterLength = 1;
      }

      this.list = new ArrayList();

      for(int i = 0; i < filterLength; ++i) {
         this.list.add(initVal);
      }

      this.current = 0;
   }

   public void clear(Object clearVal) {
      for(int i = 0; i < this.size(); ++i) {
         this.list.set(i, clearVal);
      }

   }

   public void put(Object t) {
      this.list.set(this.current, t);
      ++this.current;
      this.current %= this.size();
   }

   private int getIndex(int offset) {
      offset %= this.size();
      int index = this.current + offset;
      return index < 0?index + this.size():index % this.size();
   }

   public Object oldest() {
      return this.list.get(this.getIndex(1));
   }

   public Object get(int i) {
      return this.list.get(i);
   }

   public int size() {
      return this.list.size();
   }
}
