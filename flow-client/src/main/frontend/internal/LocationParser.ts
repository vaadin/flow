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

// Utility for parsing the document URL, migrated wholesale from
// LocationParser.java (which had no Java callers, so the class and its JUnit
// test were removed rather than left as a delegating shell).

/**
 * Gets the value of the given parameter from the given search (query) string.
 *
 * @param search the search string, including the leading '?'
 * @param parameter the parameter to retrieve
 * @returns the value of the parameter, an empty string if it is present without
 *   a value, or null if it is not present
 */
export function getParameter(search: string, parameter: string): string | null {
  const keyValues = search.substring(1).split('&');
  for (const keyValue of keyValues) {
    // Split on the first '=' only, matching Java's split("=", 2).
    const eq = keyValue.indexOf('=');
    const key = eq === -1 ? keyValue : keyValue.substring(0, eq);
    if (key === parameter) {
      return eq === -1 ? '' : keyValue.substring(eq + 1);
    }
  }
  return null;
}
