package cc.cd.relaxingocean;

import android.view.KeyEvent;

/** Remote driving after the manual-control button is pressed. */
final class Steer {
    static final String BUTTON = "id:manual-toggle";
    static final int IGNORE = 0;
    static final int CONSUME = 1;
    static final int EXIT = 2;

    boolean driving;
    boolean swallowBackUp;

    static boolean button(String id) {
        return BUTTON.equals(id);
    }

    /**
     * Page keys the live site already understands. A D-pad cannot hold two keys.
     * On this whale a rising heading is a right turn, and the page's ArrowLeft raises it.
     * The remote's left key therefore sends ArrowRight.
     */
    static String arrow(int direction) {
        if (direction == TvFocus.LEFT) return "ArrowRight";
        if (direction == TvFocus.RIGHT) return "ArrowLeft";
        if (direction == TvFocus.UP) return "ArrowUp";
        if (direction == TvFocus.DOWN) return "ArrowDown";
        return null;
    }

    static boolean pageOn(String value) {
        return "1".equals(value) || "\"1\"".equals(value) || "true".equals(value);
    }

    /**
     * Back leaves manual driving and stays in the app.
     * The matching key-up is swallowed so the activity does not finish.
     */
    int back(int action, int repeat) {
        if (!driving && !swallowBackUp) return IGNORE;
        if (action == KeyEvent.ACTION_DOWN && repeat == 0 && driving) {
            driving = false;
            swallowBackUp = true;
            return EXIT;
        }
        if (action == KeyEvent.ACTION_UP && swallowBackUp) {
            swallowBackUp = false;
            return CONSUME;
        }
        return driving || swallowBackUp ? CONSUME : IGNORE;
    }
}
