package com.kltyton.interactiveanimation.compat.geckolib;

import com.kltyton.interactiveanimation.api.SceneSubject;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;

public final class GeckoSubjects {
    private GeckoSubjects() {}

    public static <E extends Entity & GeoEntity> SceneSubject entity(E entity, String controller, String trigger,
                                                                    int serverTransitionTicks) {
        return SceneSubject.entity(entity).playback(serverTransitionTicks,
                () -> entity.triggerAnim(controller, trigger), () -> GeckoStops.entity(entity, controller, trigger));
    }

    public static <B extends BlockEntity & GeoBlockEntity> SceneSubject block(B block, float yaw, String controller,
                                                                               String trigger, int transitionTicks) {
        return SceneSubject.block(block, yaw).playback(transitionTicks,
                () -> block.triggerAnim(controller, trigger), () -> GeckoStops.block(block, controller, trigger));
    }

    public static SceneSubject item(ItemStack stack, Entity holder, long instanceId, String controller, String trigger,
                                    int transitionTicks) {
        if (!(stack.getItem() instanceof GeoItem item)) throw new IllegalArgumentException("The stack is not a GeckoLib item");
        return SceneSubject.item(stack, holder).playback(transitionTicks,
                () -> item.triggerAnim(holder, instanceId, controller, trigger),
                () -> item.stopTriggeredAnim(holder, instanceId, controller, trigger));
    }
}
