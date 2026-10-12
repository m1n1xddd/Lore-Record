package com.mini.lorerecord;

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
        /** 其他系统消息（tellraw、命令反馈等） */
        SYSTEM_OTHER
    }

    private MessageClassifier() {
    }

    /** 判断一条消息的类别 */
    public static Category categorize(ClientChatReceivedEvent event) {
        // 玩家聊天有专属子类事件
        if (event instanceof ClientChatReceivedEvent.Player) {
            return Category.PLAYER_CHAT;
        }
        // 系统消息大多由翻译组件构成，通过翻译 key 判断具体类型
        if (event.getMessage().getContents() instanceof TranslatableContents contents) {
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

    /** 按配置决定该类消息是否需要记录 */
    public static boolean shouldRecord(Category category) {
        return switch (category) {
            case PLAYER_CHAT -> true; // 玩家消息始终记录
            case DEATH -> Config.RECORD_DEATH.get();
            case ADVANCEMENT -> Config.RECORD_ADVANCEMENT.get();
            case JOIN_LEAVE -> Config.RECORD_JOIN_LEAVE.get();
            case PRIVATE -> Config.RECORD_PRIVATE.get();
            case SYSTEM_OTHER -> Config.RECORD_TELLRAW.get();
        };
    }
}
