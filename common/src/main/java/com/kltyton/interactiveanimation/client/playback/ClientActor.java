package com.kltyton.interactiveanimation.client.playback;

import com.kltyton.interactiveanimation.scene.asset.BoneTrack;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

/** Render data for one participant at a sampled instant. */
public record ClientActor(String role, Object visual, Entity bound, boolean physical, BoneTrack.Pose pose,
                          double seconds, Map<String, BoneTrack> bones, Set<String> missing) {
    private static final Logger LOGGER = LogUtils.getLogger();
    public void missingBone(String bone) {
        if (missing.add(bone)) LOGGER.warn("Scene role {} has no compatible bone {}", role, bone);
    }

    static Set<String> diagnostics() {
        return new HashSet<>();
    }
}
