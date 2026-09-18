/**
 * @license
 * Copyright (c) 2017 Anton Korzunov
 * SPDX-License-Identifier: MIT
 */

/**
 * @fileoverview
 *
 * This module includes JS code copied from the `aria-hidden` package:
 * https://github.com/theKashey/aria-hidden/blob/master/src/index.ts
 */

/** @type {WeakMap<Element, number>} */
let counterMap = new WeakMap();

/** @type {WeakMap<Element, boolean>} */
let uncontrolledNodes = new WeakMap();

/** @type {Record<string, WeakMap<Element, number>>} */
let markerMap = {};

/** @type {number} */
let lockCount = 0;

/**
 * @param {?Node} node
 * @return {boolean}
 */
const isElement = (node) => node && node.nodeType === Node.ELEMENT_NODE;

/**
 * @param  {...unknown} args
 */
const logError = (...args) => {
  console.error(`Error: ${args.join(' ')}. Skip setting aria-hidden.`);
};

/**
 * @param {HTMLElement} parent
 * @param {Element[]} targets
 * @return {Element[]}
 */
const correctTargets = (parent, targets) => {
  if (!isElement(parent)) {
    logError(parent, 'is not a valid element');
    return [];
  }

  return targets
    .map((target) => {
      if (!isElement(target)) {
        logError(target, 'is not a valid element');
        return null;
      }

      let node = target;
      while (node && node !== parent) {
        if (parent.contains(node)) {
          return target;
        }
        node = node.getRootNode().host;
      }

      logError(target, 'is not contained inside', parent);
      return null;
    })
    .filter((x) => Boolean(x));
};

/**
 * Marks everything except given node(or nodes) as aria-hidden
 * @param {Element | Element[]} originalTarget - elements to keep on the page
 * @param {HTMLElement} [parentNode] - top element, defaults to document.body
 * @param {String} [markerName] - a special attribute to mark every node
 * @param {String} [controlAttribute] - html Attribute to control
 * @return {Function}
 */
const applyAttributeToOthers = (originalTarget, parentNode, markerName, controlAttribute) => {
  const targets = correctTargets(parentNode, Array.isArray(originalTarget) ? originalTarget : [originalTarget]);

  if (!markerMap[markerName]) {
    markerMap[markerName] = new WeakMap();
  }

  const markerCounter = markerMap[markerName];

  /** @type {Element[]} */
  const hiddenNodes = [];

  /** @type {Set<Node>} */
  const elementsToKeep = new Set();

  /** @type {Set<Node>} */
  const elementsToStop = new Set(targets);

  /**
   * @param {?Node} el
   */
  const keep = (el) => {
    if (!el || elementsToKeep.has(el)) {
      return;
    }

    elementsToKeep.add(el);

    const slot = el.assignedSlot;
    if (slot) {
      keep(slot);
    }

    keep(el.parentNode || el.host);
  };

  targets.forEach(keep);

  /**
   * @param {?Node} el
   */
  const deep = (parent) => {
    if (!parent || elementsToStop.has(parent)) {
      return;
    }

    const root = parent.shadowRoot;
    const children = root ? [...parent.children, ...root.children] : [...parent.children];
    children.forEach((node) => {
      // Skip elements that don't need to be hidden
      if (['template', 'script', 'style'].includes(node.localName)) {
        return;
      }

      if (elementsToKeep.has(node)) {
        deep(node);
      } else {
        const attr = node.getAttribute(controlAttribute);
        const alreadyHidden = attr !== null && attr !== 'false';
        const counterValue = (counterMap.get(node) || 0) + 1;
        const markerValue = (markerCounter.get(node) || 0) + 1;

        counterMap.set(node, counterValue);
        markerCounter.set(node, markerValue);
        hiddenNodes.push(node);

        if (counterValue === 1 && alreadyHidden) {
          uncontrolledNodes.set(node, true);
        }

        if (markerValue === 1) {
          node.setAttribute(markerName, 'true');
        }

        if (!alreadyHidden) {
          node.setAttribute(controlAttribute, 'true');
        }
      }
    });
  };

  deep(parentNode);

  elementsToKeep.clear();

  lockCount += 1;

  return () => {
    hiddenNodes.forEach((node) => {
      const counterValue = counterMap.get(node) - 1;
      const markerValue = markerCounter.get(node) - 1;

      counterMap.set(node, counterValue);
      markerCounter.set(node, markerValue);

      if (!counterValue) {
        if (uncontrolledNodes.has(node)) {
          uncontrolledNodes.delete(node);
        } else {
          node.removeAttribute(controlAttribute);
        }
      }

      if (!markerValue) {
        node.removeAttribute(markerName);
      }
    });

    lockCount -= 1;

    if (!lockCount) {
      // clear
      counterMap = new WeakMap();
      counterMap = new WeakMap();
      uncontrolledNodes = new WeakMap();
      markerMap = {};
    }
  };
};

/**
 * Marks everything except given node(or nodes) as aria-hidden
 * @param {Element | Element[]} originalTarget - elements to keep on the page
 * @param {HTMLElement} [parentNode] - top element, defaults to document.body
 * @param {String} [markerName] - a special attribute to mark every node
 * @return {Function} undo command
 */
export const hideOthers = (originalTarget, parentNode = document.body, markerName = 'data-aria-hidden') => {
  const targets = Array.from(Array.isArray(originalTarget) ? originalTarget : [originalTarget]);

  if (parentNode) {
    // We should not hide ariaLive elements - https://github.com/theKashey/aria-hidden/issues/10
    targets.push(...Array.from(parentNode.querySelectorAll('[aria-live]')));
  }

  return applyAttributeToOthers(targets, parentNode, markerName, 'aria-hidden');
};

/**
 * Marks everything except given node(or nodes) as inert
 * @param {Element | Element[]} originalTarget - elements to keep on the page
 * @param {HTMLElement} [parentNode] - top element, defaults to document.body
 * @param {String} [markerName] - a special attribute to mark every node
 * @return {Function} undo command
 */
export const inertOthers = (originalTarget, parentNode = document.body, markerName = 'data-inert-ed') => {
  return applyAttributeToOthers(originalTarget, parentNode, markerName, 'inert');
};

/**
 * @return if current browser supports inert
 */
export const supportsInert = 'inert' in HTMLElement.prototype;

/**
 * Automatic function to "suppress" DOM elements - _hide_ or _inert_ in the best possible way
 * @param {Element | Element[]} originalTarget - elements to keep on the page
 * @param {HTMLElement} [parentNode] - top element, defaults to document.body
 * @param {String} [markerName] - a special attribute to mark every node
 * @return {Function} undo command
 */
export const suppressOthers = (originalTarget, parentNode, markerName) =>
  (supportsInert ? inertOthers : hideOthers)(originalTarget, parentNode, markerName);
