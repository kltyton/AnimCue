package com.kltyton.interactiveanimation.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class SceneWorldRendererMixin {
    @Unique private final java.util.Set<Entity> interactiveAnimation$submitted =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void interactiveAnimation$beginFrame(CallbackInfo callback) {
        interactiveAnimation$submitted.clear();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$renderBound(Entity entity, double x, double y, double z, float partialTick,
                                                   PoseStack poses, MultiBufferSource buffers, CallbackInfo callback) {
        var actor = ClientScenes.actor(entity, partialTick);
        if (actor == null) return;
        interactiveAnimation$submitted.add(entity);
        LegacyActorRenderer.render(actor, new Vec3(x, y, z), partialTick, poses, buffers);
        callback.cancel();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V", ordinal = 0))
    private void interactiveAnimation$submitPreviews(DeltaTracker delta, boolean outline, Camera camera, GameRenderer renderer, LightTexture light,
                                                     Matrix4f view, Matrix4f projection, CallbackInfo callback) {
        PoseStack poses = new PoseStack();
        float partialTick = delta.getGameTimeDeltaPartialTick(false);
        var client = Minecraft.getInstance();
        if (client.options.getCameraType().isFirstPerson() && client.player != null) {
            interactiveAnimation$submitted.add(client.player);
        }
        var buffers = client.renderBuffers().bufferSource();
        for (var actor : ClientScenes.extras(partialTick, interactiveAnimation$submitted::contains)) {
            LegacyActorRenderer.render(actor, camera.getPosition(), partialTick, poses, buffers);
        }
    }
}
