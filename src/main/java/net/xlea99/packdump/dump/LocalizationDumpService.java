package net.xlea99.packdump.dump;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.xlea99.packdump.PackdumpConfig;
import net.xlea99.packdump.PackdumpPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Dumps the resolved display names for all entries in the static Forge registries.
 * This must be called client-side, where Language.getInstance() returns the full
 * resource-pack-resolved translations (not just vanilla fallback keys).
 *
 * <p><b>The key is dumped alongside the name</b>, because they answer different questions.
 * The name tells you what a thing is called; the <i>key</i> is what a resource pack must
 * write in order to call it something else, and it cannot be reconstructed from the id
 * afterwards. {@code spawn:ant_pupa} resolves to {@code item.spawn.ant_pupa} while
 * {@code spawn:anthill} resolves to {@code block.spawn.anthill} — same registry, different
 * prefix, because a BlockItem defers to its block. Any rule guessed from the outside gets
 * that wrong, and {@code getDescriptionId()} is free here since we are already inside the
 * running game with every override applied.
 *
 * <p>Nothing marks a key as unresolved: {@code Language#getOrDefault} hands back the key
 * itself when there is no translation for it, so a reader holding both halves can see that
 * for itself without a third field to keep in sync.
 */
public final class LocalizationDumpService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LocalizationDumpService() {}

    /**
     * Dumps localization for all supported static registries into a single
     * attributes/localization.json file.
     */
    public static void dumpLocalizations() {
        LOGGER.info("packdump dumping localizations...");
        Language lang = Language.getInstance();

        JsonObject values = new JsonObject();
        JsonObject keys = new JsonObject();

        collect("minecraft:item", ForgeRegistries.ITEMS, Item::getDescriptionId,
                lang, values, keys);
        collect("minecraft:block", ForgeRegistries.BLOCKS, Block::getDescriptionId,
                lang, values, keys);
        collect("minecraft:entity_type", ForgeRegistries.ENTITY_TYPES,
                EntityType::getDescriptionId, lang, values, keys);
        collect("minecraft:enchantment", ForgeRegistries.ENCHANTMENTS,
                Enchantment::getDescriptionId, lang, values, keys);
        collect("minecraft:mob_effect", ForgeRegistries.MOB_EFFECTS,
                MobEffect::getDescriptionId, lang, values, keys);

        // NOTE: Biomes are a dynamic/data-driven registry in 1.20.1 and not in ForgeRegistries.
        // Biome localization would need RegistryAccess (server-side, requires world load).
        // We can add that later via a separate server-triggered dump path.

        // Write the output file. `keys` sits BESIDE `values` rather than nesting inside it,
        // so a reader written against schema 1 keeps working unchanged and a reader written
        // against 2 can tell an absent block from an empty one.
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", DumpSchema.VERSION);
        root.addProperty("type", "localization");
        root.addProperty("locale", "en_us");
        root.add("values", values);
        root.add("keys", keys);

        try {
            Path outDir = PackdumpPaths.attributesDir();
            Files.createDirectories(outDir);
            Path outFile = outDir.resolve("localization.json");

            if (!PackdumpConfig.overwriteExisting && Files.exists(outFile)) {
                outFile = outDir.resolve("localization_" + System.currentTimeMillis() + ".json");
            }
            Files.writeString(outFile, GSON.toJson(root), StandardCharsets.UTF_8);
            LOGGER.info("packdump localization dump wrote {}", outFile.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("packdump localization dump failed", e);
        }
    }

    /**
     * Adds one registry's display names and translation keys under {@code registryId}.
     * Entries are sorted alphabetically by registry ID.
     *
     * <p>Both outputs are filled in a single pass so they cannot disagree about which
     * entries a registry holds — two walks could drift if a registry changed underneath
     * them, and a name without its key is exactly the state this dump exists to fix.
     */
    private static <T> void collect(
            String registryId,
            IForgeRegistry<T> registry,
            Function<T, String> descriptionIdGetter,
            Language lang,
            JsonObject values,
            JsonObject keys
    ) {
        JsonObject names = new JsonObject();
        JsonObject translationKeys = new JsonObject();
        registry.getEntries().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .forEach(entry -> {
                    String id = entry.getKey().location().toString();
                    String translationKey = descriptionIdGetter.apply(entry.getValue());
                    names.addProperty(id, lang.getOrDefault(translationKey));
                    translationKeys.addProperty(id, translationKey);
                });
        values.add(registryId, names);
        keys.add(registryId, translationKeys);
    }

}
