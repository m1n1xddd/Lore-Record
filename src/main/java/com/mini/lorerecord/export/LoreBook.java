package com.mini.lorerecord.export;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

/**
 * 聊天记录书生成
 */
public final class LoreBook {

    private static final String TAG_LORE_RECORD = "lorerecord";

    private static final int PAGE_WIDTH = 114;

    private static final int PAGE_LINES = 14;

    private LoreBook() {
    }

    /**
     * 生成聊天记录书
     *
     * @param title    书名
     * @param author   作者
     * @param messages 聊天记录
     */
    public static ItemStack create(String title, String author, List<Component> messages) {
        List<String> pages = new ArrayList<>();
        MutableComponent page = Component.empty();
        int usedLines = 0;
        boolean overflow = false;
        for (Component msg : messages) {
            for (Component line : wrapLines(msg)) {
                if (pages.size() >= WrittenBookItem.MAX_PAGES - 1) {
                    page.append(Component.literal("……记录过长，只保留前 " + pages.size() + " 页"));
                    overflow = true;
                    break;
                }
                if (usedLines >= PAGE_LINES) {
                    pages.add(flushPage(page));
                    page = Component.empty();
                    usedLines = 0;
                }
                if (usedLines > 0) {
                    page.append("\n");
                }
                page.append(line);
                usedLines++;
            }
            if (overflow) {
                break;
            }
        }
        if (usedLines > 0) {
            pages.add(flushPage(page));
        }
        if (pages.isEmpty()) {
            pages.add(Component.Serializer.toJson(Component.literal("（空空如也）")));
        }
        return fromPages(title, author, pages);
    }


    public static ItemStack fromPages(String title, String author, List<String> pages) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, truncate(title, WrittenBookItem.TITLE_MAX_LENGTH));
        tag.putString(WrittenBookItem.TAG_AUTHOR, author);
        tag.putInt(WrittenBookItem.TAG_GENERATION, 0);
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        tag.putBoolean(TAG_LORE_RECORD, true);
        ListTag list = new ListTag();
        for (String page : pages) {
            list.add(StringTag.valueOf(page));
        }
        tag.put(WrittenBookItem.TAG_PAGES, list);
        return stack;
    }

    public static List<String> pagesOf(ItemStack book) {
        List<String> pages = new ArrayList<>();
        CompoundTag tag = book.getTag();
        if (tag != null && tag.contains(WrittenBookItem.TAG_PAGES)) {
            ListTag list = tag.getList(WrittenBookItem.TAG_PAGES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                pages.add(list.getString(i));
            }
        }
        return pages;
    }

    //翻页
    private static String flushPage(MutableComponent page) {
        return Component.Serializer.toJson(page);
    }

    // 拆分页
    private static List<Component> wrapLines(Component msg) {
        List<Component> lines = new ArrayList<>();
        MutableComponent line = Component.empty();
        int width = 0;
        for (Component leaf : msg.toFlatList()) {
            Style style = leaf.getStyle();
            String text = leaf.getString();
            int start = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    appendText(line, text.substring(start, i), style);
                    lines.add(line);
                    line = Component.empty();
                    width = 0;
                    start = i + 1;
                    continue;
                }
                int w = charWidth(c, style);
                if (width + w > PAGE_WIDTH && i > start) {
                    appendText(line, text.substring(start, i), style);
                    lines.add(line);
                    line = Component.empty();
                    width = 0;
                    start = i;
                }
                width += w;
            }
            appendText(line, text.substring(start), style);
        }
        if (!line.getString().isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    private static void appendText(MutableComponent line, String text, Style style) {
        if (!text.isEmpty()) {
            line.append(Component.literal(text).withStyle(style));
        }
    }


    private static int charWidth(char c, Style style) {
        int w;
        if (c >= 0x2E80) {
            w = 9;
        } else if ("ilI.,:;'!|`".indexOf(c) >= 0) {
            w = 2;
        } else if (Character.isWhitespace(c)) {
            w = 4;
        } else {
            w = 6;
        }
        if (style != null && style.isBold()) {
            w += 1;
        }
        return w;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
