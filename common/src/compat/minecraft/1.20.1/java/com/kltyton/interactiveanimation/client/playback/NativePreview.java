package com.kltyton.interactiveanimation.client.playback;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

final class NativePreview {
    private NativePreview() {}

    static Object asset(ClientLevel level, com.kltyton.interactiveanimation.scene.asset.SceneDefinition.AssetKind kind, String asset) {
        var id = new ResourceLocation(asset);
        switch (kind) {
            case ENTITY -> {
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) break;
                var type = BuiltInRegistries.ENTITY_TYPE.get(id);
                if (type == EntityType.PLAYER) return new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "Scene Player"));
                var entity = type.create(level);
                if (entity != null) return entity;
            }
            case ITEM -> {
                if (BuiltInRegistries.ITEM.containsKey(id)) return new ItemStack(BuiltInRegistries.ITEM.get(id));
            }
            case BLOCK -> {
                if (BuiltInRegistries.BLOCK.containsKey(id)) return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
            }
            case MODEL -> throw new IllegalArgumentException("Expected a registered model factory");
        }
        throw new IllegalArgumentException("Cannot create preview " + kind + " " + asset);
    }
}
