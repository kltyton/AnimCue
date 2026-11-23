package com.kltyton.interactiveanimation.platform.fabric;

import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import com.kltyton.interactiveanimation.scene.network.SceneTransport;
import com.kltyton.interactiveanimation.scene.playback.ServerScenes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class InteractiveAnimationFabric implements ModInitializer {
    static final ResourceLocation CHANNEL = new ResourceLocation("interactive_animation", "scene");

    @Override
    public void onInitialize() {

        ServerScenes.install(new SceneTransport() {
            @Override
            public boolean available(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, CHANNEL);
            }

            @Override
            public void send(ServerPlayer player, ScenePacket packet) {
                var buffer = PacketByteBufs.create();
                packet.write(buffer);
                ServerPlayNetworking.send(player, CHANNEL, buffer);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(ServerScenes::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerScenes::shutdown);
    }
}
