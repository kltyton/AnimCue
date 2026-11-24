package com.kltyton.interactiveanimation.scene.asset;

import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;

public record BoneTrack(AnimationCurve position, AnimationCurve rotation, AnimationCurve scale, AnimationCurve bend, int channels) {
    public static final String ROOT = "interactive_animation:root";
    public static final Vec3 UNIT_SCALE = new Vec3(1, 1, 1);
    public static final Pose IDENTITY = new Pose(Vec3.ZERO, Vec3.ZERO, UNIT_SCALE);

    public static BoneTrack read(JsonObject channels) {
        return new BoneTrack(AnimationCurve.read(channels.get("position"), Vec3.ZERO),
                AnimationCurve.read(channels.get("rotation"), Vec3.ZERO),
                AnimationCurve.read(channels.get("scale"), UNIT_SCALE),
                AnimationCurve.read(channels.get("bend"), Vec3.ZERO),
                (channels.has("position") ? 1 : 0) | (channels.has("rotation") ? 2 : 0)
                        | (channels.has("scale") ? 4 : 0) | (channels.has("bend") ? 8 : 0));
    }

    public boolean hasPosition() { return (channels & 1) != 0; }
    public boolean hasRotation() { return (channels & 2) != 0; }
    public boolean hasScale() { return (channels & 4) != 0; }
    public boolean hasBend() { return (channels & 8) != 0; }

    public double endTime() {
        return Math.max(Math.max(position.endTime(), rotation.endTime()), Math.max(scale.endTime(), bend.endTime()));
    }

    public Pose sample(double seconds) {
        return new Pose(position.sample(seconds), rotation.sample(seconds), scale.sample(seconds));
    }

    public record Pose(Vec3 position, Vec3 rotation, Vec3 scale) {
        public Pose lerp(Pose other, double fraction) {
            return new Pose(position.lerp(other.position, fraction), rotation.lerp(other.rotation, fraction),
                    scale.lerp(other.scale, fraction));
        }
    }
}
