package net.xlea99.packsmith.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.IModInfo;
import net.xlea99.packsmith.PacksmithPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

/**
 * Writes a top-level meta.json describing the environment this dump came from:
 * timestamp, game version, loader version, and the full loaded mod list.
 */
public final class MetaDumpService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MetaDumpService() {}

    /**
     * Writes meta.json with environment info and the registry manifest.
     * @param registryManifest the manifest array returned by StaticRegistryDumpService
     */
    public static void dumpMeta(JsonArray registryManifest) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", "packsmith_full_dump");
        root.addProperty("schema_version", DumpSchema.VERSION);
        root.addProperty("created_by", "packsmith_forge_mod");
        root.addProperty("generated_at_utc", Instant.now().toString());

        // Game and loader versions
        root.addProperty("minecraft_version", SharedConstants.getCurrentVersion().getName());
        root.addProperty("loader", "forge");
        root.addProperty("loader_version", FMLLoader.versionInfo().forgeVersion());

        // Loaded mods
        JsonArray mods = new JsonArray();
        ModList.get().getMods().stream()
                .sorted(Comparator.comparing(IModInfo::getModId))
                .forEach(mod -> {
                    JsonObject modEntry = new JsonObject();
                    modEntry.addProperty("mod_id", mod.getModId());
                    modEntry.addProperty("name", mod.getDisplayName());
                    modEntry.addProperty("version", mod.getVersion().toString());
                    mods.add(modEntry);
                });
        root.addProperty("mod_count", mods.size());
        root.add("mods", mods);

        // Registry manifest
        root.add("registries", registryManifest);

        // Write to <outputDir>/meta.json
        Path outDir = PacksmithPaths.packsmithRoot();
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("meta.json");
        Files.writeString(outFile, GSON.toJson(root), StandardCharsets.UTF_8);
        LOGGER.info("Packsmith meta dump wrote {}", outFile.toAbsolutePath());
    }

}
