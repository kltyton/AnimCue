package com.kltyton.interactiveanimation.platform.fabric;

import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import com.kltyton.interactiveanimation.scene.network.SceneTransport;
import com.kltyton.interactiveanimation.scene.playback.ServerScenes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.kltyton.interactiveanimation.scene.network.MinecraftScenePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.level.ServerPlayer;

public final class InteractiveAnimationFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(MinecraftScenePayload.TYPE, MinecraftScenePayload.CODEC);

        ServerScenes.install(new SceneTransport() {
            @Override
            public boolean available(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, MinecraftScenePayload.TYPE);
            }

            @Override
            public void send(ServerPlayer player, ScenePacket packet) {
                ServerPlayNetworking.send(player, new MinecraftScenePayload(packet));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(ServerScenes::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerScenes::shutdown);
    }
}
