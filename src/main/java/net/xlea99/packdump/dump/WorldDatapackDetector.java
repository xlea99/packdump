package net.xlea99.packdump.dump;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects world-specific datapacks that could contaminate a dump.
 *
 * In Minecraft 1.21.1 on NeoForge, enabled packs have IDs like:
 *   - "vanilla"                     -> base game, always present
 *   - "mod/modid"                   -> mod-provided datapacks (NeoForge)
 *   - "file/packname"               -> a pack found in some folder, packname being its file name
 *
 * A world-specific datapack is one the user (or someone) placed in the world save manually.
 * These can add biomes, modify worldgen, add recipes, etc. — and would make our dump
 * non-reproducible.
 *
 * <p><b>"file/" alone does not mean the world's folder.</b> Vanilla's folder source names
 * every pack it finds that way, whichever folder it was pointed at, and global pack loaders
 * reuse it: Moonlight's global datapack folder produces "file/" packs too. Those are part of
 * the modpack — exactly what a dump should capture — so a "file/" pack is world-specific
 * only when its file is actually in this world's datapacks folder.
 */
public final class WorldDatapackDetector {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER_PREFIX = "file/";

    private WorldDatapackDetector() {}

    /**
     * Returns a list of world-specific datapack names that are currently enabled.
     * An empty list means the world is clean — no pack comes from the world's own folder.
     */
    public static List<String> findWorldSpecificDatapacks(MinecraftServer server) {
        PackRepository repo = server.getPackRepository();
        Path worldFolder = server.getWorldPath(LevelResource.DATAPACK_DIR);
        List<String> worldPacks = new ArrayList<>();

        for (Pack pack : repo.getSelectedPacks()) {
            String id = pack.getId();
            if (id.startsWith(FOLDER_PREFIX)
                    && Files.exists(worldFolder.resolve(id.substring(FOLDER_PREFIX.length())))) {
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
