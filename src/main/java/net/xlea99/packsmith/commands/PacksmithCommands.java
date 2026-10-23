package net.xlea99.packsmith.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public final class PacksmithCommands {
    private PacksmithCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("packsmith")
                        .requires(source -> source.hasPermission(2))
                        .then(DumpCommand.build())
        );
    }
}
