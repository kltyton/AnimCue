package com.kltyton.interactiveanimation.platform;

import net.minecraftforge.fml.ModList;

/** Forge mod-presence probe for optional soft integrations. */
public final class PlatformMods {
    private PlatformMods() {}

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
