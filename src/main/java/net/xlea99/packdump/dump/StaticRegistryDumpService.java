package net.xlea99.packdump.dump;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.xlea99.packdump.PackdumpConfig;
import net.xlea99.packdump.PackdumpPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

public final class StaticRegistryDumpService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private StaticRegistryDumpService() {}

    /**
     * Dumps all registries as individual JSON files and returns the manifest
     * array (registry name, file, count) for inclusion in meta.json.
     */
    public static JsonArray dumpAllRegistries(RegistryAccess registryAccess) throws IOException {
        Path out_dir = PackdumpPaths.registriesDir();
        Files.createDirectories(out_dir);

        JsonArray registryManifest = new JsonArray();
        registryAccess.registries().forEach(entry -> {
            var registryKey = entry.key();
            var registry = entry.value();

            String registryId = registryKey.location().toString();

            var ids = new ArrayList<>(registry.keySet());
            ids.sort(Comparator.comparing(ResourceLocation::toString));
            JsonArray values = new JsonArray();
            for (var id : ids) {
                values.add(id.toString());
            }

            try {
                writeRegistryFile(out_dir, sanitizeFilename(registryId), registryId, values);

                JsonObject manifestEntry = new JsonObject();
                manifestEntry.addProperty("type", registryId);
                manifestEntry.addProperty("file", sanitizeFilename(registryId) + ".json");
                manifestEntry.addProperty("count", ids.size());
                registryManifest.add(manifestEntry);
            } catch (IOException e) {
                throw new RuntimeException("Failed dumping registry " + registryId, e);
            }
        });
        return registryManifest;
    }

    private static String sanitizeFilename(String s) {
        return s.replace(':', '_').replace('/', '_');
    }

    private static void writeRegistryFile(Path dir_path, String filename, String registryId, JsonArray values) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", DumpSchema.VERSION);
        root.addProperty("type", registryId);
        root.add("values", values);

        Path out = dir_path.resolve(filename + ".json");
        if (!PackdumpConfig.overwriteExisting && Files.exists(out)) {
            out = dir_path.resolve(filename + "_" + System.currentTimeMillis() + ".json");
        }
        Files.writeString(out, GSON.toJson(root), StandardCharsets.UTF_8);
        LOGGER.info("packdump registry dump wrote {}", out.toAbsolutePath());
    }
}
