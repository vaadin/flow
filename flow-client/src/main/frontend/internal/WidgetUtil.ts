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

// Implementations migrated from WidgetUtil.java, registered on
// window.Vaadin.Flow.internal.WidgetUtil by registerInternals; the Java methods
// delegate here. This module is also bundled to ES5 for the (old) HtmlUnit used
// by GwtTests, so it avoids newer syntax and the unicode regex flag.

/**
 * Redirects the browser to the given URL, or reloads the page when `url` is
 * null.
 */
export function redirect(url: string | null): void {
  if (url) {
    window.location.href = url;
  } else {
    window.location.reload();
  }
}

/**
 * Resolves a relative URL to an absolute URL based on the current document's
 * location.
 */
export function getAbsoluteUrl(url: string): string {
  const anchor = document.createElement('a');
  anchor.href = url;
  return anchor.href;
}

/**
 * Detects whether a URL is absolute. URLs without a scheme but starting with
 * double slashes (e.g. `//myhost/path`) are considered absolute.
 */
export function isAbsoluteUrl(url: string): boolean {
  return /^(?:[a-zA-Z]+:)?\/\//.test(url);
}
