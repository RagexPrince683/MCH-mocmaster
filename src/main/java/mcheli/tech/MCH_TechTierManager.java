package mcheli.tech;

import java.io.File;
import mcheli.MCH_PacketNotifyServerSettings;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_ItemBaseVehicle;
import net.minecraft.command.CommandGameMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

/** Central resolver and authority for MC Heli vehicle technology requirements. */
public final class MCH_TechTierManager {
    private static final int[] DEFAULT_MAX_YEARS = {
            1900, 1918, 1938, 1945, 1959, 1974, 1989, 2000, 2010, 2020, Integer.MAX_VALUE
    };
    private static boolean enabled;
    private static boolean operatorBypass = true;
    private static boolean creativeBypass = true;
    private static int[] maxYears = DEFAULT_MAX_YEARS.clone();
    private static volatile boolean clientEnabled;
    private static volatile int clientUnlockedHalfSteps;
    private static volatile int[] clientMaxYears = DEFAULT_MAX_YEARS.clone();

    private MCH_TechTierManager() {}

    public static void configure(File file) {
        Configuration config = new Configuration(file);
        config.load();
        enabled = config.get("TechnologyTiers", "enabled", false,
                "Enable MC Heli's independent, server-authoritative technology progression.").getBoolean(false);
        operatorBypass = config.get("TechnologyTiers", "operatorBypass", true,
                "Allow server operators to bypass MC Heli technology locks.").getBoolean(true);
        creativeBypass = config.get("TechnologyTiers", "creativeBypass", true,
                "Allow creative-mode players to bypass MC Heli technology locks.").getBoolean(true);
        int[] configured = config.get("TechnologyTiers", "tierMaximumYears", DEFAULT_MAX_YEARS,
                "Inclusive maximum year for tiers 0.0, 0.5, ... 5.0. Values must increase; the final value normally remains 2147483647.").getIntList();
        maxYears = validateYearRanges(configured, DEFAULT_MAX_YEARS);
        config.save();
    }

    private static int[] validateYearRanges(int[] configured, int[] fallback) {
        if (configured == null || configured.length != 11) return fallback.clone();
        int[] result = configured.clone();
        for (int i = 1; i < result.length; i++) if (result[i] <= result[i - 1]) return fallback.clone();
        return result;
    }

    public static boolean isValidTier(float tier) {
        int halfSteps = Math.round(tier * 2.0F);
        return tier >= 0.0F && tier <= 5.0F && Math.abs(tier * 2.0F - halfSteps) < 0.0001F;
    }
    public static int toHalfSteps(float tier) { return Math.round(tier * 2.0F); }
    public static float fromHalfSteps(int halfSteps) { return Math.max(0, Math.min(10, halfSteps)) / 2.0F; }

    public static float resolveRequiredTier(MCH_BaseVehicleInfo info, World world) {
        if (info == null) return 0.0F;
        if (info.techTierHalfSteps >= 0) return fromHalfSteps(info.techTierHalfSteps);
        if (info.techYear == null) return 0.0F;
        int[] ranges = world != null && world.isRemote ? clientMaxYears : maxYears;
        for (int i = 0; i < ranges.length; i++) if (info.techYear.intValue() <= ranges[i]) return fromHalfSteps(i);
        return 5.0F;
    }

    public static MCH_BaseVehicleInfo getInfo(ItemStack stack) {
        return stack != null && stack.getItem() instanceof MCH_ItemBaseVehicle
                ? ((MCH_ItemBaseVehicle) stack.getItem()).getAircraftInfo() : null;
    }

    public static boolean hasClassification(MCH_BaseVehicleInfo info) {
        return info != null && (info.techYear != null || info.techTierHalfSteps >= 0);
    }

    public static boolean canBypass(EntityPlayer player) {
        return player != null && ((creativeBypass && player.capabilities.isCreativeMode)
                || (operatorBypass && (new CommandGameMode()).canCommandSenderUseCommand(player)));
    }

    public static boolean isUnlocked(MCH_BaseVehicleInfo info, EntityPlayer player, World world) {
        if (!isEnabled(world) || !hasClassification(info) || canBypass(player)) return true;
        return resolveRequiredTier(info, world) <= getUnlockedTier(world) + 0.0001F;
    }

    public static boolean isUnlocked(ItemStack stack, EntityPlayer player, World world) {
        return isUnlocked(getInfo(stack), player, world);
    }

    public static boolean isEnabled(World world) {
        return world != null && world.isRemote ? clientEnabled : enabled;
    }

    public static float getUnlockedTier(World world) {
        if (world != null && world.isRemote) return fromHalfSteps(clientUnlockedHalfSteps);
        MCH_TechTierData data = MCH_TechTierData.get(world);
        return data == null ? 0.0F : fromHalfSteps(data.getUnlockedHalfSteps());
    }

    public static void setUnlockedTier(World world, float tier) {
        MCH_TechTierData data = MCH_TechTierData.get(world);
        if (data != null) data.setUnlockedHalfSteps(toHalfSteps(tier));
        MCH_PacketNotifyServerSettings.sendAll();
    }

    public static boolean serverEnabled() { return enabled; }
    public static int[] serverMaxYears() { return maxYears.clone(); }
    public static void acceptClientState(boolean enabledState, int unlocked, int[] ranges) {
        clientEnabled = enabledState;
        clientUnlockedHalfSteps = Math.max(0, Math.min(10, unlocked));
        clientMaxYears = validateYearRanges(ranges, DEFAULT_MAX_YEARS);
    }

    public static String identify(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return "none";
        Object name = Item.itemRegistry.getNameForObject(stack.getItem());
        return name == null ? stack.getItem().getUnlocalizedName() : name.toString();
    }

    public static void notifyLocked(EntityPlayer player, MCH_BaseVehicleInfo info) {
        if (player == null || player.worldObj.isRemote) return;
        String key = "MCHTechTierNotice";
        int last = player.getEntityData().getInteger(key);
        if (player.ticksExisted - last < 20 && last != 0) return;
        player.getEntityData().setInteger(key, player.ticksExisted);
        player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "LOCKED — Required MC Heli Tech Tier: "
                + resolveRequiredTier(info, player.worldObj) + "; Server Tech Tier: " + getUnlockedTier(player.worldObj)));
    }
}
