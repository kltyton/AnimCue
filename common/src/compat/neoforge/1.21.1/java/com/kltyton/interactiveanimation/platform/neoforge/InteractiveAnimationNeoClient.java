package com.kltyton.interactiveanimation.platform.neoforge;

import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.interactiveanimation.scene.network.MinecraftScenePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

final class InteractiveAnimationNeoClient {
    private InteractiveAnimationNeoClient() {}

    static void register(IEventBus events) {
        var mods = net.neoforged.fml.ModList.get();
        if (mods.isLoaded("player_animation_library")) {
            com.kltyton.interactiveanimation.client.compat.pal.PalScenes.initialize(
                    mods.isLoaded("playeranimationlibrarymorerotation"));
        }
        events.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) ClientScenes::reload));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientScenes.tick(Minecraft.getInstance()));
    }

    static void receive(MinecraftScenePayload payload) {
        ClientScenes.handle(payload.scene());
    }
}
