export type ScrollSnapshot = Record<string, { scrollTop: number; scrollLeft: number }>;

// Matches ROOT_NODE_ID in Flow.ts / StateTree.java
const ROOT_NODE_ID = 1;
const REFRESH_UI_EVENT = 'vaadin-refresh-ui';
const WINDOW_KEY = '__window__';
const SNAPSHOT_KEY = 'vaadin-hotswap-scroll';
// A stored snapshot belongs to the page load immediately following the reload
// that wrote it. An older one is from a load that never restored it, such as a
// reload that ended up on a page without dev tools, and is ignored so that it
// cannot be applied to a later, unrelated page load.
const SNAPSHOT_MAX_AGE_MS = 30000;
// How long waiting for the Flow clients to become idle is given before
// applying the positions anyway. Kept separate from the budget for applying so
// that clients which never report being idle still leave time for retries.
const IDLE_TIMEOUT_MS = 3000;
// How long applying keeps retrying before giving up. The positions can only be
// applied once the view has rendered, which in dev mode can take a while.
const RESTORE_TIMEOUT_MS = 10000;
const IDLE_POLL_INTERVAL_MS = 50;
// Animation frames do not run while the document is hidden, and a blocked main
// thread delays them as well. Charging the retry budget for more than this per
// frame would let it run out while nothing was being retried.
const MAX_FRAME_COST_MS = 100;
// A position is applied again for this many frames after it first reads back
// correctly, since content rendered afterwards can shrink the scrollable area
// and have the browser clamp the position back down.
const CONFIRMATION_FRAMES = 3;
// Scroll positions are fractional in browsers, so an exact match is not a
// usable criterion for "restored".
const SCROLL_TOLERANCE_PX = 1;
// Scrolling the page by hand means the restored position is no longer wanted.
// Pressing a key or the pointer only scrolls in some cases, see
// isUserScroll. Clicking the page to focus it after saving in the IDE must not
// count, or the positions still waiting for the view to render would be lost.
const USER_SCROLL_EVENTS = ['wheel', 'touchstart', 'pointerdown', 'keydown'];
const SCROLL_KEYS = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'PageUp', 'PageDown', 'Home', 'End', ' '];

interface StoredScrollSnapshot {
  timestamp: number;
  // The page the positions were captured on. The reload can end up on another
  // one, for example when the view forwards elsewhere or a login redirect
  // follows a server restart, and the positions do not describe that page.
  location: string;
  positions: ScrollSnapshot;
  // The value history.scrollRestoration had before it was taken over for the
  // reload, which is what it is handed back as afterwards
  scrollRestoration: ScrollRestoration;
}

interface PendingPosition {
  key: string;
  position: { scrollTop: number; scrollLeft: number };
  confirmations: number;
}

/**
 * What applying a position achieved.
 *
 * `restored` — the element is where it was captured.
 * `settled` — the element could scroll that far but the browser put it
 * somewhere else, scroll snapping being the usual reason. Applying again
 * produces the same result, so this is as restored as it gets.
 * `unreachable` — the element is missing, or does not scroll that far yet
 * because the content it needs has not rendered.
 */
type ApplyOutcome = 'restored' | 'settled' | 'unreachable';

/**
 * Builds a CSS selector path for an element. Uses the element's ID if present,
 * otherwise walks up the DOM building nth-of-type selectors, stopping at the
 * nearest ancestor with an ID.
 */
function getElementPath(el: Element): string {
  if (el.id) return '#' + CSS.escape(el.id);
  const parts: string[] = [];
  let current: Element | null = el;
  while (current && current !== document.documentElement && current !== document.body) {
    if (current.id) {
      parts.unshift('#' + CSS.escape(current.id));
      break;
    }
    const parent: Element | null = current.parentElement;
    if (!parent) break;
    let index = 1;
    let sibling: Element | null = current.previousElementSibling;
    while (sibling) {
      if (sibling.tagName === current.tagName) index++;
      sibling = sibling.previousElementSibling;
    }
    parts.unshift(current.tagName.toLowerCase() + ':nth-of-type(' + index + ')');
    current = parent;
  }
  return parts.length > 0 ? parts.join(' > ') : '';
}

function getFlowClients(): any[] {
  const anyVaadin = (window as any).Vaadin;
  return Object.keys(anyVaadin?.Flow?.clients || {})
    .filter((key) => key !== 'TypeScript')
    .map((id) => anyVaadin.Flow.clients[id]);
}

