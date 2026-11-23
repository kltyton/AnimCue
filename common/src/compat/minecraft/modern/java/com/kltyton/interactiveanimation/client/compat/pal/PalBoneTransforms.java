package com.kltyton.interactiveanimation.client.compat.pal;

import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.phys.Vec3;

final class PalBoneTransforms {
    private PalBoneTransforms() {}

    static void reset(PlayerAnimBone bone) {
        bone.position.set(0, 0, 0);
        bone.rotation.set(0, 0, 0);
        bone.scale.set(1, 1, 1);
        bone.bend = 0;
    }

    static void position(PlayerAnimBone bone, Vec3 value) {
        bone.position.set((float) value.x, (float) value.y, (float) value.z);
    }

    static void rotation(PlayerAnimBone bone, Vec3 value) {
        bone.rotation.set(radians(value.x), radians(value.y), radians(value.z));
    }

    static void scale(PlayerAnimBone bone, Vec3 value) {
        bone.scale.set((float) value.x, (float) value.y, (float) value.z);
    }

    private static float radians(double value) {
        return (float) Math.toRadians(value);
    }
}
