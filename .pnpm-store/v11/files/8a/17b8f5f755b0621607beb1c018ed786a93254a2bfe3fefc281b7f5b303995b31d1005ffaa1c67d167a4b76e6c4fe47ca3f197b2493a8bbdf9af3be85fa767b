/**
 * @license
 * Copyright (c) 2016 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

import { isSafari } from '@vaadin/component-base/src/browser-utils.js';

/**
 * Checks if the current browser supports CSS Container Query units for pseudo elements.
 * i.e. if the fix for https://bugs.webkit.org/show_bug.cgi?id=253939 is available.
 */
export function supportsCQUnitsForPseudoElements() {
  const testStyle = document.createElement('style');
  testStyle.textContent = `
    .vaadin-icon-test-element {
      container-type: size;
      height: 2px;
      visibility: hidden;
      position: fixed;
    }

    .vaadin-icon-test-element::before {
      content: '';
      display: block;
      height: 100cqh;
    `;
  const testElement = document.createElement('div');
  testElement.classList.add('vaadin-icon-test-element');

  const shadowParent = document.createElement('div');
  shadowParent.attachShadow({ mode: 'open' });
  shadowParent.shadowRoot.innerHTML = '<slot></slot>';
  shadowParent.append(testElement.cloneNode());

  document.body.append(testStyle, testElement, shadowParent);

  const needsFallback = [...document.querySelectorAll('.vaadin-icon-test-element')].find(
    (el) => getComputedStyle(el, '::before').height !== '2px',
  );

  testStyle.remove();
  testElement.remove();
  shadowParent.remove();
  return !needsFallback;
}

/**
 * Checks if the current browser needs a fallback for sizing font icons instead of relying on CSS Container Queries.
 */
export function needsFontIconSizingFallback() {
  if (!CSS.supports('container-type: inline-size')) {
    // The browser does not support CSS Container Queries at all.
    return true;
  }
  if (!isSafari) {
    // Browsers other than Safari support CSS Container Queries as expected.
    return false;
  }
  // Check if the browser does not support CSS Container Query units for pseudo elements.
  return !supportsCQUnitsForPseudoElements();
}
