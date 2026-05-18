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

interface HtmlImportsGlobal {
  HTMLImports?: { whenReady: (cb: () => void) => void };
}

interface LoadableElement extends Element {
  onload?: ((event?: Event) => void) | null;
  onerror?: ((event?: Event) => void) | null;
  onreadystatechange?: (() => void) | null;
  readyState?: string;
}

/**
 * Browser-touching helpers migrated from `com.vaadin.client.ResourceLoader`.
 * Reached from GWT code via the `NativeResourceLoader` JsType shim. The
 * registry-driven loading orchestration in `ResourceLoader.java` stays Java —
 * only the JSNI bodies move.
 */
export const ResourceLoader = {
  supportsHtmlWhenReady(): boolean {
    const g = globalThis as unknown as HtmlImportsGlobal;
    return !!g.HTMLImports?.whenReady;
  },

  addHtmlImportsReadyHandler(handler: () => void): void {
    const g = globalThis as unknown as HtmlImportsGlobal;
    g.HTMLImports?.whenReady(() => handler());
  },

  addOnloadHandler(element: Element, onLoad: () => void, onError: () => void): void {
    const elem = element as LoadableElement;
    elem.onload = () => {
      elem.onload = null;
      elem.onerror = null;
      elem.onreadystatechange = null;
      onLoad();
    };
    elem.onerror = () => {
      elem.onload = null;
      elem.onerror = null;
      elem.onreadystatechange = null;
      onError();
    };
    elem.onreadystatechange = () => {
      if (elem.readyState === 'loaded' || elem.readyState === 'complete') {
        elem.onload?.();
      }
    };
  },

  getStyleSheetLength(url: string): number {
    for (const sheet of Array.from(document.styleSheets) as CSSStyleSheet[]) {
      if (sheet.href === url) {
        try {
          const rules = sheet.cssRules ?? (sheet as CSSStyleSheet & { rules?: CSSRuleList }).rules;
          if (rules == null) {
            // Stylesheet loaded but not introspectable (e.g. CORS) — assume there is content
            return 1;
          }
          return rules.length;
        } catch {
          return 1;
        }
      }
    }
    return -1;
  },

  runPromiseExpression(
    expression: string,
    promiseSupplier: () => unknown,
    onSuccess: () => void,
    onError: () => void
  ): void {
    try {
      const promise = promiseSupplier();
      if (!(promise instanceof Promise)) {
        throw new Error(`The expression "${expression}" result is not a Promise.`);
      }
      promise.then(
        () => onSuccess(),
        (err: unknown) => {
          globalThis.console.error(err);
          onError();
        }
      );
    } catch (err) {
      globalThis.console.error(err);
      onError();
    }
  }
};
