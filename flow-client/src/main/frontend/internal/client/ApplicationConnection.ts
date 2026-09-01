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

// TypeScript port of com.vaadin.client.ApplicationConnection.
// The static create() assembles the DefaultRegistry, binds the root state node to
// the page body, and publishes the client API. The instance API
// (start/isActive/poll/resolveUri/sendEventMessage/...) drives the application.
// create() is the entry point the ported Bootstrapper calls; the live page still
// starts the GWT engine, so nothing calls it outside the tests until cutover.
//
// Two deviations from the Java class:
//
// - Java does all of this in its public constructor, which can publish the client
//   API because the instance exists inside it. Here the publication is a separate
//   module (publishClient), so the connection has to exist before it can be
//   published: the constructor takes the assembled registry and create() is the
//   entry point that mirrors the Java constructor. That is also why the members
//   the JSNI blocks reach - isActive, getDomElementByNodeId, getNodeId,
//   addDomSetListener, getJavaClass, isHiddenByServer, getElementStyleProperties -
//   are public here although Java keeps them private: JSNI reads private members
//   of the enclosing class, a separate module cannot.
// - The Styles JavaScriptObject that getElementStyleProperties fills is not
//   ported: it exists to give JSNI a typed handle on a plain JS object, which a
//   TypeScript object literal already is.

import { bind } from './flow/binding/Binder';
import { observe as observeLoadingIndicator } from './communication/LoadingIndicatorConfigurator';
import { observe as observePoll } from './communication/PollConfigurator';
import { ReconnectConfiguration } from './communication/ReconnectConfiguration';
import { DefaultRegistry } from './DefaultRegistry';
import { NodeFeatures } from '../flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../flow/internal/nodefeature/NodeProperties';
import { publishClient } from './publishClient';
import type { ApplicationConnection as PublishedClient } from './clientApi';
import type { ApplicationConfiguration } from './ApplicationConfiguration';
import { getScheduler, type TrackingScheduler } from './TrackingScheduler';
import type { Registry } from './Registry';
import type { ValueMap } from './ValueMap';

/** The main class for an application/UI; mirrors ApplicationConnection.java's engine API. */
// GWT's uncaught exception handler is a single, replaceable slot: creating
// another connection replaced the handler instead of adding one. Mirror that
// with one window listener, installed on the first connection and dispatching to
// the handler of the most recently created one, so several applications on a
// page do not report the same error once per connection.
//
// The listener is wider than the Java original in one way that has no
// TypeScript equivalent: GWT only routed exceptions thrown inside $entry-wrapped
// engine code, while a window error listener also sees errors thrown by
// unrelated page scripts.
let uncaughtErrorHandler: ((error: unknown) => void) | null = null;

function setUncaughtErrorHandler(handler: (error: unknown) => void): void {
  if (uncaughtErrorHandler === null) {
    window.addEventListener('error', (event) => uncaughtErrorHandler?.(event.error ?? event.message));
  }
  uncaughtErrorHandler = handler;
}

/**
 * Main class for an application / UI.
 *
 * Initializes the registry and starts the application.
 */
export class ApplicationConnection implements PublishedClient {
  readonly #registry: Registry;

  readonly #scheduler: Pick<TrackingScheduler, 'hasWorkQueued'>;

  constructor(registry: Registry, scheduler: Pick<TrackingScheduler, 'hasWorkQueued'>) {
    this.#registry = registry;
    this.#scheduler = scheduler;
  }

  /**
   * Creates an application connection using the given configuration: assembles
   * the {@link DefaultRegistry}, binds the root state node to the page body, and
   * publishes the client API through {@link publishClient}. Mirrors the
   * ApplicationConnection.java constructor.
   *
   * @param applicationConfiguration - the configuration object for the application
   * @param rootElement - the element to bind the root state node to; a port
   *          addition, since Java reads the body directly
   * @returns the connection, already published
   */
  static create(
    applicationConfiguration: ApplicationConfiguration,
    rootElement: Element = document.body
  ): ApplicationConnection {
    const registry = new DefaultRegistry(applicationConfiguration);

    // Route uncaught errors to the system error handler (GWT's uncaught handler).
    const systemErrorHandler = registry.getSystemErrorHandler();
    setUncaughtErrorHandler((error) => systemErrorHandler.handleErrorObject(error));

    const rootNode = registry.getStateTree().getRootNode();

    // Bind the UI configuration objects.
    observePoll(rootNode, registry.getPoller());
    ReconnectConfiguration.bind(registry.getConnectionStateHandler());
    observeLoadingIndicator(rootNode);

    rootNode.setDomNode(rootElement);
    bind(rootNode, rootElement);

    const connection = new ApplicationConnection(registry, getScheduler());
    registry.setApplicationConnection(connection);
    publishClient(connection, applicationConfiguration);
    return connection;
  }