/**
 * Captures scroll positions of the window and all scrolled elements.
 * Elements are keyed by CSS selector path so they can be found after DOM rebuild.
 */
export function captureScrollPositions(): ScrollSnapshot {
  const snapshot: ScrollSnapshot = {};
  if (window.scrollX !== 0 || window.scrollY !== 0) {
    snapshot[WINDOW_KEY] = { scrollTop: window.scrollY, scrollLeft: window.scrollX };
  }
  document.querySelectorAll('*').forEach((el) => {
    if (el.scrollTop > 0 || el.scrollLeft > 0) {
      const path = getElementPath(el);
      if (path) {
        snapshot[path] = { scrollTop: el.scrollTop, scrollLeft: el.scrollLeft };
      }
    }
  });
  return snapshot;
}

/**
 * Captures the scroll positions to bring back after a refresh or a reload.
 * Either can interrupt a restore that is still in progress, for example while
 * the page is hidden and no animation frames run. The positions that restore
 * has not applied yet are either missing from the page or clamped short of
 * their target, so a new capture alone would lose them, and they win over the
 * captured values.
 */
function captureScrollPositionsToKeep(): ScrollSnapshot {
  return { ...captureScrollPositions(), ...unrestoredPositions() };
}

/**
 * Captures scroll positions, sends a ui-refresh event to all Flow clients,
 * and restores scroll positions once the clients are idle.
 * Used by both the Push-based (vaadin-refresh-ui event) and WebSocket-based
 * hot-swap paths.
 */
export function refreshWithScrollPreservation(fullRefresh: boolean): void {
  const snapshot = captureScrollPositionsToKeep();
  getFlowClients().forEach((client: any) => {
    if (client.sendEventMessage) {
      client.sendEventMessage(ROOT_NODE_ID, 'ui-refresh', { fullRefresh });
    }
  });
  restoreScrollPositions(snapshot);
}

let refreshUIHandlerRegistered = false;

/**
 * Registers a window event listener for 'vaadin-refresh-ui' that triggers
 * a scroll-preserving UI refresh. Guards against double-registration.
 */
export function registerRefreshUIHandler(): void {
  if (refreshUIHandlerRegistered) {
    return;
  }
  refreshUIHandlerRegistered = true;
  window.addEventListener(REFRESH_UI_EVENT, (ev: any) => {
    refreshWithScrollPreservation(ev.detail?.fullRefresh === true);
  });
}

/**
 * Applies one captured position and reads back what came of it. Scrolling is
 * clamped to the currently scrollable area, so a position set before the
 * content that makes the element scrollable has rendered silently ends up
 * somewhere else, or the element is not in the DOM at all yet.
 *
 * The scroll is explicitly instant, as the application stylesheet may set
 * `scroll-behavior: smooth`, which would animate towards the position and read
 * back the value it started from.
 */
function applyScrollPosition(key: string, pos: { scrollTop: number; scrollLeft: number }): ApplyOutcome {
  const el = resolveScroller(key);
  if (!el) {
    return 'unreachable';
  }
  const target: ScrollToOptions = { left: pos.scrollLeft, top: pos.scrollTop, behavior: 'instant' };
  if (key === WINDOW_KEY) {
    window.scrollTo(target);
  } else {
    el.scrollTo(target);
  }
  const actualTop = key === WINDOW_KEY ? window.scrollY : el.scrollTop;
  const actualLeft = key === WINDOW_KEY ? window.scrollX : el.scrollLeft;
  if (isAt(actualTop, pos.scrollTop) && isAt(actualLeft, pos.scrollLeft)) {
    return 'restored';
  }
  const scrollsFarEnough =
    el.scrollHeight - el.clientHeight >= pos.scrollTop && el.scrollWidth - el.clientWidth >= pos.scrollLeft;
  return scrollsFarEnough ? 'settled' : 'unreachable';
}

function isAt(actual: number, expected: number): boolean {
  return Math.abs(actual - expected) <= SCROLL_TOLERANCE_PX;
}

// Identifies the restore that is allowed to apply positions, so that an
// earlier one stops instead of fighting it frame by frame
let latestRestoreId = 0;

// Tells which positions the latest restore has not applied yet and is still
// trying to
let unrestoredPositions: () => ScrollSnapshot = () => ({});

