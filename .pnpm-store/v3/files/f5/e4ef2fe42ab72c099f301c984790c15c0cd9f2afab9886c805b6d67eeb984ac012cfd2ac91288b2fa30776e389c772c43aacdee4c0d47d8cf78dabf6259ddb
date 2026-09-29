/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { adoptStyles, css, CSSResult, LitElement, unsafeCSS } from 'lit';
import { ThemePropertyMixin } from './vaadin-theme-property-mixin.js';

export { css, unsafeCSS };

/**
 * @typedef {Object} Theme
 * @property {string} themeFor
 * @property {CSSResult[]} styles
 * @property {string | string[]} [include]
 * @property {string} [moduleId]
 *
 * @typedef {CSSResult[] | CSSResult} CSSResultGroup
 */

/**
 * @type {Theme[]}
 */
const themeRegistry = [];

/**
 * @type {WeakRef<HTMLElement>[]}
 */
const themableInstances = new Set();

/**
 * @type {string[]}
 */
const themableTagNames = new Set();

/**
 * Check if the custom element type has themes applied.
 * @param {Function} elementClass
 * @returns {boolean}
 */
function classHasThemes(elementClass) {
  return elementClass && Object.prototype.hasOwnProperty.call(elementClass, '__themes');
}

/**
 * Check if the custom element type has themes applied.
 * @param {string} tagName
 * @returns {boolean}
 */
function hasThemes(tagName) {
  return classHasThemes(customElements.get(tagName));
}

/**
 * Flattens the styles into a single array of styles.
 * @param {CSSResultGroup} styles
 * @param {CSSResult[]} result
 * @returns {CSSResult[]}
 */
function flattenStyles(styles = []) {
  return [styles].flat(Infinity).filter((style) => {
    if (style instanceof CSSResult) {
      return true;
    }
    console.warn('An item in styles is not of type CSSResult. Use `unsafeCSS` or `css`.');
    return false;
  });
}

/**
 * Returns true if the themeFor string matches the tag name
 * @param {string} themeFor
 * @param {string} tagName
 * @returns {boolean}
 */
function matchesThemeFor(themeFor, tagName) {
  return (themeFor || '').split(' ').some((themeForToken) => {
    return new RegExp(`^${themeForToken.split('*').join('.*')}$`, 'u').test(tagName);
  });
}

/**
 * Returns the CSS text content from an array of CSSResults
 * @param {CSSResult[]} styles
 * @returns {string}
 */
function getCssText(styles) {
  return styles.map((style) => style.cssText).join('\n');
}

const STYLE_ID = 'vaadin-themable-mixin-style';

/**
 * Includes the styles to the template.
 * @param {CSSResult[]} styles
 * @param {HTMLTemplateElement} template
 */
function addStylesToTemplate(styles, template) {
  const styleEl = document.createElement('style');
  styleEl.id = STYLE_ID;
  styleEl.textContent = getCssText(styles);
  template.content.appendChild(styleEl);
}

/**
 * Dynamically updates the styles of the given component instance.
 * @param {HTMLElement} instance
 */
function updateInstanceStyles(instance) {
  if (!instance.shadowRoot) {
    return;
  }

  const componentClass = instance.constructor;

  if (instance instanceof LitElement) {
    // LitElement

    // The adoptStyles function may fall back to appending style elements to shadow root.
    // Remove them first to avoid duplicates.
    [...instance.shadowRoot.querySelectorAll('style')].forEach((style) => style.remove());

    // Adopt the updated styles
    adoptStyles(instance.shadowRoot, componentClass.elementStyles);
  } else {
    // PolymerElement

    // Update style element content in the shadow root
    const style = instance.shadowRoot.getElementById(STYLE_ID);
    const template = componentClass.prototype._template;
    style.textContent = template.content.getElementById(STYLE_ID).textContent;
  }
}

/**
 * Dynamically updates the styles of the instances matching the given component type.
 * @param {Function} componentClass
 */
