package mcheli.tech;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/** Per-world, server-authoritative MC Heli technology progression. */
public final class MCH_TechTierData extends WorldSavedData {
    private static final String DATA_NAME = "mcheli_technology_tier";
    private int unlockedHalfSteps;

    public MCH_TechTierData() { super(DATA_NAME); }
    public MCH_TechTierData(String name) { super(name); }

    public static MCH_TechTierData get(World world) {
        if (world == null) return null;
        MinecraftServer server = MinecraftServer.getServer();
        World primaryWorld = server == null ? null : server.worldServerForDimension(0);
        World storageWorld = primaryWorld == null ? world : primaryWorld;
        MCH_TechTierData data = (MCH_TechTierData) storageWorld.perWorldStorage.loadData(
                MCH_TechTierData.class, DATA_NAME);
        if (data == null) {
            data = new MCH_TechTierData();
            storageWorld.perWorldStorage.setData(DATA_NAME, data);
        }
        return data;
    }

    public int getUnlockedHalfSteps() { return unlockedHalfSteps; }
    public void setUnlockedHalfSteps(int value) {
        unlockedHalfSteps = Math.max(0, Math.min(10, value));
        markDirty();
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        unlockedHalfSteps = Math.max(0, Math.min(10, tag.getInteger("UnlockedHalfSteps")));
    }

    @Override public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("UnlockedHalfSteps", unlockedHalfSteps);
    }
}
