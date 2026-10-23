package net.xlea99.packdump;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.xlea99.packdump.commands.PackdumpCommands;
import net.xlea99.packdump.dump.DumpOrchestrator;
import org.slf4j.Logger;

@Mod(Packdump.MODID)
public class Packdump {

    public static final String MODID = "packdump";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Packdump(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, PackdumpConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("packdump loaded");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        var server = event.getServer();
        var result = DumpOrchestrator.runFullDump(server);

        // Notify all online players of the result (in singleplayer, this is just you)
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (Component message : result.chatMessages()) {
                player.sendSystemMessage(message);
            }
        }

        // If no players are online yet (common on ServerStartedEvent), log it —
        // they'll see the dump files exist when they check.
        if (server.getPlayerList().getPlayers().isEmpty()) {
            if (result.success()) {
                LOGGER.info("packdump dump completed successfully on world load.");
            } else {
                LOGGER.warn("packdump dump BLOCKED on world load. See log for details.");
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PackdumpCommands.register(event);
    }
}
