package com.kltyton.interactiveanimation.mixin.control;

import com.kltyton.interactiveanimation.api.AnimationControls;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
abstract class MobAiMixin {
    @Inject(method = "isEffectiveAi", at = @At("RETURN"), cancellable = true)
    private void interactiveAnimation$pauseSceneAi(CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ() && AnimationControls.state((Mob) (Object) this).ai()) result.setReturnValue(false);
    }
}
