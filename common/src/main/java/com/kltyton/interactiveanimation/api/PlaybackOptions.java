package com.kltyton.interactiveanimation.api;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;

public record PlaybackOptions(boolean snapWithoutTransition, boolean checkApproachPath, double alignmentTolerance,
                              boolean lockDrivenMovement, boolean pauseSubjectAi, boolean lockAttack,
                              boolean lockInteraction, CameraMode camera, Entity carrier,
                              Consumer<AlignmentResult> alignment, Consumer<Marker> marker) {
    public static final PlaybackOptions DEFAULT = new PlaybackOptions(true, false, 0.0625,
            true, false, false, false, CameraMode.AUTO, null, ignored -> {}, ignored -> {});

    public PlaybackOptions {
        if (!Double.isFinite(alignmentTolerance) || alignmentTolerance < 0) {
            throw new IllegalArgumentException("Invalid alignment tolerance");
        }
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(alignment, "alignment");
        Objects.requireNonNull(marker, "marker");
    }

    public PlaybackOptions alignment(boolean snap, boolean checkPath, double tolerance, Consumer<AlignmentResult> handler) {
        return new PlaybackOptions(snap, checkPath, tolerance, lockDrivenMovement, pauseSubjectAi, lockAttack,
                lockInteraction, camera, carrier, handler, marker);
    }

    public PlaybackOptions controls(boolean lockDriven, boolean subjectAi, boolean attack, boolean interaction) {
        return new PlaybackOptions(snapWithoutTransition, checkApproachPath, alignmentTolerance, lockDriven, subjectAi,
                attack, interaction, camera, carrier, alignment, marker);
    }

    public PlaybackOptions camera(CameraMode mode) {
        return new PlaybackOptions(snapWithoutTransition, checkApproachPath, alignmentTolerance, lockDrivenMovement,
                pauseSubjectAi, lockAttack, lockInteraction, mode, carrier, alignment, marker);
    }

    public PlaybackOptions carrier(Entity carrier) {
        return new PlaybackOptions(snapWithoutTransition, checkApproachPath, alignmentTolerance, lockDrivenMovement,
                pauseSubjectAi, lockAttack, lockInteraction, camera, carrier, alignment, marker);
    }

    public PlaybackOptions markers(Consumer<Marker> handler) {
        return new PlaybackOptions(snapWithoutTransition, checkApproachPath, alignmentTolerance, lockDrivenMovement,
                pauseSubjectAi, lockAttack, lockInteraction, camera, carrier, alignment, handler);
    }

    public enum CameraMode { AUTO, VANILLA, FOLLOW, LOCKED }

    public record Marker(SceneHandle playback, String instruction, double seconds) {}
}
