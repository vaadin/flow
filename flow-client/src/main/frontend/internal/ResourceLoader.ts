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

// TypeScript port of com.vaadin.client.ResourceLoader. The ResourceLoader class
// below composes the ResourceRegistry dedup/fanout kernel. It covers the
// non-stylesheet loaders (script / inline-script / dynamic-import) plus DOM init
// and clear-by-id, as well as the stylesheet/HTML loaders (which need the
// BrowserInfo Safari/Opera quirks).

import { isChrome, isEdge, isIE, isOpera, isSafariOrIOS } from './BrowserInfo';
import { type ResourceLoadEvent, type ResourceLoadListener, ResourceRegistry } from './ResourceRegistry';
import { getAbsoluteUrl } from './WidgetUtil';

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

/** Reports resource load errors (the SystemErrorHandler slice ResourceLoader needs). */
interface ResourceErrorHandler {
  handleError(message: string): void;
}

/**
 * Loads scripts and (later) stylesheets/HTML, deduping by key and notifying
 * listeners. Mirrors ResourceLoader.java; composes the ResourceRegistry kernel.
 */
export class ResourceLoader {
  private readonly resources: ResourceRegistry;

  private readonly supportsHtmlWhenReadyValue = supportsHtmlWhenReady();

  constructor(errorHandler: ResourceErrorHandler, initFromDom: boolean) {
    this.resources = new ResourceRegistry(errorHandler);
    if (initFromDom) {
      this.initLoadedResourcesFromDom();
    }
  }

  private makeEvent(resourceData: string): ResourceLoadEvent {
    return { getResourceLoader: () => this, getResourceData: () => resourceData };
  }

  // Marks scripts/stylesheets already present in the document as loaded.
  private initLoadedResourcesFromDom(): void {
    for (const script of Array.from(document.getElementsByTagName('script'))) {
      if (script.src) {
        this.resources.markLoaded(script.src);
      }
    }
    for (const link of Array.from(document.getElementsByTagName('link'))) {
      const rel = link.rel?.toLowerCase();
      const href = link.href;
      if ((rel === 'stylesheet' || rel === 'import') && href) {
        this.resources.markLoaded(href);
        const dependencyId = link.getAttribute('data-id');
        if (dependencyId) {
          this.resources.registerDependencyId(dependencyId, href);
        }
      }
    }
  }

  /** Loads an external script, notifying the listener when loaded (deduped). */
  // eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java loadScript(url, listener, async, defer, type) signature
  loadScript(
    scriptUrl: string,
    resourceLoadListener: ResourceLoadListener | null,
    async = false,
    defer = false,
    type = 'text/javascript'
  ): void {
    const url = getAbsoluteUrl(scriptUrl);
    const event = this.makeEvent(url);
    if (this.resources.isLoaded(url)) {
      resourceLoadListener?.onLoad(event);
      return;
    }
    if (this.resources.addListener(url, resourceLoadListener)) {
      const scriptTag = document.createElement('script');
      scriptTag.src = url;
      scriptTag.type = type;
      scriptTag.async = async;
      scriptTag.defer = defer;
      addOnloadHandler(
        scriptTag,
        () => this.resources.fireLoad(event),
        () => this.resources.fireError(event)
      );
      document.head.appendChild(scriptTag);
    }
  }

  /** Loads an external script as a module. */
  loadJsModule(
    scriptUrl: string,
    resourceLoadListener: ResourceLoadListener | null,
    async = false,
    defer = false
  ): void {
    this.loadScript(scriptUrl, resourceLoadListener, async, defer, 'module');
  }

  /** Inlines a script's contents, notifying the listener when loaded (deduped by contents). */
  inlineScript(scriptContents: string, resourceLoadListener: ResourceLoadListener | null): void {
    const event = this.makeEvent(scriptContents);
    if (this.resources.isLoaded(scriptContents)) {
      resourceLoadListener?.onLoad(event);
      return;
    }
    if (this.resources.addListener(scriptContents, resourceLoadListener)) {
      const scriptElement = document.createElement('script');
      scriptElement.textContent = scriptContents;
      scriptElement.type = 'text/javascript';
      addOnloadHandler(
        scriptElement,
        () => this.resources.fireLoad(event),
        () => this.resources.fireError(event)
      );
      document.head.appendChild(scriptElement);
    }
  }

  /** Loads a dynamic import via a JS expression returning a Promise. */
  loadDynamicImport(expression: string, resourceLoadListener: ResourceLoadListener): void {
    const event = this.makeEvent(expression);
    const fn = new Function(expression) as () => unknown;
    runPromiseExpression(
      expression,
      () => fn(),
      () => resourceLoadListener.onLoad(event),
      () => resourceLoadListener.onError(event)
    );
  }

  /** Runs a task once HTML imports are ready, or immediately if unsupported. */
  runWhenHtmlImportsReady(task: () => void): void {
    if (this.supportsHtmlWhenReadyValue) {
      addHtmlImportsReadyHandler(task);
    } else {
      task();
    }
  }

