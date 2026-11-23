package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.api.AnimationControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("RETURN"))
    private void interactiveAnimation$hideMovement(CallbackInfo callback) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.input != this || !AnimationControls.state(player).movement()) return;
        keyPresses = Input.EMPTY;
        moveVector = Vec2.ZERO;
    }
}
