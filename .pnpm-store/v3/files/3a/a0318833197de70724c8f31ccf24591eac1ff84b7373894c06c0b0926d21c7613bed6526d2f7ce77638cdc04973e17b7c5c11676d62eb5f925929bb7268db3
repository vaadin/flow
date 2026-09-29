/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Returns the actually focused element by traversing shadow
 * trees recursively to ensure it's the leaf element.
 */
export declare function getDeepActiveElement(): Element;

/**
 * Returns true if the window has received a keydown
 * event since the last mousedown event.
 */
export declare function isKeyboardActive(): boolean;

/**
 * Returns true if the element is hidden, false otherwise.
 *
 * An element is treated as hidden when any of the following conditions are met:
 * - the element itself or one of its ancestors has `display: none`.
 * - the element has or inherits `visibility: hidden`.
 */
export declare function isElementHidden(element: HTMLElement): boolean;

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
 */
export declare function isElementFocusable(element: HTMLElement): boolean;

/**
 * Returns true if the element is focused, false otherwise.
 */
export declare function isElementFocused(element: HTMLElement): boolean;

/**
 * Returns a tab-ordered array of focusable elements for a root element.
 * The resulting array will include the root element if it is focusable.
 *
 * The method traverses nodes in shadow DOM trees too if any.
 */
export declare function getFocusableElements(element: HTMLElement): HTMLElement[];