  /** Loads an external stylesheet, optionally tracked by a dependency id (deduped). */
  loadStylesheet(
    stylesheetUrl: string,
    resourceLoadListener: ResourceLoadListener | null,
    dependencyId: string | null = null
  ): void {
    const url = getAbsoluteUrl(stylesheetUrl);
    if (dependencyId !== null) {
      this.resources.registerDependencyId(dependencyId, url);
    }
    const event = this.makeEvent(url);
    if (this.resources.isLoaded(url)) {
      resourceLoadListener?.onLoad(event);
      return;
    }
    if (this.resources.addListener(url, resourceLoadListener)) {
      const linkElement = document.createElement('link');
      linkElement.rel = 'stylesheet';
      linkElement.type = 'text/css';
      linkElement.href = url;
      if (dependencyId !== null) {
        linkElement.setAttribute('data-id', dependencyId);
      }

      if (isSafariOrIOS()) {
        // Safari fires no events for link elements; poll the stylesheet rules.
        this.pollStylesheet(url, event);
      } else {
        addOnloadHandler(
          linkElement,
          () => {
            // Chrome, IE, Edge all fire load for errors, must check
            // stylesheet data
            if (isChrome() || isIE() || isEdge()) {
              // Error if there's an empty stylesheet (typically a 404).
              if (getStyleSheetLength(url) === 0) {
                this.resources.fireError(event);
                return;
              }
            }
            this.resources.fireLoad(event);
          },
          () => this.resources.fireError(event)
        );
        if (isOpera()) {
          // Opera never fires onerror; assume error if not loaded within 5s.
          setTimeout(() => {
            if (!this.resources.isLoaded(url)) {
              this.resources.fireError(event);
            }
          }, 5000);
        }
      }
      this.addInHeadBeforeComment(linkElement, 'Stylesheet end');
    }
  }

  // Polls a Safari/iOS stylesheet's rule count to detect load/error.
  private pollStylesheet(url: string, event: ResourceLoadEvent): void {
    const start = performance.now();
    const handle = setInterval(() => {
      const styleSheetLength = getStyleSheetLength(url);
      if (styleSheetLength > 0) {
        this.resources.fireLoad(event);
        clearInterval(handle);
      } else if (styleSheetLength === 0) {
        // "Loaded" empty sheet -> most likely a 404 error.
        this.resources.fireError(event);
      } else if (performance.now() - start > 60 * 1000) {
        this.resources.fireError(event);
        clearInterval(handle);
      }
    }, 10);
  }

  /** Inlines a stylesheet's contents, optionally tracked by a dependency id (deduped by contents). */
  inlineStyleSheet(
    styleSheetContents: string,
    resourceLoadListener: ResourceLoadListener | null,
    dependencyId: string | null = null
  ): void {
    if (dependencyId !== null) {
      this.resources.registerDependencyId(dependencyId, styleSheetContents);
    }
    const event = this.makeEvent(styleSheetContents);
    if (this.resources.isLoaded(styleSheetContents)) {
      resourceLoadListener?.onLoad(event);
      return;
    }
    if (this.resources.addListener(styleSheetContents, resourceLoadListener)) {
      const styleElement = document.createElement('style');
      styleElement.textContent = styleSheetContents;
      styleElement.type = 'text/css';
      if (dependencyId !== null) {
        styleElement.setAttribute('data-id', dependencyId);
      }
      this.addCssLoadHandler(styleSheetContents, event, styleElement);
      this.addInHeadBeforeComment(styleElement, 'Stylesheet end');
    }
  }

  private addCssLoadHandler(
    styleSheetContents: string,
    event: ResourceLoadEvent,
    styleElement: HTMLStyleElement
  ): void {
    if (isSafariOrIOS() || isOpera()) {
      // Safari and Opera fire no events for style elements; assume done after 5s.
      setTimeout(() => {
        if (this.resources.isLoaded(styleSheetContents)) {
          this.resources.fireLoad(event);
        } else {
          this.resources.fireError(event);
        }
      }, 5000);
    } else {
      addOnloadHandler(
        styleElement,
        () => this.resources.fireLoad(event),
        () => this.resources.fireError(event)
      );
    }
  }

  // Inserts an element into <head> before the comment with the given text, or
  // appends it (with a warning) if the comment is not found.
  private addInHeadBeforeComment(element: Element, comment: string): void {
    const commentNode = this.findCommentInHead(comment);
    if (commentNode === null) {
      console.error(`Expected to find a '${comment}' comment inside <head> but none was found. Appending instead.`);
    }
    document.head.insertBefore(element, commentNode);
  }

  private findCommentInHead(comment: string): Node | null {
    for (const childNode of Array.from(document.head.childNodes)) {
      if (childNode.nodeType === Node.COMMENT_NODE && childNode.nodeValue === comment) {
        return childNode;
      }
    }
    return null;
  }

  /** Clears a loaded resource (and its listeners) by its dependency id. */
  clearLoadedResourceById(dependencyId: string): void {
    this.resources.clearLoadedResourceById(dependencyId);
  }
}
