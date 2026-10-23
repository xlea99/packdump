package net.xlea99.packdump.dump;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects world-specific datapacks that could contaminate a dump.
 *
 * In Minecraft 1.21.1 on NeoForge, enabled packs have IDs like:
 *   - "vanilla"                     -> base game, always present
 *   - "mod/modid"                   -> mod-provided datapacks (NeoForge)
 *   - "file/packname"               -> datapacks from the world's datapacks/ folder
 *   - "file/packname.zip"           -> zipped datapacks from the world's datapacks/ folder
 *
 * Any pack whose ID starts with "file/" is a world-specific datapack that the user
 * (or someone) placed in the world save manually. These can add biomes, modify
 * worldgen, add recipes, etc. — and would make our dump non-reproducible.
 */
public final class WorldDatapackDetector {

    private static final Logger LOGGER = LogUtils.getLogger();

    private WorldDatapackDetector() {}

    /**
     * Returns a list of world-specific datapack names that are currently enabled.
     * An empty list means the world is clean — only vanilla + mod packs are loaded.
     */
    public static List<String> findWorldSpecificDatapacks(MinecraftServer server) {
        PackRepository repo = server.getPackRepository();
        List<String> worldPacks = new ArrayList<>();

        for (Pack pack : repo.getSelectedPacks()) {
            String id = pack.getId();
            // "file/" prefix = loaded from the world's datapacks/ folder
            if (id.startsWith("file/")) {
                worldPacks.add(id);
            }
        }

        if (!worldPacks.isEmpty()) {
            LOGGER.warn("packdump detected {} world-specific datapack(s): {}",
                    worldPacks.size(), worldPacks);
        }
        return worldPacks;
    }
}
