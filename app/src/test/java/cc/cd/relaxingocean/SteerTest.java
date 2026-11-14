package cc.cd.relaxingocean;

import android.view.KeyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SteerTest {
    @Test
    public void manualButtonAndArrows() {
        assertTrue(Steer.button("id:manual-toggle"));
        assertFalse(Steer.button("id:play-pause"));
        assertEquals("ArrowRight", Steer.arrow(TvFocus.LEFT));
        assertEquals("ArrowLeft", Steer.arrow(TvFocus.RIGHT));
        assertEquals("ArrowUp", Steer.arrow(TvFocus.UP));
        assertEquals("ArrowDown", Steer.arrow(TvFocus.DOWN));
        assertTrue(Steer.pageOn("1"));
        assertTrue(Steer.pageOn("\"1\""));
        assertTrue(Steer.pageOn("true"));
        assertFalse(Steer.pageOn("0"));
        assertFalse(Steer.pageOn(null));
    }

    @Test
    public void backLeavesDrivingAndSwallowsTheKeyUp() {
        Steer steer = new Steer();
        assertEquals(Steer.IGNORE, steer.back(KeyEvent.ACTION_DOWN, 0));
        steer.driving = true;
        assertEquals(Steer.EXIT, steer.back(KeyEvent.ACTION_DOWN, 0));
        assertFalse(steer.driving);
        assertEquals(Steer.CONSUME, steer.back(KeyEvent.ACTION_DOWN, 1));
        assertEquals(Steer.CONSUME, steer.back(KeyEvent.ACTION_UP, 0));
        assertEquals(Steer.IGNORE, steer.back(KeyEvent.ACTION_DOWN, 0));
    }
}
