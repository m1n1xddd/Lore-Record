package com.mini.lorerecord;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 聊天记录器
 *
 */
@Mod.EventBusSubscriber(modid = LoreRecord.MODID, value = Dist.CLIENT)
public final class ChatRecorder {

    /** 是否正在记录 */
    private static boolean recording;
    /** 当前记录名 */
    private static String recordName = "";
    /**
     * 已记录的消息
     */
    private static final List<Component> messages = new ArrayList<>();

    private ChatRecorder() {
    }


    @SubscribeEvent
    public static void onClientChat(ClientChatReceivedEvent event) {
        if (!recording) {
            return;
        }
        MessageClassifier.Category category = MessageClassifier.categorize(event);
        if (MessageClassifier.shouldRecord(category)) {
            messages.add(event.getMessage());
        }
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (LoreExporter.hasPending() && event.getPlayer() != null) {
            event.getPlayer().displayClientMessage(Component.literal(
                    "上次退出前有未结束的记录，是否继续记录？继续记录请使用/log continue，结束记录使用/log end")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LoreNetwork.resetServerFlag();
        if (recording) {
            LoreExporter.savePending(recordName, messages);
            reset();
        }
    }

    static int start(CommandSourceStack source, String name) {
        if (recording || LoreExporter.hasPending()) {
            reply(source, "请先使用/log end结束先前的记录", ChatFormatting.RED);
            return 0;
        }
        if (!LoreExporter.isValidName(name)) {
            reply(source, "名称只能包含文本、数字、下划线（_）以及减号（-）",
                    ChatFormatting.RED);
            return 0;
        }
        recording = true;
        recordName = name;
        messages.clear();
        reply(source, "已开启记录。记录名：" + name, ChatFormatting.YELLOW);
        return 1;
    }

    static int end(CommandSourceStack source) {
        if (recording) {
            exportAndReply(source, recordName, new ArrayList<>(messages));
            reset();
            return 1;
        }
        LoreExporter.Pending pending = LoreExporter.loadPending();
        if (pending != null) {
            LoreExporter.deletePending();
            exportAndReply(source, pending.name(), pending.messages());
            return 1;
        }
        reply(source, "当前没有进行中的记录", ChatFormatting.RED);
        return 0;
    }

    static int resume(CommandSourceStack source) {
        if (recording) {
            reply(source, "已经在记录中了", ChatFormatting.RED);
            return 0;
        }
        LoreExporter.Pending pending = LoreExporter.loadPending();
        if (pending == null) {
            reply(source, "没有未结束的记录", ChatFormatting.RED);
            return 0;
        }
        LoreExporter.deletePending();
        recording = true;
        recordName = pending.name();
        messages.addAll(pending.messages());
        reply(source, "继续开启记录：" + recordName + "（已记录 " + messages.size() + " 条消息）", ChatFormatting.YELLOW);
        return 1;
    }

    static int book(CommandSourceStack source, String name) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        List<Component> recorded = LoreExporter.loadMessages(name);
        if (recorded == null) {
            List<String> existing = LoreExporter.listRecords();
            reply(source, existing.isEmpty() ? "记录不存在，还没有任何已保存的记录"
                    : "记录不存在，已有记录：" + String.join("、", existing), ChatFormatting.RED);
            return 0;
        }
        ItemStack book = LoreBook.create(name, player.getName().getString(), recorded);


        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> giveToServerPlayer(server, player.getUUID(), book));
            reply(source, "记录书已生成", ChatFormatting.YELLOW);
            return 1;
        }

        if (LoreNetwork.isServerInstalled()) {
            LoreNetwork.CHANNEL.sendToServer(
                    new LoreNetwork.GiveBookPacket(name, LoreBook.pagesOf(book)));
            reply(source, "记录书已生成", ChatFormatting.YELLOW);
            return 1;
        }

        // 服务器没装模组
        reply(source, "无法获取记录书。", ChatFormatting.RED);
        return 0;
    }

    private static void giveToServerPlayer(IntegratedServer server, UUID playerId, ItemStack book) {
        ServerPlayer sp = server.getPlayerList().getPlayer(playerId);
        if (sp != null && !sp.getInventory().add(book)) {
            sp.drop(book, false);
        }
    }


    private static void exportAndReply(CommandSourceStack source, String name, List<Component> toSave) {
        java.nio.file.Path dir = LoreExporter.export(name, toSave);
        reply(source, "记录已保存。保存路径：" + dir + "。使用/log book " + name + " 来获取该记录书", ChatFormatting.GRAY);
    }

    private static void reply(CommandSourceStack source, String text, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal(text).withStyle(color), false);
    }

    private static void reset() {
        recording = false;
        recordName = "";
        messages.clear();
    }
}
