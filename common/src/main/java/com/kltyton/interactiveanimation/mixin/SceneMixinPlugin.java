package com.kltyton.interactiveanimation.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class SceneMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> ownTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return List.of(); }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        boolean nativeAddon = mixin.endsWith("KbrAddonActorMixin");
        if (!nativeAddon && !mixin.endsWith("GeckoActorMixin")) return true;
        try {
            ClassNode type = MixinService.getService().getBytecodeProvider().getClassNode(target);
            if (type == null) return false;
            return !nativeAddon || type.methods.stream().anyMatch(method -> method.name.equals("applyAddonPose")
                    && method.desc.equals("(Lcom/geckolib/renderer/base/RenderPassInfo;Lcom/geckolib/renderer/base/BoneSnapshots;"
                    + "Lcom/kltyton/bedrockruntime/client/addon/animation/BedrockAnimationPose;)V"));
        } catch (ClassNotFoundException | java.io.IOException unavailable) {
            return false;
        }
    }
}
