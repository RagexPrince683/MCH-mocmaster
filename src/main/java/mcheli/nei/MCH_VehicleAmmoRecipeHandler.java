package mcheli.nei;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_TurretInfoManager;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Shows the complete inventory reload set for each weapon mounted on an MCHeli vehicle. */
@SideOnly(Side.CLIENT)
public class MCH_VehicleAmmoRecipeHandler extends TemplateRecipeHandler {

    private static final String IDENTIFIER = "mcheli.vehicle_ammunition";

    @Override
    public String getRecipeName() {
        return I18n.format("mcheli.nei.vehicle_ammunition");
    }

    @Override
    public String getOverlayIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getGuiTexture() {
        return "textures/gui/container/inventory.png";
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!isValid(ingredient)) {
            return;
        }
        for (VehicleAmmoRecipe recipe : buildCurrentRelations()) {
            if (recipe.containsAmmo(ingredient)) {
                arecipes.add(recipe);
            }
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!isValid(result)) {
            return;
        }
        for (VehicleAmmoRecipe recipe : buildCurrentRelations()) {
            if (result.isItemEqual(recipe.vehicleStack)) {
                arecipes.add(recipe);
            }
        }
    }

    @Override
    public void drawBackground(int recipe) {
        Gui.drawRect(1, 0, 165, 65, 0x80202020);
        Gui.drawRect(73, 27, 86, 30, 0xFFFFFFFF);
        Gui.drawRect(83, 24, 86, 33, 0xFFFFFFFF);
    }

    @Override
    public void drawExtras(int recipeIndex) {
        VehicleAmmoRecipe recipe = (VehicleAmmoRecipe) arecipes.get(recipeIndex);
        String vehicle = I18n.format("mcheli.nei.vehicle") + ": " + recipe.vehicleName;
        String weapon = I18n.format("mcheli.nei.weapon") + ": " + recipe.weaponName;
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.drawString(trim(vehicle, 162), 3, 2, 0xFFFFFF);
        mc.fontRenderer.drawString(trim(weapon, 162), 3, 45, 0xFFFFFF);
        mc.fontRenderer.drawString(trim(recipe.vehicleKind, 162), 3, 56, 0xA0A0A0);
    }

    private static String trim(String value, int width) {
        return Minecraft.getMinecraft().fontRenderer.trimStringToWidth(value, width);
    }

    /**
     * Build from newly copied manager values for every query so /mcheli reload is visible in NEI
     * immediately and a replaced published map is never traversed in place.
     */
    private List<VehicleAmmoRecipe> buildCurrentRelations() {
        List<VehicleEntry> vehicles = new ArrayList<VehicleEntry>();
        addFamily(vehicles, MCH_HeliInfoManager.map, "helicopter");
        addFamily(vehicles, MCP_PlaneInfoManager.map, "plane");
        addFamily(vehicles, MCH_ShipInfoManager.map, "ship");
        addFamily(vehicles, MCH_TankInfoManager.map, "tank");
        // This manager contains legacy mobile vehicles as well as category/static turrets.
        addFamily(vehicles, MCH_TurretInfoManager.map, null);

        Map<String, VehicleAmmoRecipe> unique = new LinkedHashMap<String, VehicleAmmoRecipe>();
        for (VehicleEntry entry : vehicles) {
            addVehicleRelations(unique, entry);
        }
        List<VehicleAmmoRecipe> result = new ArrayList<VehicleAmmoRecipe>(unique.values());
        Collections.sort(result, RECIPE_ORDER);
        return result;
    }

    private static void addFamily(List<VehicleEntry> destination, Map source, String family) {
        Map published = source;
        List values = new ArrayList(published.values());
        for (Object value : values) {
            if (value instanceof MCH_BaseVehicleInfo) {
                MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo) value;
                String resolvedFamily = family == null ? info.getDirectoryName() : family;
                destination.add(new VehicleEntry(resolvedFamily, info));
            }
        }
    }

    private void addVehicleRelations(Map<String, VehicleAmmoRecipe> unique, VehicleEntry entry) {
        MCH_BaseVehicleInfo vehicle = entry.info;
        if (vehicle.getItem() == null) {
            return;
        }
        ItemStack vehicleStack = new ItemStack(vehicle.getItem(), 1, 0);
        List weaponSets = new ArrayList(vehicle.weaponSetList);
        for (Object value : weaponSets) {
            if (!(value instanceof MCH_BaseVehicleInfo.WeaponSet)) {
                continue;
            }
            MCH_BaseVehicleInfo.WeaponSet set = (MCH_BaseVehicleInfo.WeaponSet) value;
            MCH_WeaponInfo weapon = MCH_WeaponInfoManager.get(set.type);
            List<ItemStack> ammo = copyAmmo(weapon);
            if (weapon == null || ammo.isEmpty()) {
                continue;
            }
            String vehicleName = localizedVehicleName(vehicleStack, vehicle);
            String kind = empty(vehicle.getKindName()) ? entry.family : vehicle.getKindName();
            String weaponName = empty(weapon.displayName) ? weapon.name : weapon.displayName;
            VehicleAmmoRecipe recipe = new VehicleAmmoRecipe(vehicleStack, ammo, entry.family + ":" + vehicle.name,
                vehicleName, kind, weapon.name, weaponName);
            unique.put(recipe.deduplicationKey(entry.family, set.type), recipe);
        }
    }

    private static List<ItemStack> copyAmmo(MCH_WeaponInfo weapon) {
        List<ItemStack> result = new ArrayList<ItemStack>();
        if (weapon == null || weapon.roundItems == null) {
            return result;
        }
        List rounds = new ArrayList(weapon.roundItems);
        // Every Item line is required by reload logic, so they remain together on one NEI page.
        for (Object value : rounds) {
            if (!(value instanceof MCH_WeaponInfo.RoundItem)) {
                continue;
            }
            MCH_WeaponInfo.RoundItem round = (MCH_WeaponInfo.RoundItem) value;
            if (!isValid(round.itemStack)) {
                continue;
            }
            ItemStack stack = round.itemStack.copy();
            stack.stackSize = round.num;
            result.add(stack);
        }
        return result;
    }

    private static String localizedVehicleName(ItemStack stack, MCH_BaseVehicleInfo info) {
        String localized = stack.getDisplayName();
        if (!empty(localized) && !localized.equals(stack.getUnlocalizedName())) {
            return localized;
        }
        return empty(info.displayName) ? info.name : info.displayName;
    }

    private static boolean empty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static boolean isValid(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.stackSize > 0;
    }

    private static final Comparator<VehicleAmmoRecipe> RECIPE_ORDER = new Comparator<VehicleAmmoRecipe>() {
        @Override
        public int compare(VehicleAmmoRecipe left, VehicleAmmoRecipe right) {
            int compared = compareText(left.vehicleKind, right.vehicleKind);
            if (compared == 0) compared = compareText(left.vehicleName, right.vehicleName);
            if (compared == 0) compared = compareText(left.weaponName, right.weaponName);
            if (compared == 0) compared = compareText(left.weaponConfigurationName, right.weaponConfigurationName);
            return compared;
        }
    };

    private static int compareText(String left, String right) {
        return left.toLowerCase(Locale.ROOT).compareTo(right.toLowerCase(Locale.ROOT));
    }

    private static final class VehicleEntry {
        private final String family;
        private final MCH_BaseVehicleInfo info;

        private VehicleEntry(String family, MCH_BaseVehicleInfo info) {
            this.family = family;
            this.info = info;
        }
    }

    /** Immutable query result; it deliberately retains no live info or round-item objects. */
    private final class VehicleAmmoRecipe extends CachedRecipe {
        private final ItemStack vehicleStack;
        private final List<ItemStack> ammoStacks;
        private final String vehicleIdentity;
        private final String vehicleName;
        private final String vehicleKind;
        private final String weaponConfigurationName;
        private final String weaponName;

        private VehicleAmmoRecipe(ItemStack vehicle, List<ItemStack> ammo, String identity, String vehicleName,
                                  String vehicleKind, String weaponConfigurationName, String weaponName) {
            this.vehicleStack = vehicle.copy();
            List<ItemStack> copies = new ArrayList<ItemStack>();
            for (ItemStack stack : ammo) copies.add(stack.copy());
            this.ammoStacks = Collections.unmodifiableList(copies);
            this.vehicleIdentity = identity;
            this.vehicleName = vehicleName;
            this.vehicleKind = vehicleKind;
            this.weaponConfigurationName = weaponConfigurationName;
            this.weaponName = weaponName;
        }

        @Override
        public PositionedStack getResult() {
            return new PositionedStack(vehicleStack.copy(), 91, 22);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            List<PositionedStack> positioned = new ArrayList<PositionedStack>();
            for (int i = 0; i < ammoStacks.size() && i < 3; i++) {
                positioned.add(new PositionedStack(ammoStacks.get(i).copy(), 10 + i * 20, 22));
            }
            return positioned;
        }

        private boolean containsAmmo(ItemStack queried) {
            for (ItemStack ammo : ammoStacks) {
                if (queried.isItemEqual(ammo)) return true;
            }
            return false;
        }

        private String deduplicationKey(String family, String weaponType) {
            StringBuilder key = new StringBuilder(family).append('|').append(vehicleIdentity).append('|')
                .append(weaponType.toLowerCase(Locale.ROOT));
            for (ItemStack ammo : ammoStacks) {
                key.append('|').append(Item.itemRegistry.getNameForObject(ammo.getItem())).append('@')
                    .append(ammo.getItemDamage()).append('x').append(ammo.stackSize);
            }
            return key.toString();
        }
    }
}
