/**
@license
Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
Code distributed by Google as part of the polymer project is also
subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
*/

/* eslint-disable @typescript-eslint/no-use-before-define */

/**
 * @fileoverview
 *
 * Module for adding listeners to a node for the following normalized
 * cross-platform "gesture" events:
 * - `down` - mouse or touch went down
 * - `up` - mouse or touch went up
 * - `tap` - mouse click or finger tap
 * - `track` - mouse drag or touch move
 *
 * @summary Module for adding cross-platform gesture event listeners.
 */

import { microTask } from './async.js';

const passiveTouchGestures = false;
const wrap = (node) => node;

// Detect native touch action support
const HAS_NATIVE_TA = typeof document.head.style.touchAction === 'string';
const GESTURE_KEY = '__polymerGestures';
const HANDLED_OBJ = '__polymerGesturesHandled';
const TOUCH_ACTION = '__polymerGesturesTouchAction';
// Radius for tap and track
const TAP_DISTANCE = 25;
const TRACK_DISTANCE = 5;
// Number of last N track positions to keep
const TRACK_LENGTH = 2;

const MOUSE_EVENTS = ['mousedown', 'mousemove', 'mouseup', 'click'];
// An array of bitmask values for mapping MouseEvent.which to MouseEvent.buttons
const MOUSE_WHICH_TO_BUTTONS = [0, 1, 4, 2];
const MOUSE_HAS_BUTTONS = (function () {
  try {
    return new MouseEvent('test', { buttons: 1 }).buttons === 1;
  } catch (_) {
    return false;
  }
})();

/**
 * @param {string} name Possible mouse event name
 * @return {boolean} true if mouse event, false if not
 */
function isMouseEvent(name) {
  return MOUSE_EVENTS.indexOf(name) > -1;
}

/* eslint no-empty: ["error", { "allowEmptyCatch": true }] */
// check for passive event listeners
let supportsPassive = false;
(function () {
  try {
    const opts = Object.defineProperty({}, 'passive', {
      // eslint-disable-next-line getter-return
      get() {
        supportsPassive = true;
      },
    });
    window.addEventListener('test', null, opts);
    window.removeEventListener('test', null, opts);
  } catch (_) {}
})();

/**
 * Generate settings for event listeners, dependant on `passiveTouchGestures`
 *
 * @param {string} eventName Event name to determine if `{passive}` option is
 *   needed
 * @return {{passive: boolean} | undefined} Options to use for addEventListener
 *   and removeEventListener
 */
function PASSIVE_TOUCH(eventName) {
  if (isMouseEvent(eventName) || eventName === 'touchend') {
    return;
  }
  if (HAS_NATIVE_TA && supportsPassive && passiveTouchGestures) {
    return { passive: true };
  }
}

// Check for touch-only devices
const IS_TOUCH_ONLY = navigator.userAgent.match(/iP(?:[oa]d|hone)|Android/u);

// Defined at https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#enabling-and-disabling-form-controls:-the-disabled-attribute
/** @type {!Object<boolean>} */
const canBeDisabled = {
  button: true,
  command: true,
  fieldset: true,
  input: true,
  keygen: true,
  optgroup: true,
  option: true,
  select: true,
  textarea: true,
};

/**
 * @param {MouseEvent} ev event to test for left mouse button down
 * @return {boolean} has left mouse button down
 */
function hasLeftMouseButton(ev) {
  const type = ev.type;
  // Exit early if the event is not a mouse event
  if (!isMouseEvent(type)) {
    return false;
  }
  // Ev.button is not reliable for mousemove (0 is overloaded as both left button and no buttons)
  // instead we use ev.buttons (bitmask of buttons) or fall back to ev.which (deprecated, 0 for no buttons, 1 for left button)
  if (type === 'mousemove') {
    // Allow undefined for testing events
    let buttons = ev.buttons === undefined ? 1 : ev.buttons;
    if (ev instanceof window.MouseEvent && !MOUSE_HAS_BUTTONS) {
      buttons = MOUSE_WHICH_TO_BUTTONS[ev.which] || 0;
    }
    // Buttons is a bitmask, check that the left button bit is set (1)
    return Boolean(buttons & 1);
  }
  // Allow undefined for testing events
  const button = ev.button === undefined ? 0 : ev.button;
  // Ev.button is 0 in mousedown/mouseup/click for left button activation
  return button === 0;
}

