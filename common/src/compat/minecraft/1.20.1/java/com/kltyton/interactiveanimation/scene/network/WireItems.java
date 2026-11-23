package com.kltyton.interactiveanimation.scene.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

final class WireItems {
    private WireItems() {}
    static ItemStack read(FriendlyByteBuf buffer) {
        return buffer.readItem();
    }

    static void write(FriendlyByteBuf buffer, ItemStack stack) {
        buffer.writeItem(stack);
    }
}
