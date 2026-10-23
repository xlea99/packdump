package net.xlea99.packdump;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

@EventBusSubscriber(modid = Packdump.MODID)
public final class PackdumpConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> OUTPUT_DIR = BUILDER
            .comment("Directory (relative to the game directory) where packdump writes dumps.")
            .define("outputDir", "packdump");

    private static final ModConfigSpec.BooleanValue OVERWRITE_EXISTING = BUILDER
            .comment("If true, overwrite existing dump files. If false, packdump may write timestamped files instead.")
            .define("overwriteExisting", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Cached values (read once on config load/reload)
    public static String outputDir;
    public static boolean overwriteExisting;

    private PackdumpConfig() {}

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        outputDir = OUTPUT_DIR.get().trim();
        overwriteExisting = OVERWRITE_EXISTING.get();
    }
}
