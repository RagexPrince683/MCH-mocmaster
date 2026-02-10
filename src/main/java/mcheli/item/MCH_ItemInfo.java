package mcheli.item;

import mcheli.MCH_BaseInfo;
import mcheli.MCH_Color;
import mcheli.wrapper.W_Item;

import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.item.ItemStack;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//this is our craft items/ammo items. These have a way to oredict already. The method used to oredict these
// needs to be caught in where our vehicle recipes are made,
// and the oredict name needs to be added to the vehicle recipe instead of the itemstack.
// This is because we want to be able to use oredict items in our recipes,
// but we also want to be able to use our items in other mod's recipes that use oredict.
public class MCH_ItemInfo extends MCH_BaseInfo {
    public final String name;
    public String displayName;
    public HashMap displayNameLang;
    public int itemID;
    public W_Item item;
    public List recipeString;
    public List recipe;
    public boolean isShapedRecipe;
    public int stackSize;

    public List<String> oreDictNames = new ArrayList<String>();


    public MCH_ItemInfo(String name) {
        this.name = name;
        this.displayName = name;
        this.displayNameLang = new HashMap();
        this.itemID = 0;
        this.item = null;
        this.recipeString = new ArrayList();
        this.recipe = new ArrayList();
        this.isShapedRecipe = true;
        this.stackSize = 1;
    }

    public void loadItemData(String item, String data) {

        if (item.equalsIgnoreCase("displayname")) {
            this.displayName = data;

        } else if (item.equalsIgnoreCase("adddisplayname")) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 2) {
                this.displayNameLang.put(s[0].trim(), s[1].trim());
            }

        } else if (item.equalsIgnoreCase("itemid")) {
            this.itemID = this.toInt(data, 0, '\uffff');

        } else if (item.equalsIgnoreCase("StackSize")) {
            this.stackSize = this.toInt(data, 1, 64);
        //OREDICTS
        } else if (item.equalsIgnoreCase("oredict")) {
            this.oreDictNames.add(data.trim());

        } else if (item.equalsIgnoreCase("addoredict")) {
            String[] s = data.split("\\s*,\\s*");
            for (String ore : s) {
                if (!ore.isEmpty()) {
                    this.oreDictNames.add(ore.trim());
                }
            }

        } else if (item.equalsIgnoreCase("addrecipe") || item.equalsIgnoreCase("addshapelessrecipe")) {
            this.isShapedRecipe = item.equalsIgnoreCase("addrecipe");
            this.recipeString.add(data.toUpperCase());
        }
    }



}
