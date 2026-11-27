package com.kltyton.interactiveanimation.client.playback;

import com.kltyton.interactiveanimation.api.AlignmentResult;
import com.kltyton.interactiveanimation.api.AnimationControls;
import com.kltyton.interactiveanimation.api.Bindings;
import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.api.SceneHandle;
import com.kltyton.interactiveanimation.api.SceneSubject;
import com.kltyton.interactiveanimation.scene.asset.BoneTrack;
import com.kltyton.interactiveanimation.scene.asset.SceneDefinition;
import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import com.kltyton.interactiveanimation.scene.playback.RoleMotion;
import com.kltyton.interactiveanimation.scene.playback.SceneCollision;
import com.kltyton.interactiveanimation.scene.playback.SceneEffectDispatcher;
import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class ClientScene implements SceneHandle {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_QUEUED_FRAMES = 3;
    private final ClientLevel level;
    private final SceneDefinition definition;
    private final SceneSubject subject;
    private final PlaybackOptions options;
    private final ScenePacket.Start remote;
    private final Map<String, Role> roles = new LinkedHashMap<>();
    private final Map<String, AlignmentResult> alignment = new LinkedHashMap<>();
    private final long start;
    private final Map<String, BoneTrack> primaryBones = new LinkedHashMap<>();
    private final Set<String> primaryMissing = ClientActor.diagnostics();
    private State state = State.APPROACHING;
    private ScenePacket.Frame before;
    private ScenePacket.Frame current;
    private final ArrayDeque<ScenePacket.Frame> waitingFrames = new ArrayDeque<>();
    private ScenePacket.Frame latestRemoteFrame;
    private int marker;
    private final SceneEffectDispatcher effects = new SceneEffectDispatcher();
    private Entity controlled;
    private AnimationControls.Lease control;
    private boolean primaryStarted;
    private boolean finalFrame;
    private Double cameraBasisYaw;

    ClientScene(ClientLevel level, SceneDefinition definition, SceneSubject subject, Bindings bindings, PlaybackOptions options) {
        this.level = level;
        this.definition = definition;
        this.subject = subject;
        this.options = options;
        remote = null;
        start = level.getGameTime();
        if (!subject.nativePlayback()) readBones(definition.primaryAnimation().getAsJsonObject("bones"), Map.of(), primaryBones, "primary");
        var values = bindings.resolve(definition);
        int index = 0;
        for (var participant : definition.participants()) {
            roles.put(participant.role(), new Role(index++, participant, values.get(participant.role()), -1, false,
                    bindings.boneMappings().getOrDefault(participant.role(), Map.of())));
        }
    }

    ClientScene(ClientLevel level, SceneDefinition definition, ScenePacket.Start remote, ScenePacket.Frame frame) {
        this.level = level;
        this.definition = definition;
        this.remote = remote;
        subject = null;
        options = null;
        start = 0;
        if (definition.participants().size() != remote.roles().size()) throw new IllegalArgumentException("Client/server role count mismatch");
        var participants = definition.participants();
        for (int index = 0; index < participants.size(); index++) {
            var participant = participants.get(index);
            var binding = remote.roles().get(index);
            Object visual = binding.item() == null ? binding.block() : binding.item();
            roles.put(participant.role(), new Role(index, participant, visual, binding.entityId(),
                    binding.driven(), binding.bones()));
        }
        if (!remote.nativePrimary()) readBones(definition.primaryAnimation().getAsJsonObject("bones"), Map.of(), primaryBones, "primary");
        accept(frame);
    }

    void start() {
        primaryStarted = true;
        try {
            subject.play().run();
            tick(null);
        } catch (RuntimeException error) {
            cancel();
            throw error;
        }
    }

    void accept(ScenePacket.Frame frame) {
        if (frame.poses().size() != roles.size()) throw new IllegalArgumentException("Scene snapshot role count mismatch");
        if (remote != null) {
            latestRemoteFrame = frame;
            if (current != null) {
                if (waitingFrames.size() == MAX_QUEUED_FRAMES) waitingFrames.removeFirst();
                waitingFrames.addLast(frame);
                return;
            }
        }
        present(frame);
    }

    private void present(ScenePacket.Frame frame) {
        before = current == null ? frame : current;
        current = frame;
        state = frame.approaching() ? State.APPROACHING : State.PLAYING;
    }

    void tick(Entity player) {
        if (closed()) return;
        if (remote != null) {
            // A late snapshot holds the last pose. Partial-tick rollover cannot replay the previous step.
            ScenePacket.Frame frame = waitingFrames.pollFirst();
            present(frame == null ? current : frame);
            syncLocalPlayer(player);
            return;
        }
        if (finalFrame) {
            finish(State.FINISHED);
            return;
        }
        if (!subject.valid().getAsBoolean() || roles.values().stream().anyMatch(role -> !role.valid())) {
            finish(State.CANCELLED);
            return;
        }

        int ticks = subject.transitionTicks();
        long elapsed = Math.max(0, level.getGameTime() - start);
        boolean approaching = elapsed < ticks;
        double blend = approaching ? (double) elapsed / ticks : 1;
        double time = Math.min(definition.length(), Math.max(0, elapsed - ticks) / 20.0);
        var poses = new ArrayList<BoneTrack.Pose>(roles.size());
        var rolePositions = new LinkedHashMap<String, Vec3>(roles.size());
        for (var role : roles.values()) {
            var target = role.motion.sample(role.participant.root(), time);
            if (role.bound == null || role.participant.root() == null) {
                if (role.bound != null) target = role.actual(role.bound);
                poses.add(target);
                rolePositions.put(role.participant.role(), target.position());
                continue;
            }
            Vec3 firstFrame = target.position();
            if (approaching) target = role.initial.lerp(target, blend);
            if (options.checkApproachPath() && !alignment.containsKey(role.participant.role())) {
                Vec3 previous = current == null ? role.initial.position() : current.poses().get(role.index).position();
                if (!SceneCollision.canApproach(role.bound, previous, target.position())) {
                    var result = new AlignmentResult(role.participant.role(), AlignmentResult.Status.BLOCKED,
                            role.initial.position(), firstFrame, previous);
                    alignment.put(result.role(), result);
                    finish(State.CANCELLED);
                    options.alignment().accept(result);
                    return;
                }
            }
            if (!approaching && !alignment.containsKey(role.participant.role())) {
                boolean correct = ticks > 0 || options.snapWithoutTransition()
                        || role.initial.position().distanceTo(target.position()) <= options.alignmentTolerance();
                var result = new AlignmentResult(role.participant.role(), correct ? AlignmentResult.Status.ALIGNED
                        : AlignmentResult.Status.MISALIGNED, role.initial.position(), target.position(),
                        correct ? target.position() : role.initial.position());
                alignment.put(result.role(), result);
                if (!correct) finish(State.CANCELLED);
                options.alignment().accept(result);
                if (closed()) return;
            }
            poses.add(target);
            rolePositions.put(role.participant.role(), target.position());
        }
        accept(new ScenePacket.Frame(time, approaching, poses));
        if (!approaching) {
            while (marker < definition.markers().size() && definition.markers().get(marker).seconds() <= time) {
                var value = definition.markers().get(marker++);
                options.marker().accept(new PlaybackOptions.Marker(this, value.instruction(), value.seconds()));
                if (closed()) return;
            }
            effects.dispatch(level, this, definition, options, subject.nativePlayback(), time,
                    () -> subject.frame().get().position(), subject.entity(),
                    role -> rolePositions.getOrDefault(role, subject.frame().get().position()),
                    role -> {
                        Role value = roles.get(role);
                        return value == null ? null : value.entity();
                    }, this::closed);
            if (closed()) return;
        }
        finalFrame = !approaching && time >= definition.length();
    }

    private void syncLocalPlayer(Entity player) {
        if (player == null || !player.isAlive() || current == null) {
            closeControls();
            return;
        }
        for (Role role : roles.values()) {
            if (role.entityId != player.getId()) continue;
            if (role.physical) {
                player.setPos(latestRemoteFrame.poses().get(role.index).position());
                if (remote.moveLock()) player.setDeltaMovement(Vec3.ZERO);
            }
            if (controlled != player && (role.physical && remote.moveLock() || remote.attackLock() || remote.interactionLock())) {
                closeControls();
                controlled = player;
                control = AnimationControls.acquire(player, new AnimationControls.Options(false,
                        role.physical && remote.moveLock(), remote.attackLock(), remote.interactionLock()));
            }
            return;
        }
        closeControls();
    }

    ClientActor actor(Entity entity, float partialTick) {
        if (closed() || current == null) return null;
        Role preview = null;
        for (Role role : roles.values()) {
            var bound = role.entity();
            if (bound == entity) return role.actor(partialTick);
            if (preview == null && bound == null && role.visual == entity) preview = role;
        }
        int subject = subjectId();
        if (!primaryBones.isEmpty() && subject >= 0 && entity.getId() == subject) {
            var pose = new BoneTrack.Pose(entity.getPosition(partialTick), new Vec3(0, entity.getYRot(), 0), BoneTrack.UNIT_SCALE);
            return new ClientActor("primary", entity, entity, false, pose, time(partialTick), primaryBones, primaryMissing);
        }
        return preview == null ? null : preview.actor(partialTick);
    }

    Entity subjectFor(Entity participant, String roleName) {
        if (closed() || current == null) return null;
        Role role = roles.get(roleName);
        if (role == null || role.entity() != participant) return null;
        int id = subjectId();
        return id < 0 ? null : level.getEntity(id);
    }

    private int subjectId() {
        if (remote != null) return remote.subjectId();
        return subject.entity() == null ? -1 : subject.entity().getId();
    }

    void collectExtras(List<ClientActor> result, float partialTick, java.util.function.Predicate<Entity> submitted) {
        if (closed() || current == null) return;
        for (var role : roles.values()) {
            Entity entity = role.entity();
            if (entity != null ? !submitted.test(entity) : role.entityId < 0) {
                result.add(role.actor(partialTick));
            }
        }
    }

    CameraPose camera(Entity player, float partialTick) {
        if (player == null || !player.isAlive()) return null;
        var mode = remote == null ? options.camera() : remote.camera();
        if (mode == PlaybackOptions.CameraMode.VANILLA) return null;
        var actor = actor(player, partialTick);
        int subject = subjectId();
        if ((actor == null || !roles.containsKey(actor.role())) && subject >= 0 && player.getId() == subject
                && current != null && (mode == PlaybackOptions.CameraMode.LOCKED || mode == PlaybackOptions.CameraMode.AUTO && definition.camera() != null)) {
            var pose = new BoneTrack.Pose(player.getPosition(partialTick), new Vec3(0, player.getYRot(), 0), BoneTrack.UNIT_SCALE);
            actor = new ClientActor("primary", player, player, false, pose, time(partialTick), primaryBones, primaryMissing);
        }
        if (actor == null) return null;
        if (mode == PlaybackOptions.CameraMode.AUTO || mode == PlaybackOptions.CameraMode.FOLLOW) {
            var role = roles.get(actor.role());
            if ((role == null || role.participant.root() == null)
                    && (mode != PlaybackOptions.CameraMode.AUTO || definition.camera() == null)) return null;
        }
        if (cameraBasisYaw == null) cameraBasisYaw = (double) player.getYRot();
        if ((mode == PlaybackOptions.CameraMode.AUTO || mode == PlaybackOptions.CameraMode.LOCKED) && definition.camera() != null) {
            var authored = definition.camera().sample(actor.seconds());
            var anchor = new SceneSubject.Frame(actor.pose().position(), (float) actor.pose().rotation().y);
            var position = anchor.position().add(anchor.offset(authored.position().scale(1.0 / 16)));
            var rotated = authored.rotation().add(0, anchor.yaw(), 0);
            return new CameraPose(PlaybackOptions.CameraMode.LOCKED,
                    new BoneTrack.Pose(position, rotated, authored.scale()), actor.physical(), 0, true);
        }
        return new CameraPose(mode, actor.pose(), actor.physical(), cameraBasisYaw, false);
    }

    private double time(float partialTick) {
        return before.seconds() + (current.seconds() - before.seconds()) * partialTick;
    }

    private void readBones(com.google.gson.JsonObject source, Map<String, String> mapping,
                           Map<String, BoneTrack> destination, String role) {
        if (source == null) return;
        source.entrySet().forEach(entry -> {
            String name = mapping.getOrDefault(entry.getKey(), entry.getKey());
            try {
                if (destination.putIfAbsent(name, BoneTrack.read(entry.getValue().getAsJsonObject())) != null) {
                    LOGGER.warn("Scene {} role {} maps multiple tracks to {}", definition.id(), role, name);
                }
            } catch (IllegalArgumentException error) {
                LOGGER.warn("Scene {} role {} could not sample bone {}: {}", definition.id(), role, name, error.getMessage());
            }
        });
    }

    private double seconds() {
        return current == null ? 0 : current.seconds();
    }

    private void closeControls() {
        if (control != null) control.close();
        controlled = null;
        control = null;
    }

    private void finish(State finalState) {
        if (closed()) return;
        state = finalState;
        closeControls();
        if (primaryStarted) subject.stop().run();
    }

    boolean closed() {
        return state == State.FINISHED || state == State.CANCELLED;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public Map<String, AlignmentResult> alignmentResults() {
        return Map.copyOf(alignment);
    }

    @Override
    public Object binding(String role) {
        Role value = roles.get(role);
        if (value == null) return null;
        Entity entity = value.entity();
        return entity != null ? entity : value.visual;
    }

    @Override
    public void cancel() {
        finish(State.CANCELLED);
    }

    @Override
    public void useWorldCoordinates(String role) {
        Role value = localRole(role);
        var frame = value.motion.reference();
        value.motion.switchReference(() -> frame, value.participant.root(), seconds());
        value.carrier = null;
    }

    @Override
    public void follow(String role, Entity carrier) {
        Role value = localRole(role);
        if (carrier == value.bound || !carrier.isAlive() || carrier.level() != level) throw new IllegalArgumentException("Invalid carrier");
        value.motion.switchReference(() -> SceneSubject.Frame.of(carrier), value.participant.root(), seconds());
        value.carrier = carrier;
    }

    private Role localRole(String name) {
        if (remote != null || closed()) throw new IllegalStateException("Only an active local scene can change reference here");
        Role role = roles.get(name);
        if (role == null) throw new IllegalArgumentException("Unknown role " + name);
        return role;
    }

    public record CameraPose(PlaybackOptions.CameraMode mode, BoneTrack.Pose pose, boolean physical, double baseYaw, boolean authored) {}

    private final class Role {
        final SceneDefinition.Participant participant;
        final int index;
        final Object visual;
        final Entity bound;
        final int entityId;
        final boolean physical;
        final RoleMotion motion;
        final BoneTrack.Pose initial;
        final Map<String, BoneTrack> bones = new LinkedHashMap<>();
        final Set<String> missing = ClientActor.diagnostics();
        Entity carrier;

        Role(int index, SceneDefinition.Participant participant, Object value, int entityId, boolean physical,
             Map<String, String> mapping) {
            this.index = index;
            this.participant = participant;
            this.entityId = entityId;
            this.physical = physical;
            bound = value instanceof Entity entity ? entity : null;
            visual = value == null && entityId < 0 ? ClientAssets.preview(level, participant) : value;
            carrier = options == null ? null : options.carrier();
            motion = subject == null ? null : new RoleMotion(carrier == null ? subject.frame()
                    : () -> SceneSubject.Frame.of(options.carrier()));
            initial = bound == null ? BoneTrack.IDENTITY : actual(bound);
            participant.bones().forEach((authoredName, track) -> {
                String name = mapping.getOrDefault(authoredName, authoredName);
                if (bones.putIfAbsent(name, track) != null) {
                    LOGGER.warn("Scene {} role {} maps multiple tracks to {}", definition.id(), participant.role(), name);
                }
            });
        }

        Entity entity() {
            return entityId < 0 ? bound : level.getEntity(entityId);
        }

        boolean valid() {
            return (bound == null || bound.isAlive() && !bound.isRemoved() && bound.level() == level)
                    && (carrier == null || carrier.isAlive() && !carrier.isRemoved() && carrier.level() == level);
        }

        BoneTrack.Pose actual(Entity entity) {
            return new BoneTrack.Pose(entity.position(), new Vec3(0, entity.getYRot(), 0), BoneTrack.UNIT_SCALE);
        }

        ClientActor actor(float partialTick) {
            Entity entity = entity();
            var a = before.poses().get(index);
            var b = current.poses().get(index);
            double time = time(partialTick);
            return new ClientActor(participant.role(), entity == null ? visual : entity, entity, physical,
                    a.lerp(b, partialTick), time, bones, missing);
        }
    }
}
