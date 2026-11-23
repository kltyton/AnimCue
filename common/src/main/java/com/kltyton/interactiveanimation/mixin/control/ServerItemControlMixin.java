package com.kltyton.interactiveanimation.mixin.control;

import com.kltyton.interactiveanimation.api.AnimationControls;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerItemControlMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$lockItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                                 CallbackInfoReturnable<InteractionResult> result) {
        if (AnimationControls.state(player).interaction()) result.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void interactiveAnimation$lockBlock(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                                 BlockHitResult hit, CallbackInfoReturnable<InteractionResult> result) {
        if (AnimationControls.state(player).interaction()) result.setReturnValue(InteractionResult.FAIL);
    }
}