function isSyntheticClick(ev) {
  if (ev.type === 'click') {
    // Ev.detail is 0 for HTMLElement.click in most browsers
    if (ev.detail === 0) {
      return true;
    }
    // In the worst case, check that the x/y position of the click is within
    // the bounding box of the target of the event
    // Thanks IE 10 >:(
    const t = _findOriginalTarget(ev);
    // Make sure the target of the event is an element so we can use getBoundingClientRect,
    // if not, just assume it is a synthetic click
    if (!t.nodeType || /** @type {Element} */ (t).nodeType !== Node.ELEMENT_NODE) {
      return true;
    }
    const bcr = /** @type {Element} */ (t).getBoundingClientRect();
    // Use page x/y to account for scrolling
    const x = ev.pageX,
      y = ev.pageY;
    // Ev is a synthetic click if the position is outside the bounding box of the target
    return !(x >= bcr.left && x <= bcr.right && y >= bcr.top && y <= bcr.bottom);
  }
  return false;
}

const POINTERSTATE = {
  mouse: {
    target: null,
    mouseIgnoreJob: null,
  },
  touch: {
    x: 0,
    y: 0,
    id: -1,
    scrollDecided: false,
  },
};

function firstTouchAction(ev) {
  let ta = 'auto';
  const path = getComposedPath(ev);
  for (let i = 0, n; i < path.length; i++) {
    n = path[i];
    if (n[TOUCH_ACTION]) {
      ta = n[TOUCH_ACTION];
      break;
    }
  }
  return ta;
}

function trackDocument(stateObj, movefn, upfn) {
  stateObj.movefn = movefn;
  stateObj.upfn = upfn;
  document.addEventListener('mousemove', movefn);
  document.addEventListener('mouseup', upfn);
}

function untrackDocument(stateObj) {
  document.removeEventListener('mousemove', stateObj.movefn);
  document.removeEventListener('mouseup', stateObj.upfn);
  stateObj.movefn = null;
  stateObj.upfn = null;
}

/**
 * Returns the composedPath for the given event.
 * @param {Event} event to process
 * @return {!Array<!EventTarget>} Path of the event
 */
const getComposedPath =
  window.ShadyDOM && window.ShadyDOM.noPatch
    ? window.ShadyDOM.composedPath
    : (event) => (event.composedPath && event.composedPath()) || [];

/** @type {!Object<string, !GestureRecognizer>} */
export const gestures = {};

/** @type {!Array<!GestureRecognizer>} */
export const recognizers = [];

/**
 * Finds the element rendered on the screen at the provided coordinates.
 *
 * Similar to `document.elementFromPoint`, but pierces through
 * shadow roots.
 *
 * @param {number} x Horizontal pixel coordinate
 * @param {number} y Vertical pixel coordinate
 * @return {Element} Returns the deepest shadowRoot inclusive element
 * found at the screen position given.
 */
export function deepTargetFind(x, y) {
  let node = document.elementFromPoint(x, y);
  let next = node;
  // This code path is only taken when native ShadowDOM is used
  // if there is a shadowroot, it may have a node at x/y
  // if there is not a shadowroot, exit the loop
  while (next && next.shadowRoot && !window.ShadyDOM) {
    // If there is a node at x/y in the shadowroot, look deeper
    const oldNext = next;
    next = next.shadowRoot.elementFromPoint(x, y);
    // On Safari, elementFromPoint may return the shadowRoot host
    if (oldNext === next) {
      break;
    }
    if (next) {
      node = next;
    }
  }
  return node;
}

/**
 * A cheaper check than ev.composedPath()[0];
 *
 * @private
 * @param {Event|Touch} ev Event.
 * @return {EventTarget} Returns the event target.
 */
function _findOriginalTarget(ev) {
  const path = getComposedPath(/** @type {?Event} */ (ev));
  // It shouldn't be, but sometimes path is empty (window on Safari).
  return path.length > 0 ? path[0] : ev.target;
}

/**
 * @private
 * @param {Event} ev Event.
 * @return {void}
 */
