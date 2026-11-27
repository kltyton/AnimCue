package com.kltyton.interactiveanimation.scene.playback;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SceneEffectRegistry {
    private SceneEffectRegistry() {}

    public static SoundEvent sound(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.SOUND_EVENT.containsKey(key)
                ? BuiltInRegistries.SOUND_EVENT.get(key)
                : null;
    }

    public static ParticleType<?> particle(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.PARTICLE_TYPE.containsKey(key)
                ? BuiltInRegistries.PARTICLE_TYPE.get(key)
                : null;
    }
}
