package com.kltyton.interactiveanimation.client.compat.pal;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.entity.Entity;

final class PalSceneLayer implements IAnimation {
    private final PalScenePose pose;

    PalSceneLayer(Entity entity, boolean moreRotation) {
        pose = new PalScenePose(entity, moreRotation);
    }

    @Override
    public boolean isActive() {
        return pose.isActive();
    }

    @Override
    public void setupAnim(AnimationData data) {
        pose.setupAnim(data);
    }

    @Override
    public void get3DTransform(PlayerAnimBone bone) {
        pose.get3DTransform(bone);
    }
}