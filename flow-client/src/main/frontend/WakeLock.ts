/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

type VaadinWakeLockState = 'ACTIVE' | 'RELEASED';

// Whether the server-side has asked us to hold the lock. The browser releases
// the lock whenever the tab is hidden; this flag is what lets the
// visibilitychange handler re-acquire silently when the tab returns.
let wanted = false;
let sentinel: WakeLockSentinel | null = null;
let visibilityListenerInstalled = false;

function dispatch(element: HTMLElement, state: VaadinWakeLockState): void {
  element.dispatchEvent(new CustomEvent('vaadin-wake-lock-change', { detail: state }));
}

async function acquire(element: HTMLElement): Promise<void> {
  if (sentinel) {
    return;
  }
  if (!('wakeLock' in navigator)) {
    return;
  }
  try {
    const next = await (navigator as any).wakeLock.request('screen');
    // The user (or the browser) may have released the lock or the tab may have
    // been hidden again while the request was in flight.
    if (!wanted || document.visibilityState !== 'visible') {
      try {
        await next.release();
      } catch (_e) {
        // Ignore; releasing an already-released sentinel throws on some
        // browsers and there is nothing meaningful to do here.
      }
      return;
    }
    sentinel = next;
    next.addEventListener('release', () => {
      sentinel = null;
      dispatch(element, 'RELEASED');
    });
    dispatch(element, 'ACTIVE');
  } catch (_e) {
    // The browser refused (insecure context, feature unavailable, low battery,
    // user revoked, …). The signal stays in 'RELEASED'; no need to surface
    // the error — applications observe activeSignal() to know the truth.
  }
}

function installVisibilityListener(element: HTMLElement): void {
  if (visibilityListenerInstalled) {
    return;
  }
  visibilityListenerInstalled = true;
  document.addEventListener('visibilitychange', () => {
    if (wanted && !sentinel && document.visibilityState === 'visible') {
      acquire(element);
    }
  });
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.wakeLock = {
  request(element: HTMLElement): Promise<void> {
    wanted = true;
    installVisibilityListener(element);
    if (document.visibilityState !== 'visible') {
      // The browser will not grant a lock while the page is hidden; the
      // visibilitychange listener will pick it up on the next 'visible'.
      return Promise.resolve();
    }
    return acquire(element);
  },

  async release(element: HTMLElement): Promise<void> {
    wanted = false;
    if (!sentinel) {
      return;
    }
    const current = sentinel;
    sentinel = null;
    try {
      await current.release();
    } catch (_e) {
      // Ignore; the 'release' event listener installed in acquire() also
      // dispatches RELEASED, so the state still reaches the server even when
      // the explicit release() call rejects.
    }
    dispatch(element, 'RELEASED');
  }
};

// Empty export to ensure TypeScript emits this as an ES module, which is
// required for Vite to load it via import.
export {};
