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
import com.kltyton.interactiveanimation.scene.network.ScenePacket;

public final class InteractiveAnimationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation("interactive_animation", "scenes");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                ClientScenes.reload(manager);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(InteractiveAnimationFabric.CHANNEL, (client, handler, buffer, response) -> {
            ScenePacket scene = ScenePacket.read(buffer);
            client.execute(() -> ClientScenes.handle(scene));
        });
        ClientTickEvents.END_CLIENT_TICK.register(ClientScenes::tick);
    }
}
