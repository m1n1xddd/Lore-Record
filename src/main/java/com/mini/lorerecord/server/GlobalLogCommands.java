package com.mini.lorerecord.server;
import com.mini.lorerecord.LoreRecord;
import com.mini.lorerecord.export.LoreExporter;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端
 */
@Mod.EventBusSubscriber(modid = LoreRecord.MODID)
public final class GlobalLogCommands {

    private static final int OP_LEVEL = 2;

    private GlobalLogCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> start = Commands.literal("start")
                .requires(source -> source.hasPermission(OP_LEVEL))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> GlobalRecorder.start(ctx.getSource(),
                                StringArgumentType.getString(ctx, "name"))));
        LiteralArgumentBuilder<CommandSourceStack> end = Commands.literal("end")
                .requires(source -> source.hasPermission(OP_LEVEL))
                .executes(ctx -> GlobalRecorder.end(ctx.getSource()));
        LiteralArgumentBuilder<CommandSourceStack> pause = Commands.literal("pause")
                .requires(source -> source.hasPermission(OP_LEVEL))
                .executes(ctx -> GlobalRecorder.pause(ctx.getSource()));
        LiteralArgumentBuilder<CommandSourceStack> cont = Commands.literal("continue")
                .requires(source -> source.hasPermission(OP_LEVEL))
                .executes(ctx -> GlobalRecorder.resume(ctx.getSource()));
        LiteralArgumentBuilder<CommandSourceStack> book = Commands.literal("book")
                .requires(source -> source.hasPermission(OP_LEVEL))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                LoreExporter.listRecords(true), builder))
                        .executes(ctx -> GlobalRecorder.book(ctx.getSource(),
                                StringArgumentType.getString(ctx, "name"))));

        LiteralArgumentBuilder<CommandSourceStack> global = Commands.literal("global")
                .then(start).then(end).then(pause).then(cont).then(book);
        event.getDispatcher().register(Commands.literal("log").then(global));
    }
}
