package com.kltyton.interactiveanimation.api;

import com.kltyton.interactiveanimation.scene.playback.KbrParticleCompat;
import com.kltyton.interactiveanimation.scene.playback.SceneEffectRegistry;
import java.util.Objects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Built-in sound and particle playback helpers for keyframed scene events. */
public final class SceneEffects {
    private SceneEffects() {}

    /**
     * Plays a keyframed sound on {@code level}, resolving the source entity from {@code keyframe.playback()} when non-null
     * and falling back to {@link SoundSource#NEUTRAL} when {@code keyframe.playback()} is {@code null}.
     * Unknown or invalid effect IDs safely return {@code false}.
     */
    public static boolean playSound(Level level, PlaybackOptions.SoundKeyframe keyframe) {
        Objects.requireNonNull(keyframe, "keyframe");
        Entity source = keyframe.role() != null && keyframe.playback() != null
                && keyframe.playback().binding(keyframe.role()) instanceof Entity entity ? entity : null;
        return playSound(level, source, keyframe.position(), keyframe.effect(), 1.0F, 1.0F);
    }

    /**
     * Plays {@code effect} at {@code position}; unknown or invalid sound IDs safely return {@code false}.
     */
    public static boolean playSound(Level level, Entity sourceEntity, Vec3 position, String effect,
                                    float volume, float pitch) {
        if (level == null || position == null || effect == null || effect.isBlank()) return false;
        SoundEvent sound = SceneEffectRegistry.sound(effect);
        if (sound == null) return false;
        SoundSource source = sourceEntity != null ? sourceEntity.getSoundSource() : SoundSource.NEUTRAL;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound((Player) null, position.x, position.y, position.z, sound, source, volume, pitch);
            return true;
        }
        if (level.isClientSide()) {
            level.playLocalSound(position.x, position.y, position.z, sound, source, volume, pitch, false);
            return true;
        }
        return false;
    }

    /**
     * Spawns a keyframed particle at {@code keyframe.position()}, forwarding {@code keyframe.script()} and any
     * bound participant entity to optional Kltyton Bedrock Runtime compatibility when no vanilla/mod
     * {@link ParticleType} matches. Unknown, invalid, or non-{@link ParticleOptions} parameterized particle types
     * safely return {@code false}.
     */
    public static boolean spawnParticle(Level level, PlaybackOptions.ParticleKeyframe keyframe) {
        Objects.requireNonNull(keyframe, "keyframe");
        Entity source = keyframe.role() != null && keyframe.playback() != null
                && keyframe.playback().binding(keyframe.role()) instanceof Entity entity ? entity : null;
        return spawnParticle(level, source, keyframe.position(), keyframe.effect(), keyframe.script());
    }

    /**
     * Spawns {@code effect} at {@code position}; unknown, invalid, or non-{@link ParticleOptions} parameterized
     * particle types safely return {@code false}.
     */
    public static boolean spawnParticle(Level level, Vec3 position, String effect) {
        return spawnParticle(level, null, position, effect, null);
    }

    /**
     * Spawns {@code effect} at {@code position} with an optional MoLang initialization {@code script}
     * (used when falling back to optional Kltyton Bedrock Runtime particles).
     */
    public static boolean spawnParticle(Level level, Vec3 position, String effect, String script) {
        return spawnParticle(level, null, position, effect, script);
    }

    /**
     * Spawns {@code effect} at {@code position} with optional {@code sourceEntity} context and MoLang
     * initialization {@code script}. Built-in {@link ParticleType} entries always take priority; parameterized
     * vanilla types (not implementing {@link ParticleOptions}) return {@code false} without forwarding to KBR.
     */
    public static boolean spawnParticle(Level level, Entity sourceEntity, Vec3 position, String effect, String script) {
        if (level == null || position == null || effect == null || effect.isBlank()) return false;
        ParticleType<?> type = SceneEffectRegistry.particle(effect);
        if (type != null) {
            if (!(type instanceof ParticleOptions options)) return false;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(options, position.x, position.y, position.z, 1, 0.0, 0.0, 0.0, 0.0);
                return true;
            }
            if (level.isClientSide()) {
                level.addParticle(options, position.x, position.y, position.z, 0.0, 0.0, 0.0);
                return true;
            }
            return false;
        }
        return KbrParticleCompat.spawn(level, sourceEntity, position, effect, script);
    }
}
