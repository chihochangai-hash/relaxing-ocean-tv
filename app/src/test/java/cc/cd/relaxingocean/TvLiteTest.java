package cc.cd.relaxingocean;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TvLiteTest {
    @Test
    public void scriptGoesFirstInTheHead() {
        String page = TvLite.html("<html><head><meta charset=\"UTF-8\"></head><body></body></html>");
        int script = page.indexOf(TvLite.MARK);
        int meta = page.indexOf("<meta");
        assertTrue(script > 0);
        assertTrue(script < meta);
        assertTrue(page.contains("antialias=false"));
        assertTrue(page.contains("return 1"));
    }

    @Test
    public void aSecondPassDoesNotInsertTwice() {
        String once = TvLite.html("<head></head>");
        assertEquals(once, TvLite.html(once));
        assertEquals(1, count(once, TvLite.MARK));
    }

    @Test
    public void aPageWithoutAHeadStillGetsTheScript() {
        String page = TvLite.html("<html><body></body></html>");
        assertTrue(page.startsWith("<script>"));
    }

    @Test
    public void rendererDropsAntialiasAndPixelRatio() {
        String source = "const PIXEL_RATIO_CAP = (typeof matchMedia === \"function\" "
                + "&& matchMedia(\"(pointer: coarse)\").matches) || Math.min(screen.width, screen.height) < 820 ? 1.5 : 1.75;\n"
                + "this.renderer = new THREE.WebGLRenderer({ canvas: this.canvas, antialias: true });";
        String out = new String(TvLite.script(source.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        assertTrue(out.contains("const PIXEL_RATIO_CAP = 1;"));
        assertTrue(out.contains("antialias: false"));
        assertFalse(out.contains("antialias: true"));
        assertFalse(out.contains("1.5"));
        assertFalse(out.contains("1.75"));
    }

    @Test
    public void manualCoastBecomesAStop() {
        String source = "const cruiseSpeed = boost ? 185 : 112;\n"
                + "const targetSpeed = forward > 0 ? cruiseSpeed : 14;\n";
        String out = new String(TvLite.motion(source.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        assertTrue(out.contains("const targetSpeed = forward > 0 ? cruiseSpeed : 0;"));
        assertFalse(out.contains(": 14;"));
    }

    @Test
    public void anAlreadyCappedScriptStaysOneLine() {
        String source = "const PIXEL_RATIO_CAP = 1;\nnew THREE.WebGLRenderer({ antialias: false });";
        String out = new String(TvLite.script(source.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        assertEquals(source, out);
    }

    private static int count(String page, String mark) {
        int n = 0;
        int at = 0;
        while ((at = page.indexOf(mark, at)) >= 0) {
            n++;
            at += mark.length();
        }
        return n;
    }
}