// The value history.scrollRestoration is handed back as once the restore of
// the reload that produced this page load is done, or undefined when nothing
// on this page has taken it over
let scrollRestorationToHandBack: ScrollRestoration | undefined;

function resolveScroller(key: string): Element | null {
  return key === WINDOW_KEY ? document.scrollingElement : document.querySelector(key);
}

/**
 * Tells whether an input event scrolls the page, as opposed to merely focusing
 * or clicking it.
 *
 * @param event
 *          the event to check
 * @param pendingKeys
 *          the keys of the positions that are still being restored. Dragging
 *          the scrollbar of one of them presses the pointer on that element
 *          itself.
 */
function isUserScroll(event: Event, pendingKeys: string[]): boolean {
  if (event instanceof KeyboardEvent) {
    return SCROLL_KEYS.includes(event.key) && !isEditable(event.target);
  }
  if (event.type === 'pointerdown') {
    return pendingKeys.some((key) => {
      try {
        return event.target === resolveScroller(key);
      } catch {
        return false;
      }
    });
  }
  return true;
}

// Scroll keys move the caret or type in an editable element instead of
// scrolling
function isEditable(target: EventTarget | null): boolean {
  return (
    target instanceof HTMLElement &&
    (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))
  );
}

/**
 * Applies one pending position for a frame and tells whether it is done with,
 * either because it has read back correctly for long enough or because the
 * browser will not put it anywhere else.
 */
function applyPendingPosition(entry: PendingPosition): boolean {
  let outcome: ApplyOutcome;
  try {
    outcome = applyScrollPosition(entry.key, entry.position);
  } catch (e) {
    // A key that is not a valid selector cannot come from getElementPath, so
    // it is not something retrying can resolve. Dropping it keeps the rest of
    // the snapshot, and the restore, running.
    console.warn('Ignoring an unusable scroll position for ' + entry.key, e);
    return true;
  }
  if (outcome === 'settled') {
    return true;
  }
  if (outcome === 'unreachable') {
    entry.confirmations = 0;
    return false;
  }
  return ++entry.confirmations >= CONFIRMATION_FRAMES;
}

/**
 * Restores scroll positions after a hot-swap UI refresh or a full page reload.
 * Waits until the Flow clients are idle so that the positions are not applied
 * to a DOM that is still being replaced, and then keeps re-applying each
 * position until it sticks, as the view can keep growing for several frames
 * after the UIDL has been processed. Positions that never become reachable are
 * given up on after a timeout.
 *
 * The positions are dropped once the page has another address than the one
 * they were captured on, as when the view forwards elsewhere, since they do
 * not describe that page.
 *
 * @param snapshot
 *          the captured positions to restore
 * @param onSettled
 *          called once every position has been restored or dropped, or the
 *          timeout has been reached
 * @param capturedOn
 *          the address of the page the positions were captured on
 */
export function restoreScrollPositions(
  snapshot: ScrollSnapshot,
  onSettled?: () => void,
  capturedOn = window.location.href
): void {
  const pending: PendingPosition[] = Object.entries(snapshot).map(([key, position]) => ({
    key,
    position,
    confirmations: 0
  }));
  if (pending.length === 0) {
    onSettled?.();
    return;
  }

  // A position that never becomes reachable, because the new version of the
  // view has less content, would otherwise be re-applied until the timeout and
  // fight the user scrolling in the meantime. A later restore takes over from
  // this one for the same reason.
  const restoreId = ++latestRestoreId;
  let cancelled = false;
  const cancel = (event: Event) => {
    const pendingKeys = pending.map((entry) => entry.key);
    if (isUserScroll(event, pendingKeys)) {
      cancelled = true;
    }
  };
  USER_SCROLL_EVENTS.forEach((type) => window.addEventListener(type, cancel, { passive: true }));
  const stopped = () => cancelled || restoreId !== latestRestoreId || window.location.href !== capturedOn;
  const settle = () => {
    USER_SCROLL_EVENTS.forEach((type) => window.removeEventListener(type, cancel));
    pending.length = 0;
    onSettled?.();
  };
  unrestoredPositions = () =>
    stopped() ? {} : Object.fromEntries(pending.map((entry) => [entry.key, entry.position]));

  const applyUntilRestored = () => {
    let remaining = RESTORE_TIMEOUT_MS;
    let previousFrame = performance.now();
    const applyFrame = () => {
      requestAnimationFrame(() => {
        const now = performance.now();
        remaining -= Math.min(now - previousFrame, MAX_FRAME_COST_MS);
        previousFrame = now;
        if (stopped()) {
          settle();
          return;
        }
        for (let i = pending.length - 1; i >= 0; i--) {
          if (applyPendingPosition(pending[i])) {
            pending.splice(i, 1);
          }
        }
        if (pending.length === 0 || remaining <= 0) {
          settle();
        } else {
          applyFrame();
        }
      });
    };
    applyFrame();
  };

  const idleDeadline = performance.now() + IDLE_TIMEOUT_MS;
  let idleBefore = false;
  const waitForIdleClients = () => {
    const clients = getFlowClients();
    const idle = clients.length > 0 && clients.every((c: any) => !c.isActive());
    // A navigation that a response started, such as a forward, updates the
    // address only after the response has been processed, so the clients
    // have to stay idle for another poll before the address can be trusted
    const ready = (idle && idleBefore) || performance.now() > idleDeadline;
    idleBefore = idle;
    if (stopped()) {
      settle();
    } else if (ready) {
      applyUntilRestored();
    } else {
      setTimeout(waitForIdleClients, IDLE_POLL_INTERVAL_MS);
    }
  };
  setTimeout(waitForIdleClients, IDLE_POLL_INTERVAL_MS);
}

