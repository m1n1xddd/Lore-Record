package com.mini.lorerecord;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端配置
 */
public final class Config {

    public static final ForgeConfigSpec SPEC;

    /**
     * html主题
     */
    public enum HtmlTheme {
        DARK, LIGHT
    }

    /** html主题 */
    public static final ForgeConfigSpec.EnumValue<HtmlTheme> HTML_THEME;

    /** 是否记录玩家加入/退出游戏的消息 */
    public static final ForgeConfigSpec.BooleanValue RECORD_JOIN_LEAVE;
    /** 是否记录死亡消息 */
    public static final ForgeConfigSpec.BooleanValue RECORD_DEATH;
    /** 是否记录 /tell 私聊消息 */
    public static final ForgeConfigSpec.BooleanValue RECORD_PRIVATE;
    /** 是否记录 tellraw 及其他系统消息 */
    public static final ForgeConfigSpec.BooleanValue RECORD_TELLRAW;
    /** 是否记录成就获得消息 */
    public static final ForgeConfigSpec.BooleanValue RECORD_ADVANCEMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        HTML_THEME = builder
                .comment("html主题：DARK=深色；LIGHT=浅色")
                .defineEnum("htmlTheme", HtmlTheme.DARK);
        RECORD_JOIN_LEAVE = builder
                .comment("记录玩家登入登出消息")
                .define("recordJoinLeaveMessages", true);
        RECORD_DEATH = builder
                .comment("记录死亡消息")
                .define("recordDeathMessages", true);
        RECORD_PRIVATE = builder
                .comment("记录玩家私聊消息")
                .define("recordPrivateMessages", true);
        RECORD_TELLRAW = builder
                .comment("记录tellraw以及其他系统消息")
                .define("recordTellrawMessages", true);
        RECORD_ADVANCEMENT = builder
                .comment("记录成就获得消息")
                .define("recordAdvancementMessages", true);
        SPEC = builder.build();
    }

    private Config() {
    }
}
