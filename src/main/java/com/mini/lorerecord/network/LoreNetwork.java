package com.mini.lorerecord.network;
import com.mini.lorerecord.LoreRecord;
import com.mini.lorerecord.export.LoreBook;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = LoreRecord.MODID)
public final class LoreNetwork {

    /** 频道协议版本 */
    private static final String VERSION = "1";
    /** 单页最大长度 */
    private static final int MAX_PAGE_LENGTH = 8192;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.parse(LoreRecord.MODID + ":main"),
            () -> VERSION, s -> true, s -> true);

    /** 是否安装了模组（*/
    private static volatile boolean serverInstalled;

    private LoreNetwork() {
    }


    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, GiveBookPacket.class,
                GiveBookPacket::encode, GiveBookPacket::decode, GiveBookPacket::handle);
        CHANNEL.registerMessage(id++, ServerHelloPacket.class,
                ServerHelloPacket::encode, ServerHelloPacket::decode, ServerHelloPacket::handle);
    }

    /** 联机时：服务器是否装了模组 */
    public static boolean isServerInstalled() {
        return serverInstalled;
    }

    /** 断开连接时复位 */
    public static void resetServerFlag() {
        serverInstalled = false;
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ServerHelloPacket());
        }
    }



    public record GiveBookPacket(String title, List<String> pages) {

        static void encode(GiveBookPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.title, WrittenBookItem.TITLE_MAX_LENGTH);
            buf.writeVarInt(msg.pages.size());
            for (String page : msg.pages) {
                buf.writeUtf(page, MAX_PAGE_LENGTH);
            }
        }

        static GiveBookPacket decode(FriendlyByteBuf buf) {
            String title = buf.readUtf(WrittenBookItem.TITLE_MAX_LENGTH);
            int count = Math.min(buf.readVarInt(), WrittenBookItem.MAX_PAGES);
            List<String> pages = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                pages.add(buf.readUtf(MAX_PAGE_LENGTH));
            }
            return new GiveBookPacket(title, pages);
        }

        static void handle(GiveBookPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) {
                    return;
                }
                ItemStack book = LoreBook.fromPages(msg.title, player.getName().getString(), msg.pages);
                if (!player.getInventory().add(book)) {
                    player.drop(book, false);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }


    public record ServerHelloPacket() {

        static void encode(ServerHelloPacket msg, FriendlyByteBuf buf) {
        }

        static ServerHelloPacket decode(FriendlyByteBuf buf) {
            return new ServerHelloPacket();
        }

        static void handle(ServerHelloPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> serverInstalled = true);
            ctx.get().setPacketHandled(true);
        }
    }
}
