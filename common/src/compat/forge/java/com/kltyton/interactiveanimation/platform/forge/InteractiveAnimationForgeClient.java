package com.kltyton.interactiveanimation.platform.forge;

import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "interactive_animation", value = Dist.CLIENT)
public final class InteractiveAnimationForgeClient {
    private InteractiveAnimationForgeClient() {}

    static void receive(ScenePacket packet) {
        ClientScenes.handle(packet);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ClientScenes.tick(Minecraft.getInstance());
    }

    @Mod.EventBusSubscriber(modid = "interactive_animation", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Resources {
        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) ClientScenes::reload);
        }
    }
}
