var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __knownSymbol = (name, symbol) => (symbol = Symbol[name]) ? symbol : /* @__PURE__ */ Symbol.for("Symbol." + name);
var __typeError = (msg) => {
  throw TypeError(msg);
};
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __name = (target, value) => __defProp(target, "name", { value, configurable: true });
var __decoratorStart = (base) => [, , , __create(base?.[__knownSymbol("metadata")] ?? null)];
var __decoratorStrings = ["class", "method", "getter", "setter", "accessor", "field", "value", "get", "set"];
var __expectFn = (fn) => fn !== void 0 && typeof fn !== "function" ? __typeError("Function expected") : fn;
var __decoratorContext = (kind, name, done, metadata, fns) => ({ kind: __decoratorStrings[kind], name, metadata, addInitializer: (fn) => done._ ? __typeError("Already initialized") : fns.push(__expectFn(fn || null)) });
var __decoratorMetadata = (array, target) => __defNormalProp(target, __knownSymbol("metadata"), array[3]);
var __runInitializers = (array, flags, self, value) => {
  for (var i = 0, fns = array[flags >> 1], n = fns && fns.length; i < n; i++) flags & 1 ? fns[i].call(self) : value = fns[i].call(self, value);
  return value;
};
var __decorateElement = (array, flags, name, decorators, target, extra) => {
  var fn, it, done, ctx, access, k = flags & 7, s = !!(flags & 8), p = !!(flags & 16);
  var j = k > 3 ? array.length + 1 : k ? s ? 1 : 2 : 0, key = __decoratorStrings[k + 5];
  var initializers = k > 3 && (array[j - 1] = []), extraInitializers = array[j] || (array[j] = []);
  var desc = k && (!p && !s && (target = target.prototype), k < 5 && (k > 3 || !p) && __getOwnPropDesc(k < 4 ? target : { get [name]() {
    return __privateGet(this, extra);
  }, set [name](x) {
    return __privateSet(this, extra, x);
  } }, name));
  k ? p && k < 4 && __name(extra, (k > 2 ? "set " : k > 1 ? "get " : "") + name) : __name(target, name);
  for (var i = decorators.length - 1; i >= 0; i--) {
    ctx = __decoratorContext(k, name, done = {}, array[3], extraInitializers);
    if (k) {
      ctx.static = s, ctx.private = p, access = ctx.access = { has: p ? (x) => __privateIn(target, x) : (x) => name in x };
      if (k ^ 3) access.get = p ? (x) => (k ^ 1 ? __privateGet : __privateMethod)(x, target, k ^ 4 ? extra : desc.get) : (x) => x[name];
      if (k > 2) access.set = p ? (x, y) => __privateSet(x, target, y, k ^ 4 ? extra : desc.set) : (x, y) => x[name] = y;
    }
    it = (0, decorators[i])(k ? k < 4 ? p ? extra : desc[key] : k > 4 ? void 0 : { get: desc.get, set: desc.set } : target, ctx), done._ = 1;
    if (k ^ 4 || it === void 0) __expectFn(it) && (k > 4 ? initializers.unshift(it) : k ? p ? extra = it : desc[key] = it : target = it);
    else if (typeof it !== "object" || it === null) __typeError("Object expected");
    else __expectFn(fn = it.get) && (desc.get = fn), __expectFn(fn = it.set) && (desc.set = fn), __expectFn(fn = it.init) && initializers.unshift(fn);
  }
  return k || __decoratorMetadata(array, target), desc && __defProp(target, name, desc), p ? k ^ 4 ? extra : desc : target;
};
var __publicField = (obj, key, value) => __defNormalProp(obj, typeof key !== "symbol" ? key + "" : key, value);
var __accessCheck = (obj, member, msg) => member.has(obj) || __typeError("Cannot " + msg);
var __privateIn = (member, obj) => Object(obj) !== obj ? __typeError('Cannot use the "in" operator on this value') : member.has(obj);
var __privateGet = (obj, member, getter) => (__accessCheck(obj, member, "read from private field"), getter ? getter.call(obj) : member.get(obj));
var __privateAdd = (obj, member, value) => member.has(obj) ? __typeError("Cannot add the same private member more than once") : member instanceof WeakSet ? member.add(obj) : member.set(obj, value);
var __privateSet = (obj, member, value, setter) => (__accessCheck(obj, member, "write to private field"), setter ? setter.call(obj, value) : member.set(obj, value), value);
var __privateMethod = (obj, member, method) => (__accessCheck(obj, member, "access private method"), method);
var _applyDefaultTheme_dec, _loadingBarState_dec, _loading_dec, _expanded_dec, _reconnecting_dec, _offline_dec, _reconnectingText_dec, _offlineText_dec, _onlineText_dec, _expandedDuration_dec, _thirdDelay_dec, _secondDelay_dec, _firstDelay_dec, _a, _init, _firstDelay, _secondDelay, _thirdDelay, _expandedDuration, _onlineText, _offlineText, _reconnectingText, _offline, _reconnecting, _expanded, _loading, _loadingBarState, _b, loadingBarState_get, loadingBarState_set, _ConnectionIndicator_instances, __isPopover, isPopover_get, isPopover_set, _applyDefaultThemeState, _firstTimeout, _secondTimeout, _thirdTimeout, _expandedTimeout, _connectionStateStore, _lastMessageState, initPopover_fn, updateConnectionState_fn, updateLoading_fn, updatePopoverState_fn, renderMessage_fn, updateStyle_fn, updateTheme_fn, getFunctionalStyle_fn, getDefaultStyle_fn, getLoadingBarStyle_fn, timeoutFor_fn;
import { html, css, LitElement } from "lit";
import { property, state } from "lit/decorators.js";
import { classMap } from "lit/directives/class-map.js";
import { ConnectionState } from "./ConnectionState.js";
const FUNCTIONAL_STYLE_ID = "css-loading-indicator-functional";
const DEFAULT_STYLE_ID = "css-loading-indicator";
var LoadingBarState = /* @__PURE__ */ ((LoadingBarState2) => {
  LoadingBarState2["IDLE"] = "";
  LoadingBarState2["FIRST"] = "first";
  LoadingBarState2["SECOND"] = "second";
  LoadingBarState2["THIRD"] = "third";
  return LoadingBarState2;
})(LoadingBarState || {});
const _ConnectionIndicator = class _ConnectionIndicator extends (_a = LitElement, _firstDelay_dec = [property({ type: Number })], _secondDelay_dec = [property({ type: Number })], _thirdDelay_dec = [property({ type: Number })], _expandedDuration_dec = [property({ type: Number })], _onlineText_dec = [property({ type: String })], _offlineText_dec = [property({ type: String })], _reconnectingText_dec = [property({ type: String })], _offline_dec = [property({ type: Boolean, reflect: true })], _reconnecting_dec = [property({ type: Boolean, reflect: true })], _expanded_dec = [property({ type: Boolean, reflect: true })], _loading_dec = [property({ type: Boolean, reflect: true })], _loadingBarState_dec = [state()], _applyDefaultTheme_dec = [property({ type: Boolean, reflect: true })], _a) {
  constructor() {
    super();
    __runInitializers(_init, 5, this);
    __privateAdd(this, _ConnectionIndicator_instances);
    __privateAdd(this, _firstDelay, __runInitializers(_init, 8, this, 450)), __runInitializers(_init, 11, this);
    __privateAdd(this, _secondDelay, __runInitializers(_init, 12, this, 1500)), __runInitializers(_init, 15, this);
    __privateAdd(this, _thirdDelay, __runInitializers(_init, 16, this, 5e3)), __runInitializers(_init, 19, this);
    __privateAdd(this, _expandedDuration, __runInitializers(_init, 20, this, 2e3)), __runInitializers(_init, 23, this);
    __privateAdd(this, _onlineText, __runInitializers(_init, 24, this, "Online")), __runInitializers(_init, 27, this);
    __privateAdd(this, _offlineText, __runInitializers(_init, 28, this, "Connection lost")), __runInitializers(_init, 31, this);
    __privateAdd(this, _reconnectingText, __runInitializers(_init, 32, this, "Connection lost, trying to reconnect...")), __runInitializers(_init, 35, this);
    __privateAdd(this, _offline, __runInitializers(_init, 36, this, false)), __runInitializers(_init, 39, this);
    __privateAdd(this, _reconnecting, __runInitializers(_init, 40, this, false)), __runInitializers(_init, 43, this);
    __privateAdd(this, _expanded, __runInitializers(_init, 44, this, false)), __runInitializers(_init, 47, this);
    __privateAdd(this, _loading, __runInitializers(_init, 48, this, false)), __runInitializers(_init, 51, this);
    __privateAdd(this, _loadingBarState, __runInitializers(_init, 52, this, "" /* IDLE */)), __runInitializers(_init, 55, this);
    __privateAdd(this, __isPopover, false);
    __privateAdd(this, _applyDefaultThemeState, true);
    __privateAdd(this, _firstTimeout, 0);
    __privateAdd(this, _secondTimeout, 0);
    __privateAdd(this, _thirdTimeout, 0);
    __privateAdd(this, _expandedTimeout, 0);
    __privateAdd(this, _connectionStateStore);
    __publicField(this, "connectionStateListener");
    __privateAdd(this, _lastMessageState, ConnectionState.CONNECTED);
    this.connectionStateListener = () => {
      this.expanded = __privateMethod(this, _ConnectionIndicator_instances, updateConnectionState_fn).call(this);
      __privateSet(this, _expandedTimeout, __privateMethod(this, _ConnectionIndicator_instances, timeoutFor_fn).call(this, __privateGet(this, _expandedTimeout), this.expanded, () => {
        this.expanded = false;
      }, this.expandedDuration));
    };
  }
  static get instance() {
    return _ConnectionIndicator.create();
  }
  /**
   * Initialize global connection indicator instance at
   * window.Vaadin.connectionIndicator and add instance to the document body.
   */
  static create() {
    const $wnd = window;
    if (!$wnd.Vaadin?.connectionIndicator) {
      $wnd.Vaadin ??= {};
      $wnd.Vaadin.connectionIndicator = document.createElement("vaadin-connection-indicator");
      document.body.appendChild($wnd.Vaadin.connectionIndicator);
    }
    return $wnd.Vaadin?.connectionIndicator;
  }
  render() {
    return html`
      <div class="v-loading-indicator ${__privateGet(this, _ConnectionIndicator_instances, loadingBarState_get)}" style=${__privateMethod(this, _ConnectionIndicator_instances, getLoadingBarStyle_fn).call(this)}></div>

      <div
        class="v-status-message ${classMap({
      active: this.reconnecting
    })}"
      >
        <span class="text"> ${__privateMethod(this, _ConnectionIndicator_instances, renderMessage_fn).call(this)} </span>
      </div>
    `;
  }
  connectedCallback() {
    super.connectedCallback();
    __privateMethod(this, _ConnectionIndicator_instances, initPopover_fn).call(this);
    const $wnd = window;
    if ($wnd.Vaadin?.connectionState) {
      __privateSet(this, _connectionStateStore, $wnd.Vaadin.connectionState);
      __privateGet(this, _connectionStateStore).addStateChangeListener(this.connectionStateListener);
      __privateMethod(this, _ConnectionIndicator_instances, updateConnectionState_fn).call(this);
    }
    __privateMethod(this, _ConnectionIndicator_instances, updateTheme_fn).call(this);
  }
  disconnectedCallback() {
    super.disconnectedCallback();
    if (__privateGet(this, _connectionStateStore)) {
      __privateGet(this, _connectionStateStore).removeStateChangeListener(this.connectionStateListener);
    }
    __privateMethod(this, _ConnectionIndicator_instances, updateTheme_fn).call(this);
    __privateSet(this, _ConnectionIndicator_instances, false, isPopover_set);
  }
  updated(props) {
    if (["loading", "offline", "reconnecting", "expanded"].some((p) => props.has(p))) {
      __privateMethod(this, _ConnectionIndicator_instances, updatePopoverState_fn).call(this);
    }
  }
  get applyDefaultTheme() {
    return __privateGet(this, _applyDefaultThemeState);
  }
  set applyDefaultTheme(applyDefaultTheme) {
    if (applyDefaultTheme !== __privateGet(this, _applyDefaultThemeState)) {
      __privateSet(this, _applyDefaultThemeState, applyDefaultTheme);
      __privateMethod(this, _ConnectionIndicator_instances, updateTheme_fn).call(this);
    }
  }
  createRenderRoot() {
    return this;
  }
};
_init = __decoratorStart(_a);
_firstDelay = new WeakMap();
_secondDelay = new WeakMap();
_thirdDelay = new WeakMap();
_expandedDuration = new WeakMap();
_onlineText = new WeakMap();
_offlineText = new WeakMap();
_reconnectingText = new WeakMap();
_offline = new WeakMap();
_reconnecting = new WeakMap();
_expanded = new WeakMap();
_loading = new WeakMap();
_loadingBarState = new WeakMap();
_ConnectionIndicator_instances = new WeakSet();
__isPopover = new WeakMap();
isPopover_get = function() {
  return __privateGet(this, __isPopover);
};
isPopover_set = function(_) {
  __privateSet(this, __isPopover, _);
};
_applyDefaultThemeState = new WeakMap();
_firstTimeout = new WeakMap();
_secondTimeout = new WeakMap();
_thirdTimeout = new WeakMap();
_expandedTimeout = new WeakMap();
_connectionStateStore = new WeakMap();
_lastMessageState = new WeakMap();
initPopover_fn = function() {
  this.setAttribute("popover", "manual");
};
/**
 * Update state flags.
 *
 * @returns true if the connection message changes, and therefore a new
 * message should be shown
 */
