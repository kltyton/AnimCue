package com.kltyton.interactiveanimation.mixin.control;

import com.kltyton.interactiveanimation.api.AnimationControls;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerMovementMixin {
    @Shadow public ServerPlayer player;

    @ModifyVariable(method = "handleMovePlayer", at = @At("HEAD"), argsOnly = true)
    private ServerboundMovePlayerPacket interactiveAnimation$keepScenePosition(ServerboundMovePlayerPacket packet) {
        if (!interactiveAnimation$locked() || !packet.hasPosition()) {
            return packet;
        }
        double x = packet.getX(0);
        double y = packet.getY(0);
        double z = packet.getZ(0);
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return packet;
        if (packet.hasRotation()) return new ServerboundMovePlayerPacket.Rot(packet.getYRot(player.getYRot()),
                packet.getXRot(player.getXRot()), player.onGround());
        return new ServerboundMovePlayerPacket.StatusOnly(player.onGround());
    }

    @ModifyVariable(method = "handlePlayerInput", at = @At("HEAD"), argsOnly = true)
    private ServerboundPlayerInputPacket interactiveAnimation$neutralMovement(ServerboundPlayerInputPacket packet) {
        return interactiveAnimation$locked() ? new ServerboundPlayerInputPacket(0, 0, false, false) : packet;
    }

    @ModifyVariable(method = "handleMoveVehicle", at = @At("HEAD"), argsOnly = true)
    private ServerboundMoveVehiclePacket interactiveAnimation$neutralVehicle(ServerboundMoveVehiclePacket packet) {
        if (!interactiveAnimation$locked()) return packet;
        var vehicle = player.getRootVehicle();
        return vehicle == player ? packet : new ServerboundMoveVehiclePacket(vehicle);
    }

    @Unique
    private boolean interactiveAnimation$locked() {
        return player.level() instanceof ServerLevel level && level.getServer().isSameThread()
                && AnimationControls.state(player).movement();
    }
}
