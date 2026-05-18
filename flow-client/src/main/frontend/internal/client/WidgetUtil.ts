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
 * Browser-touching helpers from `com.vaadin.client.WidgetUtil`. Reached from
 * GWT code via the `NativeWidgetUtil` JsType shim. Pure-Java helpers
 * (`refresh`, `getAbsoluteUrl`, `updateAttribute`, the `toPrettyJson` wrapper,
 * and Java-aware `equals`) stay in `WidgetUtil.java`.
 */
export const WidgetUtil = {
  redirect(url: string | null): void {
    if (url) {
      globalThis.location.assign(url);
    } else {
      globalThis.location.reload();
    }
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

  toPrettyJsonJsni(value: unknown): string {
    return JSON.stringify(value, (key, v) => (key === '$H' ? undefined : v), 4);
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
