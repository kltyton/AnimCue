package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.compat.geckolib.Gecko4ActorPose;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GeoModel.class, remap = false, priority = 900)
abstract class LegacyGeckoActorMixin {
    @Inject(method = "handleAnimations", at = @At("RETURN"))
    private void interactiveAnimation$applyActor(GeoAnimatable animatable, long id, AnimationState<?> state,
                                                 float partialTick, CallbackInfo callback) {
        Gecko4ActorPose.apply((GeoModel<?>) (Object) this, animatable);
    }
}