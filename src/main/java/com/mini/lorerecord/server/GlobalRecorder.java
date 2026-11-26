package com.mini.lorerecord.server;
import com.mini.lorerecord.LoreRecord;
import com.mini.lorerecord.classifier.MessageClassifier;
import com.mini.lorerecord.export.LoreBook;
import com.mini.lorerecord.export.LoreExporter;
import com.mini.lorerecord.model.RecordEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.mini.lorerecord.classifier.MessageClassifier.Category.PLAYER_CHAT;

@Mod.EventBusSubscriber(modid = LoreRecord.MODID)
public final class GlobalRecorder {

    /** 是否正在记录 */
    private static boolean recording;
    /** 当前记录名 */
    private static String recordName = "";
    /** 已记录的消息 */
    private static final List<RecordEntry> messages = new ArrayList<>();
    /** 系统消息签名 */
    private static final Map<String, Long> seenSignatures = new HashMap<>();

    private static final long DEDUP_WINDOW_MS = 1000;

    private GlobalRecorder() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!recording || !MessageClassifier.shouldRecord(PLAYER_CHAT, true)) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        messages.add(RecordEntry.captureServer(event.getMessage(), PLAYER_CHAT, dimensionOf(player)));
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (recording) {
            LoreExporter.savePending(recordName, messages, true);
            reset();
        }
    }


    public static void onSystemMessage(Component component, ServerPlayer receiver) {
        if (!recording || component == null) {
            return;
        }
        String text = component.getString();
        if (text.isEmpty() || text.contains("日志记录已开启")) {
            return; // 空消息或自身的提示消息
        }
        long now = System.currentTimeMillis();
        Long last = seenSignatures.get(text);
        if (last != null && now - last < DEDUP_WINDOW_MS) {
            return; // 同一条广播发给多个玩家只记一次
        }
        seenSignatures.put(text, now);
        if (seenSignatures.size() > 1024) {
            seenSignatures.entrySet().removeIf(e -> now - e.getValue() > DEDUP_WINDOW_MS);
        }
        MessageClassifier.Category category = MessageClassifier.categorize(component);
        if (!MessageClassifier.shouldRecord(category, true)) {
            return;
        }
        messages.add(RecordEntry.captureServer(component.copy(), category, dimensionOf(receiver)));
    }

    private static String dimensionOf(ServerPlayer player) {
        return player == null ? "" : player.level().dimension().location().toString();
    }

    static int start(CommandSourceStack source, String name) {
        if (recording || LoreExporter.hasPending(true)) {
            reply(source, "请先使用 /log global end 结束先前的记录", ChatFormatting.RED);
            return 0;
        }
        if (!LoreExporter.isValidName(name)) {
            reply(source, "名称只能包含文本、数字、下划线（_）以及减号（-）", ChatFormatting.RED);
            return 0;
        }
        recording = true;
        recordName = name;
        messages.clear();
        seenSignatures.clear();
        MinecraftServer server = source.getServer();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("日志记录已开启。").withStyle(ChatFormatting.YELLOW), false);
        LoreRecord.LOGGER.info("聊天记录已开启：{}", name);
        return 1;
    }

    static int end(CommandSourceStack source) {
        if (recording) {
            exportAndReply(source, recordName, new ArrayList<>(messages));
            reset();
            return 1;
        }
        LoreExporter.Pending pending = LoreExporter.loadPending(true);
        if (pending != null) {
            LoreExporter.deletePending(true);
            exportAndReply(source, pending.name(), pending.messages());
            return 1;
        }
        reply(source, "当前没有进行中的记录", ChatFormatting.RED);
        return 0;
    }

    static int pause(CommandSourceStack source) {
        if (recording) {
            int count = messages.size();
            LoreExporter.savePending(recordName, messages, true);
            reset();
            reply(source, "记录已暂停（已记录 " + count + " 条消息），可使用 /log global continue 继续",
                    ChatFormatting.YELLOW);
            return 1;
        }
        reply(source, "当前没有进行中的记录", ChatFormatting.RED);
        return 0;
    }

    static int resume(CommandSourceStack source) {
        if (recording) {
            reply(source, "记录已经在进行中", ChatFormatting.RED);
            return 0;
        }
        LoreExporter.Pending pending = LoreExporter.loadPending(true);
        if (pending == null) {
            reply(source, "没有暂停的记录", ChatFormatting.RED);
            return 0;
        }
        LoreExporter.deletePending(true);
        recording = true;
        recordName = pending.name();
        messages.addAll(pending.messages());
        long now = System.currentTimeMillis();
        for (RecordEntry entry : messages) {
            seenSignatures.put(entry.message().getString(), now);
        }
        reply(source, "已继续记录：" + recordName + "，（已记录" + messages.size() + " 条消息）",
                ChatFormatting.YELLOW);
        return 1;
    }

    static int book(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            reply(source, "该命令只能由玩家执行", ChatFormatting.RED);
            return 0;
        }
        List<RecordEntry> recorded = LoreExporter.loadMessages(name, true);
        if (recorded == null) {
            List<String> existing = LoreExporter.listRecords(true);
            reply(source, existing.isEmpty() ? "记录不存在，还没有任何已保存的记录"
                    : "记录不存在，已有记录：" + String.join("、", existing), ChatFormatting.RED);
            return 0;
        }
        List<Component> pages = recorded.stream().map(RecordEntry::messagesWithStyle).toList();
        ItemStack book = LoreBook.create(name, player.getName().getString(), pages);
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        reply(source, "记录书已生成", ChatFormatting.YELLOW);
        return 1;
    }

    private static void exportAndReply(CommandSourceStack source, String name, List<RecordEntry> toSave) {
        java.nio.file.Path dir = LoreExporter.export(name, toSave, true);
        reply(source, "记录已保存：" + dir + "。使用 /log global book " + name + " 来获取该记录书",
                ChatFormatting.GRAY);
    }

    private static void reply(CommandSourceStack source, String text, ChatFormatting color) {
        Component msg = Component.literal(text).withStyle(color);
        seenSignatures.put(text, System.currentTimeMillis()); // 命令提示不进入记录
        source.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendSuccess(() -> msg, false); // 控制台执行时额外回显
        }
    }

    private static void reset() {
        recording = false;
        recordName = "";
        messages.clear();
        seenSignatures.clear();
    }
}
