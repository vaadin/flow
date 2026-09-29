/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
export { registerStyles, css, unsafeCSS } from './vaadin-themable-mixin.js';

/**
 * This is for use internally by Lumo and Material styles.
 *
 * @param {string} id the id to set on the created element, only for informational purposes
 * @param  {CSSResultGroup[]} styles the styles to add
 */
export const addGlobalThemeStyles = (id, ...styles) => {
  const styleTag = document.createElement('style');
  styleTag.id = id;
  styleTag.textContent = styles
    .map((style) => style.toString())
    .join('\n')
    .replace(':host', 'html');

  document.head.insertAdjacentElement('afterbegin', styleTag);
};
