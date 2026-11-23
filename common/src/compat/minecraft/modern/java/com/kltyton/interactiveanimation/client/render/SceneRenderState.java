package com.kltyton.interactiveanimation.client.render;

import com.kltyton.interactiveanimation.client.playback.ClientActor;

public interface SceneRenderState {
    ClientActor interactiveAnimation$actor();
    void interactiveAnimation$actor(ClientActor actor);
    float interactiveAnimation$nativeYaw();
    void interactiveAnimation$nativeYaw(float yaw);
}