  /**
   * Starts this application. Public access is required for web components.
   *
   * @param initialUidl - the initial UIDL or null if the server did not provide
   *          any
   */
  start(initialUidl: ValueMap | null): void {
    if (initialUidl === null) {
      // Initial UIDL not in the DOM; request it from the server.
      this.#registry.getMessageSender().resynchronize();
    } else {
      // Hack to avoid logging an error in endRequest().
      this.#registry.getRequestResponseTracker().startRequest();
      this.#registry.getMessageHandler().handleMessage(initialUidl);
    }

    window.addEventListener('pagehide', () => this.#registry.getMessageSender().sendUnloadBeacon());
    window.addEventListener('pageshow', () => {
      // Mainly Safari back/forward: state is likely cleared server-side, so
      // resynchronize by reloading.
      window.location.reload();
    });
  }

  /**
   * Checks if there is some work to be done on the client side.
   *
   * Java also asks the scheduler whether deferred commands are still running,
   * through a private isExecutingDeferredCommands; the port reads the injected
   * scheduler directly.
   *
   * @returns true if the client has some work to be done, false otherwise
   */
  isActive(): boolean {
    return (
      !this.#registry.getMessageHandler().isInitialUidlHandled() ||
      this.#registry.getRequestResponseTracker().hasActiveRequest() ||
      this.#scheduler.hasWorkQueued()
    );
  }

  // The members below stand in for the JSNI blocks, in the order those publish
  // them: first publishJavascriptMethods, then the development-mode block.

  /** The DOM node bound to the state node with the given id, or null. */
  getDomElementByNodeId(id: number): Node | null {
    const node = this.#registry.getStateTree().getNode(id);
    return node === null ? null : node.getDomNode();
  }

  /** The state node id bound to the given DOM element, or -1 if none. */
  getNodeId(element: Element): number {
    const node = this.#registry.getStateTree().getStateNodeForDomNode(element);
    return node === null ? -1 : node.getId();
  }

  /** The id of the UI this connection is connected to. */
  getUIId(): number {
    return this.#registry.getApplicationConfiguration().getUIId();
  }

  /** Runs the callback once the DOM node for the given state node id is set. */
  addDomSetListener(nodeId: number, callback: () => void): void {
    const node = this.#registry.getStateTree().getNode(nodeId);
    if (node === null) {
      return;
    }
    node.addDomNodeSetListener((boundNode) => {
      if (boundNode.getId() === nodeId) {
        callback();
        return true;
      }
      return false;
    });
  }

  /** Triggers a server poll. */
  poll(): void {
    this.#registry.getPoller().poll();
  }

  /** Connects the web component described by the event data with the server. */
  connectWebComponent(eventData: unknown): void {
    const nodeId = this.#registry.getStateTree().getRootNode().getId();
    this.#registry.getServerConnector().sendEventMessage(nodeId, 'connect-web-component', eventData);
  }

  /** Profiling data for the last request (processing times + server timing + bootstrap). */
  getProfilingData(): number[] {
    return this.#registry.getMessageHandler().getProfilingData();
  }

  /** Resolves a Vaadin URI (context://, base://) to an absolute URL. */
  resolveUri(uri: string): string | null {
    return this.#registry.getURIResolver().resolveVaadinUri(uri);
  }

  /** Sends an event message to the server. */
  sendEventMessage(nodeId: number, eventType: string, eventData: unknown): void {
    this.#registry.getServerConnector().sendEventMessage(nodeId, eventType, eventData);
  }

  /** A JSON description of the root node's state tree, for debugging. */
  debug(): unknown {
    return this.#registry.getStateTree().getRootNode().getDebugJson();
  }

  // Java's own private helpers, in the order it declares them.

  /** The Java class name bound to the state node with the given id, or null. */
  getJavaClass(id: number): string | null {
    const node = this.#registry.getStateTree().getNode(id);
    if (node === null) {
      return null;
    }
    return node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.JAVA_CLASS).getValueOrDefault(null);
  }

  /** Whether the element for the given state node id is hidden by the server. */
  isHiddenByServer(id: number): boolean {
    const node = this.#registry.getStateTree().getNode(id);
    const visible =
      node === null
        ? true
        : node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBLE).getValueOrDefault(true);
    return !visible;
  }

  /** The element style properties for the given state node id, as a plain object. */
  getElementStyleProperties(id: number): Record<string, unknown> {
    const styles: Record<string, unknown> = {};
    const node = this.#registry.getStateTree().getNode(id);
    if (node !== null) {
      const styleMap = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
      for (const name of styleMap.getPropertyNames()) {
        styles[name] = styleMap.getProperty(name).getValue();
      }
    }
    return styles;
  }
}
