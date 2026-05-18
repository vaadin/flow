package com.vaadin.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Base class for all unit tests that run as JavaScript compiled by GWT. The
 * name of any non-abstract subclass must begin with "Gwt" to prevent the class
 * from being run as a regular JVM unit test.
 */
public abstract class ClientEngineTestBase extends GWTTestCase {
    protected static native void createDummyConnectionState()
    /*-{
      if (!$wnd.Vaadin) {
        $wnd.Vaadin = {};
      }
      if (!$wnd.Vaadin.connectionState) {
        $wnd.Vaadin.connectionState = {
          state: 'connected',
          requestCount: 0,
          setState: function(state) {
            this.state = state;
          },
          loadingStarted: function() {
            this.state = 'loading';
            this.requestCount++;
          },
          loadingFinished: function() {
            if (this.requestCount == 0) { return; }
            this.requestCount--;
            if (this.requestCount == 0) { this.state = 'connected'; }
          },
          loadingFailed: function() {
            if (this.requestCount == 0) { return; }
            this.requestCount--;
            if (this.requestCount == 0) { this.state = 'connection-lost'; }
          }
        };
      } else {
        // reset to initial state
        $wnd.Vaadin.connectionState.setState('connected');
        $wnd.Vaadin.connectionState.requestCount = 0;
      }
    }-*/;

    @Override
    protected void gwtSetUp() throws Exception {
        installPolyfills();
        installMigratedBridgeStubs();
        super.gwtSetUp();
    }

