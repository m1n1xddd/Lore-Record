package com.mini.lorerecord.classifier;
import com.mini.lorerecord.config.Config;
import com.mini.lorerecord.config.ServerConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

/**
 * 消息分类器
 */
public final class MessageClassifier {

    /** 消息类别 */
    public enum Category {
        /** 玩家消息*/
        PLAYER_CHAT,
        /** 死亡消息*/
        DEATH,
        /** 成就获得消息*/
        ADVANCEMENT,
        /** 登入登出 */
        JOIN_LEAVE,
        /** 玩家私聊 */
        PRIVATE,
        /** 其他系统消息 */
        SYSTEM_OTHER
    }

    private MessageClassifier() {
    }

    public static Category categorize(ClientChatReceivedEvent event) {
        Category byContent = categorize(event.getMessage());
        if (byContent != Category.SYSTEM_OTHER) {
            return byContent; // 私聊/死亡/成就等按内容识别（私聊在客户端也是 Player 事件）
        }
        if (event instanceof ClientChatReceivedEvent.Player) {
            return Category.PLAYER_CHAT;
        }
        return Category.SYSTEM_OTHER;
    }


    public static Category categorize(Component message) {
        if (message.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            if (key.startsWith("death.")) {
                return Category.DEATH;
            }
            if (key.startsWith("chat.type.advancement.")) {
                return Category.ADVANCEMENT;
            }
            if (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")) {
                return Category.JOIN_LEAVE;
            }
            if (key.equals("commands.message.display.incoming")
                    || key.equals("commands.message.display.outgoing")) {
                return Category.PRIVATE;
            }
        }
        return Category.SYSTEM_OTHER;
    }

    public static boolean shouldRecord(Category category) {
        return shouldRecord(category, false);
    }

    public static boolean shouldRecord(Category category, boolean global) {
        return switch (category) {
            case PLAYER_CHAT -> true; // 始终记录
            case DEATH -> global ? ServerConfig.RECORD_DEATH.get() : Config.RECORD_DEATH.get();
            case ADVANCEMENT -> global ? ServerConfig.RECORD_ADVANCEMENT.get() : Config.RECORD_ADVANCEMENT.get();
            case JOIN_LEAVE -> global ? ServerConfig.RECORD_JOIN_LEAVE.get() : Config.RECORD_JOIN_LEAVE.get();
            case PRIVATE -> global ? ServerConfig.RECORD_PRIVATE.get() : Config.RECORD_PRIVATE.get();
            case SYSTEM_OTHER -> global ? ServerConfig.RECORD_TELLRAW.get() : Config.RECORD_TELLRAW.get();
        };
    }
}
