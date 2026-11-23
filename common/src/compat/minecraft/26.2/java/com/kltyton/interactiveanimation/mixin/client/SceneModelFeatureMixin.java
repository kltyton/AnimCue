package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.client.render.ModelBones;
import com.kltyton.interactiveanimation.client.render.SceneRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.class)
abstract class SceneModelFeatureMixin {
    @Unique private Runnable interactiveAnimation$restore;

    @Inject(method = "prepareModel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void interactiveAnimation$applyPose(ModelFeatureRenderer.Submit<?> submit, CallbackInfo callback) {
        Model<?> model = submit.model();
        Object state = submit.state();
        if (state instanceof SceneRenderState scene && scene.interactiveAnimation$actor() != null) {
            interactiveAnimation$restore = ModelBones.apply(scene.interactiveAnimation$actor(), name -> ModelBones.find(model.root(), name));
        }
    }

    @Inject(method = "prepareModel", at = @At("RETURN"))
    private void interactiveAnimation$restorePose(ModelFeatureRenderer.Submit<?> submit, CallbackInfo callback) {
        if (interactiveAnimation$restore == null) return;
        interactiveAnimation$restore.run();
        interactiveAnimation$restore = null;
    }
}
