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

import { BrowserInfo } from './BrowserInfo';
import { Console } from './Console';
import { WidgetUtil } from './WidgetUtil';

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
 * Registry-like adapter shape the TS class needs to reach back into Java
 * services. The Java {@code Registry} satisfies this shape because the
 * {@code getSystemErrorHandler()} method is already part of the
 * {@code @JsType} Java {@code Registry} class.
 */
/** Single-method error sink used for "could not load resource X" logs. */
export type ResourceLoaderErrorHandler = (message: string) => void;

/**
 * Event fired when a resource has been loaded.
 */
export class ResourceLoadEvent {
  private readonly loader: ResourceLoader;
  private readonly resourceData: string;

  constructor(loader: ResourceLoader, resourceData: string) {
    this.loader = loader;
    this.resourceData = resourceData;
  }

  getResourceLoader(): ResourceLoader {
    return this.loader;
  }

  getResourceData(): string {
    return this.resourceData;
  }
}

/**
 * Internal listener shape used inside the TS resource-loading cluster.
 * External Java callers pass an equivalent {@code @JsType} interface that this
 * matches at runtime (the Java facade {@link ResourceLoader} splits listener
 * objects into two callbacks before crossing into TS to avoid GWT-OBF method
 * name mangling on Java anonymous interface implementations).
 */
export interface ResourceLoadListener {
  onLoad(event: ResourceLoadEvent): void;
  onError(event: ResourceLoadEvent): void;
}

function getDocument(): Document {
  return document;
}

function getHead(): HTMLHeadElement {
  return document.head;
}

function supportsHtmlWhenReady(): boolean {
  const g = globalThis as unknown as HtmlImportsGlobal;
  return !!g.HTMLImports?.whenReady;
}

function addHtmlImportsReadyHandler(handler: () => void): void {
  const g = globalThis as unknown as HtmlImportsGlobal;
  g.HTMLImports?.whenReady(() => handler());
}

function addOnloadHandlerNative(element: Element, onLoad: () => void, onError: () => void): void {
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
}

function getStyleSheetLengthNative(url: string): number {
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
}

