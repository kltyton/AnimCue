package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.client.camera.SceneCamera;
import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private Entity entity;
    @Shadow private Vec3 position;
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("RETURN"))
    private void interactiveAnimation$applySceneCamera(BlockGetter level, Entity subject, boolean detached, boolean mirrored, float partialTick,
                                                        CallbackInfo callback) {
        var player = Minecraft.getInstance().player;
        if (player == null || entity != player) return;
        var camera = ClientScenes.camera(player, partialTick);
        if (camera != null) setPosition(SceneCamera.apply(camera, player, rotation, forwards, up, left, position));
    }
}
