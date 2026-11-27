package com.kltyton.interactiveanimation.scene.playback;

import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.api.SceneEffects;
import com.kltyton.interactiveanimation.api.SceneHandle;
import com.kltyton.interactiveanimation.scene.asset.SceneDefinition;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared chronological sound and particle keyframe dispatcher for server and local client playback. */
public final class SceneEffectDispatcher {
    private int soundIndex;
    private int particleIndex;

    public void dispatch(Level level, SceneHandle handle, SceneDefinition definition, PlaybackOptions options,
                         boolean nativePlayback, double seconds, Supplier<Vec3> primaryPos, Entity primaryEntity,
                         Function<String, Vec3> rolePos, Function<String, Entity> roleEntity,
                         BooleanSupplier closed) {
        var sounds = definition.sounds();
        while (soundIndex < sounds.size() && sounds.get(soundIndex).seconds() <= seconds) {
            var kf = sounds.get(soundIndex++);
            String role = kf.role();
            Vec3 pos = role == null ? primaryPos.get() : rolePos.apply(role);
            if (options.sound() != null) {
                options.sound().accept(new PlaybackOptions.SoundKeyframe(handle, role, kf.effect(), kf.locator(), kf.seconds(), pos));
            } else if (!nativePlayback || role != null) {
                Entity source = role == null ? primaryEntity : roleEntity.apply(role);
                SceneEffects.playSound(level, source, pos, kf.effect(), 1.0F, 1.0F);
            }
            if (closed.getAsBoolean()) return;
        }
        var particles = definition.particles();
        while (particleIndex < particles.size() && particles.get(particleIndex).seconds() <= seconds) {
            var kf = particles.get(particleIndex++);
            String role = kf.role();
            Vec3 pos = role == null ? primaryPos.get() : rolePos.apply(role);
            if (options.particle() != null) {
                options.particle().accept(new PlaybackOptions.ParticleKeyframe(handle, role, kf.effect(), kf.locator(), kf.script(), kf.seconds(), pos));
            } else if (!nativePlayback || role != null) {
                Entity source = role == null ? primaryEntity : roleEntity.apply(role);
                SceneEffects.spawnParticle(level, source, pos, kf.effect(), kf.script());
            }
            if (closed.getAsBoolean()) return;
        }
    }
}
