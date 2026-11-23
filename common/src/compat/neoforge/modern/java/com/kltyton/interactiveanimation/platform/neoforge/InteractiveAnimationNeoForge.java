package com.kltyton.interactiveanimation.platform.neoforge;

import com.kltyton.interactiveanimation.scene.network.MinecraftScenePayload;
import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import com.kltyton.interactiveanimation.scene.network.SceneTransport;
import com.kltyton.interactiveanimation.scene.playback.ServerScenes;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@Mod("interactive_animation")
public final class InteractiveAnimationNeoForge {
    public InteractiveAnimationNeoForge(IEventBus events) {
        ServerScenes.install(new SceneTransport() {
            @Override
            public boolean available(ServerPlayer player) {
                return player.connection.hasChannel(MinecraftScenePayload.TYPE);
            }

            @Override
            public void send(ServerPlayer player, ScenePacket packet) {
                PacketDistributor.sendToPlayer(player, new MinecraftScenePayload(packet));
            }
        });
        events.addListener(this::packets);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> ServerScenes.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> ServerScenes.shutdown(event.getServer()));
        if (FMLEnvironment.getDist().isClient()) InteractiveAnimationNeoClient.register(events);
    }

    private void packets(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(MinecraftScenePayload.TYPE, MinecraftScenePayload.CODEC);
    }
}
