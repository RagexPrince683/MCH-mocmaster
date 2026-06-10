package mcheli;

import mcheli.wrapper.W_Item;

//the item to render in our hand when in a vehicle,
// *when the player is invisible.(I don't think it even checks this, I think it just renders this regardless)
// Should probably be disabled when holding a Hand made gun item or Fl*nsmod gun item for driveby shooting compat
// but that could not be the only reason why items flicker when in a vehicle
public class MCH_InvisibleItem extends W_Item {

   public MCH_InvisibleItem(int par1) {
      super(par1);
   }
}
