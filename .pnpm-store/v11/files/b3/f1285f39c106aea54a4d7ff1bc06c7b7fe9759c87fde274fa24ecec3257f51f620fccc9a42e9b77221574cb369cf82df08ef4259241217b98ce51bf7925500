/**
 * @license
 * Copyright (c) 2023 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Convenience method for reading a value from a path.
 *
 * @param {string} path
 * @param {object} object
 */
export function get(path, object) {
  return path.split('.').reduce((obj, property) => (obj ? obj[property] : undefined), object);
}

/**
 * Convenience method for setting a value to a path.
 *
 * @param {string} path
 * @param {unknown} value
 * @param {object} object
 */
export function set(path, value, object) {
  const pathParts = path.split('.');
  const lastPart = pathParts.pop();
  const target = pathParts.reduce((target, part) => target[part], object);
  target[lastPart] = value;
}
