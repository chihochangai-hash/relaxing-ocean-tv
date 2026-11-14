package cc.cd.relaxingocean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Picks the next on-screen control for a remote arrow, and maps its box onto the WebView. */
public final class TvFocus {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;

    private TvFocus() {}

    public static final class Box {
        public final String id;
        public final String kind;
        public final float left;
        public final float top;
        public final float right;
        public final float bottom;

        public Box(String id, String kind, float left, float top, float right, float bottom) {
            this.id = id;
            this.kind = kind;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    public static final class Page {
        public final float width;
        public final float height;
        public final List<Box> boxes;

        public Page(float width, float height, List<Box> boxes) {
            this.width = width;
            this.height = height;
            this.boxes = boxes;
        }
    }

    public static Page parse(String raw) {
        Page empty = new Page(0f, 0f, Collections.emptyList());
        if (raw == null) return empty;
        String text = raw.trim();
        if (text.isEmpty() || "null".equals(text)) return empty;
        try {
            Object value = new JSONTokener(text).nextValue();
            if (value instanceof String) value = new JSONTokener((String) value).nextValue();
            if (!(value instanceof JSONObject)) return empty;
            JSONObject obj = (JSONObject) value;
            JSONArray rows = obj.optJSONArray("boxes");
            List<Box> boxes = new ArrayList<>();
            if (rows != null) {
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.optJSONObject(i);
                    if (row == null) continue;
                    String id = row.optString("id", "");
                    if (id.isEmpty()) continue;
                    boxes.add(new Box(
                            id,
                            row.optString("kind", "button"),
                            (float) row.optDouble("left"),
                            (float) row.optDouble("top"),
                            (float) row.optDouble("right"),
                            (float) row.optDouble("bottom")));
                }
            }
            return new Page((float) obj.optDouble("w"), (float) obj.optDouble("h"), boxes);
        } catch (JSONException ignored) {
            return empty;
        }
    }

    public static Box find(List<Box> boxes, String id) {
        if (id == null || boxes == null) return null;
        for (Box box : boxes) {
            if (id.equals(box.id)) return box;
        }
        return null;
    }

    /** The opening cover wins. Otherwise the top-left control. */
    public static String first(List<Box> boxes) {
        if (boxes == null || boxes.isEmpty()) return null;
        Box overlay = null;
        Box best = null;
        for (Box box : boxes) {
            if ("overlay".equals(box.kind)) overlay = box;
            if (best == null || box.top < best.top - 0.5f
                    || (Math.abs(box.top - best.top) <= 0.5f && box.left < best.left)) {
                best = box;
            }
        }
        if (overlay != null) return overlay.id;
        return best == null ? null : best.id;
    }

    /** Left and right on a slider change its value instead of leaving it. */
    public static boolean nudgeRange(String kind, int direction) {
        return "range".equals(kind) && (direction == LEFT || direction == RIGHT);
    }

    /**
     * Next control in that direction. Same row or column wins over a nearer diagonal.
     * Returns null when nothing lies that way.
     */
    public static String step(List<Box> boxes, String currentId, int direction) {
        if (boxes == null || boxes.isEmpty()) return null;
        Box current = find(boxes, currentId);
        if (current == null) return first(boxes);
        float cx = (current.left + current.right) / 2f;
        float cy = (current.top + current.bottom) / 2f;
        String bestId = null;
        float bestScore = Float.MAX_VALUE;
        for (Box box : boxes) {
            if (box.id.equals(current.id)) continue;
            float ox = (box.left + box.right) / 2f;
            float oy = (box.top + box.bottom) / 2f;
            float dx = ox - cx;
            float dy = oy - cy;
            float primary;
            float secondary;
            boolean aligned;
            if (direction == RIGHT) {
                primary = dx;
                secondary = Math.abs(dy);
                aligned = overlap(current.top, current.bottom, box.top, box.bottom);
            } else if (direction == LEFT) {
                primary = -dx;
                secondary = Math.abs(dy);
                aligned = overlap(current.top, current.bottom, box.top, box.bottom);
            } else if (direction == DOWN) {
                primary = dy;
                secondary = Math.abs(dx);
                aligned = overlap(current.left, current.right, box.left, box.right);
            } else {
                primary = -dy;
                secondary = Math.abs(dx);
                aligned = overlap(current.left, current.right, box.left, box.right);
            }
            if (primary < 8f) continue;
            float score = primary + secondary * 2.5f;
            if (aligned) score -= 400f;
            if (score < bestScore) {
                bestScore = score;
                bestId = box.id;
            }
        }
        return bestId;
    }

    /** CSS pixel center mapped into the WebView's own pixel space. */
    public static float[] viewPoint(float cssX, float cssY, float cssW, float cssH, int viewW, int viewH) {
        if (cssW <= 1f || cssH <= 1f || viewW <= 1 || viewH <= 1) return null;
        float x = cssX * viewW / cssW;
        float y = cssY * viewH / cssH;
        if (x < 1f) x = 1f;
        if (y < 1f) y = 1f;
        if (x > viewW - 2f) x = viewW - 2f;
        if (y > viewH - 2f) y = viewH - 2f;
        return new float[] { x, y };
    }

    public static String jsQuote(String id) {
        if (id == null) return "";
        return id.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static boolean overlap(float a1, float a2, float b1, float b2) {
        return Math.min(a2, b2) - Math.max(a1, b1) > 1f;
    }
}