function updateInstanceStylesOfType(componentClass) {
  // Iterate over component instances and update their styles if needed
  themableInstances.forEach((ref) => {
    const instance = ref.deref();
    if (instance instanceof componentClass) {
      updateInstanceStyles(instance);
    } else if (!instance) {
      // Clean up the weak reference to a GC'd instance
      themableInstances.delete(ref);
    }
  });
}

/**
 * Dynamically updates the styles of the given component type.
 * @param {Function} componentClass
 */
function updateComponentStyles(componentClass) {
  if (componentClass.prototype instanceof LitElement) {
    // Update LitElement-based component's elementStyles
    componentClass.elementStyles = componentClass.finalizeStyles(componentClass.styles);
  } else {
    // Update Polymer-based component's template
    const template = componentClass.prototype._template;
    template.content.getElementById(STYLE_ID).textContent = getCssText(componentClass.getStylesForThis());
  }

  // Update the styles of inheriting types
  themableTagNames.forEach((inheritingTagName) => {
    const inheritingClass = customElements.get(inheritingTagName);
    if (inheritingClass !== componentClass && inheritingClass.prototype instanceof componentClass) {
      updateComponentStyles(inheritingClass);
    }
  });
}

/**
 * Check if the component type already has a style matching the given styles.
 *
 * @param {Function} componentClass
 * @param {CSSResultGroup} styles
 * @returns {boolean}
 */
function hasMatchingStyle(componentClass, styles) {
  const themes = componentClass.__themes;
  if (!themes || !styles) {
    return false;
  }

  return themes.some((theme) =>
    theme.styles.some((themeStyle) => styles.some((style) => style.cssText === themeStyle.cssText)),
  );
}

/**
 * Registers CSS styles for a component type. Make sure to register the styles before
 * the first instance of a component of the type is attached to DOM.
 *
 * @param {string} themeFor The local/tag name of the component type to register the styles for
 * @param {CSSResultGroup} styles The CSS style rules to be registered for the component type
 * matching themeFor and included in the local scope of each component instance
 * @param {{moduleId?: string, include?: string | string[]}} options Additional options
 * @return {void}
 */
export function registerStyles(themeFor, styles, options = {}) {
  styles = flattenStyles(styles);

  if (window.Vaadin && window.Vaadin.styleModules) {
    window.Vaadin.styleModules.registerStyles(themeFor, styles, options);
  } else {
    themeRegistry.push({
      themeFor,
      styles,
      include: options.include,
      moduleId: options.moduleId,
    });
  }

  if (themeFor) {
    // Update styles of the component types that match themeFor and have already been finalized
    themableTagNames.forEach((tagName) => {
      if (matchesThemeFor(themeFor, tagName) && hasThemes(tagName)) {
        const componentClass = customElements.get(tagName);

        if (hasMatchingStyle(componentClass, styles)) {
          // Show a warning if the component type already has some of the given styles
          console.warn(`Registering styles that already exist for ${tagName}`);
        } else if (!window.Vaadin || !window.Vaadin.suppressPostFinalizeStylesWarning) {
          // Show a warning if the component type has already been finalized
          console.warn(
            `The custom element definition for "${tagName}" ` +
              `was finalized before a style module was registered. ` +
              `Ideally, import component specific style modules before ` +
              `importing the corresponding custom element. ` +
              `This warning can be suppressed by setting "window.Vaadin.suppressPostFinalizeStylesWarning = true".`,
          );
        }

        // Update the styles of the component type
        updateComponentStyles(componentClass);
        // Update the styles of the component instances matching the component type
        updateInstanceStylesOfType(componentClass);
      }
    });
  }
}

/**
 * Returns all registered themes. By default the themeRegistry is returned as is.
 * In case the style-modules adapter is imported, the themes are obtained from there instead
 * @returns {Theme[]}
 */
function getAllThemes() {
  if (window.Vaadin && window.Vaadin.styleModules) {
    return window.Vaadin.styleModules.getAllThemes();
  }
  return themeRegistry;
}

