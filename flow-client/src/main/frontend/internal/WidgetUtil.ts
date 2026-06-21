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

/** Retrieves the value of a JavaScript property. */
export function getJsProperty(object: Record<string, unknown>, name: string): unknown {
  return object[name];
}

/** Assigns a value as a JavaScript property of an object. */
export function setJsProperty(object: Record<string, unknown>, name: string, value: unknown): void {
  object[name] = value;
}

/**
 * Checks whether the object itself has a JavaScript property with the given
 * name. Inherited properties are not taken into account.
 */
export function hasOwnJsProperty(object: object, name: string): boolean {
  return Object.prototype.hasOwnProperty.call(object, name);
}

/**
 * Checks whether the object has or inherits a JavaScript property with the
 * given name.
 */
export function hasJsProperty(object: object, name: string): boolean {
  return name in object;
}

/** Checks whether the value is explicitly undefined (null returns false). */
export function isUndefined(value: unknown): boolean {
  return value === undefined;
}

/** Removes a JavaScript property from an object. */
export function deleteJsProperty(object: Record<string, unknown>, name: string): void {
  // Dynamic delete is intentional: this helper removes an arbitrary property.
  // eslint-disable-next-line @typescript-eslint/no-dynamic-delete
  delete object[name];
}

/** Gets the boolean value of the given value based on JavaScript semantics. */
export function isTrueish(value: unknown): boolean {
  return !!value;
}

/** Gets all own enumerable JavaScript property names (Object.keys) of the object. */
export function getKeys(value: object): string[] {
  return Object.keys(value);
}

/** Creates a new object with the default JavaScript prototype. */
export function createJsonObject(): object {
  return {};
}

/**
 * Creates a new object without any JavaScript prototype. Relevant only for
 * objects displayed through the browser console.
 */
export function createJsonObjectWithoutPrototype(): object {
  return Object.create(null) as object;
}

/**
 * Checks whether the values are equal as JavaScript values, using JS `==`. This
 * ignores types, so e.g. an empty string equals 0.
 */
export function equalsInJS(obj1: unknown, obj2: unknown): boolean {
  // Loose equality is intentional here; that is the contract of this helper.
  return obj1 == obj2;
}
