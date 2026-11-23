package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.client.render.ModelBones;
import com.kltyton.interactiveanimation.client.render.SceneRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.class)
abstract class SceneModelFeatureMixin {
    @Unique private Runnable interactiveAnimation$restore;

    @Inject(method = "renderModel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void interactiveAnimation$applyPose(SubmitNodeStorage.ModelSubmit<?> submit, RenderType type, VertexConsumer buffer, OutlineBufferSource outlines, MultiBufferSource.BufferSource crumbling, CallbackInfo callback) {
        Model<?> model = submit.model();
        Object state = submit.state();
        if (state instanceof SceneRenderState scene && scene.interactiveAnimation$actor() != null) {
            interactiveAnimation$restore = ModelBones.apply(scene.interactiveAnimation$actor(), name -> ModelBones.find(model.root(), name));
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void interactiveAnimation$restorePose(SubmitNodeStorage.ModelSubmit<?> submit, RenderType type, VertexConsumer buffer,
                                                  OutlineBufferSource outlines, MultiBufferSource.BufferSource crumbling, CallbackInfo callback) {
        if (interactiveAnimation$restore == null) return;
        interactiveAnimation$restore.run();
        interactiveAnimation$restore = null;
    }
}
