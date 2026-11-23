package com.kltyton.interactiveanimation.api;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** The object that owns the primary animation and provides its spatial origin. */
public record SceneSubject(Level level, Supplier<Frame> frame, BooleanSupplier valid, Entity entity,
                           int transitionTicks, boolean nativePlayback, Runnable play, Runnable stop) {
    public SceneSubject {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(valid, "valid");
        Objects.requireNonNull(play, "play");
        Objects.requireNonNull(stop, "stop");
        if (transitionTicks < 0) throw new IllegalArgumentException("Negative transition length");
    }

    public static SceneSubject entity(Entity entity) {
        return new SceneSubject(entity.level(), () -> Frame.of(entity),
                () -> entity.isAlive() && !entity.isRemoved(), entity, 0, false, () -> {}, () -> {});
    }

    public static SceneSubject block(BlockEntity block, float yaw) {
        Level level = Objects.requireNonNull(block.getLevel(), "Block entity must be in a level");
        return new SceneSubject(level, () -> new Frame(Vec3.atBottomCenterOf(block.getBlockPos()), yaw),
                () -> !block.isRemoved(), null, 0, false, () -> {}, () -> {});
    }

    public static SceneSubject item(ItemStack stack, Entity holder) {
        return new SceneSubject(holder.level(), () -> Frame.of(holder),
                () -> !stack.isEmpty() && holder.isAlive() && !holder.isRemoved(), holder, 0, false, () -> {}, () -> {});
    }

    /** Connect an existing primary controller; both callbacks run on the calling game thread. */
    public SceneSubject playback(int transitionTicks, Runnable play, Runnable stop) {
        return new SceneSubject(level, frame, valid, entity, transitionTicks, true, play, stop);
    }

    public record Frame(Vec3 position, float yaw) {
        public Frame {
            Objects.requireNonNull(position, "position");
        }

        public static Frame of(Entity entity) {
            return new Frame(entity.position(), entity.getYRot());
        }

        public Vec3 offset(Vec3 local) {
            return local.yRot((float) Math.toRadians(-yaw));
        }
    }
}
