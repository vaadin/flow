/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

// We consider the keyboard to be active if the window has received a keydown
// event since the last mousedown event.
let keyboardActive = false;

// Listen for top-level keydown and mousedown events.
// Use capture phase so we detect events even if they're handled.
window.addEventListener(
  'keydown',
  () => {
    keyboardActive = true;
  },
  { capture: true },
);

window.addEventListener(
  'mousedown',
  () => {
    keyboardActive = false;
  },
  { capture: true },
);

/**
 * Returns the actually focused element by traversing shadow
 * trees recursively to ensure it's the leaf element.
 *
 * @return {Element}
 */
export function getDeepActiveElement() {
  let host = document.activeElement || document.body;
  while (host.shadowRoot && host.shadowRoot.activeElement) {
    host = host.shadowRoot.activeElement;
  }
  return host;
}

/**
 * Returns true if the window has received a keydown
 * event since the last mousedown event.
 *
 * @return {boolean}
 */
export function isKeyboardActive() {
  return keyboardActive;
}

/**
 * Returns true if the element is hidden directly with `display: none` or `visibility: hidden`,
 * false otherwise.
 *
 * The method doesn't traverse the element's ancestors, it only checks for the CSS properties
 * set directly to or inherited by the element.
 *
 * @param {HTMLElement} element
 * @return {boolean}
 */
function isElementHiddenDirectly(element) {
  // Check inline style first to save a re-flow.
  const style = element.style;
  if (style.visibility === 'hidden' || style.display === 'none') {
    return true;
  }

  const computedStyle = window.getComputedStyle(element);
  if (computedStyle.visibility === 'hidden' || computedStyle.display === 'none') {
    return true;
  }

  return false;
}

/**
 * Returns if element `a` has lower tab order compared to element `b`
 * (both elements are assumed to be focusable and tabbable).
 * Elements with tabindex = 0 have lower tab order compared to elements
 * with tabindex > 0.
 * If both have same tabindex, it returns false.
 *
 * @param {HTMLElement} a
 * @param {HTMLElement} b
 * @return {boolean}
 */
function hasLowerTabOrder(a, b) {
  // Normalize tabIndexes
  // e.g. in Firefox `<div contenteditable>` has `tabIndex = -1`
  const ati = Math.max(a.tabIndex, 0);
  const bti = Math.max(b.tabIndex, 0);
  return ati === 0 || bti === 0 ? bti > ati : ati > bti;
}

/**
 * Merge sort iterator, merges the two arrays into one, sorted by tabindex.
 *
 * @param {HTMLElement[]} left
 * @param {HTMLElement[]} right
 * @return {HTMLElement[]}
 */
function mergeSortByTabIndex(left, right) {
  const result = [];
  while (left.length > 0 && right.length > 0) {
    if (hasLowerTabOrder(left[0], right[0])) {
      result.push(right.shift());
    } else {
      result.push(left.shift());
    }
  }

  return result.concat(left, right);
}

/**
 * Sorts an array of elements by tabindex. Returns a new array.
 *
 * @param {HTMLElement[]} elements
 * @return {HTMLElement[]}
 */
function sortElementsByTabIndex(elements) {
  // Implement a merge sort as Array.prototype.sort does a non-stable sort
  // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/sort
  const len = elements.length;
  if (len < 2) {
    return elements;
  }
  const pivot = Math.ceil(len / 2);
  const left = sortElementsByTabIndex(elements.slice(0, pivot));
  const right = sortElementsByTabIndex(elements.slice(pivot));

  return mergeSortByTabIndex(left, right);
}

/**
 * Returns true if the element is hidden, false otherwise.
 *
 * An element is treated as hidden when any of the following conditions are met:
 * - the element itself or one of its ancestors has `display: none`.
 * - the element has or inherits `visibility: hidden`.
 *
 * @param {HTMLElement} element
 * @return {boolean}
 */