updateConnectionState_fn = function() {
  const connectionState = __privateGet(this, _connectionStateStore)?.state;
  this.offline = connectionState === ConnectionState.CONNECTION_LOST;
  this.reconnecting = connectionState === ConnectionState.RECONNECTING;
  __privateMethod(this, _ConnectionIndicator_instances, updateLoading_fn).call(this, connectionState === ConnectionState.LOADING);
  if (this.loading) {
    return false;
  }
  if (connectionState !== __privateGet(this, _lastMessageState)) {
    __privateSet(this, _lastMessageState, connectionState);
    return true;
  }
  return false;
};
updateLoading_fn = function(loading) {
  this.loading = loading;
  __privateSet(this, _ConnectionIndicator_instances, "" /* IDLE */, loadingBarState_set);
  __privateSet(this, _firstTimeout, __privateMethod(this, _ConnectionIndicator_instances, timeoutFor_fn).call(this, __privateGet(this, _firstTimeout), loading, () => {
    __privateSet(this, _ConnectionIndicator_instances, "first" /* FIRST */, loadingBarState_set);
  }, this.firstDelay));
  __privateSet(this, _secondTimeout, __privateMethod(this, _ConnectionIndicator_instances, timeoutFor_fn).call(this, __privateGet(this, _secondTimeout), loading, () => {
    __privateSet(this, _ConnectionIndicator_instances, "second" /* SECOND */, loadingBarState_set);
  }, this.secondDelay));
  __privateSet(this, _thirdTimeout, __privateMethod(this, _ConnectionIndicator_instances, timeoutFor_fn).call(this, __privateGet(this, _thirdTimeout), loading, () => {
    __privateSet(this, _ConnectionIndicator_instances, "third" /* THIRD */, loadingBarState_set);
  }, this.thirdDelay));
};
updatePopoverState_fn = function() {
  const showPopover = this.loading || this.offline || this.reconnecting || this.expanded;
  if (__privateGet(this, _ConnectionIndicator_instances, isPopover_get)) {
    this.hidePopover();
  }
  if (showPopover) {
    this.showPopover();
  }
  __privateSet(this, _ConnectionIndicator_instances, showPopover, isPopover_set);
};
renderMessage_fn = function() {
  if (this.reconnecting) {
    return this.reconnectingText;
  }
  if (this.offline) {
    return this.offlineText;
  }
  return this.onlineText;
};
updateStyle_fn = function(id, shouldApply, styleTemplate) {
  if (shouldApply) {
    if (!document.getElementById(id)) {
      const style = document.createElement("style");
      style.id = id;
      style.textContent = styleTemplate.apply(this).cssText;
      document.head.appendChild(style);
    }
  } else {
    const style = document.getElementById(id);
    if (style) {
      document.head.removeChild(style);
    }
  }
};
updateTheme_fn = function() {
  __privateMethod(this, _ConnectionIndicator_instances, updateStyle_fn).call(this, FUNCTIONAL_STYLE_ID, this.isConnected, __privateMethod(this, _ConnectionIndicator_instances, getFunctionalStyle_fn));
  __privateMethod(this, _ConnectionIndicator_instances, updateStyle_fn).call(this, DEFAULT_STYLE_ID, __privateGet(this, _applyDefaultThemeState) && this.isConnected, __privateMethod(this, _ConnectionIndicator_instances, getDefaultStyle_fn));
};
getFunctionalStyle_fn = function() {
  return css`
      /* Override user agent styles for popover */
      vaadin-connection-indicator[popover] {
        display: contents;
        width: auto;
        height: auto;
        inset: 0;
        bottom: auto;
        margin: 0;
        padding: 0;
        background: none;
        border: none;
      }

      .v-loading-indicator,
      .v-status-message {
        pointer-events: none;
      }

      /*
       Make sure the connection indicator content is hidden, as expected to when
       "applyDefaultTheme" is set to false. Typically, either theme or user
       styles override these and show the indicator.
       */
      .v-loading-indicator,
      .v-status-message {
        pointer-events: none;
        opacity: 0;
      }
    `;
};
getDefaultStyle_fn = function() {
  return css`
      @keyframes v-progress-start {
        0% {
          width: 0%;
        }
        100% {
          width: 50%;
        }
      }
      @keyframes v-progress-delay {
        0% {
          width: 50%;
        }
        100% {
          width: 90%;
        }
      }
      @keyframes v-progress-wait {
        0% {
          width: 90%;
          height: 4px;
        }
        3% {
          width: 91%;
          height: 7px;
        }
        100% {
          width: 96%;
          height: 7px;
        }
      }
      @keyframes v-progress-wait-pulse {
        0% {
          opacity: 1;
        }
        50% {
          opacity: 0.1;
        }
        100% {
          opacity: 1;
        }
      }
      .v-loading-indicator,
      .v-status-message {
        box-sizing: border-box;
        position: fixed;
        left: 0;
        right: 0;
        top: 0;
        background-color: var(--lumo-primary-color, var(--material-primary-color, blue));
        transition: none;
      }
      .v-loading-indicator {
        right: auto;
        width: 50%;
        height: 4px;
        opacity: 1;
        animation: v-progress-start 1000ms 200ms both;
      }
      .v-loading-indicator[style*='none'] {
        display: block !important;
        width: 100%;
        opacity: 0;
        animation: none;
        transition:
          opacity 500ms 300ms,
          width 300ms;
      }
      .v-loading-indicator.second {
        width: 90%;
        animation: v-progress-delay 3.8s forwards;
      }
      .v-loading-indicator.third {
        width: 96%;
        animation:
          v-progress-wait 5s forwards,
          v-progress-wait-pulse 1s 4s infinite backwards;
      }

      vaadin-connection-indicator[offline] .v-loading-indicator,
      vaadin-connection-indicator[reconnecting] .v-loading-indicator {
        display: none;
      }

      .v-status-message {
        opacity: 0;
        max-height: var(--status-height-collapsed, 8px);
        overflow: hidden;
        background-color: var(--status-bg-color-online, var(--lumo-primary-color, var(--material-primary-color, blue)));
        color: var(
          --status-text-color-online,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        font-size: 0.75rem;
        font-weight: 600;
        line-height: 1;
        transition: all 0.5s;
        padding: 0 0.5em;
      }

      vaadin-connection-indicator[offline] .v-status-message,
      vaadin-connection-indicator[reconnecting] .v-status-message {
        opacity: 1;
        pointer-events: auto;
        background-color: var(--status-bg-color-offline, var(--lumo-shade, #333));
        color: var(
          --status-text-color-offline,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        background-image: repeating-linear-gradient(
          45deg,
          rgba(255, 255, 255, 0),
          rgba(255, 255, 255, 0) 10px,
          rgba(255, 255, 255, 0.1) 10px,
          rgba(255, 255, 255, 0.1) 20px
        );
      }

      vaadin-connection-indicator[reconnecting] .v-status-message {
        animation: show-reconnecting-status 2s;
      }

      vaadin-connection-indicator[offline] .v-status-message:hover,
      vaadin-connection-indicator[reconnecting] .v-status-message:hover,
      vaadin-connection-indicator[expanded] .v-status-message {
        max-height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[expanded] .v-status-message {
        opacity: 1;
        pointer-events: auto;
      }

      .v-status-message span {
        display: flex;
        align-items: center;
        justify-content: center;
        height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[reconnecting] .v-status-message span::before {
        content: '';
        width: 1em;
        height: 1em;
        border-top: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-left: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-right: 2px solid transparent;
        border-bottom: 2px solid transparent;
        border-radius: 50%;
        box-sizing: border-box;
        animation: v-spin 0.4s linear infinite;
        margin: 0 0.5em;
      }

      @keyframes v-spin {
        100% {
          transform: rotate(360deg);
        }
      }
    `;
};
getLoadingBarStyle_fn = function() {
  switch (__privateGet(this, _ConnectionIndicator_instances, loadingBarState_get)) {
    case "" /* IDLE */:
      return "display: none";
    case "first" /* FIRST */:
    case "second" /* SECOND */:
    case "third" /* THIRD */:
      return "display: block";
    default:
      return "";
  }
};
timeoutFor_fn = function(timeoutId, enabled, handler, delay) {
  if (timeoutId !== 0) {
    window.clearTimeout(timeoutId);
  }
  return enabled ? window.setTimeout(handler, delay) : 0;
};
__decorateElement(_init, 4, "firstDelay", _firstDelay_dec, _ConnectionIndicator, _firstDelay);
__decorateElement(_init, 4, "secondDelay", _secondDelay_dec, _ConnectionIndicator, _secondDelay);
__decorateElement(_init, 4, "thirdDelay", _thirdDelay_dec, _ConnectionIndicator, _thirdDelay);
__decorateElement(_init, 4, "expandedDuration", _expandedDuration_dec, _ConnectionIndicator, _expandedDuration);
__decorateElement(_init, 4, "onlineText", _onlineText_dec, _ConnectionIndicator, _onlineText);
__decorateElement(_init, 4, "offlineText", _offlineText_dec, _ConnectionIndicator, _offlineText);
__decorateElement(_init, 4, "reconnectingText", _reconnectingText_dec, _ConnectionIndicator, _reconnectingText);
__decorateElement(_init, 4, "offline", _offline_dec, _ConnectionIndicator, _offline);
__decorateElement(_init, 4, "reconnecting", _reconnecting_dec, _ConnectionIndicator, _reconnecting);
__decorateElement(_init, 4, "expanded", _expanded_dec, _ConnectionIndicator, _expanded);
__decorateElement(_init, 4, "loading", _loading_dec, _ConnectionIndicator, _loading);
_b = __decorateElement(_init, 20, "#loadingBarState", _loadingBarState_dec, _ConnectionIndicator_instances, _loadingBarState), loadingBarState_get = _b.get, loadingBarState_set = _b.set;
__decorateElement(_init, 3, "applyDefaultTheme", _applyDefaultTheme_dec, _ConnectionIndicator);
__decoratorMetadata(_init, _ConnectionIndicator);
let ConnectionIndicator = _ConnectionIndicator;
if (customElements.get("vaadin-connection-indicator") === void 0) {
  customElements.define("vaadin-connection-indicator", ConnectionIndicator);
}
const connectionIndicator = ConnectionIndicator.instance;
export {
  ConnectionIndicator,
  LoadingBarState,
  connectionIndicator
};
//# sourceMappingURL=ConnectionIndicator.js.map
