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

// Implementations migrated from ResourceLoader.java, registered on
// window.Vaadin.Flow.internal.ResourceLoader by registerInternals; the Java
// methods delegate here. Callbacks are passed in already $entry-guarded by the
// Java caller. Also bundled to ES5 for the HtmlUnit used by GwtTests.

interface HtmlImports {
  whenReady: (callback: () => void) => void;
}

function htmlImports(): HtmlImports | undefined {
  return (window as unknown as { HTMLImports?: HtmlImports }).HTMLImports;
}

/** Whether the browser supports HTMLImports.whenReady. */
export function supportsHtmlWhenReady(): boolean {
  const imports = htmlImports();
  return !!(imports && imports.whenReady);
}

/** Registers a handler to run once HTML imports are ready. */
export function addHtmlImportsReadyHandler(handler: () => void): void {
  htmlImports()?.whenReady(handler);
}

/**
 * Wires onLoad/onError handlers on a link or script element, clearing all
 * handlers once one fires. Also handles the legacy onreadystatechange path.
 */
export function addOnloadHandler(element: Element, onLoad: () => void, onError: () => void): void {
  const el = element as unknown as {
    onload: ((event?: unknown) => void) | null;
    onerror: (() => void) | null;
    onreadystatechange: (() => void) | null;
    readyState?: string;
  };
  el.onload = () => {
    el.onload = null;
    el.onerror = null;
    el.onreadystatechange = null;
    onLoad();
  };
  el.onerror = () => {
    el.onload = null;
    el.onerror = null;
    el.onreadystatechange = null;
    onError();
  };
  el.onreadystatechange = () => {
    if (el.readyState === 'loaded' || el.readyState === 'complete') {
      el.onload?.();
    }
  };
}

/**
 * Returns the number of rules in the loaded stylesheet with the given href, 1
 * if loaded but the rules are inaccessible (cross-origin), or -1 if no matching
 * stylesheet is loaded yet.
 */
export function getStyleSheetLength(url: string): number {
  for (const styleSheet of Array.from(document.styleSheets)) {
    const sheet = styleSheet as StyleSheet & {
      cssRules?: { length: number } | null;
      rules?: { length: number } | null;
    };
    if (sheet.href === url) {
      try {
        let rules = sheet.cssRules;
        if (rules === undefined) {
          rules = sheet.rules;
        }
        if (rules === null || rules === undefined) {
          // Loaded but rules are inaccessible (cross-origin) -> assume non-empty.
          return 1;
        }
        return rules.length;
      } catch {
        return 1;
      }
    }
  }
  // No matching stylesheet found -> not yet loaded.
  return -1;
}

/**
 * Invokes the promise-returning supplier and runs onSuccess/onError when it
 * settles. Runs onError synchronously if the supplier throws or does not return
 * a Promise.
 */
export function runPromiseExpression(
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
    void promise.then(
      () => {
        onSuccess();
      },
      (error: unknown) => {
        console.error(error);
        onError();
      }
    );
  } catch (error) {
    console.error(error);
    onError();
  }
}
