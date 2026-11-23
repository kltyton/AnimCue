package com.kltyton.interactiveanimation.compat.geckolib;

import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import software.bernie.geckolib.model.GeoModel;

public final class Gecko4ActorPose {
    private Gecko4ActorPose() {}

    public static void apply(GeoModel<?> model, Object animatable) {
        var actor = LegacyActorRenderer.current();
        if (actor == null || actor.visual() != animatable) return;
        actor.bones().forEach((name, track) -> {
            var bone = model.getBone(name).orElse(null);
            if (bone == null) {
                actor.missingBone(name);
                return;
            }
            float x = bone.getPosX(), y = bone.getPosY(), z = bone.getPosZ();
            float rx = bone.getRotX(), ry = bone.getRotY(), rz = bone.getRotZ();
            float sx = bone.getScaleX(), sy = bone.getScaleY(), sz = bone.getScaleZ();
            LegacyActorRenderer.restoreAfterDraw(() -> {
                bone.setPosX(x); bone.setPosY(y); bone.setPosZ(z);
                bone.updateRotation(rx, ry, rz);
                bone.setScaleX(sx); bone.setScaleY(sy); bone.setScaleZ(sz);
            });
            var pose = track.sample(actor.seconds());
            if (track.hasPosition()) {
                bone.setPosX((float) -pose.position().x);
                bone.setPosY((float) pose.position().y);
                bone.setPosZ((float) pose.position().z);
            }
            if (track.hasRotation()) bone.updateRotation((float) -Math.toRadians(pose.rotation().x),
                    (float) -Math.toRadians(pose.rotation().y), (float) Math.toRadians(pose.rotation().z));
            if (track.hasScale()) {
                bone.setScaleX((float) pose.scale().x);
                bone.setScaleY((float) pose.scale().y);
                bone.setScaleZ((float) pose.scale().z);
            }
        });
    }
}