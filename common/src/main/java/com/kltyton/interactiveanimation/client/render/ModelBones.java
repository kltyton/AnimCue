package com.kltyton.interactiveanimation.client.render;

import com.kltyton.interactiveanimation.client.playback.ClientActor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public final class ModelBones {
    private ModelBones() {}

    public static Runnable apply(ClientActor actor, Function<String, ModelPart> lookup) {
        List<Runnable> restore = new ArrayList<>();
        if (actor.visual() instanceof net.minecraft.world.entity.Entity entity && PlayerPoseOwnership.nativeLayer(entity)) return () -> {};
        actor.bones().forEach((name, track) -> {
            var part = lookup.apply(name);
            if (part == null) {
                actor.missingBone(name);
                return;
            }
            PartPose initial = part.storePose();
            float sx = part.xScale, sy = part.yScale, sz = part.zScale;
            restore.add(() -> {
                part.loadPose(initial);
                part.xScale = sx;
                part.yScale = sy;
                part.zScale = sz;
            });
            var pose = track.sample(actor.seconds());
            part.resetPose();
            part.x += (float) pose.position().x;
            part.y -= (float) pose.position().y;
            part.z += (float) pose.position().z;
            part.xRot += (float) Math.toRadians(pose.rotation().x);
            part.yRot += (float) Math.toRadians(pose.rotation().y);
            part.zRot += (float) Math.toRadians(pose.rotation().z);
            part.xScale *= (float) pose.scale().x;
            part.yScale *= (float) pose.scale().y;
            part.zScale *= (float) pose.scale().z;
        });
        return () -> restore.forEach(Runnable::run);
    }

    public static ModelPart find(ModelPart root, String name) {
        if (name.equals("root")) return root;
        return ModelPartTraversal.parts(root).filter(part -> part.hasChild(name)).findFirst().map(part -> part.getChild(name)).orElse(null);
    }
}
