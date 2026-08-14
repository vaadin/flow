export type ScrollSnapshot = Record<string, { scrollTop: number; scrollLeft: number }>;

// Matches ROOT_NODE_ID in Flow.ts / StateTree.java
const ROOT_NODE_ID = 1;
const REFRESH_UI_EVENT = 'vaadin-refresh-ui';
const WINDOW_KEY = '__window__';
const SNAPSHOT_KEY = 'vaadin-hotswap-scroll';
// A stored snapshot belongs to the page load immediately following the reload
// that wrote it. An older one is from a load that never restored it and is
// ignored so that it cannot be applied to an unrelated page.
const SNAPSHOT_MAX_AGE_MS = 30000;
// How long waiting for the Flow clients to become idle is given before
// applying the positions anyway. Kept separate from the budget for applying so
// that clients which never report being idle still leave time for retries.
const IDLE_TIMEOUT_MS = 3000;
// How long applying keeps retrying before giving up. The positions can only be
// applied once the view has rendered, which in dev mode can take a while.
const RESTORE_TIMEOUT_MS = 10000;
const IDLE_POLL_INTERVAL_MS = 50;
// Scroll positions are fractional in browsers, so an exact match is not a
// usable criterion for "restored".
const SCROLL_TOLERANCE_PX = 1;
// Scrolling the page by hand means the restored position is no longer wanted.
// Includes pointerdown so that dragging the scrollbar counts as well.
const USER_SCROLL_EVENTS = ['wheel', 'touchstart', 'pointerdown', 'keydown'];

interface StoredScrollSnapshot {
  timestamp: number;
  positions: ScrollSnapshot;
}

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
 * Captures scroll positions, sends a ui-refresh event to all Flow clients,
 * and restores scroll positions once the clients are idle.
 * Used by both the Push-based (vaadin-refresh-ui event) and WebSocket-based
 * hot-swap paths.
 */
export function refreshWithScrollPreservation(fullRefresh: boolean): void {
  const snapshot = captureScrollPositions();
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
 * Applies one captured position and tells whether it took effect. Scrolling is
 * clamped to the currently scrollable area, so a position set before the
 * content that makes the element scrollable has rendered silently ends up
 * somewhere else, or the element is not in the DOM at all yet.
 */
function applyScrollPosition(key: string, pos: { scrollTop: number; scrollLeft: number }): boolean {
  if (key === WINDOW_KEY) {
    window.scrollTo(pos.scrollLeft, pos.scrollTop);
    return isAt(window.scrollY, pos.scrollTop) && isAt(window.scrollX, pos.scrollLeft);
  }
  const el = document.querySelector(key);
  if (!el) {
    return false;
  }
  el.scrollTop = pos.scrollTop;
  el.scrollLeft = pos.scrollLeft;
  return isAt(el.scrollTop, pos.scrollTop) && isAt(el.scrollLeft, pos.scrollLeft);
}

function isAt(actual: number, expected: number): boolean {
  return Math.abs(actual - expected) <= SCROLL_TOLERANCE_PX;
}

// Identifies the restore that is allowed to apply positions, so that an
// earlier one stops instead of fighting it frame by frame
let latestRestoreId = 0;

/**
 * Restores scroll positions after a hot-swap UI refresh or a full page reload.
 * Waits until the Flow clients are idle so that the positions are not applied
 * to a DOM that is still being replaced, and then keeps re-applying each
 * position until it sticks, as the view can keep growing for several frames
 * after the UIDL has been processed. Positions that never become reachable are
 * given up on after a timeout.
 *
 * @param snapshot
 *          the captured positions to restore
 * @param onSettled
 *          called once every position has been restored or the timeout has
 *          been reached
 */
export function restoreScrollPositions(snapshot: ScrollSnapshot, onSettled?: () => void): void {
  const pending = Object.entries(snapshot);
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
  const cancel = () => (cancelled = true);
  USER_SCROLL_EVENTS.forEach((type) => window.addEventListener(type, cancel, { once: true, passive: true }));
  const stopped = () => cancelled || restoreId !== latestRestoreId;
  const settle = () => {
    USER_SCROLL_EVENTS.forEach((type) => window.removeEventListener(type, cancel));
    onSettled?.();
  };

  const applyUntilRestored = (deadline: number) => {
    requestAnimationFrame(() => {
      if (stopped()) {
        settle();
        return;
      }
      for (let i = pending.length - 1; i >= 0; i--) {
        const [key, pos] = pending[i];
        if (applyScrollPosition(key, pos)) {
          pending.splice(i, 1);
        }
      }
      if (pending.length === 0 || performance.now() > deadline) {
        settle();
      } else {
        applyUntilRestored(deadline);
      }
    });
  };

  const idleDeadline = performance.now() + IDLE_TIMEOUT_MS;
  const waitForIdleClients = () => {
    const clients = getFlowClients();
    const allIdle = clients.length > 0 && clients.every((c: any) => !c.isActive());
    if (stopped()) {
      settle();
    } else if (allIdle || performance.now() > idleDeadline) {
      applyUntilRestored(performance.now() + RESTORE_TIMEOUT_MS);
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
  const positions = captureScrollPositions();
  if (Object.keys(positions).length > 0) {
    const stored: StoredScrollSnapshot = { timestamp: Date.now(), positions };
    window.sessionStorage.setItem(SNAPSHOT_KEY, JSON.stringify(stored));
  }
  // Nothing to capture also means that this reload interrupted a restore that
  // had not landed yet, in which case the stored snapshot is still the one to
  // restore and is left in place. A snapshot whose restore did finish has
  // already been removed, and a stale one is caught when reading it.
  if (window.sessionStorage.getItem(SNAPSHOT_KEY) !== null) {
    history.scrollRestoration = 'manual';
  }
}

/**
 * Restores the scroll positions stored by {@link saveScrollPositionsForReload}
 * before the page was reloaded, if any. The snapshot is kept until the restore
 * is done so that a page load interrupted by another reload does not lose it.
 */
export function restoreScrollPositionsAfterReload(): void {
  const stored = window.sessionStorage.getItem(SNAPSHOT_KEY);
  if (stored === null) {
    // Restoration is taken over from the browser before a reload, which leaves
    // it disabled for this history entry when the reload had nothing to store.
    // Hand it back rather than leaving the page with neither mechanism.
    history.scrollRestoration = 'auto';
    return;
  }
  const snapshot = parseSnapshot(stored);
  if (!snapshot || Date.now() - snapshot.timestamp > SNAPSHOT_MAX_AGE_MS) {
    forgetStoredScrollPositions();
    return;
  }
  restoreScrollPositions(snapshot.positions, forgetStoredScrollPositions);
}

function parseSnapshot(stored: string): StoredScrollSnapshot | undefined {
  try {
    const snapshot = JSON.parse(stored) as StoredScrollSnapshot;
    return snapshot?.positions && typeof snapshot.timestamp === 'number' ? snapshot : undefined;
  } catch {
    return undefined;
  }
}

function forgetStoredScrollPositions(): void {
  window.sessionStorage.removeItem(SNAPSHOT_KEY);
  history.scrollRestoration = 'auto';
}
