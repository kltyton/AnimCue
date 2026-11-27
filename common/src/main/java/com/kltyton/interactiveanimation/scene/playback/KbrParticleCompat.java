package com.kltyton.interactiveanimation.scene.playback;

import com.kltyton.interactiveanimation.platform.PlatformMods;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Classloading-safe entry point for optional Kltyton Bedrock Runtime particle compatibility.
 * This class never references {@code com.kltyton.bedrockruntime.*} types directly so environments
 * without KBR installed never attempt to load KBR classes.
 */
public final class KbrParticleCompat {
    public static final String MOD_ID = "kltyton_bedrock_runtime";

    private KbrParticleCompat() {}

    /** Returns {@code true} when Kltyton Bedrock Runtime (Lite or full) is loaded. */
    public static boolean available() {
        return PlatformMods.isLoaded(MOD_ID);
    }

    /**
     * Dispatches a particle keyframe through KBR when available. Returns {@code false} if KBR is not
     * installed, the ID is not namespaced, or no client/viewer target handled it.
     */
    public static boolean spawn(Level level, Entity sourceEntity, Vec3 position, String effect, String script) {
        if (!available() || effect == null || effect.indexOf(':') <= 0) return false;
        Entity attach = sourceEntity != null && sourceEntity.position().distanceToSqr(position) <= 1.0E-4
                ? sourceEntity : null;
        if (level instanceof ServerLevel serverLevel) {
            return KbrParticleBridge.spawnServer(serverLevel, attach, position, effect, script);
        }
        if (level.isClientSide()) {
            return KbrClientParticleBridge.spawnClient(level, attach, position, effect, script);
        }
        return false;
    }
}
