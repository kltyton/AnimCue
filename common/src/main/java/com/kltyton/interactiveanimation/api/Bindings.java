package com.kltyton.interactiveanimation.api;

import com.kltyton.interactiveanimation.scene.asset.SceneDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class Bindings {
    private final Map<String, Object> named;
    private final List<Object> ordered;
    private final Map<String, Map<String, String>> boneMappings;

    private Bindings(Map<String, Object> named, List<Object> ordered, Map<String, Map<String, String>> boneMappings) {
        this.named = named;
        this.ordered = ordered;
        this.boneMappings = boneMappings;
    }

    public static Bindings defaults() {
        return new Bindings(Map.of(), List.of(), Map.of());
    }

    public static Bindings named(Map<String, ?> bindings) {
        return new Bindings(Map.copyOf(bindings), List.of(), Map.of());
    }

    public static Bindings ordered(Object... bindings) {
        return new Bindings(Map.of(), List.of(bindings), Map.of());
    }

    public Bindings mapBones(String role, Map<String, String> authoredToModel) {
        var maps = new LinkedHashMap<>(boneMappings);
        maps.put(role, Map.copyOf(authoredToModel));
        return new Bindings(named, ordered, Map.copyOf(maps));
    }

    public Map<String, Map<String, String>> boneMappings() {
        return boneMappings;
    }

    public Map<String, Object> resolve(SceneDefinition scene) {
        List<String> roles = scene.participants().stream().map(SceneDefinition.Participant::role).toList();
        if (ordered.size() > roles.size()) throw new IllegalArgumentException("Too many bindings for " + scene.id());
        var resolved = new LinkedHashMap<>(named);
        for (int i = 0; i < ordered.size(); i++) resolved.put(roles.get(i), ordered.get(i));
        for (var entry : resolved.entrySet()) {
            if (!roles.contains(entry.getKey())) throw new IllegalArgumentException("Unknown role " + entry.getKey());
            Object instance = entry.getValue();
            if (!(instance instanceof Entity || instance instanceof ItemStack || instance instanceof BlockState)) {
                throw new IllegalArgumentException("Unsupported binding for " + entry.getKey() + ": " + instance.getClass().getName());
            }
        }
        for (String role : boneMappings.keySet()) {
            if (!roles.contains(role)) throw new IllegalArgumentException("Unknown role for bone mapping: " + role);
        }
        return Map.copyOf(resolved);
    }
}
