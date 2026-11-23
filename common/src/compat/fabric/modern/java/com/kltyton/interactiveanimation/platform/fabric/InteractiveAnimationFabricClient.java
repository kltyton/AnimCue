package com.kltyton.interactiveanimation.platform.fabric;

import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import com.kltyton.interactiveanimation.scene.network.MinecraftScenePayload;

public final class InteractiveAnimationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var mods = net.fabricmc.loader.api.FabricLoader.getInstance();
        if (mods.isModLoaded("player_animation_library")) {
            com.kltyton.interactiveanimation.client.compat.pal.PalScenes.initialize(
                    mods.isModLoaded("playeranimationlibrarymorerotation"));
        }
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("interactive_animation", "scenes"),
                (ResourceManagerReloadListener) ClientScenes::reload);
        ClientPlayNetworking.registerGlobalReceiver(MinecraftScenePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientScenes.handle(payload.scene())));
        ClientTickEvents.END_CLIENT_TICK.register(ClientScenes::tick);
    }
}
