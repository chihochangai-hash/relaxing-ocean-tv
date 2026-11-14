(function () {
  if (window.__oceanTv) return;
  var style = document.createElement("style");
  style.textContent = "[data-ocean-tv-on='1']{outline:3px solid #7fffd4 !important;outline-offset:3px !important;}";
  (document.head || document.documentElement).appendChild(style);
  var seq = 0;
  var held = "";
  var going = false;

  function pulse(code, down) {
    window.dispatchEvent(new KeyboardEvent(down ? "keydown" : "keyup", {
      code: code,
      key: code,
      bubbles: true,
      cancelable: true
    }));
  }

  function visible(el) {
    if (!el || el.disabled) return false;
    var css = window.getComputedStyle(el);
    if (!css || css.display === "none" || css.visibility === "hidden" || Number(css.opacity) === 0) return false;
    var rect = el.getBoundingClientRect();
    if (rect.width < 8 || rect.height < 8) return false;
    if (rect.bottom <= 0 || rect.right <= 0 || rect.top >= window.innerHeight || rect.left >= window.innerWidth) return false;
    var cx = Math.min(Math.max(rect.left + rect.width / 2, 0), window.innerWidth - 1);
    var cy = Math.min(Math.max(rect.top + rect.height / 2, 0), window.innerHeight - 1);
    var top = document.elementFromPoint(cx, cy);
    if (!top) return false;
    return top === el || el.contains(top) || top.contains(el);
  }

  function kindOf(el) {
    if (el.id === "start-overlay") return "overlay";
    if (el.tagName === "INPUT" && String(el.type || "").toLowerCase() === "range") return "range";
    return "button";
  }

  function candidates() {
    var nodes = document.querySelectorAll("button, a[href], input, select, textarea, label, [role='button'], #start-overlay");
    var out = [];
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      var type = el.tagName === "INPUT" ? String(el.type || "").toLowerCase() : "";
      if (type === "hidden") continue;
      if ((type === "checkbox" || type === "radio") && el.closest && el.closest("label")) continue;
      if (el.id !== "start-overlay" && el.closest && el.closest("#start-overlay")) continue;
      if (!visible(el)) continue;
      if (!el.getAttribute("data-ocean-tv")) {
        seq += 1;
        el.setAttribute("data-ocean-tv", el.id ? "id:" + el.id : "n:" + seq);
      }
      var rect = el.getBoundingClientRect();
      out.push({
        id: el.getAttribute("data-ocean-tv"),
        kind: kindOf(el),
        left: rect.left,
        top: rect.top,
        right: rect.right,
        bottom: rect.bottom
      });
    }
    return { w: window.innerWidth, h: window.innerHeight, boxes: out };
  }

  function find(id) {
    var all = document.querySelectorAll("[data-ocean-tv]");
    for (var i = 0; i < all.length; i++) {
      if (all[i].getAttribute("data-ocean-tv") === id) return all[i];
    }
    return null;
  }

  window.__oceanTv = {
    collect: candidates,
    focus: function (id) {
      var prev = document.querySelector("[data-ocean-tv-on='1']");
      if (prev) prev.removeAttribute("data-ocean-tv-on");
      var el = find(id);
      if (!el) return false;
      el.setAttribute("data-ocean-tv-on", "1");
      if (el.id !== "start-overlay") {
        try { el.scrollIntoView({ block: "nearest", inline: "nearest" }); } catch (ignore) {}
      }
      return true;
    },
    manualOn: function () {
      var button = document.getElementById("manual-toggle");
      return !!(button && button.classList.contains("is-active"));
    },
    drive: function (code, down) {
      if (code === "ArrowUp") {
        if (!down || going) return true;
        going = true;
        pulse("ArrowUp", true);
        return true;
      }
      if (code === "ArrowDown") {
        if (down) this.stopDrive();
        return true;
      }
      if (code !== "ArrowLeft" && code !== "ArrowRight") return false;
      if (down) {
        if (held === code) return true;
        if (held) pulse(held, false);
        held = code;
        pulse(code, true);
        return true;
      }
      if (held === code) {
        pulse(code, false);
        held = "";
      }
      return true;
    },
    stopDrive: function () {
      going = false;
      if (held) pulse(held, false);
      held = "";
      pulse("ArrowUp", false);
      pulse("ArrowLeft", false);
      pulse("ArrowRight", false);
      pulse("ArrowDown", false);
    },
    releaseSteer: function () {
      this.stopDrive();
    },
    leaveManual: function () {
      this.stopDrive();
      var button = document.getElementById("manual-toggle");
      if (button && button.classList.contains("is-active")) button.click();
      this.focus("id:manual-toggle");
      return true;
    },
    hideRing: function () {
      var prev = document.querySelector("[data-ocean-tv-on='1']");
      if (prev) prev.removeAttribute("data-ocean-tv-on");
    },
    nudge: function (id, sign) {
      var el = find(id);
      if (!el || String(el.type || "").toLowerCase() !== "range") return false;
      var min = Number(el.min);
      var max = Number(el.max);
      if (!isFinite(min)) min = 0;
      if (!isFinite(max)) max = 100;
      var current = Number(el.value);
      if (!isFinite(current)) current = min;
      var delta = Math.max((max - min) / 20, 1) * sign;
      var next = Math.min(max, Math.max(min, current + delta));
      el.value = String(next);
      el.dispatchEvent(new Event("input", { bubbles: true }));
      el.dispatchEvent(new Event("change", { bubbles: true }));
      return true;
    }
  };
})();