function _handleNative(ev) {
  const type = ev.type;
  const node = ev.currentTarget;
  const gobj = node[GESTURE_KEY];
  if (!gobj) {
    return;
  }
  const gs = gobj[type];
  if (!gs) {
    return;
  }
  if (!ev[HANDLED_OBJ]) {
    ev[HANDLED_OBJ] = {};
    if (type.startsWith('touch')) {
      const t = ev.changedTouches[0];
      if (type === 'touchstart') {
        // Only handle the first finger
        if (ev.touches.length === 1) {
          POINTERSTATE.touch.id = t.identifier;
        }
      }
      if (POINTERSTATE.touch.id !== t.identifier) {
        return;
      }
      if (!HAS_NATIVE_TA) {
        if (type === 'touchstart' || type === 'touchmove') {
          _handleTouchAction(ev);
        }
      }
    }
  }
  const handled = ev[HANDLED_OBJ];
  // Used to ignore synthetic mouse events
  if (handled.skip) {
    return;
  }
  // Reset recognizer state
  for (let i = 0, r; i < recognizers.length; i++) {
    r = recognizers[i];
    if (gs[r.name] && !handled[r.name]) {
      if (r.flow && r.flow.start.indexOf(ev.type) > -1 && r.reset) {
        r.reset();
      }
    }
  }
  // Enforce gesture recognizer order
  for (let i = 0, r; i < recognizers.length; i++) {
    r = recognizers[i];
    if (gs[r.name] && !handled[r.name]) {
      handled[r.name] = true;
      r[type](ev);
    }
  }
}

/**
 * @private
 * @param {TouchEvent} ev Event.
 * @return {void}
 */
function _handleTouchAction(ev) {
  const t = ev.changedTouches[0];
  const type = ev.type;
  if (type === 'touchstart') {
    POINTERSTATE.touch.x = t.clientX;
    POINTERSTATE.touch.y = t.clientY;
    POINTERSTATE.touch.scrollDecided = false;
  } else if (type === 'touchmove') {
    if (POINTERSTATE.touch.scrollDecided) {
      return;
    }
    POINTERSTATE.touch.scrollDecided = true;
    const ta = firstTouchAction(ev);
    let shouldPrevent = false;
    const dx = Math.abs(POINTERSTATE.touch.x - t.clientX);
    const dy = Math.abs(POINTERSTATE.touch.y - t.clientY);
    if (!ev.cancelable) {
      // Scrolling is happening
    } else if (ta === 'none') {
      shouldPrevent = true;
    } else if (ta === 'pan-x') {
      shouldPrevent = dy > dx;
    } else if (ta === 'pan-y') {
      shouldPrevent = dx > dy;
    }
    if (shouldPrevent) {
      ev.preventDefault();
    } else {
      prevent('track');
    }
  }
}

/**
 * Adds an event listener to a node for the given gesture type.
 *
 * @param {!EventTarget} node Node to add listener on
 * @param {string} evType Gesture type: `down`, `up`, `track`, or `tap`
 * @param {!function(!Event):void} handler Event listener function to call
 * @return {boolean} Returns true if a gesture event listener was added.
 */
export function addListener(node, evType, handler) {
  if (gestures[evType]) {
    _add(node, evType, handler);
    return true;
  }
  return false;
}

/**
 * Removes an event listener from a node for the given gesture type.
 *
 * @param {!EventTarget} node Node to remove listener from
 * @param {string} evType Gesture type: `down`, `up`, `track`, or `tap`
 * @param {!function(!Event):void} handler Event listener function previously passed to
 *  `addListener`.
 * @return {boolean} Returns true if a gesture event listener was removed.
 */
export function removeListener(node, evType, handler) {
  if (gestures[evType]) {
    _remove(node, evType, handler);
    return true;
  }
  return false;
}

/**
 * Automate the event listeners for the native events
 *
 * @private
 * @param {!EventTarget} node Node on which to add the event.
 * @param {string} evType Event type to add.
 * @param {function(!Event)} handler Event handler function.
 * @return {void}
 */
function _add(node, evType, handler) {
  const recognizer = gestures[evType];
  const deps = recognizer.deps;
  const name = recognizer.name;
  let gobj = node[GESTURE_KEY];
  if (!gobj) {
    node[GESTURE_KEY] = gobj = {};
  }
  for (let i = 0, dep, gd; i < deps.length; i++) {
    dep = deps[i];
    // Don't add mouse handlers on iOS because they cause gray selection overlays
    if (IS_TOUCH_ONLY && isMouseEvent(dep) && dep !== 'click') {
      continue;
    }
    gd = gobj[dep];
    if (!gd) {
      gobj[dep] = gd = { _count: 0 };
    }
    if (gd._count === 0) {
      node.addEventListener(dep, _handleNative, PASSIVE_TOUCH(dep));
    }
    gd[name] = (gd[name] || 0) + 1;
    gd._count = (gd._count || 0) + 1;
  }
  node.addEventListener(evType, handler);
  if (recognizer.touchAction) {
    setTouchAction(node, recognizer.touchAction);
  }
}

