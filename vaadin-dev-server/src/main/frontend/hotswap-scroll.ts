export type ScrollSnapshot = Record<string, { scrollTop: number; scrollLeft: number }>;

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
    const parent = current.parentElement;
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

/**
 * Captures scroll positions of the window and all scrolled elements.
 * Elements are keyed by CSS selector path so they can be found after DOM rebuild.
 */
export function captureScrollPositions(): ScrollSnapshot {
  const snapshot: ScrollSnapshot = {};
  if (window.scrollX !== 0 || window.scrollY !== 0) {
    snapshot['__window__'] = { scrollTop: window.scrollY, scrollLeft: window.scrollX };
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
 * Restores scroll positions after a hot-swap UI refresh completes.
 * Polls Flow client isActive() to wait until UIDL processing is done,
 * then uses requestAnimationFrame to restore all captured scroll positions.
 */
export function restoreScrollPositions(snapshot: ScrollSnapshot): void {
  if (Object.keys(snapshot).length === 0) {
    return;
  }
  const anyVaadin = (window as any).Vaadin;
  const poll = () => {
    const clients = Object.keys(anyVaadin?.Flow?.clients || {})
      .filter((key) => key !== 'TypeScript')
      .map((id) => anyVaadin.Flow.clients[id]);
    const allIdle = clients.length > 0 && clients.every((c: any) => !c.isActive());
    if (allIdle) {
      requestAnimationFrame(() => {
        for (const [key, pos] of Object.entries(snapshot)) {
          if (key === '__window__') {
            window.scrollTo(pos.scrollLeft, pos.scrollTop);
          } else {
            const el = document.querySelector(key);
            if (el) {
              el.scrollTop = pos.scrollTop;
              el.scrollLeft = pos.scrollLeft;
            }
          }
        }
      });
    } else {
      setTimeout(poll, 50);
    }
  };
  setTimeout(poll, 50);
}
