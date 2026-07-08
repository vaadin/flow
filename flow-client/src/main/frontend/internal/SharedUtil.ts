/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// TypeScript port of the client-used part of
// com.vaadin.flow.shared.util.SharedUtil (the GET-parameter helpers), built
// alongside the Java version.

/**
 * Adds the given query parameters to a URI, before any fragment. Mirrors
 * SharedUtil.addGetParameters.
 */
export function addGetParameters(uri: string, extraParams: string | null): string {
  if (extraParams === null || extraParams.length === 0) {
    return uri;
  }
  // RFC 3986: the query starts at the first "?" and ends at "#" or the URI end.
  let base = uri;
  let fragment: string | null = null;
  const hashPosition = base.indexOf('#');
  if (hashPosition !== -1) {
    fragment = base.substring(hashPosition);
    base = base.substring(0, hashPosition);
  }

  base += base.includes('?') ? '&' : '?';
  base += extraParams;

  if (fragment !== null) {
    base += fragment;
  }
  return base;
}

/**
 * Adds a single `parameter=value` query parameter to a URI. Mirrors
 * SharedUtil.addGetParameter.
 */
export function addGetParameter(uri: string, parameter: string, value: string | number): string {
  return addGetParameters(uri, `${parameter}=${value}`);
}