/**
 * Automate event listener removal for native events
 *
 * @private
 * @param {!EventTarget} node Node on which to remove the event.
 * @param {string} evType Event type to remove.
 * @param {function(!Event): void} handler Event handler function.
 * @return {void}
 */
function _remove(node, evType, handler) {
  const recognizer = gestures[evType];
  const deps = recognizer.deps;
  const name = recognizer.name;
  const gobj = node[GESTURE_KEY];
  if (gobj) {
    for (let i = 0, dep, gd; i < deps.length; i++) {
      dep = deps[i];
      gd = gobj[dep];
      if (gd && gd[name]) {
        gd[name] = (gd[name] || 1) - 1;
        gd._count = (gd._count || 1) - 1;
        if (gd._count === 0) {
          node.removeEventListener(dep, _handleNative, PASSIVE_TOUCH(dep));
        }
      }
    }
  }
  node.removeEventListener(evType, handler);
}

/**
 * Registers a new gesture event recognizer for adding new custom
 * gesture event types.
 *
 * @param {!GestureRecognizer} recog Gesture recognizer descriptor
 * @return {void}
 */
export function register(recog) {
  recognizers.push(recog);
  recog.emits.forEach((emit) => {
    gestures[emit] = recog;
  });
}

/**
 * @private
 * @param {string} evName Event name.
 * @return {Object} Returns the gesture for the given event name.
 */
function _findRecognizerByEvent(evName) {
  for (let i = 0, r; i < recognizers.length; i++) {
    r = recognizers[i];
    for (let j = 0, n; j < r.emits.length; j++) {
      n = r.emits[j];
      if (n === evName) {
        return r;
      }
    }
  }
  return null;
}

/**
 * Sets scrolling direction on node.
 *
 * This value is checked on first move, thus it should be called prior to
 * adding event listeners.
 *
 * @param {!EventTarget} node Node to set touch action setting on
 * @param {string} value Touch action value
 * @return {void}
 */
export function setTouchAction(node, value) {
  if (HAS_NATIVE_TA && node instanceof HTMLElement) {
    // NOTE: add touchAction async so that events can be added in
    // custom element constructors. Otherwise we run afoul of custom
    // elements restriction against settings attributes (style) in the
    // constructor.
    microTask.run(() => {
      node.style.touchAction = value;
    });
  }
  node[TOUCH_ACTION] = value;
}

/**
 * Dispatches an event on the `target` element of `type` with the given
 * `detail`.
 * @private
 * @param {!EventTarget} target The element on which to fire an event.
 * @param {string} type The type of event to fire.
 * @param {!Object=} detail The detail object to populate on the event.
 * @return {void}
 */
function _fire(target, type, detail) {
  const ev = new Event(type, { bubbles: true, cancelable: true, composed: true });
  ev.detail = detail;
  wrap(/** @type {!Node} */ (target)).dispatchEvent(ev);
  // Forward `preventDefault` in a clean way
  if (ev.defaultPrevented) {
    const preventer = detail.preventer || detail.sourceEvent;
    if (preventer && preventer.preventDefault) {
      preventer.preventDefault();
    }
  }
}

/**
 * Prevents the dispatch and default action of the given event name.
 *
 * @param {string} evName Event name.
 * @return {void}
 */
export function prevent(evName) {
  const recognizer = _findRecognizerByEvent(evName);
  if (recognizer.info) {
    recognizer.info.prevent = true;
  }
}

