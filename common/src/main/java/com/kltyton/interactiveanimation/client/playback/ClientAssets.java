package com.kltyton.interactiveanimation.client.playback;

import com.kltyton.interactiveanimation.scene.asset.SceneDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ClientAssets {
    private static final Map<String, Function<ClientLevel, Object>> MODELS = new HashMap<>();

    private ClientAssets() {}

    /** Register the client-side renderer subject for a model ID, such as a GeoEntity or an ItemStack. */
    public static void registerModel(String id, Function<ClientLevel, Object> factory) {
        if (MODELS.putIfAbsent(id, factory) != null) throw new IllegalArgumentException("Duplicate preview model " + id);
    }

    static Object preview(ClientLevel level, SceneDefinition.Participant participant) {
        if (participant.kind() == SceneDefinition.AssetKind.MODEL) {
            var provider = MODELS.get(participant.asset());
            if (provider == null) throw new IllegalArgumentException("Unregistered preview model " + participant.asset());
            Object visual = provider.apply(level);
            if (visual instanceof Entity || visual instanceof ItemStack || visual instanceof BlockState) return visual;
            throw new IllegalArgumentException("Unsupported preview renderer subject for " + participant.asset());
        }
        return NativePreview.asset(level, participant.kind(), participant.asset());
    }
}