export function isElementHidden(element) {
  if (element.checkVisibility) {
    return !element.checkVisibility({ visibilityProperty: true });
  }

  // TODO: checkVisibility is supported only from Safari 17.4, so we still need to
  // keep the custom implementation as a fallback for older versions until Vaadin 25:

  // `offsetParent` is `null` when the element itself
  // or one of its ancestors is hidden with `display: none`.
  // https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/offsetParent
  // However `offsetParent` is also null when the element is using fixed
  // positioning, so additionally check if the element takes up layout space.
  if (element.offsetParent === null && element.clientWidth === 0 && element.clientHeight === 0) {
    return true;
  }

  return isElementHiddenDirectly(element);
}

/**
 * Returns true if the element is focusable, otherwise false.
 *
 * The list of focusable elements is taken from http://stackoverflow.com/a/1600194/4228703.
 * However, there isn't a definite list, it's up to the browser.
 * The only standard we have is DOM Level 2 HTML https://www.w3.org/TR/DOM-Level-2-HTML/html.html,
 * according to which the only elements that have a `focus()` method are:
 * - HTMLInputElement
 * - HTMLSelectElement
 * - HTMLTextAreaElement
 * - HTMLAnchorElement
 *
 * This notably omits HTMLButtonElement and HTMLAreaElement.
 * Referring to these tests with tabbables in different browsers
 * http://allyjs.io/data-tables/focusable.html
 *
 * @param {HTMLElement} element
 * @return {boolean}
 */
export function isElementFocusable(element) {
  // The element cannot be focused if its `tabindex` attribute is set to `-1`.
  if (element.matches('[tabindex="-1"]')) {
    return false;
  }

  // Elements that cannot be focused if they have a `disabled` attribute.
  if (element.matches('input, select, textarea, button, object')) {
    return element.matches(':not([disabled])');
  }

  // Elements that can be focused even if they have a `disabled` attribute.
  return element.matches('a[href], area[href], iframe, [tabindex], [contentEditable]');
}

/**
 * Returns true if the element is focused, false otherwise.
 *
 * @param {HTMLElement} element
 * @return {boolean}
 */
export function isElementFocused(element) {
  return element.getRootNode().activeElement === element;
}

/**
 * Returns the normalized element tabindex. If not focusable, returns -1.
 * It checks for the attribute "tabindex" instead of the element property
 * `tabIndex` since browsers assign different values to it.
 * e.g. in Firefox `<div contenteditable>` has `tabIndex = -1`
 *
 * @param {HTMLElement} element
 * @return {number}
 */
function normalizeTabIndex(element) {
  if (!isElementFocusable(element)) {
    return -1;
  }

  const tabIndex = element.getAttribute('tabindex') || 0;
  return Number(tabIndex);
}

/**
 * Searches for nodes that are tabbable and adds them to the `result` array.
 * Returns if the `result` array needs to be sorted by tabindex.
 *
 * @param {Node} node The starting point for the search; added to `result` if tabbable.
 * @param {HTMLElement[]} result
 * @return {boolean}
 * @private
 */
function collectFocusableNodes(node, result) {
  if (node.nodeType !== Node.ELEMENT_NODE || isElementHiddenDirectly(node)) {
    // Don't traverse children if the node is not an HTML element or not visible.
    return false;
  }

  const element = /** @type {HTMLElement} */ (node);
  const tabIndex = normalizeTabIndex(element);
  let needsSort = tabIndex > 0;
  if (tabIndex >= 0) {
    result.push(element);
  }

  let children = [];
  if (element.localName === 'slot') {
    children = element.assignedNodes({ flatten: true });
  } else {
    // Use shadow root if possible, will check for distributed nodes.
    children = (element.shadowRoot || element).children;
  }
  [...children].forEach((child) => {
    // Ensure method is always invoked to collect focusable children.
    needsSort = collectFocusableNodes(child, result) || needsSort;
  });
  return needsSort;
}

/**
 * Returns a tab-ordered array of focusable elements for a root element.
 * The resulting array will include the root element if it is focusable.
 *
 * The method traverses nodes in shadow DOM trees too if any.
 *
 * @param {HTMLElement} element
 * @return {HTMLElement[]}
 */
export function getFocusableElements(element) {
  const focusableElements = [];
  const needsSortByTabIndex = collectFocusableNodes(element, focusableElements);
  // If there is at least one element with tabindex > 0,
  // we need to sort the final array by tabindex.
  if (needsSortByTabIndex) {
    return sortElementsByTabIndex(focusableElements);
  }
  return focusableElements;
}