register({
  name: 'downup',
  deps: ['mousedown', 'touchstart', 'touchend'],
  flow: {
    start: ['mousedown', 'touchstart'],
    end: ['mouseup', 'touchend'],
  },
  emits: ['down', 'up'],

  info: {
    movefn: null,
    upfn: null,
  },

  /**
   * @this {GestureRecognizer}
   * @return {void}
   */
  reset() {
    untrackDocument(this.info);
  },

  /**
   * @this {GestureRecognizer}
   * @param {MouseEvent} e
   * @return {void}
   */
  mousedown(e) {
    if (!hasLeftMouseButton(e)) {
      return;
    }
    const t = _findOriginalTarget(e);
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this;
    const movefn = (e) => {
      if (!hasLeftMouseButton(e)) {
        downupFire('up', t, e);
        untrackDocument(self.info);
      }
    };
    const upfn = (e) => {
      if (hasLeftMouseButton(e)) {
        downupFire('up', t, e);
      }
      untrackDocument(self.info);
    };
    trackDocument(this.info, movefn, upfn);
    downupFire('down', t, e);
  },

  /**
   * @this {GestureRecognizer}
   * @param {TouchEvent} e
   * @return {void}
   */
  touchstart(e) {
    downupFire('down', _findOriginalTarget(e), e.changedTouches[0], e);
  },

  /**
   * @this {GestureRecognizer}
   * @param {TouchEvent} e
   * @return {void}
   */
  touchend(e) {
    downupFire('up', _findOriginalTarget(e), e.changedTouches[0], e);
  },
});

/**
 * @param {string} type
 * @param {EventTarget} target
 * @param {Event|Touch} event
 * @param {Event=} preventer
 * @return {void}
 */
function downupFire(type, target, event, preventer) {
  if (!target) {
    return;
  }
  _fire(target, type, {
    x: event.clientX,
    y: event.clientY,
    sourceEvent: event,
    preventer,
    prevent(e) {
      return prevent(e);
    },
  });
}

register({
  name: 'track',
  touchAction: 'none',
  deps: ['mousedown', 'touchstart', 'touchmove', 'touchend'],
  flow: {
    start: ['mousedown', 'touchstart'],
    end: ['mouseup', 'touchend'],
  },
  emits: ['track'],

  info: {
    x: 0,
    y: 0,
    state: 'start',
    started: false,
    moves: [],
    /** @this {GestureInfo} */
    addMove(move) {
      if (this.moves.length > TRACK_LENGTH) {
        this.moves.shift();
      }
      this.moves.push(move);
    },
    movefn: null,
    upfn: null,
    prevent: false,
  },

  /**
   * @this {GestureRecognizer}
   * @return {void}
   */
  reset() {
    this.info.state = 'start';
    this.info.started = false;
    this.info.moves = [];
    this.info.x = 0;
    this.info.y = 0;
    this.info.prevent = false;
    untrackDocument(this.info);
  },

  /**
   * @this {GestureRecognizer}
   * @param {MouseEvent} e
   * @return {void}
   */
  mousedown(e) {
    if (!hasLeftMouseButton(e)) {
      return;
    }
    const t = _findOriginalTarget(e);
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this;
    const movefn = (e) => {
      const x = e.clientX,
        y = e.clientY;
      if (trackHasMovedEnough(self.info, x, y)) {
        // First move is 'start', subsequent moves are 'move', mouseup is 'end'
        self.info.state = self.info.started ? (e.type === 'mouseup' ? 'end' : 'track') : 'start';
        if (self.info.state === 'start') {
          // If and only if tracking, always prevent tap
          prevent('tap');
        }
        self.info.addMove({ x, y });
        if (!hasLeftMouseButton(e)) {
          // Always fire "end"
          self.info.state = 'end';
          untrackDocument(self.info);
        }
        if (t) {
          trackFire(self.info, t, e);
        }
        self.info.started = true;
      }
    };
    const upfn = (e) => {
      if (self.info.started) {
        movefn(e);
      }

      // Remove the temporary listeners
      untrackDocument(self.info);
    };
    // Add temporary document listeners as mouse retargets
    trackDocument(this.info, movefn, upfn);
    this.info.x = e.clientX;
    this.info.y = e.clientY;
  },

  /**
   * @this {GestureRecognizer}
   * @param {TouchEvent} e
   * @return {void}
   */
  touchstart(e) {
    const ct = e.changedTouches[0];
    this.info.x = ct.clientX;
    this.info.y = ct.clientY;
  },

  /**
   * @this {GestureRecognizer}
   * @param {TouchEvent} e
   * @return {void}
   */
  touchmove(e) {
    const t = _findOriginalTarget(e);
    const ct = e.changedTouches[0];
    const x = ct.clientX,
      y = ct.clientY;
    if (trackHasMovedEnough(this.info, x, y)) {
      if (this.info.state === 'start') {
        // If and only if tracking, always prevent tap
        prevent('tap');
      }
      this.info.addMove({ x, y });
      trackFire(this.info, t, ct);
      this.info.state = 'track';
      this.info.started = true;
    }
  },

  /**
   * @this {GestureRecognizer}
   * @param {TouchEvent} e
   * @return {void}
   */
  touchend(e) {
    const t = _findOriginalTarget(e);
    const ct = e.changedTouches[0];
    // Only trackend if track was started and not aborted
    if (this.info.started) {
      // Reset started state on up
      this.info.state = 'end';
      this.info.addMove({ x: ct.clientX, y: ct.clientY });
      trackFire(this.info, t, ct);
    }
  },
});

