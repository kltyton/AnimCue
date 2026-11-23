package com.kltyton.interactiveanimation.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.interactiveanimation.client.render.RootPose;
import com.kltyton.interactiveanimation.client.render.SceneRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class SceneWorldRendererMixin {
    @Inject(method = "submitEntities", at = @At("RETURN"))
    private void interactiveAnimation$submitPreviews(PoseStack poses, LevelRenderState state, SubmitNodeCollector output, CallbackInfo callback) {
        var client = Minecraft.getInstance();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var camera = state.cameraRenderState;
        var dispatcher = client.getEntityRenderDispatcher();
        var submitted = java.util.Collections.<Entity>newSetFromMap(new java.util.IdentityHashMap<>());
        for (var entry : state.entityRenderStates) {
            var actor = ((SceneRenderState) entry).interactiveAnimation$actor();
            if (actor != null && actor.bound() != null) submitted.add(actor.bound());
        }
        if (client.options.getCameraType().isFirstPerson() && client.player != null) submitted.add(client.player);
        for (var actor : ClientScenes.extras(partialTick, submitted::contains)) {
            var position = actor.pose().position();
            var offset = position.subtract(camera.pos);
            if (actor.visual() instanceof Entity entity) {
                if (actor.bound() == null) {
                    entity.setPos(position);
                    entity.setOldPosAndRot();
                }
                var renderState = dispatcher.extractEntity(entity, partialTick);
                ((SceneRenderState) renderState).interactiveAnimation$actor(actor);
                dispatcher.submit(renderState, camera, offset.x, offset.y, offset.z, poses, output);
                continue;
            }

            BlockPos blockPos = BlockPos.containing(position);
            int light = LightCoordsUtil.pack(client.level.getBrightness(LightLayer.BLOCK, blockPos),
                    client.level.getBrightness(LightLayer.SKY, blockPos));
            poses.pushPose();
            try {
                poses.translate(offset.x, offset.y, offset.z);
                RootPose.orient(poses, actor, 0);
                if (actor.visual() instanceof ItemStack item) {
                    var renderState = new ItemStackRenderState();
                    client.getItemModelResolver().updateForTopItem(renderState, item, ItemDisplayContext.FIXED, client.level, null, 0);
                    renderState.submit(poses, output, light, OverlayTexture.NO_OVERLAY, 0);
                } else if (actor.visual() instanceof BlockState block) {
                    var renderState = new BlockModelRenderState();
                    new BlockModelResolver(client.getModelManager()).update(renderState, block, BlockDisplayContext.create());
                    renderState.submit(poses, output, light, OverlayTexture.NO_OVERLAY, 0);
                }
            } finally {
                poses.popPose();
            }
        }
    }
}
