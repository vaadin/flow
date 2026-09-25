/**
 * @license
 * Copyright (c) 2000 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

const issuedWarnings = new Set();

/**
 * Issues a warning in the browser console if it has not been issued before.
 * @param {string} warning
 */
export function issueWarning(warning) {
  if (issuedWarnings.has(warning)) {
    return;
  }

  issuedWarnings.add(warning);
  console.warn(warning);
}

/**
 * Clears all issued warnings. Only intended for testing purposes.
 */
export function clearWarnings() {
  issuedWarnings.clear();
}
