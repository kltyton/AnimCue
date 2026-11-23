package com.kltyton.interactiveanimation.scene.playback;

import com.kltyton.interactiveanimation.api.AlignmentResult;
import com.kltyton.interactiveanimation.api.AnimationControls;
import com.kltyton.interactiveanimation.api.Bindings;
import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.api.SceneHandle;
import com.kltyton.interactiveanimation.api.SceneSubject;
import com.kltyton.interactiveanimation.scene.asset.BoneTrack;
import com.kltyton.interactiveanimation.scene.asset.SceneDefinition;
import com.kltyton.interactiveanimation.scene.network.ScenePacket;
import com.kltyton.interactiveanimation.scene.network.SceneTransport;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

final class ServerScene implements SceneHandle {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final UUID id = UUID.randomUUID();
    private final ServerLevel level;
    private final SceneSubject subject;
    private final SceneDefinition definition;
    private final PlaybackOptions options;
    private final SceneTransport transport;
    private final Map<String, Object> bindings;
    private final Map<String, Role> roles = new LinkedHashMap<>();
    private final Map<String, AlignmentResult> alignment = new LinkedHashMap<>();
    private final Set<Entity> entities = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ServerPlayer> viewers = new HashSet<>();
    private final List<AnimationControls.Lease> leases = new ArrayList<>();
    private final Set<String> alignmentNotified = new HashSet<>();
    private final ScenePacket.Start startPacket;
    private final long startTick;
    private State state = State.APPROACHING;
    private double seconds;
    private int marker;
    private boolean primaryStarted;
    private boolean finalFrame;

    ServerScene(ServerLevel level, SceneSubject subject, SceneDefinition definition, Bindings bindings,
                PlaybackOptions options, SceneTransport transport) {
        this.level = level;
        this.subject = subject;
        this.definition = definition;
        this.options = options;
        this.transport = transport;
        startTick = level.getGameTime();
        this.bindings = bindings.resolve(definition);
        if (subject.entity() != null) entities.add(subject.entity());
        var wire = new ArrayList<ScenePacket.Role>();
        for (var participant : definition.participants()) {
            Object instance = this.bindings.get(participant.role());
            Entity entity = instance instanceof Entity value ? value : null;
            if (entity != null && (!valid(entity) || !entities.add(entity))) {
                throw new IllegalArgumentException("Unavailable or duplicate entity for " + participant.role());
            }
            if (entity instanceof ServerPlayer player && !transport.available(player)) {
                throw new IllegalArgumentException("Player for " + participant.role() + " lacks scene networking");
            }
            var role = new Role(participant, entity);
            roles.put(participant.role(), role);
            wire.add(new ScenePacket.Role(entity == null ? -1 : entity.getId(),
                    instance instanceof ItemStack item ? item.copy() : null, instance instanceof BlockState block ? block : null,
                    role.driven(), bindings.boneMappings().getOrDefault(participant.role(), Map.of())));
        }
        int primary = subject.entity() == null ? -1 : subject.entity().getId();
        startPacket = new ScenePacket.Start(definition.id(), primary, subject.nativePlayback(), options.camera(),
                options.lockDrivenMovement(), options.lockAttack(), options.lockInteraction(), wire);
    }

