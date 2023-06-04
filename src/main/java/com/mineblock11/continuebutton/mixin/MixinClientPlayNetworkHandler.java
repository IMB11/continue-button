package com.mineblock11.continuebutton.mixin;

import com.mineblock11.continuebutton.ContinueButtonMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {
    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At("TAIL"), method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
    public void onGameJoin(GameJoinS2CPacket packet, CallbackInfo info) {
        ServerInfo serverInfo = this.client.getCurrentServerEntry();
        if(serverInfo != null) {
            ContinueButtonMod.lastLocal = false;
            ContinueButtonMod.serverName = serverInfo.name;
            ContinueButtonMod.serverAddress = serverInfo.address;
        } else {
            ContinueButtonMod.lastLocal = true;
        }
        ContinueButtonMod.saveConfig();
    }
}
