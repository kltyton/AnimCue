package com.kltyton.interactiveanimation.client.camera;

import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.client.playback.ClientScene;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SceneCamera {
    private SceneCamera() {}

    public static Vec3 apply(ClientScene.CameraPose camera, Entity player, Quaternionf rotation,
                             Vector3f forwards, Vector3f up, Vector3f left, Vec3 position) {
        var angles = camera.pose().rotation();
        Quaternionf orientation = new Quaternionf().rotationZYX(radians(angles.z), radians(angles.y - camera.baseYaw()), radians(angles.x));
        if (camera.mode() == PlaybackOptions.CameraMode.LOCKED) rotation.set(orientation);
        else rotation.premul(orientation);
        rotation.transform(0, 0, 1, forwards);
        rotation.transform(0, 1, 0, up);
        rotation.transform(1, 0, 0, left);
        if (camera.authored()) return camera.pose().position();
        if (!camera.physical()) return position.add(camera.pose().position().subtract(player.position()));
        return position;
    }

    private static float radians(double degrees) {
        return (float) Math.toRadians(degrees);
    }
}
