/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { removeAriaIDReference, restoreGeneratedAriaIDReference, setAriaIDReference } from './aria-id-reference.js';

/**
 * A controller for managing ARIA attributes for a field element:
 * either the component itself or slotted `<input>` element.
 */
export class FieldAriaController {
  constructor(host) {
    this.host = host;
    this.__required = false;
  }

  /**
   * Sets a target element to which ARIA attributes are added.
   *
   * @param {HTMLElement} target
   */
  setTarget(target) {
    this.__target = target;
    this.__setAriaRequiredAttribute(this.__required);
    // We need to make sure that value in __labelId is stored
    this.__setLabelIdToAriaAttribute(this.__labelId, this.__labelId);
    if (this.__labelIdFromUser != null) {
      this.__setLabelIdToAriaAttribute(this.__labelIdFromUser, this.__labelIdFromUser, true);
    }
    this.__setErrorIdToAriaAttribute(this.__errorId);
    this.__setHelperIdToAriaAttribute(this.__helperId);
    this.setAriaLabel(this.__label);
  }

  /**
   * Toggles the `aria-required` attribute on the target element
   * if the target is the host component (e.g. a field group).
   * Otherwise, it does nothing.
   *
   * @param {boolean} required
   */
  setRequired(required) {
    this.__setAriaRequiredAttribute(required);
    this.__required = required;
  }

  /**
   * Defines the `aria-label` attribute of the target element.
   *
   * To remove the attribute, pass `null` as `label`.
   *
   * @param {string | null | undefined} label
   */
  setAriaLabel(label) {
    this.__setAriaLabelToAttribute(label);
    this.__label = label;
  }

  /**
   * Links the target element with a slotted label element
   * via the target's attribute `aria-labelledby`.
   *
   * To unlink the previous slotted label element, pass `null` as `labelId`.
   *
   * @param {string | null} labelId
   */
  setLabelId(labelId, fromUser = false) {
    const oldLabelId = fromUser ? this.__labelIdFromUser : this.__labelId;
    this.__setLabelIdToAriaAttribute(labelId, oldLabelId, fromUser);
    if (fromUser) {
      this.__labelIdFromUser = labelId;
    } else {
      this.__labelId = labelId;
    }
  }

  /**
   * Links the target element with a slotted error element via the target's attribute:
   * - `aria-labelledby` if the target is the host component (e.g a field group).
   * - `aria-describedby` otherwise.
   *
   * To unlink the previous slotted error element, pass `null` as `errorId`.
   *
   * @param {string | null} errorId
   */
  setErrorId(errorId) {
    this.__setErrorIdToAriaAttribute(errorId, this.__errorId);
    this.__errorId = errorId;
  }

  /**
   * Links the target element with a slotted helper element via the target's attribute:
   * - `aria-labelledby` if the target is the host component (e.g a field group).
   * - `aria-describedby` otherwise.
   *
   * To unlink the previous slotted helper element, pass `null` as `helperId`.
   *
   * @param {string | null} helperId
   */
  setHelperId(helperId) {
    this.__setHelperIdToAriaAttribute(helperId, this.__helperId);
    this.__helperId = helperId;
  }

  /**
   * @param {string | null | undefined} label
   * @private
   * */
  __setAriaLabelToAttribute(label) {
    if (!this.__target) {
      return;
    }
    if (label) {
      removeAriaIDReference(this.__target, 'aria-labelledby');
      this.__target.setAttribute('aria-label', label);
    } else if (this.__label) {
      restoreGeneratedAriaIDReference(this.__target, 'aria-labelledby');
      this.__target.removeAttribute('aria-label');
    }
  }

  /**
   * @param {string | null | undefined} labelId
   * @param {string | null | undefined} oldLabelId
   * @param {boolean | null | undefined} fromUser
   * @private
   */
  __setLabelIdToAriaAttribute(labelId, oldLabelId, fromUser) {
    setAriaIDReference(this.__target, 'aria-labelledby', { newId: labelId, oldId: oldLabelId, fromUser });
  }

  /**
   * @param {string | null | undefined} errorId
   * @param {string | null | undefined} oldErrorId
   * @private
   */
  __setErrorIdToAriaAttribute(errorId, oldErrorId) {
    setAriaIDReference(this.__target, 'aria-describedby', { newId: errorId, oldId: oldErrorId, fromUser: false });
  }

  /**
   * @param {string | null | undefined} helperId
   * @param {string | null | undefined} oldHelperId
   * @private
   */
  __setHelperIdToAriaAttribute(helperId, oldHelperId) {
    setAriaIDReference(this.__target, 'aria-describedby', { newId: helperId, oldId: oldHelperId, fromUser: false });
  }

  /**
   * @param {boolean} required
   * @private
   */
  __setAriaRequiredAttribute(required) {
    if (!this.__target) {
      return;
    }

    if (['input', 'textarea'].includes(this.__target.localName)) {
      // Native <input> or <textarea>, required is enough
      return;
    }

    if (required) {
      this.__target.setAttribute('aria-required', 'true');
    } else {
      this.__target.removeAttribute('aria-required');
    }
  }
}
