package com.kltyton.interactiveanimation.scene.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MinecraftScenePayload(ScenePacket scene) implements CustomPacketPayload {
    public static final Type<MinecraftScenePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("interactive_animation", "scene"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MinecraftScenePayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> packet.scene.write(buffer), buffer -> new MinecraftScenePayload(ScenePacket.read(buffer)));

    @Override
    public Type<MinecraftScenePayload> type() {
        return TYPE;
    }
}
