package com.kltyton.interactiveanimation.scene.playback;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class SceneEffectRegistry {
    private SceneEffectRegistry() {}

    public static SoundEvent sound(String id) {
        Identifier key = Identifier.tryParse(id);
        return key != null && BuiltInRegistries.SOUND_EVENT.containsKey(key)
                ? BuiltInRegistries.SOUND_EVENT.getValue(key)
                : null;
    }

    public static ParticleType<?> particle(String id) {
        Identifier key = Identifier.tryParse(id);
        return key != null && BuiltInRegistries.PARTICLE_TYPE.containsKey(key)
                ? BuiltInRegistries.PARTICLE_TYPE.getValue(key)
                : null;
    }
}
