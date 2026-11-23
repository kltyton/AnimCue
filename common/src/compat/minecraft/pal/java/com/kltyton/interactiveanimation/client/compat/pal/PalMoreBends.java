package com.kltyton.interactiveanimation.client.compat.pal;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;

final class PalMoreBends {
    private PalMoreBends() {}

    static void apply(PlayerAnimBone bone, float x, float y, float z) {
        if (bone instanceof PalMoreBendHolder extended) {
            extended.palMore$setBend(x, y, z);
            extended.palMore$setBendVectorOverride(true);
        }
    }

    static void clear(PlayerAnimBone bone) {
        if (bone instanceof PalMoreBendHolder extended) {
            extended.palMore$setBend(0, 0, 0);
            extended.palMore$setBendVectorOverride(false);
        }
    }
}
