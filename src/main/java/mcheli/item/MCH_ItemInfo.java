package mcheli.item;

import mcheli.MCH_BaseInfo;
import mcheli.MCH_Color;
import mcheli.wrapper.W_Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        if(item.compareTo("displayname") == 0) {
            this.displayName = data;
        } else {
            String[] s;
            if(item.compareTo("adddisplayname") == 0) {
                s = data.split("\\s*,\\s*");
                if(s != null && s.length == 2) {
                    this.displayNameLang.put(s[0].trim(), s[1].trim());
                }
            } else if(item.compareTo("itemid") == 0) {
                this.itemID = this.toInt(data, 0, '\uffff');
            } else if(item.compareTo("addrecipe") != 0 && item.compareTo("addshapelessrecipe") != 0) {
                if(item.equalsIgnoreCase("StackSize")) {
                    this.stackSize = this.toInt(data, 1, 64);
                }
            } else {
                this.isShapedRecipe = item.compareTo("addrecipe") == 0;
                this.recipeString.add(data.toUpperCase());
            }
        }

    }


}
