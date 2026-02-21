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

type VaadinScreenOrientationType =
  | 'portrait-primary'
  | 'portrait-secondary'
  | 'landscape-primary'
  | 'landscape-secondary';

interface VaadinScreenOrientationDetail {
  type: VaadinScreenOrientationType | '';
  angle: number;
}

/**
 * Returns the current screen orientation type synchronously, or the empty
 * string if the Screen Orientation API is unavailable. Used by the bootstrap
 * path to seed the server-side signal without waiting for a DOM event.
 */
export function currentScreenOrientationType(): string {
  return screen.orientation?.type ?? '';
}

/**
 * Returns the current screen orientation angle synchronously, or 0 if the
 * Screen Orientation API is unavailable.
 */
export function currentScreenOrientationAngle(): number {
  return screen.orientation?.angle ?? 0;
}

// Dispatch on document.body so the server-side Page facade (listening on the
// UI element, which is body) can update its signal.
function dispatch(detail: VaadinScreenOrientationDetail): void {
  document.body.dispatchEvent(new CustomEvent('vaadin-screen-orientation-change', { detail }));
}

if (screen.orientation) {
  screen.orientation.addEventListener('change', () => {
    dispatch({
      type: screen.orientation.type as VaadinScreenOrientationType,
      angle: screen.orientation.angle
    });
  });
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.screenOrientation = {
  lock(type: string): Promise<void> {
    return screen.orientation.lock(type as OrientationLockType);
  },
  unlock(): void {
    screen.orientation.unlock();
  }
};
