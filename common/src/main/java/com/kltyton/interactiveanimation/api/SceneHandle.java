package com.kltyton.interactiveanimation.api;

import java.util.Map;
import net.minecraft.world.entity.Entity;

public interface SceneHandle {
    enum State { APPROACHING, PLAYING, FINISHED, CANCELLED }

    State state();
    Map<String, AlignmentResult> alignmentResults();
    Object binding(String role);
    void cancel();
    void useWorldCoordinates(String role);
    void follow(String role, Entity carrier);
}
