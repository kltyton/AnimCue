package com.kltyton.interactiveanimation.mixin.client;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.kltyton.interactiveanimation.client.compat.geckolib.GeckoScenePose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Composes the scene after KBR's native addon pose, including additional geometry passes. */
@Pseudo
@Mixin(targets = "com.kltyton.bedrockruntime.client.addon.BedrockAddonRenderer", remap = false)
abstract class KbrAddonActorMixin {
    @Inject(method = "applyAddonPose", at = @At("RETURN"))
    private static void interactiveAnimation$applyActor(RenderPassInfo<?> pass, BoneSnapshots snapshots,
                                                        @Coerce Object pose, CallbackInfo callback) {
        GeckoScenePose.apply(pass, snapshots);
    }
}