function runPromiseExpressionNative(
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

function findCommentInHead(comment: string): Node | null {
  const childNodes = getHead().childNodes;
  for (let i = 0; i < childNodes.length; i++) {
    const childNode = childNodes.item(i);
    if (childNode.nodeType === Node.COMMENT_NODE && childNode.nodeValue === comment) {
      return childNode;
    }
  }
  return null;
}

/**
 * ResourceLoader lets you dynamically include external scripts and styles on
 * the page and lets you know when the resource has been loaded.
 *
 * You can also preload resources, allowing them to get cached by the browser
 * without being evaluated. This enables downloading multiple resources at once
 * while still controlling in which order e.g. scripts are executed.
 *
 * Migrated from {@code com.vaadin.client.ResourceLoader}. Reached from
 * GWT-compiled Java code via the {@code @JsType(isNative=true)} facade
 * published at {@code Vaadin.Flow.internal.client.ResourceLoader}.
 */
export class ResourceLoader {
  private readonly loadedResources = new Set<string>();
  private readonly loadListeners = new Map<string, ResourceLoadListener[]>();
  /** Map from dependency ID to resource key (URL or content) for removal. */
  private readonly dependencyIdToResourceKey = new Map<string, string>();
  private readonly errorHandler: ResourceLoaderErrorHandler;
  private readonly supportsHtmlWhenReady: boolean = supportsHtmlWhenReady();

  /**
   * Creates a new resource loader.
   *
   * @param errorHandler function called with a human-readable message when a
   *                     load fails (typically wired to {@code Console.error}
   *                     via SystemErrorHandler)
   * @param initFromDom {@code true} if currently loaded resources should be
   *                    marked as loaded
   */
  constructor(errorHandler: ResourceLoaderErrorHandler, initFromDom: boolean) {
    this.errorHandler = errorHandler;
    if (initFromDom) {
      this.initLoadedResourcesFromDom();
    }
  }

  /**
   * Clears a resource from the loaded resources set by its dependency ID.
   * Used when a resource is removed from the DOM using its dependency ID.
   */
  clearLoadedResourceById(dependencyId: string | null | undefined): void {
    if (dependencyId != null) {
      const resourceKey = this.dependencyIdToResourceKey.get(dependencyId);
      if (resourceKey != null) {
        this.loadedResources.delete(resourceKey);
        this.loadListeners.delete(resourceKey);
        this.dependencyIdToResourceKey.delete(dependencyId);
      }
    }
  }

  /** Populates the resource loader with the scripts currently added to the page. */
  private initLoadedResourcesFromDom(): void {
    const doc = getDocument();

    // Detect already loaded scripts and stylesheets
    const scripts = doc.getElementsByTagName('script');
    for (let i = 0; i < scripts.length; i++) {
      const element = scripts.item(i) as HTMLScriptElement;
      const src = element.src;
      if (src != null && src.length !== 0) {
        this.loadedResources.add(src);
      }
    }

    const links = doc.getElementsByTagName('link');
    for (let i = 0; i < links.length; i++) {
      const linkElement = links.item(i) as HTMLLinkElement;
      const rel = linkElement.rel;
      const href = linkElement.href;
      if (
        (rel?.toLowerCase() === 'stylesheet' || rel?.toLowerCase() === 'import') &&
        href != null &&
        href.length !== 0
      ) {
        this.loadedResources.add(href);
        // Handle stylesheet loaded by AppShellRegistry
        const dependencyId = linkElement.getAttribute('data-id');
        if (dependencyId != null) {
          this.dependencyIdToResourceKey.set(dependencyId, href);
        }
      }
    }
  }

  /**
   * Loads a script (defaulting to {@code async=false, defer=false}) and
   * dispatches the two callbacks when loading completes.
   */
  loadScript(
    scriptUrl: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void
  ): void {
    this.loadScriptWithOptions(scriptUrl, onLoad, onError, false, false, 'text/javascript');
  }

  /** Loads a script with explicit {@code async} / {@code defer} attribute values. */
  // eslint-disable-next-line @typescript-eslint/max-params
  loadScriptAsyncDefer(
    scriptUrl: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void,
    async: boolean,
    defer: boolean
  ): void {
    this.loadScriptWithOptions(scriptUrl, onLoad, onError, async, defer, 'text/javascript');
  }

  /** Loads a script with {@code type="module"}. */
  // eslint-disable-next-line @typescript-eslint/max-params
  loadJsModule(
    scriptUrl: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void,
    async: boolean,
    defer: boolean
  ): void {
    this.loadScriptWithOptions(scriptUrl, onLoad, onError, async, defer, 'module');
  }

  // eslint-disable-next-line @typescript-eslint/max-params
  private loadScriptWithOptions(
    scriptUrl: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void,
    async: boolean,
    defer: boolean,
    type: string
  ): void {
    const url = WidgetUtil.getAbsoluteUrl(scriptUrl);
    const event = new ResourceLoadEvent(this, url);
    if (this.loadedResources.has(url)) {
      onLoad(event);
      return;
    }

    if (this.addListener(url, { onLoad, onError })) {
      const scriptTag = getDocument().createElement('script');
      scriptTag.src = url;
      scriptTag.type = type;
      scriptTag.async = async;
      scriptTag.defer = defer;

      this.addOnloadHandler(scriptTag, new SimpleLoadListener(this), event);
      getHead().appendChild(scriptTag);
    }
  }

  /** Inlines a script and notifies callbacks when it's loaded. */
  inlineScript(
    scriptContents: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void
  ): void {
    const event = new ResourceLoadEvent(this, scriptContents);
    if (this.loadedResources.has(scriptContents)) {
      onLoad(event);
      return;
    }

    if (this.addListener(scriptContents, { onLoad, onError })) {
      const scriptElement = getDocument().createElement('script');
      scriptElement.textContent = scriptContents;
      scriptElement.type = 'text/javascript';

      this.addOnloadHandler(scriptElement, new SimpleLoadListener(this), event);
      getHead().appendChild(scriptElement);
    }
  }

  /**
   * Sets the provided task to be run by {@code HTMLImports.whenReady}.
   * Runs immediately if {@code HTMLImports.whenReady} is not supported.
   */
  runWhenHtmlImportsReady(task: () => void): void {
    if (this.supportsHtmlWhenReady) {
      addHtmlImportsReadyHandler(task);
    } else {
      task();
    }
  }

  /**
   * Adds an onload listener to the given element, which should be a link or a
   * script tag.
   */
  private addOnloadHandler(element: Element, listener: ResourceLoadListener, event: ResourceLoadEvent): void {
    addOnloadHandlerNative(
      element,
      () => listener.onLoad(event),
      () => listener.onError(event)
    );
  }

  /** Loads a stylesheet. */
  loadStylesheet(
    stylesheetUrl: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void,
    dependencyId: string | null | undefined
  ): void {
    const url = WidgetUtil.getAbsoluteUrl(stylesheetUrl);

    if (dependencyId != null) {
      this.dependencyIdToResourceKey.set(dependencyId, url);
    }

    const event = new ResourceLoadEvent(this, url);
    if (this.loadedResources.has(url)) {
      onLoad(event);
      return;
    }

    if (this.addListener(url, { onLoad, onError })) {
      const linkElement = getDocument().createElement('link');
      linkElement.rel = 'stylesheet';
      linkElement.type = 'text/css';
      linkElement.href = url;
      if (dependencyId != null) {
        linkElement.setAttribute('data-id', dependencyId);
      }

      const browser = BrowserInfo.get();
      if (browser.isSafariOrIOS()) {
        // Safari doesn't fire any events for link elements
        // See http://www.phpied.com/when-is-a-stylesheet-really-loaded/
        const start = Date.now();
        const intervalId = setInterval(() => {
          const styleSheetLength = getStyleSheetLengthNative(url);
          if (styleSheetLength > 0) {
            clearInterval(intervalId);
            this.fireLoad(event);
          } else if (styleSheetLength === 0) {
            // "Loaded" empty sheet -> most likely 404 error
            this.fireError(event);
            // GWT behavior: returning `true` continues repeating; we keep
            // polling rather than stopping so a later non-empty state can fire
            // load. This matches the Java RepeatingCommand returning true.
          } else if (Date.now() - start > 60 * 1000) {
            clearInterval(intervalId);
            this.fireError(event);
          }
        }, 10);
      } else {
        this.addOnloadHandler(linkElement, new StyleSheetLoadListener(this, url), event);
        if (browser.isOpera()) {
          // Opera onerror never fired, assume error if no onload in x seconds
          setTimeout(() => {
            if (!this.loadedResources.has(url)) {
              this.fireError(event);
            }
          }, 5 * 1000);
        }
      }

      this.addInHeadBeforeComment(linkElement, 'Stylesheet end');
    }
  }

  /** Inlines a stylesheet. */
  inlineStyleSheet(
    styleSheetContents: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void,
    dependencyId: string | null | undefined
  ): void {
    if (dependencyId != null) {
      this.dependencyIdToResourceKey.set(dependencyId, styleSheetContents);
    }

    const event = new ResourceLoadEvent(this, styleSheetContents);
    if (this.loadedResources.has(styleSheetContents)) {
      onLoad(event);
      return;
    }

    if (this.addListener(styleSheetContents, { onLoad, onError })) {
      const styleSheetElement = getDocument().createElement('style');
      styleSheetElement.textContent = styleSheetContents;
      styleSheetElement.type = 'text/css';
      if (dependencyId != null) {
        styleSheetElement.setAttribute('data-id', dependencyId);
      }

      this.addCssLoadHandler(styleSheetContents, event, styleSheetElement);

      this.addInHeadBeforeComment(styleSheetElement, 'Stylesheet end');
    }
  }

  private addInHeadBeforeComment(element: Element, comment: string): void {
    const commentNode = findCommentInHead(comment);
    if (commentNode == null) {
      Console.error(`Expected to find a '${comment}' comment inside <head> but none was found. Appending instead.`);
    }
    getHead().insertBefore(element, commentNode);
  }

  /**
   * Loads a dynamic import via the provided JS {@code expression} and reports
   * the result via the two callbacks.
   */
  loadDynamicImport(
    expression: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void
  ): void {
    const event = new ResourceLoadEvent(this, expression);
    const fn = new Function(expression) as () => unknown;
    runPromiseExpressionNative(
      expression,
      () => fn.call(null),
      () => onLoad(event),
      () => onError(event)
    );
  }

  private addCssLoadHandler(styleSheetContents: string, event: ResourceLoadEvent, styleSheetElement: Element): void {
    const browser = BrowserInfo.get();
    if (browser.isSafariOrIOS() || browser.isOpera()) {
      // Safari and Opera don't fire any events for link elements
      // See http://www.phpied.com/when-is-a-stylesheet-really-loaded/
      setTimeout(() => {
        if (this.loadedResources.has(styleSheetContents)) {
          this.fireLoad(event);
        } else {
          this.fireError(event);
        }
      }, 5 * 1000);
    } else {
      this.addOnloadHandler(
        styleSheetElement,
        {
          onLoad: (e) => this.fireLoad(e),
          onError: (e) => this.fireError(e)
        },
        event
      );
    }
  }

  private addListener(resourceId: string, listener: ResourceLoadListener): boolean {
    const existing = this.loadListeners.get(resourceId);
    if (existing == null) {
      this.loadListeners.set(resourceId, [listener]);
      return true;
    }
    existing.push(listener);
    return false;
  }

  fireError(event: ResourceLoadEvent): void {
    this.errorHandler(`Error loading ${event.getResourceData()}`);
    const resource = event.getResourceData();

    const listeners = this.loadListeners.get(resource);
    this.loadListeners.delete(resource);
    if (listeners != null && listeners.length !== 0) {
      for (const listener of listeners) {
        listener.onError(event);
      }
    }
  }

  fireLoad(event: ResourceLoadEvent): void {
    Console.debug(`Loaded ${event.getResourceData()}`);
    const resource = event.getResourceData();
    const listeners = this.loadListeners.get(resource);
    this.loadedResources.add(resource);
    this.loadListeners.delete(resource);
    if (listeners != null && listeners.length !== 0) {
      for (const listener of listeners) {
        listener.onLoad(event);
      }
    }
  }

  // ---------------- Static helpers retained for legacy GWT call sites ----------------
  // The previous NativeResourceLoader shim exposed these names as static
  // members on the same JS object. Java code compiled before the migration
  // still references them via the namespace lookup, so keep them here so any
  // not-yet-recompiled callers continue to link.

  static supportsHtmlWhenReady(): boolean {
    return supportsHtmlWhenReady();
  }

  static addHtmlImportsReadyHandler(handler: () => void): void {
    addHtmlImportsReadyHandler(handler);
  }

  static addOnloadHandler(element: Element, onLoad: () => void, onError: () => void): void {
    addOnloadHandlerNative(element, onLoad, onError);
  }

  static getStyleSheetLength(url: string): number {
    return getStyleSheetLengthNative(url);
  }

  static runPromiseExpression(
    expression: string,
    promiseSupplier: () => unknown,
    onSuccess: () => void,
    onError: () => void
  ): void {
    runPromiseExpressionNative(expression, promiseSupplier, onSuccess, onError);
  }
}

/**
 * Simple internal listener that just forwards to {@link ResourceLoader#fireLoad}
 * / {@link ResourceLoader#fireError} on the owning ResourceLoader.
 */
class SimpleLoadListener implements ResourceLoadListener {
  private readonly loader: ResourceLoader;

  constructor(loader: ResourceLoader) {
    this.loader = loader;
  }

  onLoad(event: ResourceLoadEvent): void {
    this.loader.fireLoad(event);
  }

  onError(event: ResourceLoadEvent): void {
    this.loader.fireError(event);
  }
}

/**
 * Stylesheet load listener that detects loading failures on Chrome / IE / Edge
 * (these browsers fire {@code onload} for HTTP errors too, so we check whether
 * the stylesheet is actually empty before reporting success).
 */
class StyleSheetLoadListener implements ResourceLoadListener {
  private readonly loader: ResourceLoader;
  private readonly url: string;

  constructor(loader: ResourceLoader, url: string) {
    this.loader = loader;
    this.url = url;
  }

  onLoad(event: ResourceLoadEvent): void {
    const browser = BrowserInfo.get();
    if (browser.isChrome() || browser.isIE() || browser.isEdge()) {
      const styleSheetLength = getStyleSheetLengthNative(this.url);
      // Error if there's an empty stylesheet
      if (styleSheetLength === 0) {
        this.loader.fireError(event);
        return;
      }
    }
    this.loader.fireLoad(event);
  }

  onError(event: ResourceLoadEvent): void {
    this.loader.fireError(event);
  }
}
