package com.kltyton.interactiveanimation.compat.geckolib;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoEntity;

final class GeckoStops {
    private GeckoStops() {}

    static void entity(GeoEntity entity, String controller, String trigger) {
        entity.stopTriggeredAnim(controller, trigger);
    }

    static void block(GeoBlockEntity block, String controller, String trigger) {
        block.stopTriggeredAnim(controller, trigger);
    }
}