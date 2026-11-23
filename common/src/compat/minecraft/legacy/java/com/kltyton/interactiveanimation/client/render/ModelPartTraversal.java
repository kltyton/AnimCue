package com.kltyton.interactiveanimation.client.render;

import java.util.stream.Stream;
import net.minecraft.client.model.geom.ModelPart;

final class ModelPartTraversal {
    private ModelPartTraversal() {}
    static Stream<ModelPart> parts(ModelPart part) {
        return part.getAllParts();
    }
}