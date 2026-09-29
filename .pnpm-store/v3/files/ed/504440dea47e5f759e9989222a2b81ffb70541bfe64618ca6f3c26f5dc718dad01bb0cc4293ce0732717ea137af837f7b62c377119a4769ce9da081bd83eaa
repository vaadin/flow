/**
 * @license
 * Copyright (c) 2023 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Checks if one set of URL parameters contains all the parameters
 * with the same values from another set.
 *
 * @param {URLSearchParams} actual
 * @param {URLSearchParams} expected
 */
function containsQueryParams(actual, expected) {
  return [...expected.entries()].every(([key, value]) => {
    return actual.getAll(key).includes(value);
  });
}

/**
 * Checks if two paths match based on their origin, pathname, and query parameters.
 *
 * The function matches an actual URL against an expected URL to see if they share
 * the same base origin (like https://example.com), the same path (like /path/to/page),
 * and if the actual URL contains at least all the query parameters with the same values
 * from the expected URL.
 *
 * @param {string} actual The actual URL to match.
 * @param {string} expected The expected URL to match.
 * @param {Object} matchOptions Options for path matching.
 */
export function matchPaths(actual, expected, matchOptions = { matchNested: false }) {
  const base = document.baseURI;
  const actualUrl = new URL(actual, base);
  const expectedUrl = new URL(expected, base);

  const matchesOrigin = actualUrl.origin === expectedUrl.origin;
  const matchesPath = matchOptions.matchNested
    ? actualUrl.pathname === expectedUrl.pathname || actualUrl.pathname.startsWith(`${expectedUrl.pathname}/`)
    : actualUrl.pathname === expectedUrl.pathname;

  return matchesOrigin && matchesPath && containsQueryParams(actualUrl.searchParams, expectedUrl.searchParams);
}
