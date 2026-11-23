package com.kltyton.interactiveanimation.scene.asset;

import com.kltyton.interactiveanimation.api.SceneId;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;

public final class SceneAssets {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    public static final List<String> DIRECTORIES = List.of("geckolib/animations", "player_animations", "animations");
    private static volatile Map<SceneId, SceneDefinition> packaged = Map.of();

    private SceneAssets() {}

    public static synchronized void registerPackagedFile(Class<?> owner, String file) {
        String path = "/assets/" + file.replace(':', '/');
        try (InputStream input = owner.getResourceAsStream(path)) {
            if (input == null) throw new IllegalArgumentException("Missing packaged scene " + path);
            var next = new HashMap<>(packaged);
            SceneReader.read(file, new InputStreamReader(input, StandardCharsets.UTF_8)).forEach((id, value) -> {
                if (next.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate scene " + id);
            });
            packaged = Map.copyOf(next);
        } catch (IOException error) {
            throw new IllegalStateException("Could not read packaged scene " + path, error);
        }
    }

    public static SceneDefinition server(SceneId id) {
        SceneDefinition scene = packaged.get(id);
        if (scene == null) throw new IllegalArgumentException("Unregistered server scene " + id);
        return scene;
    }

    public static Map<SceneId, SceneDefinition> client(ResourceManager resources) {
        var scenes = new HashMap<SceneId, SceneDefinition>();
        for (String directory : DIRECTORIES) {
            resources.listResources(directory, id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
                try (var reader = resource.openAsReader()) {
                    scenes.putAll(SceneReader.read(id.toString(), reader));
                } catch (IOException | IllegalArgumentException error) {
                    LOGGER.error("Could not load interaction animation {}", id, error);
                }
            });
        }
        return Map.copyOf(scenes);
    }
}
