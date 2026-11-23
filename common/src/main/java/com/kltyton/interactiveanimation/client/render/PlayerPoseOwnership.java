package com.kltyton.interactiveanimation.client.render;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;

/** Tracks avatars with a native player-animation layer to prevent duplicate bone application. */
public final class PlayerPoseOwnership {
    private static final ReferenceQueue<Entity> REMOVED = new ReferenceQueue<>();
    private static final Map<Integer, List<Owner>> NATIVE = new HashMap<>();

    private PlayerPoseOwnership() {}

    public static void register(Entity entity) {
        // PAL can register from the player constructor, before the spawn packet assigns its numeric ID.
        Owner removed;
        while ((removed = (Owner) REMOVED.poll()) != null) {
            var bucket = NATIVE.get(removed.identity);
            if (bucket != null) {
                bucket.remove(removed);
                if (bucket.isEmpty()) NATIVE.remove(removed.identity);
            }
        }
        if (!nativeLayer(entity)) {
            var owner = new Owner(entity);
            NATIVE.computeIfAbsent(owner.identity, ignored -> new ArrayList<>()).add(owner);
        }
    }

    public static boolean nativeLayer(Entity entity) {
        var bucket = NATIVE.get(System.identityHashCode(entity));
        if (bucket == null) return false;
        for (Owner owner : bucket) {
            if (owner.get() == entity) return true;
        }
        return false;
    }

    private static final class Owner extends WeakReference<Entity> {
        final int identity;

        Owner(Entity entity) {
            super(entity, REMOVED);
            identity = System.identityHashCode(entity);
        }
    }
}