    void start() {
        try {
            if (options.carrier() != null && !valid(options.carrier())) {
                throw new IllegalArgumentException("Unavailable scene carrier");
            }
            for (Role role : roles.values()) {
                if (!role.driven()) continue;
                BoneTrack.Pose target = role.sample(0);
                if (subject.transitionTicks() == 0 && !options.snapWithoutTransition()
                        && role.initial.position().distanceTo(target.position()) > options.alignmentTolerance()) {
                    finishAlignment(role, AlignmentResult.Status.MISALIGNED, target.position());
                    finish(State.CANCELLED);
                    return;
                }
                if (options.checkApproachPath() && subject.transitionTicks() == 0
                        && !SceneCollision.canApproach(role.entity, role.entity.position(), target.position())) {
                    finishAlignment(role, AlignmentResult.Status.BLOCKED, target.position());
                    finish(State.CANCELLED);
                    return;
                }
            }

            if (subject.entity() instanceof Mob && options.pauseSubjectAi()) {
                leases.add(AnimationControls.acquire(subject.entity(), new AnimationControls.Options(true, false, false, false)));
            }
            for (Role role : roles.values()) {
                if (role.entity == null) continue;
                boolean lock = role.driven() && options.lockDrivenMovement();
                if (lock || options.lockAttack() || options.lockInteraction()) {
                    leases.add(AnimationControls.acquire(role.entity, new AnimationControls.Options(lock && role.entity instanceof Mob,
                            lock && role.entity instanceof ServerPlayer, options.lockAttack(), options.lockInteraction())));
                }
            }
            primaryStarted = true;
            subject.play().run();
            tick();
        } catch (RuntimeException error) {
            finish(State.CANCELLED);
            throw error;
        }
    }

    void tick() {
        if (closed()) return;
        if (finalFrame) {
            finish(State.FINISHED);
            return;
        }

        try {
            if (!subject.valid().getAsBoolean() || entities.stream().anyMatch(entity -> !valid(entity))
                    || roles.values().stream().anyMatch(role -> role.carrier != null && !valid(role.carrier))) {
                finish(State.CANCELLED);
                return;
            }
            tickScene();
        } catch (RuntimeException error) {
            finish(State.CANCELLED);
            LOGGER.error("Interactive animation {} failed", definition.id(), error);
        }
    }

    private void tickScene() {
        long elapsed = Math.max(0, level.getGameTime() - startTick);
        int transition = subject.transitionTicks();
        boolean approaching = elapsed < transition;
        double blend = approaching ? (double) elapsed / transition : 1;
        seconds = Math.min(definition.length(), Math.max(0, elapsed - transition) / 20.0);
        List<BoneTrack.Pose> poses = new ArrayList<>(roles.size());
        for (Role role : roles.values()) {
            BoneTrack.Pose target = role.sample(seconds);
            BoneTrack.Pose pose = target;
            if (role.driven()) {
                if (approaching) pose = role.initial.lerp(target, blend);
                boolean aligning = !alignment.containsKey(role.participant.role());
                if (aligning && options.checkApproachPath()
                        && !SceneCollision.canApproach(role.entity, role.entity.position(), pose.position())) {
                    finishAlignment(role, AlignmentResult.Status.BLOCKED, target.position());
                    finish(State.CANCELLED);
                    return;
                }
                move(role.entity, pose);
                if (aligning && !approaching) {
                    finishAlignment(role, AlignmentResult.Status.ALIGNED, target.position());
                    deliverAlignment(role.participant.role());
                    if (closed()) return;
                }
            }
            poses.add(pose);
        }
        if (!approaching) {
            state = State.PLAYING;
            while (marker < definition.markers().size() && definition.markers().get(marker).seconds() <= seconds) {
                var event = definition.markers().get(marker++);
                options.marker().accept(new PlaybackOptions.Marker(this, event.instruction(), event.seconds()));
                if (closed()) return;
            }
        }
        publish(new ScenePacket.Frame(seconds, approaching, poses));
        finalFrame = !approaching && seconds >= definition.length();
    }

    private void publish(ScenePacket.Frame frame) {
        viewers.removeIf(player -> !level.players().contains(player));
        for (ServerPlayer player : level.players()) {
            if (!transport.available(player)) continue;
            boolean joined = viewers.add(player);
            transport.send(player, new ScenePacket(id, joined ? startPacket : null, frame));
        }
    }

