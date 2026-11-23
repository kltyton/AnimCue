package com.kltyton.interactiveanimation.api;

import com.kltyton.interactiveanimation.scene.asset.SceneAssets;
import com.kltyton.interactiveanimation.scene.playback.ServerScenes;

public final class InteractiveScenes {
    private InteractiveScenes() {}

    /** Load the same mod-owned asset on the server that the client reads from its animation resources. */
    public static void register(Class<?> resourceOwner, String animationFile) {
        SceneAssets.registerPackagedFile(resourceOwner, animationFile);
    }

    public static SceneHandle play(SceneSubject subject, SceneId scene, Object... participants) {
        return play(subject, scene, Bindings.ordered(participants), PlaybackOptions.DEFAULT);
    }

    public static SceneHandle play(SceneSubject subject, SceneId scene, Bindings bindings, PlaybackOptions options) {
        return ServerScenes.play(subject, scene, bindings, options);
    }
}
