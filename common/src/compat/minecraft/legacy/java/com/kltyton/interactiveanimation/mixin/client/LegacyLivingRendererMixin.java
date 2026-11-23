package com.kltyton.interactiveanimation.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import com.kltyton.interactiveanimation.client.render.ModelBones;
import com.kltyton.interactiveanimation.client.render.PlayerPoseOwnership;
import com.kltyton.interactiveanimation.client.render.PlayerSkinLayers;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
abstract class LegacyLivingRendererMixin {
    @Shadow protected EntityModel<?> model;

    // LegacyActorRenderer already applies the difference between native entity yaw and scene yaw.
    // Feed that same native baseline into vanilla instead of its independent, interpolated body yaw.
    @Group(name = "interactiveAnimation$sceneBodyYaw", min = 1, max = 1)
    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"),
            index = 3, require = 0)
    private float interactiveAnimation$bodyYawLegacy(float bodyYaw) {
        return interactiveAnimation$sceneBodyBaseline(bodyYaw);
    }

    @Group(name = "interactiveAnimation$sceneBodyYaw", min = 1, max = 1)
    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"),
            index = 3, require = 0)
    private float interactiveAnimation$bodyYawScaled(float bodyYaw) {
        return interactiveAnimation$sceneBodyBaseline(bodyYaw);
    }

    @Unique
    private static float interactiveAnimation$sceneBodyBaseline(float nativeBodyYaw) {
        var actor = LegacyActorRenderer.current();
        return actor != null && actor.visual() instanceof LivingEntity living ? living.getYRot() : nativeBodyYaw;
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", shift = At.Shift.AFTER))
    private void interactiveAnimation$applyBones(LivingEntity entity, float yaw, float partialTick, PoseStack poses,
                                                MultiBufferSource buffers, int light, CallbackInfo callback) {
        var actor = LegacyActorRenderer.current();
        if (actor == null || actor.visual() != entity || PlayerPoseOwnership.nativeLayer(entity)) return;
        LegacyActorRenderer.restoreAfterDraw(ModelBones.apply(actor, this::interactiveAnimation$bone));
        if (model instanceof PlayerModel<?> player) PlayerSkinLayers.follow(actor, player);
    }

    @Unique
    private ModelPart interactiveAnimation$bone(String name) {
        if (model instanceof HierarchicalModel<?> hierarchy) return ModelBones.find(hierarchy.root(), name);
        if (!(model instanceof HumanoidModel<?> body)) return null;
        return switch (name) {
            case "head" -> body.head;
            case "hat" -> body.hat;
            case "body", "torso" -> body.body;
            case "left_arm" -> body.leftArm;
            case "right_arm" -> body.rightArm;
            case "left_leg" -> body.leftLeg;
            case "right_leg" -> body.rightLeg;
            default -> model instanceof PlayerModel<?> player ? PlayerSkinLayers.find(player, name) : null;
        };
    }
}
