package com.mini.lorerecord.mixin;

import com.mini.lorerecord.server.GlobalRecorder;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSystemMessageMixin {
    //注入，唉，描述符是人写的不
    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void onSendSystemMessage(Component component, CallbackInfo ci) {
        GlobalRecorder.onSystemMessage(component, (ServerPlayer) (Object) this);
    }

    @Inject(method = "sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V",at = @At("HEAD"))
    private void onSendChatMessage(OutgoingChatMessage message, boolean overlay, ChatType.Bound bound,CallbackInfo ci) {
        GlobalRecorder.onChatMessage(message, bound, (ServerPlayer) (Object) this);
    }
}
