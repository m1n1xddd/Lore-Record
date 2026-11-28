package com.mini.lorerecord.export;
import com.mini.lorerecord.LoreRecord;
import com.mini.lorerecord.config.Config;
import com.mini.lorerecord.model.RecordEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 导出器
 */
public final class LoreExporter {

    /** 导出目录*/
    public static final Path DIR = FMLPaths.GAMEDIR.get().resolve("lore_record");

    private static final Path PENDING = DIR.resolve("pending_record.txt");

    public static final Path GLOBAL_DIR = DIR.resolve("global");

    private static final Path GLOBAL_PENDING = GLOBAL_DIR.resolve("pending_record.txt");

    private LoreExporter() {
    }


    public record Pending(String name, List<RecordEntry> messages) {
    }


    /** 是否有暂存 */
    public static boolean hasPending() {
        return hasPending(false);
    }

    public static boolean hasPending(boolean global) {
        return Files.isRegularFile(global ? GLOBAL_PENDING : PENDING);
    }

    public static void savePending(String name, List<RecordEntry> msgs) {
        savePending(name, msgs, false);
    }

    public static void savePending(String name, List<RecordEntry> msgs, boolean global) {
        Path pending = global ? GLOBAL_PENDING : PENDING;
        try {
            Files.createDirectories(pending.getParent());
            List<String> lines = new ArrayList<>();
            lines.add(name);
            for (RecordEntry msg : msgs) {
                lines.add(msg.toJson());
            }
            Files.write(pending, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LoreRecord.LOGGER.error("暂存聊天记录失败", e);
        }
    }

    public static Pending loadPending() {
        return loadPending(false);
    }

    public static Pending loadPending(boolean global) {
        Path pending = global ? GLOBAL_PENDING : PENDING;
        try {
            List<String> lines = Files.readAllLines(pending, StandardCharsets.UTF_8);
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
        deletePending(false);
    }

    public static void deletePending(boolean global) {
        try {
            Files.deleteIfExists(global ? GLOBAL_PENDING : PENDING);
        } catch (IOException e) {
            LoreRecord.LOGGER.error("删除暂存文件失败", e);
        }
    }

    public static Path export(String name, List<RecordEntry> msgs) {
        return export(name, msgs, false);
    }

    public static Path export(String name, List<RecordEntry> msgs, boolean global) {
        Path dir = (global ? GLOBAL_DIR : DIR).resolve(name); // 导出同名记录直接覆盖
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + ".txt"), toPlainText(msgs), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(name + ".md"), toMarkdown(name, msgs), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(name + ".html"), toHtmlDocument(name, msgs), StandardCharsets.UTF_8);
            Files.write(dir.resolve(name + ".json"), toJsonLines(msgs), StandardCharsets.UTF_8);
            writeDocx(dir.resolve(name + ".docx"), name, msgs);
        } catch (IOException e) {
            LoreRecord.LOGGER.error("导出聊天记录失败", e);
        }
        return dir;
    }

    public static List<RecordEntry> loadMessages(String name) {
        return loadMessages(name, false);
    }

    public static List<RecordEntry> loadMessages(String name, boolean global) {
        Path file = (global ? GLOBAL_DIR : DIR).resolve(name).resolve(name + ".json");
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
        return listRecords(false);
    }

    public static List<String> listRecords(boolean global) {
        Path dir = global ? GLOBAL_DIR : DIR;
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().equals("global")) // 排除全服记录子目录
                    .forEach(p -> names.add(p.getFileName().toString()));
        } catch (IOException e) {
            LoreRecord.LOGGER.error("读取记录列表失败", e);
        }
        names.sort(String::compareTo);
        return names;
    }


    private static List<String> toJsonLines(List<RecordEntry> msgs) {
        List<String> lines = new ArrayList<>();
        for (RecordEntry msg : msgs) {
            lines.add(msg.toJson());
        }
        return lines;
    }

    private static String toPlainText(List<RecordEntry> msgs) {
        StringBuilder sb = new StringBuilder();
        for (RecordEntry msg : msgs) {
            sb.append(msg.messagesWithStyle().getString()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static String toMarkdown(String name, List<RecordEntry> msgs) {
        StringBuilder sb = new StringBuilder();
        sb.append("#### &lt; ").append(name).append(" &gt; 记录开始。\n\n");
        for (RecordEntry msg : msgs) {
            sb.append(msg.messagesWithStyle().getString().replace("\n", "  \n")).append('\n');
        }
        sb.append("#### 记录结束。\n");
        return sb.toString();
    }

    private static String toHtmlDocument(String name, List<RecordEntry> msgs) {
        boolean light = Config.HTML_THEME.get() == Config.HtmlTheme.LIGHT;
        String background = light ? "#ffffff" : "#1e1e1e";
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
        for (RecordEntry msg : msgs) {
            sb.append("<p>").append(toHtmlLine(msg.messagesWithStyle())).append("</p>\n");
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
        String text = escapeHtml(leaf.getString()).replace("\n", "<br>\n");
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

    private static void writeDocx(Path file, String name, List<RecordEntry> msgs) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
            putZipEntry(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                            + "<Override PartName=\"/word/settings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml\"/>"
                            + "</Types>");
            putZipEntry(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                            + "</Relationships>");
            putZipEntry(zos, "word/_rels/document.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings\" Target=\"settings.xml\"/>"
                            + "</Relationships>");
            putZipEntry(zos, "word/settings.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<w:settings xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                            + "<w:displayBackgroundShape/>"
                            + "</w:settings>");
            putZipEntry(zos, "word/document.xml", toDocxDocument(name, msgs));
        }
    }

    private static void putZipEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String toDocxDocument(String name, List<RecordEntry> msgs) {
        boolean light = Config.DOCX_THEME.get() == Config.HtmlTheme.LIGHT;
        String background = light ? "F6F5F5" : "1E1E1E";
        String foreground = light ? "000000" : "FFFFFF";
        StringBuilder body = new StringBuilder();
        body.append(docxParagraph(
                docxRun("< " + name + " > 记录开始。", "898787", false, false, false, false)));
        for (RecordEntry msg : msgs) {
            StringBuilder runs = new StringBuilder();
            for (Component leaf : msg.messagesWithStyle().toFlatList()) {
                Style style = leaf.getStyle();
                String color = style.getColor() != null
                        ? String.format("%06X", style.getColor().getValue())
                        : foreground;
                runs.append(docxRun(leaf.getString(), color,
                        style.isBold(), style.isItalic(), style.isUnderlined(), style.isStrikethrough()));
            }
            body.append(docxParagraph(runs.toString()));
        }
        body.append(docxParagraph(
                docxRun("记录结束。", "898787", false, false, false, false)));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:background w:color=\"" + background + "\"/>"
                + "<w:body>" + body
                + "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
                + "<w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/>"
                + "</w:sectPr></w:body></w:document>";
    }

    private static String docxParagraph(String runs) {
        return "<w:p><w:pPr><w:spacing w:before=\"0\" w:after=\"0\" w:line=\"360\" w:lineRule=\"auto\"/>"
                + "</w:pPr>" + runs + "</w:p>";
    }

    private static String docxRun(String text, String color, boolean bold, boolean italic,
                                  boolean underline, boolean strike) {
        StringBuilder sb = new StringBuilder("<w:r><w:rPr>");
        sb.append("<w:rFonts w:ascii=\"Consolas\" w:hAnsi=\"Consolas\" w:eastAsia=\"Consolas\"/>");
        sb.append("<w:color w:val=\"").append(color).append("\"/>");
        if (bold) {
            sb.append("<w:b/>");
        }
        if (italic) {
            sb.append("<w:i/>");
        }
        if (underline) {
            sb.append("<w:u w:val=\"single\"/>");
        }
        if (strike) {
            sb.append("<w:strike/>");
        }
        sb.append("</w:rPr>");
        String[] parts = text.split("\n", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append("<w:br/>");
            }
            if (!parts[i].isEmpty()) {
                sb.append("<w:t xml:space=\"preserve\">").append(escapeXml(parts[i])).append("</w:t>");
            }
        }
        sb.append("</w:r>");
        return sb.toString();
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static List<RecordEntry> parseJsonLines(List<String> lines) {
        List<RecordEntry> msgs = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            RecordEntry entry = RecordEntry.fromJson(line);
            if (entry != null) {
                msgs.add(entry);
            }
        }
        return msgs;
    }
}
