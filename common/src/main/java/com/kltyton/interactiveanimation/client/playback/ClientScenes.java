package com.kltyton.interactiveanimation.client.playback;

import com.kltyton.interactiveanimation.api.Bindings;
import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.api.SceneHandle;
import com.kltyton.interactiveanimation.api.SceneId;
import com.kltyton.interactiveanimation.api.SceneSubject;
import com.kltyton.interactiveanimation.scene.asset.SceneAssets;
import com.kltyton.interactiveanimation.scene.asset.SceneDefinition;
import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public final class ClientScenes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, ClientScene> ACTIVE = new HashMap<>();
    private static volatile Map<SceneId, SceneDefinition> definitions = Map.of();
    private static ClientLevel world;

    private ClientScenes() {}

    public static void reload(ResourceManager resources) {
        definitions = SceneAssets.client(resources);
    }

    public static SceneHandle playLocal(SceneSubject subject, SceneId scene, Object... participants) {
        return playLocal(subject, scene, Bindings.ordered(participants), PlaybackOptions.DEFAULT);
    }

    public static SceneHandle playLocal(SceneSubject subject, SceneId scene, Bindings bindings, PlaybackOptions options) {
        var client = Minecraft.getInstance();
        if (!client.isSameThread() || client.level != subject.level()) {
            throw new IllegalStateException("Start a local scene on its client game thread");
        }
        if (!subject.valid().getAsBoolean()) throw new IllegalArgumentException("Inactive primary animation subject");
        updateWorld(client);
        var definition = definitions.get(scene);
        if (definition == null) throw new IllegalArgumentException("Unknown client scene " + scene);
        var playback = new ClientScene(client.level, definition, subject, bindings, options);
        ACTIVE.put(UUID.randomUUID(), playback);
        playback.start();
        return playback;
    }

    public static void handle(ScenePacket packet) {
        var client = Minecraft.getInstance();
        updateWorld(client);
        if (client.level == null) return;
        try {
            if (packet.frame() == null) {
                var stopped = ACTIVE.remove(packet.session());
                if (stopped != null) stopped.cancel();
            } else if (packet.start() != null) {
                var definition = definitions.get(packet.start().scene());
                if (definition == null) throw new IllegalArgumentException("Missing client scene " + packet.start().scene());
                var scene = new ClientScene(client.level, definition, packet.start(), packet.frame());
                var previous = ACTIVE.put(packet.session(), scene);
                if (previous != null) previous.cancel();
            } else {
                var scene = ACTIVE.get(packet.session());
                if (scene != null) scene.accept(packet.frame());
            }
        } catch (RuntimeException error) {
            var failed = ACTIVE.remove(packet.session());
            if (failed != null) failed.cancel();
            LOGGER.error("Scene {} could not be received", packet.session(), error);
        }
    }

    public static void tick(Minecraft client) {
        updateWorld(client);
        for (var scene : List.copyOf(ACTIVE.values())) {
            try {
                scene.tick(client.player);
            } catch (RuntimeException error) {
                scene.cancel();
                LOGGER.error("Interactive animation failed on the client", error);
            }
        }
        ACTIVE.values().removeIf(ClientScene::closed);
    }

    private static void updateWorld(Minecraft client) {
        if (client.level == world) return;
        var old = List.copyOf(ACTIVE.values());
        ACTIVE.clear();
        world = client.level;
        old.forEach(ClientScene::cancel);
    }

    public static ClientActor actor(Entity entity, float partialTick) {
        for (var scene : ACTIVE.values()) {
            ClientActor actor = scene.actor(entity, partialTick);
            if (actor != null) return actor;
        }
        return null;
    }

    /** Returns the tracked subject of an active role, if that subject is an entity visible to this client. */
    public static Entity subjectFor(Entity participant, String role) {
        if (participant == null || role == null) return null;
        for (var scene : ACTIVE.values()) {
            Entity subject = scene.subjectFor(participant, role);
            if (subject != null) return subject;
        }
        return null;
    }

    public static List<ClientActor> extras(float partialTick, Predicate<Entity> submitted) {
        var visible = new ArrayList<ClientActor>();
        for (var scene : ACTIVE.values()) scene.collectExtras(visible, partialTick, submitted);
        return visible;
    }

    public static ClientScene.CameraPose camera(Entity player, float partialTick) {
        for (var scene : ACTIVE.values()) {
            var camera = scene.camera(player, partialTick);
            if (camera != null) return camera;
        }
        return null;
    }
}
