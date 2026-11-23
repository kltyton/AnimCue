package com.kltyton.interactiveanimation.scene.network;

import net.minecraft.server.level.ServerPlayer;

public interface SceneTransport {
    boolean available(ServerPlayer player);
    void send(ServerPlayer player, ScenePacket packet);
}
