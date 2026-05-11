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

type VaadinFullscreenState = 'UNSUPPORTED' | 'NOT_FULLSCREEN' | 'FULLSCREEN';

/**
 * Returns the current fullscreen state synchronously. Used by the bootstrap
 * path to seed the server-side signal without waiting for a DOM event.
 */
export function currentFullscreenState(): VaadinFullscreenState {
  if (document.fullscreenEnabled !== true) {
    return 'UNSUPPORTED';
  }
  return document.fullscreenElement ? 'FULLSCREEN' : 'NOT_FULLSCREEN';
}

// Dispatch on document.body so the server-side Page facade (listening on
// the UI element, which is body) can update its signal.
function dispatch(state: VaadinFullscreenState): void {
  document.body.dispatchEvent(new CustomEvent('vaadin-fullscreen-change', { detail: state }));
}

// Tracks the most recent component-fullscreen setup so the wrapper can be
// torn down when fullscreen exits (programmatically or via Escape) or when
// a new fullscreen request supersedes it.
let activeComponentReset: (() => void) | undefined;

function resetComponentIfActive(): void {
  if (activeComponentReset) {
    const fn = activeComponentReset;
    activeComponentReset = undefined;
    fn();
  }
}

document.addEventListener('fullscreenchange', () => {
  if (!document.fullscreenElement) {
    resetComponentIfActive();
  }
  dispatch(currentFullscreenState());
});

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.fullscreen = {
  /**
   * Requests fullscreen for the entire page (document.documentElement).
   * No-op if the browser does not support fullscreen.
   */
  requestPageFullscreen(): void {
    resetComponentIfActive();
    if (document.fullscreenEnabled !== true) {
      return;
    }
    document.documentElement.requestFullscreen();
  },

  /**
   * Requests fullscreen for a specific component by moving it into the
   * given wrapper element and hiding the rest of the view. Fullscreens
   * document.documentElement so that Vaadin theming and overlay
   * components keep working. The component is restored to its original
   * position on exit (programmatic, Escape, or a superseding request).
   * No-op if the browser does not support fullscreen.
   */
  requestComponentFullscreen(element: HTMLElement, wrapper: HTMLElement): void {
    resetComponentIfActive();
    if (document.fullscreenEnabled !== true) {
      return;
    }
    const originalParent = element.parentNode;
    if (!originalParent) {
      return;
    }
    const placeholder = document.createComment('vaadin-fullscreen-placeholder');
    originalParent.insertBefore(placeholder, element);

    wrapper.appendChild(element);
    const viewRoot = wrapper.firstChild as HTMLElement | null;
    const previousDisplay = viewRoot?.style.display ?? '';
    if (viewRoot) {
      viewRoot.style.display = 'none';
    }

    activeComponentReset = () => {
      placeholder.parentNode?.insertBefore(element, placeholder);
      placeholder.remove();
      if (viewRoot) {
        viewRoot.style.display = previousDisplay;
      }
    };

    document.documentElement.requestFullscreen();
  },

  /**
   * Exits fullscreen mode if the page is currently in fullscreen.
   */
  exitFullscreen(): void {
    if (document.fullscreenElement) {
      document.exitFullscreen();
    }
  }
};
