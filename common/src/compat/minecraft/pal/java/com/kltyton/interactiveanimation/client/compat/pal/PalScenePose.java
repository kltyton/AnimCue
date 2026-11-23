package com.kltyton.interactiveanimation.client.compat.pal;

import com.kltyton.interactiveanimation.client.playback.ClientActor;
import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.entity.Entity;

final class PalScenePose {
    private final Entity entity;
    private final boolean extendedBend;
    private float partialTick;

    PalScenePose(Entity entity, boolean extendedBend) {
        this.entity = entity;
        this.extendedBend = extendedBend;
    }

    public void setupAnim(AnimationData data) {
        partialTick = data.getPartialTick();
    }

    public boolean isActive() {
        ClientActor actor = ClientScenes.actor(entity, partialTick);
        return actor != null && !actor.bones().isEmpty();
    }

    public void get3DTransform(PlayerAnimBone bone) {
        ClientActor actor = ClientScenes.actor(entity, partialTick);
        if (actor == null) return;
        String name = bone.getName();
        var bones = actor.bones();
        // PAL uses "body" for whole-avatar transforms and "torso" for the chest part.
        // Only forward "body" to PAL's avatar root when the animation distinguishes "torso";
        // otherwise "body" is the vanilla/Blockbench chest bone and kltyton:root owns whole-avatar motion.
        var track = switch (name) {
            case "body" -> bones.containsKey("torso") ? bones.get("body") : null;
            case "torso" -> bones.getOrDefault("torso", bones.get("body"));
            default -> bones.get(name);
        };
        if (track == null) {
            if (name.equals("body")) {
                PalBoneTransforms.reset(bone);
                if (extendedBend) PalMoreBends.clear(bone);
            }
            return;
        }
        PalBoneTransforms.reset(bone);
        if (extendedBend) PalMoreBends.clear(bone);
        var pose = track.sample(actor.seconds());
        if (track.hasPosition()) PalBoneTransforms.position(bone, pose.position());
        if (track.hasRotation()) PalBoneTransforms.rotation(bone, pose.rotation());
        if (track.hasScale()) PalBoneTransforms.scale(bone, pose.scale());
        if (track.hasBend()) {
            var bend = track.bend().sample(actor.seconds());
            bone.bend = radians(bend.x);
            if (extendedBend) PalMoreBends.apply(bone, radians(bend.x), radians(bend.y), radians(bend.z));
        }
    }

    private static float radians(double degrees) {
        return (float) Math.toRadians(degrees);
    }
}
