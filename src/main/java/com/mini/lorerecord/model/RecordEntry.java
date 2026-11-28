package com.mini.lorerecord.model;
import com.mini.lorerecord.classifier.MessageClassifier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

import static com.mini.lorerecord.classifier.MessageClassifier.Category.PLAYER_CHAT;

public record RecordEntry(
        String time,
        String dimension,
        Component message,
        MessageClassifier.Category category
) {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, ChatFormatting> THEMES = Map.of(
            "minecraft:overworld",ChatFormatting.GREEN,
            "minecraft:the_nether",ChatFormatting.RED,
            "minecraft:the_end",ChatFormatting.LIGHT_PURPLE
    );

    public static RecordEntry capture(Component msg, MessageClassifier.Category category){
        LocalPlayer pl = Minecraft.getInstance().player;
        String dim = pl == null ? "" : pl.level().dimension().location().toString();
        return new RecordEntry(LocalDateTime.now().format(FORMATTER),dim,msg,category);
    }

    /** 服务端 */
    public static RecordEntry captureServer(Component msg, MessageClassifier.Category category, String dim){
        return new RecordEntry(LocalDateTime.now().format(FORMATTER), dim, msg, category);
    }

    private  ChatFormatting theme(){
        return THEMES.getOrDefault(dimension,ChatFormatting.BLUE);
    }

    private String dimLabel(){
        return Objects.equals(dimension, "minecraft:overworld") ? "主世界" : Objects.equals(dimension, "minecraft:the_nether") ? "下界" : Objects.equals(dimension, "minecraft:the_end") ? "末地" : dimension.substring(dimension.lastIndexOf(':') + 1);
    }

    private  static  Component recolor(Component msg,ChatFormatting color){
        MutableComponent out = Component.empty();
        for (Component part : msg.toFlatList()) {
            out.append(part.copy().setStyle(part.getStyle().withColor(color)));
        }
        return out;
    }

    public Component messagesWithStyle() {
        ChatFormatting theme = theme();
        Component body = category == PLAYER_CHAT ? recolor(message, theme) : message;
        String label = dimension.isEmpty() ? "" : "[" + dimLabel() + "] ";
        return Component.empty()
                .append(Component.literal(label).withStyle(theme))
                .append(body)
                .append(Component.literal("\n" + time).withStyle(ChatFormatting.GRAY));
    }

    //json序列化
    public  String toJson(){
        JsonObject obj = new JsonObject();
        obj.addProperty("time",time);
        obj.addProperty("dimension",dimension);
        obj.add("message", JsonParser.parseString(Component.Serializer.toJson(message)));
        obj.addProperty("category",category.name());
        return obj.toString();
    }

    public static RecordEntry fromJson(String line) {
        JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
        if (!obj.has("message")) {
            Component old = Component.Serializer.fromJson(line);
            return old == null ? null
                    : new RecordEntry("", "", old, MessageClassifier.Category.SYSTEM_OTHER);
        }
        MessageClassifier.Category cat;
        try {
            cat = MessageClassifier.Category.valueOf(obj.get("category").getAsString());
        } catch (Exception e) {
            cat = MessageClassifier.Category.SYSTEM_OTHER;
        }
        Component msg = Component.Serializer.fromJson(obj.get("message").toString());
        return msg == null ? null : new RecordEntry(
                        obj.get("time").getAsString(),
                        obj.get("dimension").getAsString(),
                msg, cat);
    }
}
