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
 * Returns the current screen orientation type synchronously, or
 * {@code 'unsupported'} if the Screen Orientation API is unavailable. Used by
 * the bootstrap path to seed the server-side signal without waiting for a DOM
 * event.
 */
export function currentScreenOrientationType(): string {
  return screen.orientation?.type ?? 'unsupported';
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
// Constant names of the server-side ScreenOrientationLockErrorCode enum; the
// server deserializes the code field directly into that enum, so the raw
// DOMException name never crosses the wire.
type VaadinScreenOrientationLockErrorCode = 'NOT_SUPPORTED' | 'SECURITY' | 'ABORT' | 'UNKNOWN';

interface VaadinScreenOrientationLockResult {
  success: boolean;
  code?: VaadinScreenOrientationLockErrorCode;
  message?: string;
}

function lockErrorCode(domExceptionName: string | undefined): VaadinScreenOrientationLockErrorCode {
  switch (domExceptionName) {
    case 'NotSupportedError':
      return 'NOT_SUPPORTED';
    case 'SecurityError':
      return 'SECURITY';
    case 'AbortError':
      return 'ABORT';
    default:
      return 'UNKNOWN';
  }
}

$wnd.Vaadin.Flow.screenOrientation = {
  // Always resolves so the server-side .then(success, error) chain only
  // receives the "error" branch on a bridge failure (lost connection, etc.).
  // Rejected DOMExceptions are folded into the resolved result so the server
  // can decode them as a record without forfeiting the JS-bridge error arm.
  lock(type: string): Promise<VaadinScreenOrientationLockResult> {
    if (!screen.orientation || typeof screen.orientation.lock !== 'function') {
      return Promise.resolve({
        success: false,
        code: 'NOT_SUPPORTED',
        message: 'Screen Orientation API is not supported in this browser.'
      });
    }
    return screen.orientation
      .lock(type as OrientationLockType)
      .then(() => ({ success: true }))
      .catch((e: DOMException) => {
        const code = lockErrorCode(e.name);
        const message = e.message ?? '';
        return {
          success: false,
          code,
          // The DOMException name is dropped once mapped to a typed code;
          // keep it in the message for diagnostics when no code matches.
          message: code === 'UNKNOWN' && e.name ? `${e.name}: ${message}` : message
        };
      });
  },
  unlock(): void {
    screen.orientation?.unlock();
  }
};
