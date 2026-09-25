/**
 * @license
 * Copyright (c) 2000 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { issueWarning } from '@vaadin/component-base/src/warnings.js';

/** @type {WeakMap<CSSStyleSheet, Record<string, Map>>} */
const cache = new WeakMap();

function getRuleMediaText(rule) {
  try {
    return rule.media.mediaText;
  } catch {
    issueWarning(
      '[LumoInjector] Browser denied to access property "mediaText" for some CSS rules, so they were skipped.',
    );
    return '';
  }
}

function getStyleSheetRules(styleSheet) {
  try {
    return styleSheet.cssRules;
  } catch {
    issueWarning(
      '[LumoInjector] Browser denied to access property "cssRules" for some CSS stylesheets, so they were skipped.',
    );
    return [];
  }
}

function parseStyleSheet(
  styleSheet,
  result = {
    tags: new Map(),
    modules: new Map(),
  },
) {
  for (const rule of getStyleSheetRules(styleSheet)) {
    if (rule instanceof CSSImportRule) {
      const mediaText = getRuleMediaText(rule);
      if (mediaText.startsWith('lumo_')) {
        result.modules.set(mediaText, [...rule.styleSheet.cssRules]);
      } else {
        parseStyleSheet(rule.styleSheet, result);
      }

      continue;
    }

    if (rule instanceof CSSMediaRule) {
      const mediaText = getRuleMediaText(rule);
      if (mediaText.startsWith('lumo_')) {
        result.modules.set(mediaText, [...rule.cssRules]);
      }

      continue;
    }

    if (rule instanceof CSSStyleRule && rule.cssText.includes('-inject')) {
      for (const property of rule.style) {
        const tagName = property.match(/^--_lumo-(.*)-inject-modules$/u)?.[1];
        if (!tagName) {
          continue;
        }

        const value = rule.style.getPropertyValue(property);

        result.tags.set(
          tagName,
          value.split(',').map((module) => module.trim().replace(/'|"/gu, '')),
        );
      }

      continue;
    }
  }

  return result;
}

/**
 * Parses the provided CSSStyleSheet objects and returns an object with
 * tag-to-modules mappings and module rules.
 *
 * Modules are defined using CSS media rules with names starting with `lumo_`:
 *
 * ```css
 * \@media lumo_base-field {
 *  #label {
 *    color: gray;
 *  }
 * }
 *
 * \@media lumo_text-field {
 *   #input {
 *     color: yellow;
 *   }
 * }
 * ```
 *
 * Also, an entire CSS import can be defined as a module:
 *
 * ```css
 * \@import 'lumo-base-field.css' lumo_base-field;
 * ```
 *
 * Tag-to-modules mappings are defined as CSS custom properties that list
 * the module names to be applied to specific tags, e.g. `vaadin-text-field`:
 *
 * ```css
 * html {
 *   --_lumo-vaadin-text-field-inject-modules:
 *      lumo_base-field,
 *      lumo_text-field;
 * }
 * ```
 *
 * Example output:
 *
 * ```js
 * {
 *   tags: Map {
 *    'vaadin-text-field': ['lumo_base-field', 'lumo_text-field']
 *   },
 *   modules: Map {
 *     'lumo_base-field': [CSSStyleRule],
 *     'lumo_text-field': [CSSStyleRule]
 *   }
 * }
 * ```
 *
 * @param {CSSStyleSheet[]} styleSheets - An array of CSSStyleSheet objects to parse.
 * @return {{tags: Map<string, string[]>, modules: Map<string, CSSRule[]>}}
 */
export function parseStyleSheets(styleSheets) {
  let tags = new Map();
  let modules = new Map();

  for (const styleSheet of styleSheets) {
    let result = cache.get(styleSheet);
    if (!result) {
      result = parseStyleSheet(styleSheet);
      cache.set(styleSheet, result);
    }

    tags = new Map([...tags, ...result.tags]);
    modules = new Map([...modules, ...result.modules]);
  }

  return { tags, modules };
}
