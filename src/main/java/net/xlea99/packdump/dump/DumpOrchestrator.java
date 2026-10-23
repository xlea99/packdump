package net.xlea99.packdump.dump;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Single entry point for running every dump.
 * Checks for world-specific datapacks before proceeding, and runs the registry,
 * localization and block-item dumps together.
 */
public final class DumpOrchestrator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DumpOrchestrator() {}

    /**
     * Result of a dump attempt. Contains whether it succeeded and chat messages
     * to display to the player.
     */
    public record DumpResult(boolean success, List<Component> chatMessages) {}

    /**
     * Runs the full dump pipeline:
     * 1. Check for world-specific datapacks (abort if found)
     * 2. Dump all registries
     * 3. Dump all localizations
     */
    public static DumpResult runFullDump(MinecraftServer server) {
        List<Component> messages = new ArrayList<>();

        // ── Safety check: world-specific datapacks ──
        List<String> worldPacks = WorldDatapackDetector.findWorldSpecificDatapacks(server);
        if (!worldPacks.isEmpty()) {
            LOGGER.warn("packdump dump ABORTED: world-specific datapacks detected: {}", worldPacks);

            messages.add(Component.literal("[packdump] ")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .append(Component.literal("DUMP ABORTED: world-specific datapacks detected!")
                            .withStyle(ChatFormatting.RED)));

            messages.add(Component.literal("[packdump] ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal("The following datapacks are loaded from this world's save folder:")
                            .withStyle(ChatFormatting.RED)));

            for (String packName : worldPacks) {
                messages.add(Component.literal("  • " + packName)
                        .withStyle(ChatFormatting.RED));
            }

            messages.add(Component.literal("[packdump] ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal("Remove these datapacks and reload to get a clean dump.")
                            .withStyle(ChatFormatting.YELLOW)));

            return new DumpResult(false, messages);
        }

        // ── Dump registries (must run first to produce manifest for meta) ──
        JsonArray registryManifest;
        try {
            registryManifest = StaticRegistryDumpService.dumpAllRegistries(server.registryAccess());
        } catch (Exception e) {
            LOGGER.error("packdump registry dump failed", e);
            messages.add(Component.literal("[packdump] Registry dump FAILED: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return new DumpResult(false, messages);
        }

        // ── Dump meta (includes registry manifest) ──
        try {
            MetaDumpService.dumpMeta(registryManifest);
        } catch (Exception e) {
            LOGGER.error("packdump meta dump failed", e);
            messages.add(Component.literal("[packdump] Meta dump FAILED: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return new DumpResult(false, messages);
        }

        // ── Dump localizations ──
        try {
            LocalizationDumpService.dumpLocalizations(server.registryAccess());
        } catch (Exception e) {
            LOGGER.error("packdump localization dump failed", e);
            messages.add(Component.literal("[packdump] Localization dump FAILED: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return new DumpResult(false, messages);
        }

        // ── Dump block items ──
        // After localization rather than before: both walk the item registry, and keeping
        // the older dump first means a failure in the newer one cannot cost a snapshot the
        // data every existing reader already depends on.
        try {
            BlockItemDumpService.dumpBlockItems();
        } catch (Exception e) {
            LOGGER.error("packdump block item dump failed", e);
            messages.add(Component.literal("[packdump] Block item dump FAILED: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return new DumpResult(false, messages);
        }

        // ── Dump block textures ──
        // Last, for the same reason block items go after localization: the newest dump is
        // the least proven, and a failure in it must not cost a snapshot the data every
        // existing reader already depends on. Skips itself on a dedicated server.
        try {
            TextureDumpService.dumpTextures();
        } catch (Exception e) {
            LOGGER.error("packdump texture dump failed", e);
            messages.add(Component.literal("[packdump] Texture dump FAILED: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return new DumpResult(false, messages);
        }

        // ── Success ──
        messages.add(Component.literal("[packdump] ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Dump complete (meta + registries + localization + block items + textures).")
                        .withStyle(ChatFormatting.GREEN)));

        return new DumpResult(true, messages);
    }
}
