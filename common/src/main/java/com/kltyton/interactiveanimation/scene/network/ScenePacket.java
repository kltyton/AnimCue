package com.kltyton.interactiveanimation.scene.network;

import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.api.SceneId;
import com.kltyton.interactiveanimation.scene.asset.BoneTrack;
import com.kltyton.interactiveanimation.scene.asset.SceneReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record ScenePacket(UUID session, Start start, Frame frame) {
    public record Start(SceneId scene, int subjectId, boolean nativePrimary, PlaybackOptions.CameraMode camera, boolean moveLock,
                        boolean attackLock, boolean interactionLock, List<Role> roles) {
        public Start {
            roles = List.copyOf(roles);
        }
    }

    public record Frame(double seconds, boolean approaching, List<BoneTrack.Pose> poses) {
        public Frame {
            poses = List.copyOf(poses);
        }
    }

    public record Role(int entityId, ItemStack item, BlockState block, boolean driven, Map<String, String> bones) {
        public Role {
            bones = Map.copyOf(bones);
        }
    }

    public static ScenePacket read(FriendlyByteBuf input) {
        UUID session = input.readUUID();
        int type = input.readUnsignedByte();
        if (type == 0) return new ScenePacket(session, null, null);
        if (type != 1 && type != 2) throw new IllegalArgumentException("Invalid scene packet type " + type);
        Start start = null;
        if (type == 2) {
            var id = new SceneId(input.readUtf(512), input.readUtf(512));
            int subjectId = input.readVarInt();
            boolean nativePrimary = input.readBoolean();
            var camera = input.readEnum(PlaybackOptions.CameraMode.class);
            boolean movement = input.readBoolean();
            boolean attack = input.readBoolean();
            boolean interaction = input.readBoolean();
            int count = count(input, SceneReader.MAX_PARTICIPANTS);
            var roles = new ArrayList<Role>(count);
            for (int i = 0; i < count; i++) {
                int entity = input.readVarInt();
                ItemStack item = input.readBoolean() ? WireItems.read(input) : null;
                BlockState block = input.readBoolean() ? Block.stateById(input.readVarInt()) : null;
                boolean driven = input.readBoolean();
                var mapping = new LinkedHashMap<String, String>();
                int size = count(input, 256);
                for (int j = 0; j < size; j++) mapping.put(input.readUtf(256), input.readUtf(256));
                roles.add(new Role(entity, item, block, driven, mapping));
            }
            start = new Start(id, subjectId, nativePrimary, camera, movement, attack, interaction, roles);
        }
        double seconds = input.readDouble();
        boolean approaching = input.readBoolean();
        int count = count(input, SceneReader.MAX_PARTICIPANTS);
        var poses = new ArrayList<BoneTrack.Pose>(count);
        for (int i = 0; i < count; i++) {
            poses.add(new BoneTrack.Pose(readVector(input), readVector(input), readVector(input)));
        }
        return new ScenePacket(session, start, new Frame(seconds, approaching, poses));
    }

    public void write(FriendlyByteBuf output) {
        output.writeUUID(session);
        output.writeByte(frame == null ? 0 : start == null ? 1 : 2);
        if (frame == null) return;
        if (start != null) {
            output.writeUtf(start.scene.file(), 512);
            output.writeUtf(start.scene.animation(), 512);
            output.writeVarInt(start.subjectId);
            output.writeBoolean(start.nativePrimary);
            output.writeEnum(start.camera);
            output.writeBoolean(start.moveLock);
            output.writeBoolean(start.attackLock);
            output.writeBoolean(start.interactionLock);
            output.writeVarInt(start.roles.size());
            for (Role role : start.roles) {
                output.writeVarInt(role.entityId);
                output.writeBoolean(role.item != null);
                if (role.item != null) WireItems.write(output, role.item);
                output.writeBoolean(role.block != null);
                if (role.block != null) output.writeVarInt(Block.getId(role.block));
                output.writeBoolean(role.driven);
                output.writeVarInt(role.bones.size());
                role.bones.forEach((from, to) -> {
                    output.writeUtf(from, 256);
                    output.writeUtf(to, 256);
                });
            }
        }
        output.writeDouble(frame.seconds);
        output.writeBoolean(frame.approaching);
        output.writeVarInt(frame.poses.size());
        for (BoneTrack.Pose pose : frame.poses) {
            writeVector(output, pose.position());
            writeVector(output, pose.rotation());
            writeVector(output, pose.scale());
        }
    }

    private static Vec3 readVector(FriendlyByteBuf input) {
        return new Vec3(input.readDouble(), input.readDouble(), input.readDouble());
    }

    private static void writeVector(FriendlyByteBuf output, Vec3 vector) {
        output.writeDouble(vector.x);
        output.writeDouble(vector.y);
        output.writeDouble(vector.z);
    }

    private static int count(FriendlyByteBuf input, int limit) {
        int count = input.readVarInt();
        if (count < 0 || count > limit) throw new IllegalArgumentException("Invalid scene list length " + count);
        return count;
    }
}
