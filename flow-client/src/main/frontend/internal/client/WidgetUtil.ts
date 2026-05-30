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

const ABSOLUTE_URL_RE = /^(?:[a-zA-Z]+:)?\/\//;

/**
 * Browser-touching helpers from `com.vaadin.client.WidgetUtil`. The Java
 * class is a pure `@JsType(isNative=true)` facade onto this module.
 */
export const WidgetUtil = {
  refresh(): void {
    WidgetUtil.redirect(null);
  },

  redirect(url: string | null): void {
    if (url) {
      globalThis.location.assign(url);
    } else {
      globalThis.location.reload();
    }
  },

  getAbsoluteUrl(url: string): string {
    // Same trick the JVM-side helper used: an anchor's href getter returns
    // the URL resolved against the current document base, even if the
    // original input was relative.
    const a = document.createElement('a');
    a.href = url;
    return a.href;
  },

  isAbsoluteUrl(url: string): boolean {
    return ABSOLUTE_URL_RE.test(url);
  },

  crazyJsCast<T>(value: unknown): T {
    return value as T;
  },

  crazyJsoCast<T>(value: unknown): T {
    return value as T;
  },

  toPrettyJson(value: unknown): string {
    return WidgetUtil.toPrettyJsonJsni(value);
  },

  toPrettyJsonJsni(value: unknown): string {
    return JSON.stringify(value, (key, v) => (key === '$H' ? undefined : v), 4);
  },

  updateAttribute(element: Element, attribute: string, value: string | null): void {
    if (value === null) {
      element.removeAttribute(attribute);
    } else {
      element.setAttribute(attribute, value);
    }
  },

  equals(a: unknown, b: unknown): boolean {
    // Mirrors Objects.equals + a JS-loose fallback for the rare case the
    // Java caller compares JSO references whose `equals` is identity-only.
    if (a === b) return true;
    if (a == null || b == null) return false;

    return a == b;
  },

  setJsProperty(object: Record<string, unknown>, name: string, value: unknown): void {
    object[name] = value;
  },

  getJsProperty(object: Record<string, unknown>, name: string): unknown {
    return object[name];
  },

  hasOwnJsProperty(object: object, name: string): boolean {
    return Object.prototype.hasOwnProperty.call(object, name);
  },

  hasJsProperty(object: object, name: string): boolean {
    return name in object;
  },

  isUndefined(property: unknown): boolean {
    return property === undefined;
  },

  deleteJsProperty(object: Record<string, unknown>, name: string): void {
    // eslint-disable-next-line @typescript-eslint/no-dynamic-delete
    delete object[name];
  },

  createJsonObjectWithoutPrototype(): object {
    return Object.create(null);
  },

  createJsonObject(): object {
    return {};
  },

  isTrueish(value: unknown): boolean {
    return !!value;
  },

  getKeys(value: object): string[] {
    return Object.keys(value);
  },

  stringify(payload: unknown): string {
    return JSON.stringify(payload, (_key, value) => {
      if (value instanceof Node) {
        throw new Error(
          'Message JsonObject contained a dom node reference which should not be sent to the server and can cause a cyclic dependecy.'
        );
      }
      return value;
    });
  },

  equalsInJS(a: unknown, b: unknown): boolean {
    // Intentionally uses loose equality to mirror the original JSNI behavior.
    return a == b;
  }
};