    private static void move(Entity entity, BoneTrack.Pose pose) {
        entity.setPos(pose.position());
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof ServerPlayer player) {
            player.connection.resetPosition();
        } else {
            entity.setYRot((float) pose.rotation().y);
        }
    }

    private boolean valid(Entity entity) {
        return entity.isAlive() && !entity.isRemoved() && entity.level() == level;
    }

    private void finishAlignment(Role role, AlignmentResult.Status status, Vec3 target) {
        alignment.put(role.participant.role(), new AlignmentResult(role.participant.role(), status,
                role.initial.position(), target, role.entity.position()));
    }

    private void deliverAlignment(String role) {
        if (!alignmentNotified.add(role)) return;
        try {
            options.alignment().accept(alignment.get(role));
        } catch (RuntimeException error) {
            LOGGER.error("Alignment callback failed for {} role {}", definition.id(), role, error);
        }
    }

    private void finish(State terminal) {
        if (closed()) return;
        state = terminal;
        try {
            if (primaryStarted) subject.stop().run();
        } catch (RuntimeException error) {
            LOGGER.error("Could not release primary animation {}", definition.id(), error);
        }
        for (int index = leases.size() - 1; index >= 0; index--) {
            try {
                leases.get(index).close();
            } catch (RuntimeException error) {
                LOGGER.error("Could not release controls for {}", definition.id(), error);
            }
        }
        leases.clear();
        for (ServerPlayer player : viewers) {
            if (transport.available(player)) transport.send(player, new ScenePacket(id, null, null));
        }
        viewers.clear();
        if (terminal == State.CANCELLED) {
            for (Role role : roles.values()) {
                if (!role.driven()) continue;
                var result = alignment.get(role.participant.role());
                if (result == null) {
                    finishAlignment(role, AlignmentResult.Status.CANCELLED, role.sample(0).position());
                    result = alignment.get(role.participant.role());
                }
                if (!result.aligned()) deliverAlignment(role.participant.role());
            }
        }
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
        return bindings.get(role);
    }

    @Override
    public void cancel() {
        finish(State.CANCELLED);
    }

    @Override
    public void useWorldCoordinates(String name) {
        Role role = role(name);
        SceneSubject.Frame fixed = role.motion.reference();
        role.motion.switchReference(() -> fixed, role.participant.root(), seconds);
        role.carrier = null;
    }

    @Override
    public void follow(String name, Entity carrier) {
        if (!valid(carrier)) throw new IllegalArgumentException("Unavailable scene carrier");
        Role role = role(name);
        if (carrier == role.entity) throw new IllegalArgumentException("A participant cannot follow itself");
        role.motion.switchReference(() -> SceneSubject.Frame.of(carrier), role.participant.root(), seconds);
        role.carrier = carrier;
    }

    private Role role(String name) {
        Role role = roles.get(name);
        if (role == null) throw new IllegalArgumentException("Unknown role " + name);
        if (closed()) throw new IllegalStateException("The scene has ended");
        return role;
    }

    boolean conflicts(ServerScene other) {
        return !closed() && other.entities.stream().anyMatch(entities::contains);
    }

    boolean closed() {
        return state == State.CANCELLED || state == State.FINISHED;
    }

    private final class Role {
        final SceneDefinition.Participant participant;
        final Entity entity;
        final RoleMotion motion;
        final BoneTrack.Pose initial;
        Entity carrier = options.carrier();

        Role(SceneDefinition.Participant participant, Entity entity) {
            this.participant = participant;
            this.entity = entity;
            Supplier<SceneSubject.Frame> reference = carrier == null ? subject.frame() : () -> SceneSubject.Frame.of(options.carrier());
            motion = new RoleMotion(reference);
            initial = entity == null ? sample(0) : new BoneTrack.Pose(entity.position(),
                    new Vec3(0, entity.getYRot(), 0), BoneTrack.UNIT_SCALE);
        }

        boolean driven() {
            return entity != null && participant.root() != null;
        }

        BoneTrack.Pose sample(double time) {
            if (entity != null && !driven()) {
                return new BoneTrack.Pose(entity.position(), new Vec3(0, entity.getYRot(), 0), BoneTrack.UNIT_SCALE);
            }
            return motion.sample(participant.root(), time);
        }
    }
}
