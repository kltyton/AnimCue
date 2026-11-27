package com.kltyton.interactiveanimation.scene.asset;

import com.google.gson.JsonObject;
import com.kltyton.interactiveanimation.api.SceneId;
import java.util.List;
import java.util.Map;

public record SceneDefinition(SceneId id, double length, JsonObject primaryAnimation,
                              List<Participant> participants, List<Marker> markers,
                              List<SoundKeyframe> sounds, List<ParticleKeyframe> particles,
                              BoneTrack camera) {
    public SceneDefinition {
        primaryAnimation = primaryAnimation.deepCopy();
        participants = List.copyOf(participants);
        markers = List.copyOf(markers);
        sounds = List.copyOf(sounds);
        particles = List.copyOf(particles);
    }

    public SceneDefinition(SceneId id, double length, JsonObject primaryAnimation,
                           List<Participant> participants, List<Marker> markers, BoneTrack camera) {
        this(id, length, primaryAnimation, participants, markers, List.of(), List.of(), camera);
    }

    @Override
    public JsonObject primaryAnimation() {
        return primaryAnimation.deepCopy();
    }

    public enum AssetKind { MODEL, ENTITY, BLOCK, ITEM }

    public record Participant(String role, AssetKind kind, String asset, BoneTrack root, Map<String, BoneTrack> bones) {
        public Participant {
            bones = Map.copyOf(bones);
        }
    }

    public record Marker(double seconds, String instruction) {}

    public record SoundKeyframe(double seconds, String role, String effect, String locator) {}

    public record ParticleKeyframe(double seconds, String role, String effect, String locator, String script) {}
}