    /**
     * Publishes pass-through stubs for TS-migrated classes that GWT code now
     * reaches via {@code @JsType(isNative = true, namespace = "Vaadin.Flow.internal.*")}.
     * Production code gets these from {@code Flow.ts} importing
     * {@code internal/bridge.ts}; Gwt tests don't load Flow.ts, so this hook
     * installs equivalent stubs before each test.
     */
    private static native void installMigratedBridgeStubs()
    /*-{
        var vaadin = $wnd.Vaadin = $wnd.Vaadin || {};
        var flow = vaadin.Flow = vaadin.Flow || {};
        var internal = flow.internal = flow.internal || {};
        var client = internal.client = internal.client || {};
        client.Console = {
            setProductionMode: function() {},
            debug: function(m) { if ($wnd.console) $wnd.console.debug(m); },
            log: function(m) { if ($wnd.console) $wnd.console.log(m); },
            warn: function(m) { if ($wnd.console) $wnd.console.warn(m); },
            error: function(m) { if ($wnd.console) $wnd.console.error(m); },
            reportStacktrace: function(e) { if ($wnd.console) $wnd.console.error(e); }
        };
        client.LitUtils = {
            isLitElement: function(e) {
                return !!e && typeof e.update == "function" && e.updateComplete instanceof Promise
                    && typeof e.shouldUpdate == "function" && typeof e.firstUpdated == "function";
            },
            whenRendered: function(e, runnable) {
                if (e && e.updateComplete) { e.updateComplete.then(function() { runnable(); }); }
            }
        };
        client.ReactUtils = {
            addReadyCallback: function(e, name, runnable) {
                if (e && typeof e.addReadyCallback == "function") {
                    e.addReadyCallback(name, function() { runnable(); });
                }
            }
        };
        client.ConnectionIndicator = {
            setState: function(s) { if ($wnd.Vaadin.connectionState) { $wnd.Vaadin.connectionState.state = s; } },
            getState: function() { return $wnd.Vaadin.connectionState ? $wnd.Vaadin.connectionState.state : null; },
            setProperty: function(p, v) { if ($wnd.Vaadin.connectionIndicator) { $wnd.Vaadin.connectionIndicator[p] = v; } },
            loadingStarted: function() { if ($wnd.Vaadin.connectionState) { $wnd.Vaadin.connectionState.loadingStarted(); } },
            loadingFinished: function() { if ($wnd.Vaadin.connectionState) { $wnd.Vaadin.connectionState.loadingFinished(); } },
            loadingFailed: function() { if ($wnd.Vaadin.connectionState) { $wnd.Vaadin.connectionState.loadingFailed(); } }
        };
        var bootstrap = client.bootstrap = client.bootstrap || {};
        bootstrap.Bootstrapper = {
            getJsoConfiguration: function(appId) {
                return $wnd.Vaadin && $wnd.Vaadin.Flow && $wnd.Vaadin.Flow.getApp ? $wnd.Vaadin.Flow.getApp(appId) : null;
            },
            vaadinBootstrapLoaded: function() {
                return !!($wnd.Vaadin && $wnd.Vaadin.Flow);
            },
            deferStartApplication: function(appId, doStart) {
                $wnd.addEventListener('WebComponentsReady', function() { doStart(appId); });
            },
            startApplicationImmediately: function() {
                return !$wnd.WebComponents || $wnd.WebComponents.ready;
            },
            registerCallback: function(name, startApp) {
                if ($wnd.Vaadin && $wnd.Vaadin.Flow && $wnd.Vaadin.Flow.registerWidgetset) {
                    $wnd.Vaadin.Flow.registerWidgetset(name, startApp);
                }
            }
        };
        var communication = client.communication = client.communication || {};
        communication.MessageHandler = {
            removeStylesheetByIdFromDom: function(id) {
                var sel = 'link[data-id="' + id + '"], style[data-id="' + id + '"]';
                var els = $doc.querySelectorAll(sel);
                for (var i = 0; i < els.length; i++) { els[i].remove(); }
            },
            callAfterServerUpdates: function(node) {
                if (node && node.afterServerUpdate) { node.afterServerUpdate(); }
            },
            calculateBootstrapTime: function() {
                if ($wnd.performance && $wnd.performance.timing) {
                    return (new Date()).getTime() - $wnd.performance.timing.responseStart;
                }
                return -1;
            },
            parseJSONResponse: function(text) { return JSON.parse(text); },
            getFetchStartTime: function() {
                return ($wnd.performance && $wnd.performance.timing && $wnd.performance.timing.fetchStart) || 0;
            }
        };
        communication.MessageSender = {
            sendBeacon: function(url, payload) { $wnd.navigator.sendBeacon(url, payload); }
        };
        communication.XhrConnection = {
            resendRequest: function(xhr) {
                if (xhr.readyState != 1) { return false; }
                try { xhr.send(); return true; } catch (e) { return false; }
            }
        };
        client.SystemErrorHandler = {
            recreateNodes: function(tagName) {
                var els = document.getElementsByTagName(tagName);
                for (var i = 0; i < els.length; i++) {
                    var el = els[i];
                    if (el.$server) { el.$server.disconnected = function() {}; }
                    el.parentNode.replaceChild(el.cloneNode(false), el);
                }
            },
            showPopover: function(el) {
                var fn = el && el.showPopover;
                if (typeof fn === 'function') { fn.call(el); }
            },
            getShadowRootElement: function(host) { return host.shadowRoot; }
        };
        client.ResourceLoader = {
            supportsHtmlWhenReady: function() {
                return !!($wnd.HTMLImports && $wnd.HTMLImports.whenReady);
            },
            addHtmlImportsReadyHandler: function(h) { $wnd.HTMLImports.whenReady(function() { h(); }); },
            addOnloadHandler: function(elem, onLoad, onError) {
                elem.onload = function() { elem.onload = null; elem.onerror = null; elem.onreadystatechange = null; onLoad(); };
                elem.onerror = function() { elem.onload = null; elem.onerror = null; elem.onreadystatechange = null; onError(); };
                elem.onreadystatechange = function() {
                    if ('loaded' === elem.readyState || 'complete' === elem.readyState) { elem.onload(); }
                };
            },
            getStyleSheetLength: function(url) {
                for (var i = 0; i < $doc.styleSheets.length; i++) {
                    if ($doc.styleSheets[i].href === url) {
                        var sheet = $doc.styleSheets[i];
                        try {
                            var rules = sheet.cssRules || sheet.rules;
                            if (rules == null) { return 1; }
                            return rules.length;
                        } catch (e) { return 1; }
                    }
                }
                return -1;
            },
            runPromiseExpression: function(expr, supplier, onSuccess, onError) {
                try {
                    var p = supplier();
                    if (!(p instanceof $wnd.Promise)) {
                        throw new Error('The expression "' + expr + '" result is not a Promise.');
                    }
                    p.then(function() { onSuccess(); }, function(e) { console.error(e); onError(); });
                } catch (e) { console.error(e); onError(); }
            }
        };
        client.BrowserInfo = {
            checkForTouchDevice: function() {
                if (navigator && "maxTouchPoints" in navigator) { return navigator.maxTouchPoints > 0; }
                if (navigator && "msMaxTouchPoints" in navigator) { return navigator.msMaxTouchPoints > 0; }
                var mQ = $wnd.matchMedia && matchMedia("(pointer:coarse)");
                if (mQ && mQ.media === "(pointer:coarse)") { return !!mQ.matches; }
                try { $doc.createEvent("TouchEvent"); return true; } catch(e) { return false; }
            },
            getBrowserString: function() { return $wnd.navigator.userAgent; },
            isIos: function() {
                return /iPad|iPhone|iPod/.test(navigator.platform)
                    || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
            }
        };
        client.WidgetUtil = {
            redirect: function(url) {
                if (url) { $wnd.location.assign(url); } else { $wnd.location.reload(); }
            },
            isAbsoluteUrl: function(url) { return /^(?:[a-zA-Z]+:)?\/\//.test(url); },
            crazyJsCast: function(v) { return v; },
            crazyJsoCast: function(v) { return v; },
            toPrettyJsonJsni: function(v) {
                return $wnd.JSON.stringify(v, function(k, vv) { return k === '$H' ? undefined : vv; }, 4);
            },
            setJsProperty: function(o, n, v) { o[n] = v; },
            getJsProperty: function(o, n) { return o[n]; },
            hasOwnJsProperty: function(o, n) { return Object.prototype.hasOwnProperty.call(o, n); },
            hasJsProperty: function(o, n) { return n in o; },
            isUndefined: function(p) { return p === undefined; },
            deleteJsProperty: function(o, n) { delete o[n]; },
            createJsonObjectWithoutPrototype: function() { return $wnd.Object.create(null); },
            createJsonObject: function() { return {}; },
            isTrueish: function(v) { return !!v; },
            getKeys: function(v) { return Object.keys(v); },
            stringify: function(p) {
                return JSON.stringify(p, function(k, v) {
                    if (v instanceof Node) { throw 'Message JsonObject contained a dom node reference'; }
                    return v;
                });
            },
            equalsInJS: function(a, b) { return a == b; }
        };
        client.PolymerUtils = {
            setListValueByIndex: function(n, p, i, v) { if (n.set) { n.set(p + '.' + i, v); } },
            splice: function(n, p, s, d, items) {
                if (n.splice) { n.splice.apply(n, [p, s, d].concat(items || [])); }
            },
            storeNodeId: function(n, id, p) {
                if (n.get) {
                    var prop = n.get(p);
                    if (typeof prop === 'object' && prop && prop.nodeId === undefined) { prop.nodeId = id; }
                }
            },
            isPolymerElement: function(n) {
                var p = $wnd.Polymer;
                var isP2 = typeof p === 'function' && p.Element && n instanceof p.Element;
                var isP3 = n.constructor && n.constructor.polymerElementVersion !== undefined;
                return isP2 || isP3;
            },
            mayBePolymerElement: function(n) {
                return !!$wnd.customElements && n.localName && n.localName.indexOf('-') > -1;
            },
            searchForElementInShadowRoot: function(sr, q) { return sr.querySelector(q); },
            getElementInShadowRootById: function(sr, id) { return sr.getElementById(id); },
            getDomElementById: function(sp, id) { return sp.$ ? sp.$[id] : undefined; },
            isReady: function(sp) { return sp.$ !== undefined; },
            getDomRoot: function(t) { return t.root || null; },
            invokeWhenDefined: function(tag, runnable) {
                $wnd.customElements.whenDefined(tag).then(function() { runnable(); });
            },
            setProperty: function(e, p, v) { if (e.set) { e.set(p, v); } },
            isInShadowRoot: function(e) {
                var c = e.parentNode;
                while (c) {
                    if (Object.prototype.toString.call(c) === '[object ShadowRoot]') { return true; }
                    c = c.parentNode;
                }
                return false;
            }
        };
        client.ExecuteJavaScriptElementUtils = {
            isPropertyDefined: function(node, property) {
                var props = node.constructor && node.constructor.properties;
                return !!(props && props[property]) && typeof props[property].value !== 'undefined';
            }
        };
        var flowUtil = (client.flow = client.flow || {}).util = ((client.flow || {}).util) || {};
        flowUtil.ClientJsonCodec = {
            createReturnChannelCallback: function(send) {
                return function() {
                    var args = Array.prototype.slice.call(arguments);
                    send(args);
                };
            }
        };
        client.ElementUtil = {
            getElementById: function(context, id) {
                if (document.body.$ && document.body.$.hasOwnProperty && document.body.$.hasOwnProperty(id)) {
                    return document.body.$[id];
                } else if (context.shadowRoot) {
                    return context.shadowRoot.getElementById(id);
                } else if (context.getElementById) {
                    return context.getElementById(id);
                } else if (id && id.match("^[a-zA-Z0-9-_]*$")) {
                    return context.querySelector("#" + id);
                } else {
                    return Array.from(context.querySelectorAll('[id]')).find(function(e) { return e.id == id; });
                }
            },
            getElementByName: function(context, name) {
                return Array.from(context.querySelectorAll('[name]')).find(function(e) { return e.getAttribute('name') == name; });
            }
        };
    }-*/;

