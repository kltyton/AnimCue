package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.api.AnimationControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftActionsMixin {
    @Shadow public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$lockAttack(CallbackInfoReturnable<Boolean> result) {
        if (player != null && AnimationControls.state(player).attacks()) result.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$lockHeldAttack(boolean down, CallbackInfo callback) {
        if (down && player != null && AnimationControls.state(player).attacks()) callback.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$lockInteraction(CallbackInfo callback) {
        if (player != null && AnimationControls.state(player).interaction()) callback.cancel();
    }
}
