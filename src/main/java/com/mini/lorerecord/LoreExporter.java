package com.mini.lorerecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 导出器
 */
public final class LoreExporter {

    /** 导出目录：<游戏目录>/lore_record */
    public static final Path DIR = FMLPaths.GAMEDIR.get().resolve("lore_record");

    /** 暂存文件*/
    private static final Path PENDING = DIR.resolve("pending_record.txt");

    private LoreExporter() {
    }

    /** 记录名+消息列表 */
    public record Pending(String name, List<Component> messages) {
    }


    public static boolean hasPending() {
        return Files.isRegularFile(PENDING);
    }

    public static void savePending(String name, List<Component> msgs) {
        try {
            Files.createDirectories(DIR);
            List<String> lines = new ArrayList<>();
            lines.add(name);
            for (Component msg : msgs) {
                lines.add(Component.Serializer.toJson(msg));
            }
            Files.write(PENDING, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LoreRecord.LOGGER.error("暂存聊天记录失败", e);
        }
    }

    public static Pending loadPending() {
        try {
            List<String> lines = Files.readAllLines(PENDING, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return null;
            }
            return new Pending(lines.get(0), parseJsonLines(lines.subList(1, lines.size())));
        } catch (Exception e) {
            LoreRecord.LOGGER.error("读取暂存聊天记录失败", e);
            return null;
        }
    }

    public static void deletePending() {
        try {
            Files.deleteIfExists(PENDING);
        } catch (IOException e) {
            LoreRecord.LOGGER.error("删除暂存文件失败", e);
        }
    }

    public static Path export(String name, List<Component> msgs) {
        Path dir = DIR.resolve(name); // 每条记录独占一个子文件夹，重新导出同名记录时直接覆盖
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + ".txt"), toPlainText(msgs), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(name + ".md"), toMarkdown(name, msgs), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(name + ".html"), toHtmlDocument(name, msgs), StandardCharsets.UTF_8);
            Files.write(dir.resolve(name + ".json"), toJsonLines(msgs), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LoreRecord.LOGGER.error("导出聊天记录失败", e);
        }
        return dir;
    }

    public static List<Component> loadMessages(String name) {
        Path file = DIR.resolve(name).resolve(name + ".json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return parseJsonLines(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LoreRecord.LOGGER.error("读取聊天记录失败：{}", name, e);
            return null;
        }
    }

    public static boolean isValidName(String name) {
        if (name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                return false;
            }
        }
        return true;
    }

    public static List<String> listRecords() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(DIR)) {
            return names;
        }
        try (var stream = Files.list(DIR)) {
            stream.filter(Files::isDirectory).forEach(p -> names.add(p.getFileName().toString()));
        } catch (IOException e) {
            LoreRecord.LOGGER.error("读取记录列表失败", e);
        }
        names.sort(String::compareTo);
        return names;
    }


    private static List<String> toJsonLines(List<Component> msgs) {
        List<String> lines = new ArrayList<>();
        for (Component msg : msgs) {
            lines.add(Component.Serializer.toJson(msg));
        }
        return lines;
    }

    private static String toPlainText(List<Component> msgs) {
        StringBuilder sb = new StringBuilder();
        for (Component msg : msgs) {
            sb.append(msg.getString()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static String toMarkdown(String name, List<Component> msgs) {
        StringBuilder sb = new StringBuilder();
        sb.append("#### &lt; ").append(name).append(" &gt; 记录开始。\n\n");
        for (Component msg : msgs) {
            sb.append("- ").append(msg.getString().replace("\n", " ")).append('\n');
        }
        sb.append("#### 记录结束。\n");
        return sb.toString();
    }

    private static String toHtmlDocument(String name, List<Component> msgs) {
        boolean light = Config.HTML_THEME.get() == Config.HtmlTheme.LIGHT;
        String background = light ? "#f6f5f5" : "#1e1e1e";
        String foreground = light ? "#000000" : "#ffffff";
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"zh\">\n<head>\n<meta charset=\"utf-8\">\n");
        sb.append("<title>").append(escapeHtml(name)).append("</title>\n<style>\n");
        sb.append("body{background:").append(background).append(";color:").append(foreground).append(";")
                .append("font-family:Consolas,'Courier New',monospace;padding:1.5em;line-height:1.6}\n");
        sb.append("p{margin:.2em 0}\n");
        sb.append(".obf{display:inline-block;animation:obf .2s steps(2) infinite}\n");
        sb.append("@keyframes obf{to{transform:translateX(2px)}}\n");
        sb.append("</style>\n</head>\n<body>\n")
                .append("<h4 style=\"color:#898787;\">")
                .append("&lt;").append(escapeHtml(name)).append("&gt; 记录开始。</h4>\n");
        for (Component msg : msgs) {
            sb.append("<p>").append(toHtmlLine(msg)).append("</p>\n");
        }
        sb.append("\n<h4 style=\"color:#898787;\">\n记录结束。</h4>\n");
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private static String toHtmlLine(Component msg) {
        StringBuilder sb = new StringBuilder();
        for (Component leaf : msg.toFlatList()) {
            sb.append(toStyledSpan(leaf));
        }
        return sb.toString();
    }

    private static String toStyledSpan(Component leaf) {
        String text = escapeHtml(leaf.getString());
        Style style = leaf.getStyle();

        List<String> props = new ArrayList<>();
        if (style.getColor() != null) {
            props.add("color:" + String.format("#%06X", style.getColor().getValue()));
        }
        if (style.isBold()) {
            props.add("font-weight:bold");
        }
        if (style.isItalic()) {
            props.add("font-style:italic");
        }
        String deco = (style.isUnderlined() ? "underline " : "") + (style.isStrikethrough() ? "line-through" : "");
        if (!deco.isBlank()) {
            props.add("text-decoration:" + deco.trim());
        }
        if (props.isEmpty() && !style.isObfuscated()) {
            return text; // 无样式，直接输出纯文本
        }
        return "<span" + (style.isObfuscated() ? " class=\"obf\"" : "")
                + " style=\"" + String.join(";", props) + "\">" + text + "</span>";
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static List<Component> parseJsonLines(List<String> lines) {
        List<Component> msgs = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            Component component = Component.Serializer.fromJson(line);
            if (component != null) {
                msgs.add(component);
            }
        }
        return msgs;
    }
}
