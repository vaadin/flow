/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Get the scrollLeft value of the element relative to the direction
 *
 * @param {HTMLElement} element
 * @param {string} direction current direction of the element
 * @return {number} the scrollLeft value.
 */
export function getNormalizedScrollLeft(element, direction) {
  const { scrollLeft } = element;
  if (direction !== 'rtl') {
    return scrollLeft;
  }

  return element.scrollWidth - element.clientWidth + scrollLeft;
}

/**
 * Set the scrollLeft value of the element relative to the direction
 *
 * @param {HTMLElement} element
 * @param {string} direction current direction of the element
 * @param {number} scrollLeft the scrollLeft value to be set
 */
export function setNormalizedScrollLeft(element, direction, scrollLeft) {
  if (direction !== 'rtl') {
    element.scrollLeft = scrollLeft;
  } else {
    element.scrollLeft = element.clientWidth - element.scrollWidth + scrollLeft;
  }
}