/**
 * Captures scroll positions for the page load that follows a full page reload.
 * Also takes scroll restoration over from the browser: the browser restores
 * the position of a reloaded page on its own, but gives up when the content is
 * rendered after the load event, as a Flow view is. Leaving it enabled means
 * the two mechanisms race, and that the restore here only ever runs in the
 * cases where the browser has already given up.
 */
export function saveScrollPositionsForReload(): void {
  const positions = captureScrollPositionsToKeep();
  if (Object.keys(positions).length === 0) {
    window.sessionStorage.removeItem(SNAPSHOT_KEY);
    // Nothing is taken over for the next load, so a restore of this one that
    // has not handed restoration back yet must not leave it taken over
    if (scrollRestorationToHandBack) {
      history.scrollRestoration = scrollRestorationToHandBack;
    }
    return;
  }
  const stored: StoredScrollSnapshot = {
    timestamp: Date.now(),
    location: window.location.href,
    positions,
    scrollRestoration: scrollRestorationToHandBack ?? history.scrollRestoration
  };
  window.sessionStorage.setItem(SNAPSHOT_KEY, JSON.stringify(stored));
  history.scrollRestoration = 'manual';
}

/**
 * Restores the scroll positions stored by {@link saveScrollPositionsForReload}
 * before the page was reloaded, if any, and then hands scroll restoration back
 * as it was before the reload, so that a value the application set itself is
 * kept.
 *
 * @param triggeredByReload
 *          whether this page load is the one the reload produced. The
 *          positions describe the page that was reloaded and are discarded
 *          for any other load, such as one the developer navigated to while a
 *          restore was still running. They are also discarded when the
 *          reload itself ended up on another page, or when the snapshot is
 *          too old to have been written for this load.
 */
export function restoreScrollPositionsAfterReload(triggeredByReload: boolean): void {
  const stored = window.sessionStorage.getItem(SNAPSHOT_KEY);
  if (stored === null) {
    return;
  }
  window.sessionStorage.removeItem(SNAPSHOT_KEY);
  if (!triggeredByReload) {
    // Scroll restoration was taken over for the history entry of the page
    // that was reloaded, not for this one
    return;
  }
  const snapshot = parseSnapshot(stored);
  if (!snapshot) {
    return;
  }
  scrollRestorationToHandBack = snapshot.scrollRestoration;
  const handBack = () => {
    history.scrollRestoration = snapshot.scrollRestoration;
    scrollRestorationToHandBack = undefined;
  };
  if (Date.now() - snapshot.timestamp > SNAPSHOT_MAX_AGE_MS) {
    handBack();
    return;
  }
  restoreScrollPositions(snapshot.positions, handBack, snapshot.location);
}

function parseSnapshot(stored: string): StoredScrollSnapshot | undefined {
  try {
    const snapshot = JSON.parse(stored) as StoredScrollSnapshot;
    const valid =
      snapshot?.positions &&
      typeof snapshot.timestamp === 'number' &&
      typeof snapshot.location === 'string' &&
      ['auto', 'manual'].includes(snapshot.scrollRestoration);
    return valid ? snapshot : undefined;
  } catch {
    return undefined;
  }
}
