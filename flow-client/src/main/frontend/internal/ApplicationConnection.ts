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

// TypeScript port of the engine API of com.vaadin.client.ApplicationConnection,
// built alongside the Java version. It starts the application from the initial
// UIDL (or resynchronizes) and exposes the post-bootstrap client API
// (isActive/poll/resolveUri/sendEventMessage/...). The registry construction
// (DefaultRegistry assembly, root-node DOM binding, publishClient) is the cutover
// step; here the Registry and the deferred-work scheduler are contracts.

/** The slice of Registry ApplicationConnection uses. */
interface ApplicationConnectionRegistry {
  getMessageSender(): { resynchronize(): void; sendUnloadBeacon(): void };
  getRequestResponseTracker(): { startRequest(): void; hasActiveRequest(): boolean };
  getMessageHandler(): { handleMessage(json: Record<string, unknown>): void; isInitialUidlHandled(): boolean };
  getPoller(): { poll(): void };
  getURIResolver(): { resolveVaadinUri(uri: string): string | null };
  getServerConnector(): { sendEventMessage(nodeId: number, eventType: string, eventData: unknown): void };
  getApplicationConfiguration(): { getUIId(): number };
  getStateTree(): { getRootNode(): { getId(): number; getDebugJson(): unknown } };
}

/** Reports whether deferred commands are still executing (the TrackingScheduler). */
interface DeferredWorkScheduler {
  hasWorkQueued(): boolean;
}

/** The main class for an application/UI; mirrors ApplicationConnection.java's engine API. */
export class ApplicationConnection {
  private readonly registry: ApplicationConnectionRegistry;

  private readonly scheduler: DeferredWorkScheduler;

  constructor(registry: ApplicationConnectionRegistry, scheduler: DeferredWorkScheduler) {
    this.registry = registry;
    this.scheduler = scheduler;
  }

  /** Starts the application from the initial UIDL, or resynchronizes if none. */
  start(initialUidl: Record<string, unknown> | null): void {
    if (initialUidl === null) {
      // Initial UIDL not in the DOM; request it from the server.
      this.registry.getMessageSender().resynchronize();
    } else {
      // Hack to avoid logging an error in endRequest().
      this.registry.getRequestResponseTracker().startRequest();
      this.registry.getMessageHandler().handleMessage(initialUidl);
    }

    window.addEventListener('pagehide', () => this.registry.getMessageSender().sendUnloadBeacon());
    window.addEventListener('pageshow', () => {
      // Mainly Safari back/forward: state is likely cleared server-side, so
      // resynchronize by reloading.
      window.location.reload();
    });
  }

  /** Whether there is client-side work pending (initial UIDL, active request, or deferred commands). */
  isActive(): boolean {
    return (
      !this.registry.getMessageHandler().isInitialUidlHandled() ||
      this.registry.getRequestResponseTracker().hasActiveRequest() ||
      this.scheduler.hasWorkQueued()
    );
  }

  /** Triggers a server poll. */
  poll(): void {
    this.registry.getPoller().poll();
  }

  /** Resolves a Vaadin URI (context://, base://) to an absolute URL. */
  resolveUri(uri: string): string | null {
    return this.registry.getURIResolver().resolveVaadinUri(uri);
  }

  /** Sends an event message to the server. */
  sendEventMessage(nodeId: number, eventType: string, eventData: unknown): void {
    this.registry.getServerConnector().sendEventMessage(nodeId, eventType, eventData);
  }

  /** The id of the UI this connection is connected to. */
  getUIId(): number {
    return this.registry.getApplicationConfiguration().getUIId();
  }

  /** Connects the web component described by the event data with the server. */
  connectWebComponent(eventData: unknown): void {
    const nodeId = this.registry.getStateTree().getRootNode().getId();
    this.registry.getServerConnector().sendEventMessage(nodeId, 'connect-web-component', eventData);
  }

  /** A JSON description of the root node's state tree, for debugging. */
  debug(): unknown {
    return this.registry.getStateTree().getRootNode().getDebugJson();
  }
}
