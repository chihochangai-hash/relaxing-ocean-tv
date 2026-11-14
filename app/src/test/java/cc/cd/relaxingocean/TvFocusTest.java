package cc.cd.relaxingocean;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TvFocusTest {
    @Test
    public void coverComesFirst() {
        List<TvFocus.Box> boxes = Arrays.asList(
                box("id:play-pause", "button", 900, 20, 940, 60),
                box("id:start-overlay", "overlay", 0, 0, 960, 540));
        assertEquals("id:start-overlay", TvFocus.first(boxes));
    }

    @Test
    public void otherwiseTheTopLeftControl() {
        List<TvFocus.Box> boxes = Arrays.asList(
                box("id:play-pause", "button", 900, 20, 940, 60),
                box("id:manual-toggle", "button", 700, 20, 740, 60));
        assertEquals("id:manual-toggle", TvFocus.first(boxes));
        assertNull(TvFocus.first(Collections.emptyList()));
    }

    @Test
    public void arrowsStayOnTheSameRow() {
        List<TvFocus.Box> boxes = Arrays.asList(
                box("manual", "button", 700, 20, 740, 60),
                box("eye", "button", 750, 20, 790, 60),
                box("play", "button", 900, 20, 940, 60),
                box("cruise", "button", 40, 480, 110, 516),
                box("eco", "button", 800, 200, 980, 228));
        assertEquals("eye", TvFocus.step(boxes, "manual", TvFocus.RIGHT));
        assertEquals("manual", TvFocus.step(boxes, "eye", TvFocus.LEFT));
        assertNull(TvFocus.step(boxes, "play", TvFocus.RIGHT));
        assertEquals("eco", TvFocus.step(boxes, "play", TvFocus.DOWN));
        assertNull(TvFocus.step(boxes, "cruise", TvFocus.DOWN));
        assertEquals("manual", TvFocus.step(boxes, "cruise", TvFocus.UP));
    }

    @Test
    public void aLostFocusStartsAtTheFirstControl() {
        List<TvFocus.Box> boxes = Collections.singletonList(
                box("id:manual-toggle", "button", 700, 20, 740, 60));
        assertEquals("id:manual-toggle", TvFocus.step(boxes, null, TvFocus.DOWN));
    }

    @Test
    public void sidewaysOnASliderDoesNotLeaveIt() {
        assertTrue(TvFocus.nudgeRange("range", TvFocus.LEFT));
        assertTrue(TvFocus.nudgeRange("range", TvFocus.RIGHT));
        assertFalse(TvFocus.nudgeRange("range", TvFocus.UP));
        assertFalse(TvFocus.nudgeRange("button", TvFocus.LEFT));
    }

    @Test
    public void pageJsonSurvivesTheWebViewStringWrap() {
        String inner = "{\"w\":960,\"h\":540,\"boxes\":[{\"id\":\"id:play-pause\",\"kind\":\"button\",\"left\":10,\"top\":20,\"right\":50,\"bottom\":60}]}";
        String wrapped = "\"" + inner.replace("\"", "\\\"") + "\"";
        TvFocus.Page page = TvFocus.parse(wrapped);
        assertEquals(960f, page.width, 0.1f);
        assertEquals(540f, page.height, 0.1f);
        assertEquals(1, page.boxes.size());
        assertEquals("id:play-pause", page.boxes.get(0).id);
        assertEquals(0, TvFocus.parse("null").boxes.size());
    }

    @Test
    public void cssCenterMapsOntoTheView() {
        float[] point = TvFocus.viewPoint(100f, 50f, 960f, 540f, 1920, 1080);
        assertEquals(200f, point[0], 0.1f);
        assertEquals(100f, point[1], 0.1f);
        assertNull(TvFocus.viewPoint(1f, 1f, 0f, 540f, 1920, 1080));
    }

    @Test
    public void quoteKeepsTheScriptStringIntact() {
        assertEquals("id:play-pause", TvFocus.jsQuote("id:play-pause"));
        assertEquals("a\\'b", TvFocus.jsQuote("a'b"));
    }

    private static TvFocus.Box box(String id, String kind, float left, float top, float right, float bottom) {
        return new TvFocus.Box(id, kind, left, top, right, bottom);
    }
}
