package com.kltyton.interactiveanimation.platform.neoforge;

import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.interactiveanimation.scene.network.MinecraftScenePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
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
        events.addListener((AddClientReloadListenersEvent event) -> event.addListener(
                Identifier.fromNamespaceAndPath("interactive_animation", "scenes"),
                (ResourceManagerReloadListener) ClientScenes::reload));
        events.addListener((RegisterClientPayloadHandlersEvent event) -> event.register(MinecraftScenePayload.TYPE,
                (payload, context) -> context.enqueueWork(() -> receive(payload))));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientScenes.tick(Minecraft.getInstance()));
    }

    static void receive(MinecraftScenePayload payload) {
        ClientScenes.handle(payload.scene());
    }
}
