package com.kltyton.interactiveanimation.platform;

import net.fabricmc.loader.api.FabricLoader;

/** Fabric mod-presence probe for optional soft integrations. */
public final class PlatformMods {
    private PlatformMods() {}

    public static boolean isLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
