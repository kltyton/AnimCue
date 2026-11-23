package com.kltyton.interactiveanimation.platform.forge;

import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import com.kltyton.interactiveanimation.scene.network.SceneTransport;
import com.kltyton.interactiveanimation.scene.playback.ServerScenes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod("interactive_animation")
public final class InteractiveAnimationForge {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("interactive_animation", "scene"), () -> "1",
            NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));

    public InteractiveAnimationForge() {
        CHANNEL.messageBuilder(ScenePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScenePacket::write).decoder(ScenePacket::read)
                .consumerMainThread((scene, context) -> InteractiveAnimationForgeClient.receive(scene)).add();

        ServerScenes.install(new SceneTransport() {
            @Override
            public boolean available(ServerPlayer player) {
                return CHANNEL.isRemotePresent(player.connection.connection);
            }

            @Override
            public void send(ServerPlayer player, ScenePacket packet) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) ServerScenes.tick(event.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> ServerScenes.shutdown(event.getServer()));
    }
}
