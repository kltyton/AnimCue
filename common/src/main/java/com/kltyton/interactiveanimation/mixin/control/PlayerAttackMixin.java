package com.kltyton.interactiveanimation.mixin.control;

import com.kltyton.interactiveanimation.api.AnimationControls;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$lockAttack(Entity target, CallbackInfo callback) {
        if (AnimationControls.state((Player) (Object) this).attacks()) callback.cancel();
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$lockInteraction(CallbackInfoReturnable<InteractionResult> result) {
        if (AnimationControls.state((Player) (Object) this).interaction()) result.setReturnValue(InteractionResult.FAIL);
    }
}
