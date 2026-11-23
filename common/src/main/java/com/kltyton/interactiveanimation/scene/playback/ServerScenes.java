package com.kltyton.interactiveanimation.scene.playback;

import com.kltyton.interactiveanimation.api.Bindings;
import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.api.SceneHandle;
import com.kltyton.interactiveanimation.api.SceneId;
import com.kltyton.interactiveanimation.api.SceneSubject;
import com.kltyton.interactiveanimation.scene.asset.SceneAssets;
import com.kltyton.interactiveanimation.scene.network.SceneTransport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ServerScenes {
    private static final Map<MinecraftServer, List<ServerScene>> SCENES = new HashMap<>();
    private static SceneTransport transport;

    private ServerScenes() {}

    public static void install(SceneTransport sender) {
        if (transport != null) throw new IllegalStateException("Scene transport already present");
        transport = sender;
    }

    public static SceneHandle play(SceneSubject subject, SceneId id, Bindings bindings, PlaybackOptions options) {
        if (!(subject.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) {
            throw new IllegalStateException("Start a networked scene on the server thread");
        }
        if (!subject.valid().getAsBoolean()) throw new IllegalArgumentException("Inactive primary animation subject");
        if (transport == null) throw new IllegalStateException("Scene networking has not been initialized");

        var scenes = SCENES.computeIfAbsent(level.getServer(), ignored -> new ArrayList<>());
        var scene = new ServerScene(level, subject, SceneAssets.server(id), bindings, options, transport);
        if (scenes.stream().anyMatch(old -> old.conflicts(scene))) {
            throw new IllegalStateException("A bound entity already belongs to a playing scene");
        }
        scenes.add(scene);
        scene.start();
        return scene;
    }

    public static void tick(MinecraftServer server) {
        List<ServerScene> scenes = SCENES.get(server);
        if (scenes == null) return;
        for (ServerScene scene : List.copyOf(scenes)) scene.tick();
        scenes.removeIf(ServerScene::closed);
        if (scenes.isEmpty()) SCENES.remove(server);
    }

    public static void shutdown(MinecraftServer server) {
        var scenes = SCENES.remove(server);
        if (scenes != null) scenes.forEach(ServerScene::cancel);
    }
}
