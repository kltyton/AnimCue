package com.kltyton.interactiveanimation.client.render;

import com.kltyton.interactiveanimation.client.playback.ClientActor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class LegacyActorRenderer {
    private static ClientActor actor;
    private static java.util.List<Runnable> restore;

    private LegacyActorRenderer() {}

    public static ClientActor current() {
        return actor;
    }

    public static void restoreAfterDraw(Runnable restorePose) {
        if (restore != null) restore.add(restorePose);
    }

    public static void render(ClientActor value, Vec3 camera, float partialTick, PoseStack poses, MultiBufferSource buffers) {
        var client = Minecraft.getInstance();
        Vec3 offset = value.pose().position().subtract(camera);
        poses.pushPose();
        ClientActor previous = actor;
        var previousRestore = restore;
        restore = new java.util.ArrayList<>();
        try {
            actor = value;
            poses.translate(offset.x, offset.y, offset.z);
            Object visual = value.visual();
            if (visual instanceof Entity entity) {
                var dispatcher = client.getEntityRenderDispatcher();
                RootPose.orient(poses, value, entity.getYRot());
                dispatcher.render(entity, 0, 0, 0, entity.getYRot(), partialTick, poses, buffers,
                        dispatcher.getPackedLightCoords(entity, partialTick));
            } else {
                RootPose.orient(poses, value, 0);
                int light = LevelRenderer.getLightColor(client.level, BlockPos.containing(value.pose().position()));
                if (visual instanceof ItemStack item) {
                    client.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                            poses, buffers, client.level, 0);
                } else if (visual instanceof BlockState block) {
                    poses.translate(-0.5, 0, -0.5);
                    client.getBlockRenderer().renderSingleBlock(block, poses, buffers, light, OverlayTexture.NO_OVERLAY);
                }
            }
        } finally {
            for (int i = restore.size() - 1; i >= 0; i--) restore.get(i).run();
            restore = previousRestore;
            actor = previous;
            poses.popPose();
        }
    }
}
