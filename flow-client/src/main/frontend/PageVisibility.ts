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

function currentVisibility(): VaadinPageVisibility {
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

interface PageVisibilityInstance {
  dispose(): void;
}

function install(element: HTMLElement): PageVisibilityInstance {
  let blurTimer: ReturnType<typeof setTimeout> | undefined;

  const dispatch = (state: VaadinPageVisibility): void => {
    element.dispatchEvent(new CustomEvent('vaadin-page-visibility-change', { detail: state }));
  };

  const clearBlurTimer = (): void => {
    if (blurTimer !== undefined) {
      clearTimeout(blurTimer);
      blurTimer = undefined;
    }
  };

  const onVisibilityChange = (): void => {
    clearBlurTimer();
    dispatch(document.hidden ? 'HIDDEN' : 'VISIBLE');
  };

  const onBlur = (): void => {
    clearBlurTimer();
    const delay = isFirefox() ? FIREFOX_BLUR_SETTLE_MS : DEFAULT_BLUR_SETTLE_MS;
    blurTimer = setTimeout(() => {
      blurTimer = undefined;
      if (!document.hidden) {
        dispatch('VISIBLE_NOT_FOCUSED');
      }
    }, delay);
  };

  const onFocus = (): void => {
    clearBlurTimer();
    if (!document.hidden) {
      dispatch('VISIBLE');
    }
  };

  document.addEventListener('visibilitychange', onVisibilityChange);
  window.addEventListener('blur', onBlur);
  window.addEventListener('focus', onFocus);

  return {
    dispose(): void {
      clearBlurTimer();
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('blur', onBlur);
      window.removeEventListener('focus', onFocus);
    }
  };
}

const installedElements = new WeakMap<HTMLElement, PageVisibilityInstance>();

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.pageVisibility = {
  /**
   * Returns the current visibility state synchronously. Used by the bootstrap
   * path to seed the server-side signal without waiting for a DOM event.
   */
  current(): VaadinPageVisibility {
    return currentVisibility();
  },

  /**
   * Starts dispatching `vaadin-page-visibility-change` events on the given
   * element. Idempotent: calling it twice on the same element tears down the
   * previous listeners before installing new ones, so the element only ever
   * has one set attached.
   */
  init(element: HTMLElement): void {
    installedElements.get(element)?.dispose();
    installedElements.set(element, install(element));
  }
};

// Ensure this file is emitted as an ES module so Vite can load it via import.
export {};
