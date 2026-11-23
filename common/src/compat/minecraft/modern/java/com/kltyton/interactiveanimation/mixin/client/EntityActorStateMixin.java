package com.kltyton.interactiveanimation.mixin.client;

import com.kltyton.interactiveanimation.client.playback.ClientActor;
import com.kltyton.interactiveanimation.client.render.SceneRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
abstract class EntityActorStateMixin implements SceneRenderState {
    @Unique private ClientActor interactiveAnimation$actor;
    @Unique private float interactiveAnimation$nativeYaw;

    @Override public ClientActor interactiveAnimation$actor() { return interactiveAnimation$actor; }
    @Override public void interactiveAnimation$actor(ClientActor actor) { interactiveAnimation$actor = actor; }
    @Override public float interactiveAnimation$nativeYaw() { return interactiveAnimation$nativeYaw; }
    @Override public void interactiveAnimation$nativeYaw(float yaw) { interactiveAnimation$nativeYaw = yaw; }
}
