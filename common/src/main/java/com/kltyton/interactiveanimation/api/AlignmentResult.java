package com.kltyton.interactiveanimation.api;

import net.minecraft.world.phys.Vec3;

public record AlignmentResult(String role, Status status, Vec3 initial, Vec3 target, Vec3 reached) {
    public boolean aligned() {
        return status == Status.ALIGNED;
    }

    public Vec3 initialOffset() {
        return target.subtract(initial);
    }

    public Vec3 remainingOffset() {
        return target.subtract(reached);
    }

    public enum Status { ALIGNED, MISALIGNED, BLOCKED, CANCELLED }
}
