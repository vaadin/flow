// Copyright 2000-2026 Vaadin Ltd.
// Licensed under the Apache License, Version 2.0
//
// In-browser collector for vaadin-micrometer. Injected per UI by
// MetricsCollectorElement via Page.executeJs. The IIFE is idempotent so the
// re-attach path does not double-install hooks.
(function () {
  if (window.__vaadinMicrometerInstalled) {
    return;
  }
  window.__vaadinMicrometerInstalled = true;

  var COLLECTOR_TAG = 'vaadin-metrics-collector';
  var BUFFER_MAX = 200;
  var FLUSH_INTERVAL_MS = 5000;
  var UIDL_MARKER = 'v-r=uidl';

  var buffer = [];

  function pushSample(name, tags, valueMs) {
    if (typeof valueMs !== 'number' || isNaN(valueMs) || valueMs < 0) {
      valueMs = 0;
    }
    if (buffer.length >= BUFFER_MAX) {
      buffer.shift();
    }
    buffer.push({
      name: name,
      tags: tags || {},
      valueMs: valueMs,
      ts: Date.now()
    });
  }

  function currentRoute() {
    return window.location.pathname || '/';
  }

  function flush() {
    if (buffer.length === 0) {
      return;
    }
    var el = document.querySelector(COLLECTOR_TAG);
    if (!el || !el.$server || typeof el.$server.recordSamples !== 'function') {
      return;
    }
    var batch = buffer.splice(0, buffer.length);
    try {
      el.$server.recordSamples(batch);
    } catch (e) {
      // server unreachable: drop this batch, do not requeue to avoid
      // unbounded buffer growth on persistent failure.
    }
  }

  // Bootstrap timing.
  try {
    var navEntries = performance.getEntriesByType('navigation');
    if (navEntries && navEntries.length > 0) {
      var nav = navEntries[0];
      var dur =
        nav.loadEventEnd > 0 ? nav.loadEventEnd - nav.fetchStart : nav.domContentLoadedEventEnd - nav.fetchStart;
      if (dur > 0) {
        pushSample('vaadin.client.bootstrap.duration', { route: currentRoute() }, dur);
      }
    }
  } catch (e) {
    /* ignore */
  }

  // Web Vitals: LCP.
  try {
    var lcpObserver = new PerformanceObserver(function (list) {
      var entries = list.getEntries();
      var last = entries[entries.length - 1];
      if (last) {
        var value = last.renderTime || last.loadTime || last.startTime;
        pushSample('vaadin.client.web_vitals.lcp', { route: currentRoute() }, value);
      }
    });
    lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true });
  } catch (e) {
    /* unsupported, skip */
  }

  // Web Vitals: FCP (from paint timing).
  try {
    var paintObserver = new PerformanceObserver(function (list) {
      list.getEntries().forEach(function (entry) {
        if (entry.name === 'first-contentful-paint') {
          pushSample('vaadin.client.web_vitals.fcp', { route: currentRoute() }, entry.startTime);
        }
      });
    });
    paintObserver.observe({ type: 'paint', buffered: true });
  } catch (e) {
    /* unsupported, skip */
  }

  // Errors.
  window.addEventListener('error', function () {
    pushSample('vaadin.client.errors', { kind: 'uncaught' }, 0);
  });
  window.addEventListener('unhandledrejection', function () {
    pushSample('vaadin.client.errors', { kind: 'promise' }, 0);
  });

  // RPC timing: wrap XHR send only for UIDL requests.
  try {
    var origOpen = XMLHttpRequest.prototype.open;
    var origSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (method, url) {
      this.__vaadinMetricsUrl = url || '';
      return origOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
      var url = this.__vaadinMetricsUrl;
      if (url && url.indexOf(UIDL_MARKER) >= 0) {
        var start = performance.now();
        this.addEventListener('loadend', function () {
          var outcome = this.status >= 200 && this.status < 400 ? 'success' : 'error';
          pushSample('vaadin.client.rpc.duration', { outcome: outcome }, performance.now() - start);
        });
      }
      return origSend.apply(this, arguments);
    };
  } catch (e) {
    /* ignore */
  }

  // Navigation timing: observe history changes.
  var navStart = null;
  function startNav() {
    navStart = performance.now();
  }
  function endNav(trigger) {
    if (navStart === null) {
      return;
    }
    var duration = performance.now() - navStart;
    navStart = null;
    pushSample('vaadin.client.navigation.duration', { route: currentRoute(), trigger: trigger }, duration);
  }
  try {
    window.addEventListener('popstate', function () {
      startNav();
      // navigation completes by the next animation frame typically; the
      // route path is already updated by the time popstate fires.
      requestAnimationFrame(function () {
        endNav('back');
      });
    });
    // Wrap pushState / replaceState to detect programmatic navigation.
    ['pushState', 'replaceState'].forEach(function (op) {
      var orig = history[op];
      history[op] = function () {
        startNav();
        var result = orig.apply(this, arguments);
        requestAnimationFrame(function () {
          endNav('programmatic');
        });
        return result;
      };
    });
  } catch (e) {
    /* ignore */
  }

  // Periodic flush.
  setInterval(flush, FLUSH_INTERVAL_MS);

  // Visibility flush: best-effort, no Beacon fallback in v1.
  document.addEventListener('visibilitychange', function () {
    if (document.visibilityState === 'hidden') {
      flush();
    }
  });

  // Expose for tests / dashboards (debug only).
  window.__vaadinMicrometer = {
    flush: flush,
    bufferSize: function () {
      return buffer.length;
    }
  };
})();
