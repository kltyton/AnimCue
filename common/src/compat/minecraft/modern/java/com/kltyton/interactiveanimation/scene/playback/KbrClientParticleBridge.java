package com.kltyton.interactiveanimation.scene.playback;

import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Client-only optional KBR emitter bridge for modern targets (26.1.2 / 26.2). */
final class KbrClientParticleBridge {
    private KbrClientParticleBridge() {}

    static boolean spawnClient(Level level, Entity attachEntity, Vec3 position, String effect, String script) {
        Identifier id = Identifier.tryParse(effect);
        if (id == null) return false;
        Identifier resolved = MolangParticleEngine.INSTANCE.resolveParticleId(id);
        if (resolved == null) return false;
        MolangExp expression = (script == null || script.isBlank()) ? MolangExp.EMPTY : new MolangExp(script);
        ParticleEmitter emitter = new ParticleEmitter(level, position, resolved, expression,
                attachEntity == null ? null : IEntity.of(attachEntity));
        MolangParticleEngine.INSTANCE.addEmitter(emitter, false);
        return true;
    }
}
