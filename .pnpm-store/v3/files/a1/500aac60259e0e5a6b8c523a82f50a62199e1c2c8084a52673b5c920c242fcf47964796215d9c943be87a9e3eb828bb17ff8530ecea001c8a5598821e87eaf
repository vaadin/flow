/**
 * @license
 * Copyright (c) 2023 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import {
  addValueToAttribute,
  deserializeAttributeValue,
  removeValueFromAttribute,
  serializeAttributeValue,
} from '@vaadin/component-base/src/dom-utils.js';

const attributeToTargets = new Map();

/**
 * Gets or creates a Set with the stored values for each element controlled by this helper
 *
 * @param {string} attr the attribute name used as key in the map
 *
 * @returns {WeakMap<HTMLElement, Set<string>} a weak map with the stored values for the elements being controlled by the helper
 */
function getAttrMap(attr) {
  if (!attributeToTargets.has(attr)) {
    attributeToTargets.set(attr, new WeakMap());
  }
  return attributeToTargets.get(attr);
}

/**
 * Cleans the values set on the attribute to the given element.
 * It also stores the current values in the map, if `storeValue` is `true`.
 *
 * @param {HTMLElement} target
 * @param {string} attr the attribute to be cleared
 * @param {boolean} storeValue whether or not the current value of the attribute should be stored on the map
 * @returns
 */
function cleanAriaIDReference(target, attr) {
  if (!target) {
    return;
  }

  target.removeAttribute(attr);
}

/**
 * Storing values of the accessible attributes in a Set inside of the WeakMap.
 *
 * @param {HTMLElement} target
 * @param {string} attr the attribute to be stored
 */
function storeAriaIDReference(target, attr) {
  if (!target || !attr) {
    return;
  }
  const attributeMap = getAttrMap(attr);
  if (attributeMap.has(target)) {
    return;
  }
  const values = deserializeAttributeValue(target.getAttribute(attr));
  attributeMap.set(target, new Set(values));
}

/**
 * Restores the generated values of the attribute to the given element.
 *
 * @param {HTMLElement} target
 * @param {string} attr
 */
export function restoreGeneratedAriaIDReference(target, attr) {
  if (!target || !attr) {
    return;
  }
  const attributeMap = getAttrMap(attr);
  const values = attributeMap.get(target);
  if (!values || values.size === 0) {
    target.removeAttribute(attr);
  } else {
    addValueToAttribute(target, attr, serializeAttributeValue(values));
  }
  attributeMap.delete(target);
}

/**
 * Sets a new ID reference for a target element and an ARIA attribute.
 *
 * @typedef {Object} AriaIdReferenceConfig
 * @property {string | null | undefined} newId
 * @property {string | null | undefined} oldId
 * @property {boolean | null | undefined} fromUser
 * @param {HTMLElement} target
 * @param {string} attr
 * @param {AriaIdReferenceConfig | null | undefined} config
 * @param config.newId The new ARIA ID reference to set. If `null`, the attribute is removed,
 * and `config.fromUser` is true, any stored values are restored. If there are stored values
 * and `config.fromUser` is `false`, then `config.newId` is added to the stored values set.
 * @param config.oldId The ARIA ID reference to be removed from the attribute. If there are
 * stored values and `config.fromUser` is `false`, then `config.oldId` is removed from the
 * stored values set.
 * @param config.fromUser Indicates whether the function is called by the user or internally.
 * When `config.fromUser` is called with `true` for the first time, the function will clear
 * and store the attribute value for the given element.
 */
export function setAriaIDReference(target, attr, config = { newId: null, oldId: null, fromUser: false }) {
  if (!target || !attr) {
    return;
  }

  const { newId, oldId, fromUser } = config;

  const attributeMap = getAttrMap(attr);
  const storedValues = attributeMap.get(target);

  if (!fromUser && !!storedValues) {
    // If there's any stored values, it means the attribute is being handled by the user
    // Replace the "oldId" with "newId" on the stored values set and leave
    oldId && storedValues.delete(oldId);
    newId && storedValues.add(newId);
    return;
  }

  if (fromUser) {
    if (!storedValues) {
      // If it's called from user and there's no stored values for the attribute,
      // then store the current value
      storeAriaIDReference(target, attr);
    } else if (!newId) {
      // If called from user with newId == null, it means the attribute will no longer
      // be in control of the user and the stored values should be restored
      // Removing the entry on the map for this target
      attributeMap.delete(target);
    }

    // If it's from user, then clear the attribute value before setting newId
    cleanAriaIDReference(target, attr);
  }

  removeValueFromAttribute(target, attr, oldId);

  const attributeValue = !newId ? serializeAttributeValue(storedValues) : newId;
  if (attributeValue) {
    addValueToAttribute(target, attr, attributeValue);
  }
}

/**
 * Removes the {@link attr | attribute} value of the given {@link target} element.
 * It also stores the current value, if no stored values are present.
 *
 * @param {HTMLElement} target
 * @param {string} attr
 */
export function removeAriaIDReference(target, attr) {
  storeAriaIDReference(target, attr);
  cleanAriaIDReference(target, attr);
}
