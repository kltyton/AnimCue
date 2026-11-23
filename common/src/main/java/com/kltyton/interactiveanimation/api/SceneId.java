package com.kltyton.interactiveanimation.api;

import java.util.Objects;

/** The full resource path and the animation key within it. */
public record SceneId(String file, String animation) {
    public SceneId {
        Objects.requireNonNull(file, "file");
        if (animation == null || animation.isBlank()) throw new IllegalArgumentException("Missing animation name");
        if (!file.matches("[a-z0-9_.-]+:[a-z0-9_./-]+[.]json")) throw new IllegalArgumentException("Expected an animation JSON: " + file);
    }

    @Override
    public String toString() {
        return file + "#" + animation;
    }
}
