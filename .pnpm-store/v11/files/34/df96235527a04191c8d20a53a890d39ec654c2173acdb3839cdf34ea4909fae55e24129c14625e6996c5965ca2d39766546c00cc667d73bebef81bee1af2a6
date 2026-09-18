/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Returns an array of ancestor root nodes for the given node.
 *
 * A root node is either a document node or a document fragment node (Shadow Root).
 * The array is collected by a bottom-up DOM traversing that starts with the given node
 * and involves both the light DOM and ancestor shadow DOM trees.
 *
 * @param {Node} node
 * @return {Node[]}
 */
export function getAncestorRootNodes(node) {
  const result = [];

  while (node) {
    if (node.nodeType === Node.DOCUMENT_NODE) {
      result.push(node);
      break;
    }

    if (node.nodeType === Node.DOCUMENT_FRAGMENT_NODE) {
      result.push(node);
      node = node.host;
      continue;
    }

    if (node.assignedSlot) {
      node = node.assignedSlot;
      continue;
    }

    node = node.parentNode;
  }

  return result;
}

/**
 * Returns the list of flattened elements for the given `node`.
 * This list consists of a node's children and, for any children that are
 * `<slot>` elements, the expanded flattened list of `assignedElements`.
 *
 * @param {Node} node
 * @return {Element[]}
 */
export function getFlattenedElements(node) {
  const result = [];
  let elements;
  if (node.localName === 'slot') {
    elements = node.assignedElements();
  } else {
    result.push(node);
    elements = [...node.children];
  }
  elements.forEach((elem) => result.push(...getFlattenedElements(elem)));
  return result;
}

/**
 * Traverses the given node and its parents, including those that are across
 * the shadow root boundaries, until it finds a node that matches the selector.
 *
 * @param {string} selector The CSS selector to match against
 * @param {Node} node The starting node for the traversal
 * @return {Node | null} The closest matching element, or null if no match is found
 */
export function getClosestElement(selector, node) {
  if (!node) {
    return null;
  }

  return node.closest(selector) || getClosestElement(selector, node.getRootNode().host);
}

/**
 * Takes a string with values separated by space and returns a set the values
 *
 * @param {string} value
 * @return {Set<string>}
 */
export function deserializeAttributeValue(value) {
  if (!value) {
    return new Set();
  }

  return new Set(value.split(' '));
}

/**
 * Takes a set of string values and returns a string with values separated by space
 *
 * @param {Set<string>} values
 * @return {string}
 */
export function serializeAttributeValue(values) {
  return values ? [...values].join(' ') : '';
}

/**
 * Adds a value to an attribute containing space-delimited values.
 *
 * @param {HTMLElement} element
 * @param {string} attr
 * @param {string} value
 */
export function addValueToAttribute(element, attr, value) {
  const values = deserializeAttributeValue(element.getAttribute(attr));
  values.add(value);
  element.setAttribute(attr, serializeAttributeValue(values));
}

/**
 * Removes a value from an attribute containing space-delimited values.
 * If the value is the last one, the whole attribute is removed.
 *
 * @param {HTMLElement} element
 * @param {string} attr
 * @param {string} value
 */
export function removeValueFromAttribute(element, attr, value) {
  const values = deserializeAttributeValue(element.getAttribute(attr));
  values.delete(value);
  if (values.size === 0) {
    element.removeAttribute(attr);
    return;
  }
  element.setAttribute(attr, serializeAttributeValue(values));
}

/**
 * Returns true if the given node is an empty text node, false otherwise.
 *
 * @param {Node} node
 * @return {boolean}
 */
export function isEmptyTextNode(node) {
  return node.nodeType === Node.TEXT_NODE && node.textContent.trim() === '';
}
