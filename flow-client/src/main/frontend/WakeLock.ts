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
type VaadinWakeLockAvailability = 'SUPPORTED' | 'UNSUPPORTED' | 'UNKNOWN';
type VaadinWakeLockErrorCode = 'UNSUPPORTED' | 'NOT_ALLOWED' | 'UNKNOWN';

/**
 * Outcome of a request() call. 'granted' = lock held; 'deferred' = page hidden,
 * the visibilitychange listener will re-attempt; 'error' = persistent failure
 * the application can surface to the user.
 */
type VaadinWakeLockRequestResult =
  | { state: 'granted' }
  | { state: 'deferred' }
  | { state: 'error'; errorCode: VaadinWakeLockErrorCode; message: string };

// Whether the server-side has asked us to hold the lock. The browser releases
// the lock whenever the tab is hidden; this flag is what lets the
// visibilitychange handler re-acquire silently when the tab returns.
let wanted = false;
let sentinel: WakeLockSentinel | null = null;
let visibilityListenerInstalled = false;

function dispatch(element: HTMLElement, state: VaadinWakeLockState): void {
  element.dispatchEvent(new CustomEvent('vaadin-wake-lock-change', { detail: state }));
}

async function acquire(element: HTMLElement): Promise<VaadinWakeLockRequestResult> {
  if (sentinel) {
    return { state: 'granted' };
  }
  if (!window.isSecureContext || !('wakeLock' in navigator)) {
    return {
      state: 'error',
      errorCode: 'UNSUPPORTED',
      message: window.isSecureContext
        ? 'Screen Wake Lock API not implemented in this browser'
        : 'Screen Wake Lock API requires a secure context (HTTPS or localhost)'
    };
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
      return { state: 'deferred' };
    }
    sentinel = next;
    next.addEventListener('release', () => {
      sentinel = null;
      dispatch(element, 'RELEASED');
    });
    dispatch(element, 'ACTIVE');
    return { state: 'granted' };
  } catch (e: any) {
    const name = e?.name;
    const errorCode: VaadinWakeLockErrorCode = name === 'NotAllowedError' ? 'NOT_ALLOWED' : 'UNKNOWN';
    return {
      state: 'error',
      errorCode,
      message: e?.message ? String(e.message) : String(e)
    };
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
  request(element: HTMLElement): Promise<VaadinWakeLockRequestResult> {
    wanted = true;
    installVisibilityListener(element);
    if (document.visibilityState !== 'visible') {
      // The browser will not grant a lock while the page is hidden; the
      // visibilitychange listener will pick it up on the next 'visible'.
      return Promise.resolve({ state: 'deferred' });
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
  },

  queryAvailability(): VaadinWakeLockAvailability {
    if (!window.isSecureContext) {
      return 'UNSUPPORTED';
    }
    return 'wakeLock' in navigator ? 'SUPPORTED' : 'UNSUPPORTED';
  }
};

// Empty export to ensure TypeScript emits this as an ES module, which is
// required for Vite to load it via import.
export {};
