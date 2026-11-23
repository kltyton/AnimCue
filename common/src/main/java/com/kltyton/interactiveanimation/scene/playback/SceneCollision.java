package com.kltyton.interactiveanimation.scene.playback;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SceneCollision {
    private static final double EPSILON_SQUARED = 1e-7;

    private SceneCollision() {}

    public static boolean canApproach(Entity entity, Vec3 from, Vec3 to) {
        Vec3 wanted = to.subtract(from);
        var box = entity.getBoundingBox().move(from.subtract(entity.position()));
        Vec3 allowed = Entity.collideBoundingBox(entity, wanted, box, entity.level(), List.of());
        return allowed.distanceToSqr(wanted) <= EPSILON_SQUARED;
    }
}
