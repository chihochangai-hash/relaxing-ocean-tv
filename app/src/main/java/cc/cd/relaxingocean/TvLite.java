package cc.cd.relaxingocean;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** TV-only quality cap applied to the live page. The website files stay unchanged. */
final class TvLite {
    static final String MARK = "ocean-tv-lite";
    private static final Pattern HEAD = Pattern.compile("(?i)<head\\b[^>]*>");
    private static final Pattern CAP = Pattern.compile("const PIXEL_RATIO_CAP = [^;]*;");
    private static final String CAP_ONE = "const PIXEL_RATIO_CAP = 1;";
    private static final String SCRIPT = "<script>(function(){window.__oceanTvLite=1;"
            + "try{Object.defineProperty(window,\"devicePixelRatio\",{configurable:true,get:function(){return 1;}});}catch(e){}"
            + "var proto=HTMLCanvasElement.prototype,orig=proto.getContext;if(!orig)return;"
            + "proto.getContext=function(type,attrs){"
            + "if(type===\"webgl\"||type===\"webgl2\"||type===\"experimental-webgl\"||type===\"experimental-webgl2\"){"
            + "attrs=Object.assign({},attrs||{});attrs.antialias=false;return orig.call(this,type,attrs);}"
            + "return orig.apply(this,arguments);};"
            + "console.log(\"ocean-tv-lite dpr=\"+window.devicePixelRatio);})();</script>";

    private TvLite() {}

    /** Puts the cap ahead of every other script. A page that already has it is left as it is. */
    static String html(String page) {
        if (page == null || page.contains(MARK)) return page;
        Matcher head = HEAD.matcher(page);
        if (!head.find()) return SCRIPT + page;
        int at = head.end();
        return page.substring(0, at) + SCRIPT + page.substring(at);
    }

    /** Letting go of forward coasts at 14. On the remote, Down means a real stop. */
    static byte[] motion(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        String coast = "const targetSpeed = forward > 0 ? cruiseSpeed : 14;";
        String stop = "const targetSpeed = forward > 0 ? cruiseSpeed : 0;";
        if (text.contains(coast)) text = text.replace(coast, stop);
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /** Locks the scene script to one pixel per screen pixel and turns multisampling off. */
    static byte[] script(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        if (!text.contains(CAP_ONE)) text = CAP.matcher(text).replaceFirst(CAP_ONE);
        text = text.replace("antialias: true", "antialias: false");
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
