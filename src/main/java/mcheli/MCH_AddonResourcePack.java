package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

/**
 * IResourcePack implementation that serves addon files from the filesystem.
 * Registered with Minecraft's resource manager so that bindTexture(),
 * getResource(), etc. automatically find addon textures, models, and sounds.
 */
@SideOnly(Side.CLIENT)
public class MCH_AddonResourcePack implements IResourcePack {

    private static final String DOMAIN = "mcheli";
    private final File[] addonRoots;

    /** Dynamic pack used by reload; it follows MCH_ResourceHelper's live precedence. */
    public MCH_AddonResourcePack() {
        this.addonRoots = null;
    }

    public MCH_AddonResourcePack(File[] addonRoots) {
        this.addonRoots = addonRoots;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (addonRoots == null) {
            InputStream stream = MCH_ResourceHelper.openResourceStream(toAssetPath(location));
            if (stream != null) return stream;
            throw new IOException("Resource not found: " + location);
        }
        File file = findFile(location);
        if (file == null) {
            throw new IOException("Resource not found: " + location);
        }
        return new FileInputStream(file);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (addonRoots == null) return MCH_ResourceHelper.resourceExists(toAssetPath(location));
        return findFile(location) != null;
    }

    @Override
    public Set getResourceDomains() {
        Set domains = new HashSet();
        domains.add(DOMAIN);
        return domains;
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) throws IOException {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new IOException("No pack image");
    }

    @Override
    public String getPackName() {
        return addonRoots == null ? "MCHeli Live Development Resources" : "MCHeli Addon Pack";
    }

    private File findFile(ResourceLocation location) {
        String path = location.getResourcePath().replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        while (path.contains("//")) path = path.replace("//", "/");
        for (File root : addonRoots) {
            File candidate = new File(root, "assets/" + DOMAIN + "/" + path);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    private String toAssetPath(ResourceLocation location) {
        return "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
    }
}
