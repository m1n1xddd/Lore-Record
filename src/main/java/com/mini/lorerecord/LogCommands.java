package com.mini.lorerecord;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 命令注册
 */
@Mod.EventBusSubscriber(modid = LoreRecord.MODID, value = Dist.CLIENT)
public final class LogCommands {

    private LogCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("log")
                .then(Commands.literal("start")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> ChatRecorder.start(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("end")
                        .executes(ctx -> ChatRecorder.end(ctx.getSource())))
                .then(Commands.literal("continue")
                        .executes(ctx -> ChatRecorder.resume(ctx.getSource())))
                .then(Commands.literal("book")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        LoreExporter.listRecords(), builder))
                                .executes(ctx -> ChatRecorder.book(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));
    }
}
