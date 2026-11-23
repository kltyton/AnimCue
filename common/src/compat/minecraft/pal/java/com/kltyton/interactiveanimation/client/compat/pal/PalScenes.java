package com.kltyton.interactiveanimation.client.compat.pal;

import com.kltyton.interactiveanimation.client.render.PlayerPoseOwnership;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;

public final class PalScenes {
    private PalScenes() {}

    public static void initialize(boolean moreRotation) {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((actor, stack) -> {
            PlayerPoseOwnership.register(actor);
            stack.addAnimLayer(900, new PalSceneLayer(actor, moreRotation));
        });
    }
}
