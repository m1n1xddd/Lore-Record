package com.mini.lorerecord;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

/**
 * 聊天记录书生成
 */
public final class LoreBook {

    private static final String TAG_LORE_RECORD = "lorerecord";
    /** 行数 */
    private static final int LINES_PER_PAGE = 14;
    /** 字符数*/
    private static final int CHARS_PER_LINE = 19;

    private LoreBook() {
    }

    /**
     * 生成聊天记录书
     *
     * @param title    书名
     * @param author   作者
     * @param messages 消息
     */
    public static ItemStack create(String title, String author, List<Component> messages) {
        List<String> pages = new ArrayList<>();
        MutableComponent page = Component.empty();
        int usedLines = 0;
        for (Component msg : messages) {
            if (pages.size() >= WrittenBookItem.MAX_PAGES - 1) {
                page.append(Component.literal("……记录过长，只保留前 " + pages.size() + " 页"));
                break;
            }
            int lines = estimatedLines(msg) + 1; // 换行
            if (usedLines > 0 && usedLines + lines > LINES_PER_PAGE) {
                pages.add(flushPage(page));
                page = Component.empty();
                usedLines = 0;
            }
            page.append(msg);
            page.append("\n");
            usedLines += lines;
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

    private static int estimatedLines(Component msg) {
        int len = msg.getString().length();
        return Math.max(1, (len + CHARS_PER_LINE - 1) / CHARS_PER_LINE);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
