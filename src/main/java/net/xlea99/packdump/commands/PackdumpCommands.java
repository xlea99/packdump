package net.xlea99.packdump.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.xlea99.packdump.dump.DumpOrchestrator;

public final class PackdumpCommands {
    private PackdumpCommands() {}

    /** {@code /packdump}: runs a full dump, same as the one on world load. */
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("packdump")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            var result = DumpOrchestrator.runFullDump(ctx.getSource().getServer());

                            for (Component message : result.chatMessages()) {
                                if (result.success()) {
                                    ctx.getSource().sendSuccess(() -> message, true);
                                } else {
                                    ctx.getSource().sendFailure(message);
                                }
                            }

                            return result.success() ? 1 : 0;
                        })
        );
    }
}
