package com.kltyton.interactiveanimation.platform.fabric;

import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import com.kltyton.interactiveanimation.scene.network.MinecraftScenePayload;

public final class InteractiveAnimationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var mods = net.fabricmc.loader.api.FabricLoader.getInstance();
        if (mods.isModLoaded("player_animation_library")) {
            com.kltyton.interactiveanimation.client.compat.pal.PalScenes.initialize(
                    mods.isModLoaded("playeranimationlibrarymorerotation"));
        }
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath("interactive_animation", "scenes");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                ClientScenes.reload(manager);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(MinecraftScenePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientScenes.handle(payload.scene())));
        ClientTickEvents.END_CLIENT_TICK.register(ClientScenes::tick);
    }
}
