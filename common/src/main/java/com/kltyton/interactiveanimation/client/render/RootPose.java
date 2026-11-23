package com.kltyton.interactiveanimation.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.kltyton.interactiveanimation.client.playback.ClientActor;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class RootPose {
    private RootPose() {}

    public static void orient(PoseStack poses, ClientActor actor, float nativeYaw) {
        Vec3 angles = actor.pose().rotation();
        poses.mulPose(new Quaternionf().rotationZYX(radians(angles.z), radians(nativeYaw - angles.y), radians(angles.x)));
        Vec3 scale = actor.pose().scale();
        poses.scale((float) scale.x, (float) scale.y, (float) scale.z);
    }

    private static float radians(double value) {
        return (float) Math.toRadians(value);
    }
}
