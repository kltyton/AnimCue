package com.kltyton.interactiveanimation.scene.playback;

import com.kltyton.interactiveanimation.api.SceneSubject;
import com.kltyton.interactiveanimation.scene.asset.BoneTrack;
import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;

public final class RoleMotion {
    private static final double PIXELS_PER_BLOCK = 16;
    private Supplier<SceneSubject.Frame> reference;
    private Vec3 correction = Vec3.ZERO;
    private double rotationCorrection;

    public RoleMotion(Supplier<SceneSubject.Frame> reference) {
        this.reference = reference;
    }

    public BoneTrack.Pose sample(BoneTrack track, double seconds) {
        return pose(reference.get(), track == null ? BoneTrack.IDENTITY : track.sample(seconds));
    }

    private BoneTrack.Pose pose(SceneSubject.Frame reference, BoneTrack.Pose root) {
        Vec3 position = reference.position().add(reference.offset(root.position().scale(1 / PIXELS_PER_BLOCK)));
        Vec3 rotation = root.rotation().add(0, reference.yaw() + rotationCorrection, 0);
        return new BoneTrack.Pose(position.add(correction), rotation, root.scale());
    }

    public void switchReference(Supplier<SceneSubject.Frame> next, BoneTrack track, double seconds) {
        BoneTrack.Pose old = sample(track, seconds);
        reference = next;
        correction = Vec3.ZERO;
        rotationCorrection = 0;
        BoneTrack.Pose changed = sample(track, seconds);
        correction = old.position().subtract(changed.position());
        rotationCorrection = old.rotation().y - changed.rotation().y;
    }

    public SceneSubject.Frame reference() {
        return reference.get();
    }
}