/**
 * @param {!GestureInfo} info
 * @param {number} x
 * @param {number} y
 * @return {boolean}
 */
function trackHasMovedEnough(info, x, y) {
  if (info.prevent) {
    return false;
  }
  if (info.started) {
    return true;
  }
  const dx = Math.abs(info.x - x);
  const dy = Math.abs(info.y - y);
  return dx >= TRACK_DISTANCE || dy >= TRACK_DISTANCE;
}

/**
 * @param {!GestureInfo} info
 * @param {?EventTarget} target
 * @param {Touch} touch
 * @return {void}
 */
function trackFire(info, target, touch) {
  if (!target) {
    return;
  }
  const secondlast = info.moves[info.moves.length - 2];
  const lastmove = info.moves[info.moves.length - 1];
  const dx = lastmove.x - info.x;
  const dy = lastmove.y - info.y;
  let ddx,
    ddy = 0;
  if (secondlast) {
    ddx = lastmove.x - secondlast.x;
    ddy = lastmove.y - secondlast.y;
  }
  _fire(target, 'track', {
    state: info.state,
    x: touch.clientX,
    y: touch.clientY,
    dx,
    dy,
    ddx,
    ddy,
    sourceEvent: touch,
    hover() {
      return deepTargetFind(touch.clientX, touch.clientY);
    },
  });
}

register({
  name: 'tap',
  deps: ['mousedown', 'click', 'touchstart', 'touchend'],
  flow: {
    start: ['mousedown', 'touchstart'],
    end: ['click', 'touchend'],
  },
  emits: ['tap'],
  info: {
    x: NaN,
    y: NaN,
    prevent: false,
  },

  /**
   * @this {GestureRecognizer}
   * @return {void}
   */
  reset() {
    this.info.x = NaN;
    this.info.y = NaN;
    this.info.prevent = false;
  },

  /**
   * @this {GestureRecognizer}
   * @param {MouseEvent} e
   * @return {void}
   */
  mousedown(e) {
    if (hasLeftMouseButton(e)) {
      this.info.x = e.clientX;
      this.info.y = e.clientY;
    }
  },

  /**
   * @this {GestureRecognizer}
   * @param {MouseEvent} e
   * @return {void}
   */
  click(e) {
    if (hasLeftMouseButton(e)) {
      trackForward(this.info, e);
    }
  },

  /**
   * @this {GestureRecognizer}
   * @param {TouchEvent} e
   * @return {void}
   */
  touchstart(e) {
    const touch = e.changedTouches[0];
    this.info.x = touch.clientX;
    this.info.y = touch.clientY;
  },

  /**
   * @this {GestureRecognizer}
   * @param {TouchEvent} e
   * @return {void}
   */
  touchend(e) {
    trackForward(this.info, e.changedTouches[0], e);
  },
});

/**
 * @param {!GestureInfo} info
 * @param {Event | Touch} e
 * @param {Event=} preventer
 * @return {void}
 */
function trackForward(info, e, preventer) {
  const dx = Math.abs(e.clientX - info.x);
  const dy = Math.abs(e.clientY - info.y);
  // Find original target from `preventer` for TouchEvents, or `e` for MouseEvents
  const t = _findOriginalTarget(preventer || e);
  if (!t || (canBeDisabled[/** @type {!HTMLElement} */ (t).localName] && t.hasAttribute('disabled'))) {
    return;
  }
  // Dx,dy can be NaN if `click` has been simulated and there was no `down` for `start`
  if (isNaN(dx) || isNaN(dy) || (dx <= TAP_DISTANCE && dy <= TAP_DISTANCE) || isSyntheticClick(e)) {
    // Prevent taps from being generated if an event has canceled them
    if (!info.prevent) {
      _fire(t, 'tap', {
        x: e.clientX,
        y: e.clientY,
        sourceEvent: e,
        preventer,
      });
    }
  }
}
