package com.kltyton.interactiveanimation.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.kltyton.interactiveanimation.client.playback.ClientActor;
import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.interactiveanimation.client.render.RootPose;
import com.kltyton.interactiveanimation.client.render.SceneRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
abstract class ActorRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void interactiveAnimation$extract(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> result) {
        var state = result.getReturnValue();
        var sceneState = (SceneRenderState) state;
        ClientActor actor = ClientScenes.actor(entity, partialTick);
        sceneState.interactiveAnimation$actor(actor);
        sceneState.interactiveAnimation$nativeYaw(state instanceof LivingEntityRenderState living ? living.bodyRot : entity.getYRot());
        if (actor == null) return;
        state.x = actor.pose().position().x;
        state.y = actor.pose().position().y;
        state.z = actor.pose().position().z;
    }

    @Inject(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void interactiveAnimation$orient(EntityRenderState state, CameraRenderState camera, double x, double y,
                                             double z, PoseStack poses, SubmitNodeCollector output, CallbackInfo callback) {
        var rendered = (SceneRenderState) state;
        var actor = rendered.interactiveAnimation$actor();
        if (actor == null) return;
        poses.pushPose();
        RootPose.orient(poses, actor, rendered.interactiveAnimation$nativeYaw());
    }

    @Inject(method = "submit", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void interactiveAnimation$unorient(EntityRenderState state, CameraRenderState camera, double x, double y,
                                               double z, PoseStack poses, SubmitNodeCollector output, CallbackInfo callback) {
        if (((SceneRenderState) state).interactiveAnimation$actor() != null) poses.popPose();
    }
}
