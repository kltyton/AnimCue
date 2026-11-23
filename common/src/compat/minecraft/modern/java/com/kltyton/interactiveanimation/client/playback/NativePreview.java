package com.kltyton.interactiveanimation.client.playback;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;

final class NativePreview {
    private NativePreview() {}

    static Object asset(ClientLevel level, com.kltyton.interactiveanimation.scene.asset.SceneDefinition.AssetKind kind, String asset) {
        var id = Identifier.parse(asset);
        switch (kind) {
            case ENTITY -> {
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) break;
                var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                if (id.equals(Identifier.withDefaultNamespace("player"))) {
                    return new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "Scene Player"));
                }
                var entity = type.create(level, EntitySpawnReason.COMMAND);
                if (entity != null) return entity;
            }
            case ITEM -> {
                if (BuiltInRegistries.ITEM.containsKey(id)) return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            }
            case BLOCK -> {
                if (BuiltInRegistries.BLOCK.containsKey(id)) return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
            }
            case MODEL -> throw new IllegalArgumentException("Expected a registered model factory");
        }
        throw new IllegalArgumentException("Cannot create preview " + kind + " " + asset);
    }
}
