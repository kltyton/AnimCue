package com.kltyton.interactiveanimation.client.compat.geckolib;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.kltyton.interactiveanimation.client.render.SceneRenderState;

public final class GeckoScenePose {
    private GeckoScenePose() {}

    public static void apply(RenderPassInfo<?> pass, BoneSnapshots snapshots) {
        if (!(pass.renderState() instanceof SceneRenderState scene)) return;
        var actor = scene.interactiveAnimation$actor();
        if (actor == null) return;
        actor.bones().forEach((name, track) -> {
            var bone = snapshots.get(name).orElse(null);
            if (bone == null) {
                actor.missingBone(name);
                return;
            }
            var pose = track.sample(actor.seconds());
            if (track.hasPosition()) bone.setTranslation((float) -pose.position().x, (float) pose.position().y, (float) pose.position().z);
            if (track.hasRotation()) bone.setRotation((float) -Math.toRadians(pose.rotation().x),
                    (float) -Math.toRadians(pose.rotation().y), (float) Math.toRadians(pose.rotation().z));
            if (track.hasScale()) bone.setScale((float) pose.scale().x, (float) pose.scale().y, (float) pose.scale().z);
        });
    }
}
