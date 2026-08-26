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

type VaadinPageVisibility = 'VISIBLE' | 'VISIBLE_NOT_FOCUSED' | 'HIDDEN';

// Firefox defers the visibilitychange event while the window is blurred, so
// a blur handler needs to wait long enough for that delivery to land before
// concluding the state is really "visible but not focused".
const FIREFOX_BLUR_SETTLE_MS = 500;
const DEFAULT_BLUR_SETTLE_MS = 10;

/**
 * Returns the current visibility state synchronously. Used by the bootstrap
 * path to seed the server-side signal without waiting for a DOM event.
 */
export function currentVisibility(): VaadinPageVisibility {
  if (document.hidden) {
    return 'HIDDEN';
  }
  return document.hasFocus() ? 'VISIBLE' : 'VISIBLE_NOT_FOCUSED';
}

function isFirefox(): boolean {
  // Firefox is the only supported browser that reorders visibilitychange
  // relative to blur; UA sniffing is acceptable here because the alternative
  // is waiting the longer interval on every browser.
  return navigator.userAgent.indexOf('Firefox') > -1;
}

let blurTimer: ReturnType<typeof setTimeout> | undefined;

// Dispatch on document.body so the server-side Page facade (listening on
// the UI element, which is body) can update its signal.
function dispatch(state: VaadinPageVisibility): void {
  document.body.dispatchEvent(new CustomEvent('vaadin-page-visibility-change', { detail: state }));
}

function clearBlurTimer(): void {
  if (blurTimer !== undefined) {
    clearTimeout(blurTimer);
    blurTimer = undefined;
  }
}

document.addEventListener('visibilitychange', () => {
  clearBlurTimer();
  dispatch(document.hidden ? 'HIDDEN' : 'VISIBLE');
});

window.addEventListener('blur', () => {
  clearBlurTimer();
  const delay = isFirefox() ? FIREFOX_BLUR_SETTLE_MS : DEFAULT_BLUR_SETTLE_MS;
  blurTimer = setTimeout(() => {
    blurTimer = undefined;
    if (!document.hidden) {
      dispatch('VISIBLE_NOT_FOCUSED');
    }
  }, delay);
});

window.addEventListener('focus', () => {
  clearBlurTimer();
  if (!document.hidden) {
    dispatch('VISIBLE');
  }
});
