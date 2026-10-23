package net.xlea99.packdump;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.xlea99.packdump.commands.PackdumpCommands;
import net.xlea99.packdump.dump.DumpOrchestrator;
import org.slf4j.Logger;

@Mod(Packdump.MODID)
public class Packdump {

    public static final String MODID = "packdump";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Packdump() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PackdumpConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
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
