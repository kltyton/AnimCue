package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.api.AnimationControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("RETURN"))
    private void interactiveAnimation$hideMovement(boolean slow, float speed, CallbackInfo callback) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.input != this || !AnimationControls.state(player).movement()) return;
        leftImpulse = 0;
        forwardImpulse = 0;
        up = down = left = right = jumping = shiftKeyDown = false;
    }
}
