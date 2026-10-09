package mcheli.compat;

import cpw.mods.fml.common.Loader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_ItemFuel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Native HBM/RTM fluid containers are consumed in one direction only. No fuel export. */
public final class MCH_FuelCompat {
    private static final String RESERVE = "MCH_FuelReserve";
    private static final Map<Item, List<FuelContainer>> INPUTS = new HashMap<Item, List<FuelContainer>>();
    // Audited petroleum fuels and processed alternatives. Crude and ungraded intermediates stay in refining.
    private static final String[] PRODUCTS = {
        "HEAVYOIL", "HEAVYOIL_VACUUM", "SMEAR", "RECLAIMED", "HEATINGOIL", "HEATINGOIL_VACUUM",
        "PETROIL", "PETROIL_LEADED", "NAPHTHA", "NAPHTHA_DS", "NAPHTHA_CRACK", "NAPHTHA_COKER",
        "LIGHTOIL", "LIGHTOIL_DS", "LIGHTOIL_CRACK", "LIGHTOIL_VACUUM",
        "DIESEL", "DIESEL_CRACK", "DIESEL_REFORM", "DIESEL_CRACK_REFORM",
        "GASOLINE", "GASOLINE_LEADED", "KEROSENE", "KEROSENE_REFORM", "REFORMATE", "XYLENE",
        "GAS", "GAS_COKER", "PETROLEUM", "REFORMGAS", "LPG", "COALGAS", "COALGAS_LEADED",
        "BIOFUEL", "BIOGAS", "ETHANOL", "METHANOL", "SYNGAS"
    };

    private MCH_FuelCompat() {}

    public static void initialize() {
        INPUTS.clear();
        if (!Loader.isModLoaded("hbm")) return;
        try {
            Class<?> fluids = Class.forName("com.hbm.inventory.fluid.Fluids");
            Class<?> type = Class.forName("com.hbm.inventory.fluid.FluidType");
            Class<?> trait = Class.forName("com.hbm.inventory.fluid.trait.FT_Combustible");
            Class<?> registry = Class.forName("com.hbm.inventory.FluidContainerRegistry");
            Class<?> container = Class.forName("com.hbm.inventory.FluidContainer");
            Method getTrait = type.getMethod("getTrait", Class.class);
            Method energy;
            try { energy = trait.getMethod("getCombustionEnergyQuanta"); }
            catch (NoSuchMethodException legacyHBM) { energy = trait.getMethod("getCombustionEnergy"); }
            Method grade = trait.getMethod("getGrade");
            Object diesel = fluids.getField("DIESEL").get(null);
            double dieselEnergy = ((Number)energy.invoke(getTrait.invoke(diesel, trait))).doubleValue();
            if (dieselEnergy <= 0) throw new IllegalStateException("Diesel has no combustion energy");
            Map<Object, Double> values = new HashMap<Object, Double>();
            for (String name : PRODUCTS) {
                Object fluid;
                try { fluid = fluids.getField(name).get(null); }
                catch (NoSuchFieldException absentInThisHBM) { continue; }
                Object combustion = getTrait.invoke(fluid, trait);
                if (combustion == null) continue;
                Object fuelGrade = grade.invoke(combustion);
                if (!(fuelGrade instanceof Enum)) continue;
                String tier = ((Enum<?>)fuelGrade).name();
                double suitability = "AERO".equals(tier) ? 2.0D : "HIGH".equals(tier) ? 1.0D
                        : "LOW".equals(tier) ? 0.25D : 0.5D;
                double unitsPerBucket = 6000D * ((Number)energy.invoke(combustion)).doubleValue()
                        / dieselEnergy * suitability;
                if (unitsPerBucket <= 0 || Double.isInfinite(unitsPerBucket) || Double.isNaN(unitsPerBucket)) continue;
                values.put(fluid, unitsPerBucket);
                MCH_Lib.Log("Petroleum fuel %s (%s): %.2f MCH units per 1000 mB", name, tier, unitsPerBucket);
            }
            for (Object entry : (List<?>)registry.getField("allContainers").get(null)) {
                Object fluid = container.getField("type").get(entry);
                Double value = values.get(fluid);
                if (value == null) continue;
                ItemStack full = (ItemStack)container.getField("fullContainer").get(entry);
                ItemStack empty = (ItemStack)container.getField("emptyContainer").get(entry);
                int volume = container.getField("content").getInt(entry);
                // Reject ore blocks, missing return containers and MCHeli cans (including third-party exports).
                if (full == null || empty == null || volume <= 0 || full.getItem() instanceof MCH_ItemFuel) continue;
                long units = Math.round(value * volume / 1000D);
                if (units <= 0) continue;
                List<FuelContainer> list = INPUTS.get(full.getItem());
                if (list == null) {
                    list = new ArrayList<FuelContainer>();
                    INPUTS.put(full.getItem(), list);
                }
                list.add(new FuelContainer(full.copy(), empty.copy(), units));
            }
            if (INPUTS.isEmpty()) throw new IllegalStateException("No native petroleum containers registered");
            MCH_Lib.Log("HBM/RTM petroleum containers enabled; coal fallback retained; no MCH fuel export");
        } catch (ReflectiveOperationException | RuntimeException failure) {
            INPUTS.clear();
            MCH_Lib.Log("HBM/RTM petroleum integration unavailable: %s", failure.toString());
        }
    }

    public static FuelContainer getInput(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return null;
        List<FuelContainer> list = INPUTS.get(stack.getItem());
        if (list != null) for (FuelContainer input : list) {
            if (input.full.getItemDamage() == stack.getItemDamage()
                    && ItemStack.areItemStackTagsEqual(input.full, stack)) return input;
        }
        return null;
    }

    /** Per-can fuel. A consumed native container's surplus stays in this item, never in an external fluid registry. */
    public static long getFuel(ItemStack can) {
        long reserve = can.hasTagCompound() ? Math.max(0L, can.getTagCompound().getLong(RESERVE)) : 0L;
        long visible = Math.max(0, can.getMaxDamage() - can.getItemDamage());
        return reserve > Long.MAX_VALUE - visible ? Long.MAX_VALUE : reserve + visible;
    }

    public static void setFuel(ItemStack can, long units) {
        units = Math.max(0L, units);
        long visible = Math.min(can.getMaxDamage(), units);
        can.setItemDamage(can.getMaxDamage() - (int)visible);
        long reserve = units - visible;
        if (reserve > 0) {
            if (!can.hasTagCompound()) can.setTagCompound(new NBTTagCompound());
            can.getTagCompound().setLong(RESERVE, reserve);
        } else if (can.hasTagCompound()) {
            can.getTagCompound().removeTag(RESERVE);
            if (can.getTagCompound().hasNoTags()) can.setTagCompound(null);
        }
    }

    /** Caller equips one can before mutation; stacked cans are split by the GUI/right-click paths. */
    public static int drain(ItemStack can, int wanted) {
        if (can.stackSize != 1 || wanted <= 0) return 0;
        long stored = getFuel(can);
        int moved = (int)Math.min(stored, wanted);
        setFuel(can, stored - moved);
        return moved;
    }

    public static final class FuelContainer {
        private final ItemStack full, empty;
        public final long units;
        private FuelContainer(ItemStack full, ItemStack empty, long units) {
            this.full = full;
            this.empty = empty;
            this.units = units;
        }
        public ItemStack getEmpty() {
            ItemStack result = empty.copy();
            result.stackSize = 1;
            return result;
        }
    }
}
