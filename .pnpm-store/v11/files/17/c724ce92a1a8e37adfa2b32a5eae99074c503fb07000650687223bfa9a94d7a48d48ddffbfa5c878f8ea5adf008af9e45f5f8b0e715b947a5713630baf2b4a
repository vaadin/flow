/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { DomModule } from '@polymer/polymer/lib/elements/dom-module.js';
import { stylesFromTemplate } from '@polymer/polymer/lib/utils/style-gather.js';
import { unsafeCSS } from 'lit';
import { __themeRegistry as themeRegistry } from '@vaadin/vaadin-themable-mixin';

/**
 * @typedef CSSResult
 * @type {import('lit').CSSResult}
 *
 * @typedef DomModuleWithCachedStyles
 * @type {DomModule & {__allStyles?: CSSResult[], __partialStyles?: CSSResult[]}}
 */

let moduleIdIndex = 0;

/**
 * Registers CSS styles for a component type. Make sure to register the styles before
 * the first instance of a component of the type is attached to DOM.
 *
 * @param {string} themeFor The local/tag name of the component type to register the styles for
 * @param {CSSResult[]} styles The CSS style rules to be registered for the component type
 * matching themeFor and included in the local scope of each component instance
 * @param {{moduleId?: string, include?: string | string[]}} options Additional options
 * @return {void}
 */
function registerStyles(themeFor, styles = [], options = {}) {
  const themeId = options.moduleId || `custom-style-module-${moduleIdIndex}`;
  moduleIdIndex += 1;

  /** @type {DomModuleWithCachedStyles} */
  const module = document.createElement('dom-module');

  if (themeFor) {
    module.setAttribute('theme-for', themeFor);
  }

  // The styles array only needs to be included in the template in case options.moduleId is used,
  // so that it's possible to include the styles by moduleId in some other <dom-module>
  // (with <style include="module-id">)
  const includeStylesToTemplate = !!(styles.length && options.moduleId);

  // Options.include may be undefined, string or an array of strings. Convert it to an array
  const moduleIncludes = [].concat(options.include || []);
  if (moduleIncludes.length === 0) {
    // No includes are used so the styles array is considered complete and can be cached as is
    module.__allStyles = styles;
  } else if (!includeStylesToTemplate) {
    // Includes are used so the styles array can be cached,
    // but the included styles must be later added on top of it.
    // Don't cache anything in case the styles will get included in the
    // <dom-module> template anyways to avoid duplicate styles.
    module.__partialStyles = styles;
  }

  module.innerHTML = `
    <template>
      ${moduleIncludes.map((include) => `<style include=${include}></style>`)}
      ${includeStylesToTemplate ? `<style>${styles.map((style) => style.cssText).join('\n')}</style>` : ''}
    </template>
  `;

  module.register(themeId);
}

/**
 * Returns an array of CSS results obtained from the style module
 * @param {DomModule} module
 * @returns {CSSResult[]}
 */
function getModuleStyles(module) {
  return stylesFromTemplate(module.querySelector('template')).map((styleElement) => {
    return unsafeCSS(styleElement.textContent);
  });
}

/**
 * @typedef {Object} Theme
 * @property {string} themeFor
 * @property {CSSResult[]} styles
 * @property {string} [moduleId]
 */

/**
 * Returns all the registered dom-modules mapped as themable-mixin -compatible Theme objects
 * @returns {Theme[]}
 */
function getAllThemes() {
  const domModule = DomModule;
  const modules = domModule.prototype.modules;

  return Object.keys(modules).map((moduleId) => {
    /** @type {DomModuleWithCachedStyles} */
    const module = modules[moduleId];
    const themeFor = module.getAttribute('theme-for');
    if (!module.__allStyles) {
      module.__allStyles = getModuleStyles(module).concat(module.__partialStyles || []);
    }

    return {
      themeFor,
      moduleId,
      styles: module.__allStyles,
    };
  });
}

if (!window.Vaadin) {
  window.Vaadin = {};
}

window.Vaadin.styleModules = {
  getAllThemes,
  registerStyles,
};

// Convert any existing themes from the themable-mixin's themeRegistry to the style modules format
if (themeRegistry && themeRegistry.length > 0) {
  themeRegistry.forEach((theme) => {
    registerStyles(theme.themeFor, theme.styles, {
      moduleId: theme.moduleId,
      include: theme.include,
    });
  });
  // Clear the themeRegistry
  themeRegistry.length = 0;
}
