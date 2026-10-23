package net.xlea99.packsmith;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Packsmith.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PacksmithConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> OUTPUT_DIR = BUILDER
            .comment("Directory (relative to the game directory) where Packsmith will write dumps.")
            .define("outputDir", "packsmith");

    private static final ForgeConfigSpec.BooleanValue OVERWRITE_EXISTING = BUILDER
            .comment("If true, overwrite existing dump files. If false, Packsmith may write timestamped files instead.")
            .define("overwriteExisting", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached values (read once on config load/reload)
    public static String outputDir;
    public static boolean overwriteExisting;

    private PacksmithConfig() {}

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        outputDir = OUTPUT_DIR.get().trim();
        overwriteExisting = OVERWRITE_EXISTING.get();
    }
}
