/**
 * @license
 * Copyright (c) 2023 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

export type AriaIDReferenceConfig = {
  newId: string | null;
  oldId: string | null;
  fromUser: boolean | null;
};

/**
 * Sets a new ID reference for a target element and an ARIA attribute.
 *
 * @param config.newId
 *  The new ARIA ID reference to set. If `null`, the attribute is removed,
 *  and `config.fromUser` is `true`, any stored values are restored. If there
 *  are stored values and `config.fromUser` is `false`, then `config.newId`
 *  is added to the stored values set.
 * @param config.oldId
 *  The ARIA ID reference to be removed from the attribute. If there are stored
 *  values and `config.fromUser` is `false`, then `config.oldId` is removed from
 *  the stored values set.
 * @param config.fromUser
 *  Indicates whether the function is called by the user or internally.
 *  When `config.fromUser` is called with `true` for the first time,
 *  the function will clear and store the attribute value for the given element.
 */
export function setAriaIDReference(target: HTMLElement, attr: string, config: AriaIDReferenceConfig): void;

/**
 * Removes the attribute value of the given target element.
 * It also stores the current value, if no stored values are present.
 */
export function removeAriaIDReference(target: HTMLElement, attr: string): void;

/**
 * Restores the generated values of the attribute to the given target element.
 */
export function restoreGeneratedAriaIDReference(target: HTMLElement, attr: string): void;
