package com.mini.lorerecord.mixin;

import com.mini.lorerecord.server.GlobalRecorder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSystemMessageMixin {

    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void onSendSystemMessage(Component component, CallbackInfo ci) {
        GlobalRecorder.onSystemMessage(component, (ServerPlayer) (Object) this);
    }
}