/**
 * Maps the moduleName to an include priority number which is used for
 * determining the order in which styles are applied.
 * @param {string} moduleName
 * @returns {number}
 */
function getIncludePriority(moduleName = '') {
  let includePriority = 0;
  if (moduleName.startsWith('lumo-') || moduleName.startsWith('material-')) {
    includePriority = 1;
  } else if (moduleName.startsWith('vaadin-')) {
    includePriority = 2;
  }
  return includePriority;
}

/**
 * Gets an array of CSSResults matching the include property of the theme.
 * @param {Theme} theme
 * @returns {CSSResult[]}
 */
function getIncludedStyles(theme) {
  const includedStyles = [];
  if (theme.include) {
    [].concat(theme.include).forEach((includeModuleId) => {
      const includedTheme = getAllThemes().find((s) => s.moduleId === includeModuleId);
      if (includedTheme) {
        includedStyles.push(...getIncludedStyles(includedTheme), ...includedTheme.styles);
      } else {
        console.warn(`Included moduleId ${includeModuleId} not found in style registry`);
      }
    }, theme.styles);
  }
  return includedStyles;
}

/**
 * Returns an array of themes that should be used for styling a component matching
 * the tag name. The array is sorted by the include order.
 * @param {string} tagName
 * @returns {Theme[]}
 */
function getThemes(tagName) {
  const defaultModuleName = `${tagName}-default-theme`;

  const themes = getAllThemes()
    // Filter by matching themeFor properties
    .filter((theme) => theme.moduleId !== defaultModuleName && matchesThemeFor(theme.themeFor, tagName))
    .map((theme) => ({
      ...theme,
      // Prepend styles from included themes
      styles: [...getIncludedStyles(theme), ...theme.styles],
      // Map moduleId to includePriority
      includePriority: getIncludePriority(theme.moduleId),
    }))
    // Sort by includePriority
    .sort((themeA, themeB) => themeB.includePriority - themeA.includePriority);

  if (themes.length > 0) {
    return themes;
  }
  // No theme modules found, return the default module if it exists
  return getAllThemes().filter((theme) => theme.moduleId === defaultModuleName);
}

/**
 * @polymerMixin
 * @mixes ThemePropertyMixin
 */
export const ThemableMixin = (superClass) =>
  class VaadinThemableMixin extends ThemePropertyMixin(superClass) {
    constructor() {
      super();
      // Store a weak reference to the instance
      themableInstances.add(new WeakRef(this));
    }

    /**
     * Covers PolymerElement based component styling
     * @protected
     */
    static finalize() {
      super.finalize();

      if (this.is) {
        themableTagNames.add(this.is);
      }

      // Make sure not to run the logic intended for PolymerElement when LitElement is used.
      if (this.elementStyles) {
        return;
      }

      const template = this.prototype._template;
      if (!template || classHasThemes(this)) {
        return;
      }

      addStylesToTemplate(this.getStylesForThis(), template);
    }

    /**
     * Covers LitElement based component styling
     *
     * @protected
     */
    static finalizeStyles(styles) {
      // The "styles" object originates from the "static get styles()" function of
      // a LitElement based component. The theme styles are added after it
      // so that they can override the component styles.
      const themeStyles = this.getStylesForThis();
      return styles ? [...[styles].flat(Infinity), ...themeStyles] : themeStyles;
    }

    /**
     * Get styles for the component type
     *
     * @private
     */
    static getStylesForThis() {
      const superClassThemes = superClass.__themes || [];
      const parent = Object.getPrototypeOf(this.prototype);
      const inheritedThemes = (parent ? parent.constructor.__themes : []) || [];
      this.__themes = [...superClassThemes, ...inheritedThemes, ...getThemes(this.is)];
      const themeStyles = this.__themes.flatMap((theme) => theme.styles);
      // Remove duplicates
      return themeStyles.filter((style, index) => index === themeStyles.lastIndexOf(style));
    }
  };

export { themeRegistry as __themeRegistry };
