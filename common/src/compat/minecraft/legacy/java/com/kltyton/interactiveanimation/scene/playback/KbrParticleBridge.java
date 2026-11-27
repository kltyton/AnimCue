package com.kltyton.interactiveanimation.scene.playback;

import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.particle.network.EmitterCreationPacketS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-safe optional KBR emitter dispatch bridge for legacy targets (1.20.1 / 1.21.1). */
final class KbrParticleBridge {
    private static final double SYNC_RANGE_SQR = 128.0 * 128.0;

    private KbrParticleBridge() {}

    static boolean spawnServer(ServerLevel level, Entity attachEntity, Vec3 position, String effect, String script) {
        ResourceLocation id = ResourceLocation.tryParse(effect);
        if (id == null) return false;
        MolangExp expression = (script == null || script.isBlank()) ? MolangExp.EMPTY : new MolangExp(script);
        Vector3f pos = new Vector3f((float) position.x, (float) position.y, (float) position.z);
        boolean sent = false;
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(position) <= SYNC_RANGE_SQR) {
                EmitterCreationPacketS2C.sendToClient(player, id, pos, expression, attachEntity);
                sent = true;
            }
        }
        return sent;
    }
}
