/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Passes the component to the template renderer callback if the template renderer is imported.
 * Otherwise, if there is a template, it warns that the template renderer needs to be imported.
 *
 * @param {HTMLElement} component
 */
export function processTemplates(component) {
  if (window.Vaadin && window.Vaadin.templateRendererCallback) {
    window.Vaadin.templateRendererCallback(component);
    return;
  }

  if (component.querySelector('template')) {
    console.warn(
      `WARNING: <template> inside <${component.localName}> is no longer supported. Import @vaadin/polymer-legacy-adapter/template-renderer.js to enable compatibility.`,
    );
  }
}
