package com.kltyton.interactiveanimation.api;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Shared, reference-counted controls attached to a live entity instance. */
public final class AnimationControls {
    private record Active(Options options, Lease custom) {}
    private static final Map<Entity, Map<Object, Active>> ACTIVE = new IdentityHashMap<>();
    private static final Map<EntityType<?>, Strategy> STRATEGIES = new HashMap<>();

    private AnimationControls() {}

    public static synchronized void register(EntityType<?> type, Strategy strategy) {
        if (STRATEGIES.putIfAbsent(Objects.requireNonNull(type), Objects.requireNonNull(strategy)) != null) {
            throw new IllegalArgumentException("A control strategy is already registered for " + type);
        }
    }

    public static synchronized Lease acquire(Entity entity, Options options) {
        Strategy strategy = STRATEGIES.get(entity.getType());
        Lease custom = strategy == null ? () -> {} : Objects.requireNonNull(strategy.acquire(entity, options));
        Object owner = new Object();
        ACTIVE.computeIfAbsent(entity, ignored -> new HashMap<>()).put(owner, new Active(options, custom));
        var released = new AtomicBoolean();
        return () -> {
            if (released.compareAndSet(false, true)) release(entity, owner);
        };
    }

    private static synchronized void release(Entity entity, Object owner) {
        var controls = ACTIVE.get(entity);
        if (controls == null) return;
        Active released = controls.remove(owner);
        if (controls.isEmpty()) ACTIVE.remove(entity);
        if (released != null) released.custom.close();
    }

    public static synchronized Options state(Entity entity) {
        var controls = ACTIVE.get(entity);
        if (controls == null) return Options.NONE;
        Options state = Options.NONE;
        for (var active : controls.values()) state = state.combine(active.options);
        return state;
    }

    public record Options(boolean ai, boolean movement, boolean attacks, boolean interaction) {
        public static final Options NONE = new Options(false, false, false, false);
        private Options combine(Options other) {
            return new Options(ai || other.ai, movement || other.movement, attacks || other.attacks,
                    interaction || other.interaction);
        }
    }

    @FunctionalInterface
    public interface Strategy {
        Lease acquire(Entity entity, Options controls);
    }

    @FunctionalInterface
    public interface Lease extends AutoCloseable {
        @Override
        void close();
    }
}
