package com.kltyton.interactiveanimation.client.playback;

import com.kltyton.interactiveanimation.scene.asset.SceneDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ClientAssets {
    private static final Map<String, Function<ClientLevel, Object>> MODELS = new HashMap<>();
    private static final AtomicInteger NEXT_PREVIEW_ENTITY_ID = new AtomicInteger(-2);

    private ClientAssets() {}

    /** Register the client-side renderer subject for a model ID, such as a GeoEntity or an ItemStack. */
    public static void registerModel(String id, Function<ClientLevel, Object> factory) {
        if (MODELS.putIfAbsent(id, factory) != null) throw new IllegalArgumentException("Duplicate preview model " + id);
    }

    static Object preview(ClientLevel level, SceneDefinition.Participant participant) {
        Object visual;
        if (participant.kind() == SceneDefinition.AssetKind.MODEL) {
            var provider = MODELS.get(participant.asset());
            if (provider == null) throw new IllegalArgumentException("Unregistered preview model " + participant.asset());
            visual = provider.apply(level);
            if (!(visual instanceof Entity || visual instanceof ItemStack || visual instanceof BlockState)) {
                throw new IllegalArgumentException("Unsupported preview renderer subject for " + participant.asset());
            }
        } else {
            visual = NativePreview.asset(level, participant.kind(), participant.asset());
        }
        if (visual instanceof Entity entity) ensurePreviewEntityId(entity);
        return visual;
    }

    private static void ensurePreviewEntityId(Entity entity) {
        try {
            entity.getId();
        } catch (IllegalStateException unassigned) {
            if (!"Tried to access entity ID before ID assignment".equals(unassigned.getMessage())) throw unassigned;
            // Minecraft 26.2 allocates tracked IDs on the server. Detached previews also need a render seed.
            entity.setId(NEXT_PREVIEW_ENTITY_ID.getAndDecrement());
        }
    }
}
