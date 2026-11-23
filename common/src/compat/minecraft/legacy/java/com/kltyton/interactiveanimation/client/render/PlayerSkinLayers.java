package com.kltyton.interactiveanimation.client.render;

import com.kltyton.interactiveanimation.client.playback.ClientActor;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Mirrors the changes made after vanilla has copied the body pose to its outer skin. */
public final class PlayerSkinLayers {
    private PlayerSkinLayers() {}

    public static void follow(ClientActor actor, PlayerModel<?> model) {
        attach(actor, "hat", model.hat, model.head, "head");
        attach(actor, "jacket", model.jacket, model.body, "body", "torso");
        attach(actor, "left_sleeve", model.leftSleeve, model.leftArm, "left_arm");
        attach(actor, "right_sleeve", model.rightSleeve, model.rightArm, "right_arm");
        attach(actor, "left_pants", model.leftPants, model.leftLeg, "left_leg");
        attach(actor, "right_pants", model.rightPants, model.rightLeg, "right_leg");
    }

    public static ModelPart find(PlayerModel<?> model, String name) {
        return switch (name) {
            case "jacket" -> model.jacket;
            case "left_sleeve" -> model.leftSleeve;
            case "right_sleeve" -> model.rightSleeve;
            case "left_pants" -> model.leftPants;
            case "right_pants" -> model.rightPants;
            default -> null;
        };
    }

    private static void attach(ClientActor actor, String layer, ModelPart overlay, ModelPart base, String... possibleTracks) {
        if (actor.bones().containsKey(layer)) return;
        for (String track : possibleTracks) {
            if (!actor.bones().containsKey(track)) continue;
            var previous = overlay.storePose();
            float sx = overlay.xScale, sy = overlay.yScale, sz = overlay.zScale;
            LegacyActorRenderer.restoreAfterDraw(() -> {
                overlay.loadPose(previous);
                overlay.xScale = sx;
                overlay.yScale = sy;
                overlay.zScale = sz;
            });
            overlay.copyFrom(base);
            return;
        }
    }
}
