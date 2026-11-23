package com.kltyton.interactiveanimation.scene.asset;

import com.google.gson.JsonElement;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.phys.Vec3;

/** Numeric Bedrock vector curve. Rotation components retain their authored winding. */
public final class AnimationCurve {
    private record Frame(Vec3 before, Vec3 after, boolean spline) {}

    private final double[] times;
    private final Frame[] frames;

    private AnimationCurve(TreeMap<Double, Frame> keys) {
        times = new double[keys.size()];
        frames = new Frame[keys.size()];
        int index = 0;
        for (Map.Entry<Double, Frame> key : keys.entrySet()) {
            times[index] = key.getKey();
            frames[index++] = key.getValue();
        }
    }

    static AnimationCurve read(JsonElement json, Vec3 fallback) {
        TreeMap<Double, Frame> keys = new TreeMap<>();
        if (json == null || json.isJsonNull()) {
            keys.put(0.0, new Frame(fallback, fallback, false));
        } else if (json.isJsonObject() && (json.getAsJsonObject().has("pre") || json.getAsJsonObject().has("post"))) {
            keys.put(0.0, frame(json));
        } else if (json.isJsonObject()) {
            for (var entry : json.getAsJsonObject().entrySet()) {
                double time = Double.parseDouble(entry.getKey());
                if (!Double.isFinite(time) || time < 0 || keys.putIfAbsent(time == 0 ? 0 : time, frame(entry.getValue())) != null) {
                    throw new IllegalArgumentException("Invalid or duplicate keyframe time " + entry.getKey());
                }
            }
            if (keys.isEmpty()) keys.put(0.0, new Frame(fallback, fallback, false));
        } else {
            Vec3 value = vector(json);
            keys.put(0.0, new Frame(value, value, false));
        }
        return new AnimationCurve(keys);
    }

    private static Frame frame(JsonElement json) {
        if (!json.isJsonObject()) {
            Vec3 value = vector(json);
            return new Frame(value, value, false);
        }
        var key = json.getAsJsonObject();
        JsonElement pre = key.has("pre") ? key.get("pre") : key.get("post");
        JsonElement post = key.has("post") ? key.get("post") : pre;
        if (pre == null) throw new IllegalArgumentException("A keyframe requires pre or post");
        String interpolation = key.has("lerp_mode") ? key.get("lerp_mode").getAsString() : "linear";
        if (!interpolation.equals("linear") && !interpolation.equals("catmullrom")) {
            throw new IllegalArgumentException("Unknown Bedrock interpolation " + interpolation);
        }
        return new Frame(vector(pre), vector(post), interpolation.equals("catmullrom"));
    }

    private static Vec3 vector(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            double value = component(json);
            return new Vec3(value, value, value);
        }
        if (!json.isJsonArray() || json.getAsJsonArray().size() != 3) {
            throw new IllegalArgumentException("Expected a numeric Bedrock vector");
        }
        var array = json.getAsJsonArray();
        return new Vec3(component(array.get(0)), component(array.get(1)), component(array.get(2)));
    }

    private static double component(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected a numeric animation component");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) throw new IllegalArgumentException("Animation component must be finite");
        return number;
    }

    public double endTime() {
        return times[times.length - 1];
    }

    public Vec3 sample(double time) {
        int index = java.util.Arrays.binarySearch(times, time);
        if (index >= 0) return frames[index].after;
        int next = -index - 1;
        if (next == 0) return frames[0].before;
        if (next == times.length) return frames[next - 1].after;

        int previous = next - 1;
        Frame a = frames[previous];
        Frame b = frames[next];
        double fraction = (time - times[previous]) / (times[next] - times[previous]);
        if (!b.spline) return a.after.lerp(b.before, fraction);
        Vec3 p0 = previous > 0 ? frames[previous - 1].after : a.after;
        Vec3 p3 = next + 1 < frames.length ? frames[next + 1].before : b.before;
        return catmullRom(p0, a.after, b.before, p3, fraction);
    }

    private static Vec3 catmullRom(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
        Vec3 tangent = c.subtract(a);
        Vec3 quadratic = a.scale(2).subtract(b.scale(5)).add(c.scale(4)).subtract(d);
        Vec3 cubic = b.subtract(c).scale(3).add(d).subtract(a);
        return b.add(tangent.add(quadratic.add(cubic.scale(t)).scale(t)).scale(t * 0.5));
    }
}
