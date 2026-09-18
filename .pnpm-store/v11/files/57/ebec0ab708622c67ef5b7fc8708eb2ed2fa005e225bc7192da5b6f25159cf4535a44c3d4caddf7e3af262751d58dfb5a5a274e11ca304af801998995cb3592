/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

// Based on https://github.com/jouni/j-elements/blob/main/test/old-components/Stylable.js

/**
 * Check if the media query is a non-standard "tag scoped selector".
 *
 * Examples of such media queries:
 * - `@media vaadin-text-field { ... }`
 * - `@import "styles.css" vaadin-text-field`.
 *
 * @param {string} media
 * @return {boolean}
 */
function isTagScopedMedia(media) {
  return /^\w+(-\w+)+$/u.test(media);
}

/**
 * Check if the media query string matches the given tag name.
 *
 * @param {string} media
 * @param {string} tagName
 * @return {boolean}
 */
function matchesTagScopedMedia(media, tagName) {
  return media === tagName;
}

/**
 * Recursively processes a style sheet for matching "tag scoped" rules.
 *
 * @param {CSSStyleSheet} styleSheet
 * @param {string} tagName
 */
function extractStyleSheetTagScopedCSSRules(styleSheet, tagName) {
  const matchingRules = [];

  for (const rule of styleSheet.cssRules) {
    const ruleType = rule.constructor.name;

    if (ruleType === 'CSSImportRule') {
      if (!isTagScopedMedia(rule.media.mediaText)) {
        matchingRules.push(...extractStyleSheetTagScopedCSSRules(rule.styleSheet, tagName));
        continue;
      }

      if (matchesTagScopedMedia(rule.media.mediaText, tagName)) {
        matchingRules.push(...rule.styleSheet.cssRules);
      }
    }

    if (ruleType === 'CSSMediaRule') {
      if (matchesTagScopedMedia(rule.media.mediaText, tagName)) {
        matchingRules.push(...rule.cssRules);
      }
    }
  }

  return matchingRules;
}

/**
 * Recursively processes style sheets of the specified root element, including both
 * `adoptedStyleSheets` and regular `styleSheets`, and returns all CSS rules from
 * `@media` and `@import` blocks where the media query is (a) "tag scoped selector",
 * and (b) matches the specified tag name.
 *
 * Examples of such media queries:
 * - `@media vaadin-text-field { ... }`
 * - `@import "styles.css" vaadin-text-field`
 *
 * The returned rules are ordered in the same way as they are in the original stylesheet.
 *
 * @param {DocumentOrShadowRoot} root
 * @param {string} tagName
 * @return {CSSRule[]}
 */
export function extractTagScopedCSSRules(root, tagName) {
  const styleSheets = new Set([...root.styleSheets]);
  const adoptedStyleSheets = new Set([...root.adoptedStyleSheets]);

  return [...styleSheets.union(adoptedStyleSheets)].flatMap((styleSheet) => {
    return extractStyleSheetTagScopedCSSRules(styleSheet, tagName);
  });
}
