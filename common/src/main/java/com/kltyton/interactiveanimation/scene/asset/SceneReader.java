package com.kltyton.interactiveanimation.scene.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.interactiveanimation.api.SceneId;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SceneReader {
    public static final String SECTION = "interactive_animation:interactions";
    public static final int MAX_PARTICIPANTS = 64;

    private SceneReader() {}

    public static Map<SceneId, SceneDefinition> read(String file, Reader input) {
        try {
            JsonElement document = JsonParser.parseReader(input);
            return document.isJsonObject() ? read(file, document.getAsJsonObject()) : Map.of();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Invalid interaction animation " + file + ": " + error.getMessage(), error);
        }
    }

    private static Map<SceneId, SceneDefinition> read(String file, JsonObject document) {
        Map<SceneId, SceneDefinition> scenes = new LinkedHashMap<>();
        if (!document.has(SECTION)) return scenes;

        JsonObject animations = document.getAsJsonObject("animations");
        if (animations == null) throw new IllegalArgumentException("Missing animations object");
        for (var entry : document.getAsJsonObject(SECTION).entrySet()) {
            SceneId id = new SceneId(file, entry.getKey());
            JsonObject primary = animations.getAsJsonObject(id.animation());
            if (primary == null) throw new IllegalArgumentException("No primary animation " + id.animation());
            scenes.put(id, parse(id, primary, entry.getValue().getAsJsonObject()));
        }
        return scenes;
    }

    private static SceneDefinition parse(SceneId id, JsonObject primary, JsonObject extension) {
        var participants = new ArrayList<SceneDefinition.Participant>();
        var roles = new HashSet<String>();
        var list = extension.getAsJsonArray("participants");
        if (list == null || list.isEmpty() || list.size() > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException("Expected 1 to " + MAX_PARTICIPANTS + " participants in " + id.animation());
        }
        double length = lastBoneTime(primary.getAsJsonObject("bones"));
        for (JsonElement entry : list) {
            JsonObject participant = entry.getAsJsonObject();
            String role = string(participant, "role");
            if (!roles.add(role)) throw new IllegalArgumentException("Repeated participant role " + role);
            String kind = participant.has("type") ? string(participant, "type") : "model";
            String asset = string(participant, "id");
            var bones = new LinkedHashMap<String, BoneTrack>();
            if (participant.has("bones")) {
                for (var entryBone : participant.getAsJsonObject("bones").entrySet()) {
                    try {
                        bones.put(entryBone.getKey(), BoneTrack.read(entryBone.getValue().getAsJsonObject()));
                    } catch (RuntimeException error) {
                        throw new IllegalArgumentException("Invalid track for role " + role + " bone " + entryBone.getKey(), error);
                    }
                }
            }
            BoneTrack rootTrack = bones.remove(BoneTrack.ROOT);
            if (rootTrack != null) length = Math.max(length, rootTrack.endTime());
            for (BoneTrack track : bones.values()) length = Math.max(length, track.endTime());
            participants.add(new SceneDefinition.Participant(role,
                    SceneDefinition.AssetKind.valueOf(kind.toUpperCase(Locale.ROOT)), asset, rootTrack, bones));
        }
        var markers = new ArrayList<SceneDefinition.Marker>();
        JsonObject timeline = primary.getAsJsonObject("timeline");
        if (timeline != null) {
            for (var marker : timeline.entrySet()) {
                double seconds = time(marker.getKey());
                if (marker.getValue().isJsonArray()) {
                    for (var instruction : marker.getValue().getAsJsonArray()) addMarker(markers, seconds, instruction);
                } else addMarker(markers, seconds, marker.getValue());
                length = Math.max(length, seconds);
            }
            markers.sort(Comparator.comparingDouble(SceneDefinition.Marker::seconds));
        }
        var camera = extension.getAsJsonObject("camera");
        BoneTrack cameraTrack = camera == null ? null : BoneTrack.read(camera);
        if (cameraTrack != null) length = Math.max(length, cameraTrack.endTime());
        if (primary.has("animation_length")) {
            double authoredLength = time(primary.get("animation_length").getAsString());
            if (length > authoredLength + 0.00001) throw new IllegalArgumentException("A track or marker exceeds animation_length");
            length = authoredLength;
        }
        return new SceneDefinition(id, length, primary, participants, markers, cameraTrack);
    }

    private static void addMarker(ArrayList<SceneDefinition.Marker> markers, double seconds, JsonElement instruction) {
        if (!instruction.isJsonPrimitive() || !instruction.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Timeline instruction must be text at " + seconds);
        }
        markers.add(new SceneDefinition.Marker(seconds, instruction.getAsString()));
    }

    private static double lastBoneTime(JsonObject bones) {
        if (bones == null) return 0;
        double max = 0;
        for (var bone : bones.entrySet()) {
            for (var channel : bone.getValue().getAsJsonObject().entrySet()) {
                if (!channel.getValue().isJsonObject()) continue;
                for (String key : channel.getValue().getAsJsonObject().keySet()) {
                    try {
                        double seconds = Double.parseDouble(key);
                        if (seconds >= 0 && Double.isFinite(seconds)) max = Math.max(max, seconds);
                    } catch (NumberFormatException ignored) {
                        // Non-time entries can be native animation metadata such as easing.
                    }
                }
            }
        }
        return max;
    }

    private static String string(JsonObject object, String name) {
        JsonElement field = object.get(name);
        if (field == null || !field.isJsonPrimitive() || !field.getAsJsonPrimitive().isString()
                || field.getAsString().isBlank()) {
            throw new IllegalArgumentException("Expected a nonempty string: " + name);
        }
        return field.getAsString();
    }

    private static double time(String input) {
        double seconds = Double.parseDouble(input);
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid time " + input);
        return seconds;
    }
}
