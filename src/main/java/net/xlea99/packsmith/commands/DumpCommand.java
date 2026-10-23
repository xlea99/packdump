package net.xlea99.packsmith.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.xlea99.packsmith.dump.DumpOrchestrator;

public final class DumpCommand {
    private DumpCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("dump")
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
                });
    }
}
