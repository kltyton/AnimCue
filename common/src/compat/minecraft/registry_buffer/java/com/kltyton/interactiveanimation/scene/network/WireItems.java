package com.kltyton.interactiveanimation.scene.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

final class WireItems {
    private WireItems() {}
    static ItemStack read(FriendlyByteBuf buffer) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    }

    static void write(FriendlyByteBuf buffer, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
    }
}