    @Override
    public String getModuleName() {
        return "com.vaadin.ClientEngineXSI";
    }

    private static native void installPolyfills()
    /*-{
        // Remove broken HtmlUnit versions (polyfill checks window, but installs to $wnd)
        delete window.Set;
        delete window.Map;
        delete window.WeakSet;
        delete window.WeakMap;

        // es6-collections 0.5.5 (with 'window' replaced to '$wnd' at the end)

        (function(e){function f(a,c){function b(a){if(!this||this.constructor!==b)return new b(a);this._keys=[];this._values=[];this._itp=[];this.objectOnly=c;a&&v.call(this,a)}c||w(a,"size",{get:x});a.constructor=b;b.prototype=a;return b}function v(a){this.add?a.forEach(this.add,this):a.forEach(function(a){this.set(a[0],a[1])},this)}function d(a){this.has(a)&&(this._keys.splice(b,1),this._values.splice(b,1),this._itp.forEach(function(a){b<a[0]&&a[0]--}));return-1<b}function m(a){return this.has(a)?this._values[b]:
        void 0}function n(a,c){if(this.objectOnly&&c!==Object(c))throw new TypeError("Invalid value used as weak collection key");if(c!=c||0===c)for(b=a.length;b--&&!y(a[b],c););else b=a.indexOf(c);return-1<b}function p(a){return n.call(this,this._values,a)}function q(a){return n.call(this,this._keys,a)}function r(a,c){this.has(a)?this._values[b]=c:this._values[this._keys.push(a)-1]=c;return this}function t(a){this.has(a)||this._values.push(a);return this}function h(){(this._keys||0).length=this._values.length=
        0}function z(){return k(this._itp,this._keys)}function l(){return k(this._itp,this._values)}function A(){return k(this._itp,this._keys,this._values)}function B(){return k(this._itp,this._values,this._values)}function k(a,c,b){var g=[0],e=!1;a.push(g);return{next:function(){var f,d=g[0];!e&&d<c.length?(f=b?[c[d],b[d]]:c[d],g[0]++):(e=!0,a.splice(a.indexOf(g),1));return{done:e,value:f}}}}function x(){return this._values.length}function u(a,c){for(var b=this.entries();;){var d=b.next();if(d.done)break;
        a.call(c,d.value[1],d.value[0],this)}}var b,w=Object.defineProperty,y=function(a,b){return isNaN(a)?isNaN(b):a===b};"undefined"==typeof WeakMap&&(e.WeakMap=f({"delete":d,clear:h,get:m,has:q,set:r},!0));"undefined"!=typeof Map&&"function"===typeof(new Map).values&&(new Map).values().next||(e.Map=f({"delete":d,has:q,get:m,set:r,keys:z,values:l,entries:A,forEach:u,clear:h}));"undefined"!=typeof Set&&"function"===typeof(new Set).values&&(new Set).values().next||(e.Set=f({has:p,add:t,"delete":d,clear:h,
        keys:l,values:l,entries:B,forEach:u}));"undefined"==typeof WeakSet&&(e.WeakSet=f({"delete":d,add:t,clear:h,has:p},!0))})("undefined"!=typeof exports&&"undefined"!=typeof global?global:$wnd);

        // Remove broken HtmlUnit implementation
        delete window.Promise;

        // promise-polyfill 8.1.3

        !function(e,n){"object"==typeof exports&&"undefined"!=typeof module?n():"function"==typeof define&&define.amd?define(n):n()}(0,function(){"use strict";function e(e){var n=this.constructor;return this.then(function(t){return n.resolve(e()).then(function(){return t})},function(t){return n.resolve(e()).then(function(){return n.reject(t)})})}function n(e){return!(!e||"undefined"==typeof e.length)}function t(){}function o(e){if(!(this instanceof o))throw new TypeError("Promises must be constructed via new");if("function"!=typeof e)throw new TypeError("not a function");this._state=0,this._handled=!1,this._value=undefined,this._deferreds=[],c(e,this)}function r(e,n){for(;3===e._state;)e=e._value;0!==e._state?(e._handled=!0,o._immediateFn(function(){var t=1===e._state?n.onFulfilled:n.onRejected;if(null!==t){var o;try{o=t(e._value)}catch(r){return void f(n.promise,r)}i(n.promise,o)}else(1===e._state?i:f)(n.promise,e._value)})):e._deferreds.push(n)}function i(e,n){try{if(n===e)throw new TypeError("A promise cannot be resolved with itself.");if(n&&("object"==typeof n||"function"==typeof n)){var t=n.then;if(n instanceof o)return e._state=3,e._value=n,void u(e);if("function"==typeof t)return void c(function(e,n){return function(){e.apply(n,arguments)}}(t,n),e)}e._state=1,e._value=n,u(e)}catch(r){f(e,r)}}function f(e,n){e._state=2,e._value=n,u(e)}function u(e){2===e._state&&0===e._deferreds.length&&o._immediateFn(function(){e._handled||o._unhandledRejectionFn(e._value)});for(var n=0,t=e._deferreds.length;t>n;n++)r(e,e._deferreds[n]);e._deferreds=null}function c(e,n){var t=!1;try{e(function(e){t||(t=!0,i(n,e))},function(e){t||(t=!0,f(n,e))})}catch(o){if(t)return;t=!0,f(n,o)}}var a=setTimeout;o.prototype["catch"]=function(e){return this.then(null,e)},o.prototype.then=function(e,n){var o=new this.constructor(t);return r(this,new function(e,n,t){this.onFulfilled="function"==typeof e?e:null,this.onRejected="function"==typeof n?n:null,this.promise=t}(e,n,o)),o},o.prototype["finally"]=e,o.all=function(e){return new o(function(t,o){function r(e,n){try{if(n&&("object"==typeof n||"function"==typeof n)){var u=n.then;if("function"==typeof u)return void u.call(n,function(n){r(e,n)},o)}i[e]=n,0==--f&&t(i)}catch(c){o(c)}}if(!n(e))return o(new TypeError("Promise.all accepts an array"));var i=Array.prototype.slice.call(e);if(0===i.length)return t([]);for(var f=i.length,u=0;i.length>u;u++)r(u,i[u])})},o.resolve=function(e){return e&&"object"==typeof e&&e.constructor===o?e:new o(function(n){n(e)})},o.reject=function(e){return new o(function(n,t){t(e)})},o.race=function(e){return new o(function(t,r){if(!n(e))return r(new TypeError("Promise.race accepts an array"));for(var i=0,f=e.length;f>i;i++)o.resolve(e[i]).then(t,r)})},o._immediateFn="function"==typeof setImmediate&&function(e){setImmediate(e)}||function(e){a(e,0)},o._unhandledRejectionFn=function(e){void 0!==console&&console&&console.warn("Possible Unhandled Promise Rejection:",e)};var l=function(){if("undefined"!=typeof self)return self;if("undefined"!=typeof window)return window;if("undefined"!=typeof global)return global;throw Error("unable to locate global object")}();"Promise"in l?l.Promise.prototype["finally"]||(l.Promise.prototype["finally"]=e):l.Promise=o});

        // Run promise callbacks immediately in tests
        // Keep also original _immediateFn so it can be restored if needed
        window.Promise._originalImmediateFn = window.Promise._immediateFn;
        window.Promise._immediateFn = function(callback) { callback(); };
    }-*/;

}
