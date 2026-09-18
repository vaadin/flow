/**
 * @license
 * Copyright (c) 2022 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { animationFrame } from '@vaadin/component-base/src/async.js';
import { Debouncer } from '@vaadin/component-base/src/debounce.js';

const region = document.createElement('div');

region.style.position = 'fixed';
region.style.clip = 'rect(0px, 0px, 0px, 0px)';
region.setAttribute('aria-live', 'polite');

document.body.appendChild(region);

let alertDebouncer;
/**
 * Cause a text string to be announced by screen readers.
 *
 * @param {string} text The text that should be announced by the screen reader.
 * @param {{mode?: string, timeout?: number}} options Additional options.
 */
export function announce(text, options = {}) {
  const mode = options.mode || 'polite';
  const timeout = options.timeout === undefined ? 150 : options.timeout;

  if (mode === 'alert') {
    region.removeAttribute('aria-live');
    region.removeAttribute('role');
    alertDebouncer = Debouncer.debounce(alertDebouncer, animationFrame, () => {
      region.setAttribute('role', 'alert');
    });
  } else {
    if (alertDebouncer) {
      alertDebouncer.cancel();
    }
    region.removeAttribute('role');
    region.setAttribute('aria-live', mode);
  }

  region.textContent = '';

  setTimeout(() => {
    region.textContent = text;
  }, timeout);
}
