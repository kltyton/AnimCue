package com.kltyton.interactiveanimation.mixin.client;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRendererInternals;
import com.geckolib.renderer.base.RenderPassInfo;
import com.kltyton.interactiveanimation.client.compat.geckolib.GeckoScenePose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies participant channels after the host controllers have produced their pose. */
@Mixin(value = GeoRendererInternals.class, remap = false, priority = 900)
public interface GeckoActorMixin {
    @Inject(method = "applyAnimationControllers", at = @At("RETURN"))
    private void interactiveAnimation$applyActor(RenderPassInfo<?> pass, BoneSnapshots snapshots, CallbackInfo callback) {
        GeckoScenePose.apply(pass, snapshots);
    }
}
