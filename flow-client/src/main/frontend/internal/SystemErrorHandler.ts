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

// Implementations migrated from SystemErrorHandler.java, registered on
// window.Vaadin.Flow.internal.SystemErrorHandler by registerInternals; the Java
// methods delegate here. Also bundled to ES5 for the HtmlUnit used by GwtTests.

import { addGetParameters } from './SharedUtil';
import { getScheduler } from './TrackingScheduler';
import { UIState } from './UILifecycle';
import { redirect } from './WidgetUtil';

// com.vaadin.flow.shared.ApplicationConstants
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_WEBCOMPONENT_RESYNC = 'webcomponent-resync';
const UI_ID = 'uiId';

/** Sends a credentialed GET request, mirroring Java's Xhr.getWithCredentials. */
function getWithCredentials(
  uri: string,
  onSuccess: (responseText: string) => void,
  onFail: (error: Error) => void
): void {
  const xhr = new XMLHttpRequest();
  xhr.open('GET', uri, true);
  xhr.withCredentials = true;
  xhr.onreadystatechange = () => {
    if (xhr.readyState === XMLHttpRequest.DONE) {
      if (xhr.status === 200) {
        onSuccess(xhr.responseText);
      } else {
        onFail(new Error(`Session resynchronization request failed with status ${xhr.status}`));
      }
    }
  };
  xhr.onerror = () => onFail(new Error('Session resynchronization request failed'));
  xhr.send();
}

/**
 * Replaces every element with the given tag name by a shallow clone, after
 * mocking its disconnected callback. Used to detach stale components without
 * triggering their server-side disconnect handling.
 */
export function recreateNodes(elementName: string): void {
  // Snapshot the live collection before mutating it.
  const elements = Array.from(document.getElementsByTagName(elementName)) as Array<
    Element & {
      $server: { disconnected: () => void };
    }
  >;
  for (const elem of elements) {
    // Mock the disconnected callback so it does not throw a TypeError.
    elem.$server.disconnected = () => {};
    elem.parentNode?.replaceChild(elem.cloneNode(false), elem);
  }
}

/** Invokes the native showPopover() of the element if it supports it. */
export function showPopover(el: Element): void {
  const fn = el && (el as Element & { showPopover?: () => void }).showPopover;
  if (typeof fn === 'function') {
    fn.call(el);
  }
}

/** Returns the shadow root of the given host element, if any. */
export function getShadowRootElement(host: Element): ShadowRoot | null {
  return host.shadowRoot;
}

/**
 * Builds and shows the system error notification for an unrecoverable error.
 * Each provided part (caption, message, details) becomes a labelled div and is
 * also reported through the logError callback (which keeps the Java Console's
 * production-mode gating). When a querySelector is given the notification is
 * placed inside the matching element (its shadow root if it has one); otherwise
 * it is appended to the document body. The container is always shown and
 * returned, even when a querySelector matched no element (it is then left
 * unattached), matching the original behaviour the caller relies on.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- positional JSNI delegation mirrors the Java signature plus the log callback
export function handleError(
  caption: string | null,
  message: string | null,
  details: string | null,
  querySelector: string | null,
  logError: (text: string) => void
): Element {
  const systemErrorContainer = document.createElement('div');
  // Set the popover attribute for native popovers.
  systemErrorContainer.setAttribute('popover', 'manual');
  systemErrorContainer.className = 'v-system-error';

  const appendPart = (text: string | null, partClassName: string): void => {
    if (text !== null) {
      const partDiv = document.createElement('div');
      partDiv.className = partClassName;
      partDiv.textContent = text;
      systemErrorContainer.appendChild(partDiv);
      logError(text);
    }
  };
  appendPart(caption, 'caption');
  appendPart(message, 'message');
  appendPart(details, 'details');

  if (querySelector !== null) {
    const baseElement = document.querySelector(querySelector);
    // If the querySelector matches no element on the page the notification is
    // left unattached (and thus not displayed), but is still returned.
    if (baseElement !== null) {
      // If the base element has a shadow root, add the notification to the
      // shadow root; otherwise add it to the base element.
      (getShadowRootElement(baseElement) ?? baseElement).appendChild(systemErrorContainer);
    }
  } else {
    document.body.appendChild(systemErrorContainer);
  }
  showPopover(systemErrorContainer);

  return systemErrorContainer;
}

// The SystemErrorHandler class is the build-alongside TS port of the
// orchestration in SystemErrorHandler.java, composing the DOM rendering above.
// This installment covers the logging / web-component-mode / recreate-web-
// components orchestration; the unrecoverable-error notification flow
// (handleUnrecoverableError) and the web-component session resynchronization
// (resynchronizeSession, XHR + heartbeat/push/reset) are DOM/network-bound and
// IT-validated. The Registry is a contract satisfied at cutover.

/** A system message (caption/message/url), as configured for unrecoverable errors. */
interface SystemMessage {
  caption?: string;
  message?: string;
  url?: string;
}

/** The slice of Registry SystemErrorHandler uses. */
interface SystemErrorRegistry {
  getApplicationConfiguration(): {
    isWebComponentMode(): boolean;
    getExportedWebComponents(): string[];
    getSessionExpiredError(): SystemMessage | null;
    getServiceUrl(): string;
    getUIId(): number;
    setUIId(uiId: number): void;
    getHeartbeatInterval(): number;
  };
  getHeartbeat(): { setInterval(interval: number): void };
  getPushConfiguration(): { isPushEnabled(): boolean };
  getMessageSender(): { setPushEnabled(enabled: boolean, reEnableIfNeeded?: boolean): void };
  getUILifecycle(): { setState(state: UIState): void };
  getMessageHandler(): { handleMessage(json: Record<string, unknown>): void };
  reset(): void;
}

/** Handles system-level errors and web-component recreation; mirrors SystemErrorHandler.java. */
export class SystemErrorHandler {
  private readonly registry: SystemErrorRegistry;

  private resyncInProgress = false;

  constructor(registry: SystemErrorRegistry) {
    this.registry = registry;
  }

  /** Logs the given error message. Mirrors SystemErrorHandler.handleError(String). */
  handleError(errorMessage: string): void {
    console.error(errorMessage);
  }

  /** Logs the message of the given error/throwable. Mirrors handleError(Throwable). */
  handleErrorObject(error: unknown): void {
    this.handleError(error instanceof Error ? error.message : String(error));
  }

  /** Whether the application runs in web-component (embedded) mode. */
  isWebComponentMode(): boolean {
    return this.registry.getApplicationConfiguration().isWebComponentMode();
  }

  /** Recreates every exported web component's elements (detaching stale ones). */
  recreateWebComponents(): void {
    for (const elementName of this.registry.getApplicationConfiguration().getExportedWebComponents()) {
      recreateNodes(elementName);
    }
    this.resyncInProgress = false;
  }

  /**
   * Resynchronizes a web-component (embedded) session after server-side session
   * expiration: requests a fresh JSESSIONID, resets the registry, replays the
   * returned UIDL and re-establishes push, then recreates the exported web
   * components. Mirrors SystemErrorHandler.resynchronizeSession.
   */
  private resynchronizeSession(): void {
    if (this.resyncInProgress) {
      console.debug('Web components resynchronization already in progress');
      return;
    }
    this.resyncInProgress = true;

    const configuration = this.registry.getApplicationConfiguration();
    const serviceUrl = `${configuration.getServiceUrl()}web-component/web-component-bootstrap.js`;

    // Stop the heartbeat to prevent requests during resynchronization.
    this.registry.getHeartbeat().setInterval(-1);
    if (this.registry.getPushConfiguration().isPushEnabled()) {
      this.registry.getMessageSender().setPushEnabled(false, false);
    }

    const sessionResyncUri = addGetParameters(
      serviceUrl,
      `${REQUEST_TYPE_PARAMETER}=${REQUEST_TYPE_WEBCOMPONENT_RESYNC}`
    );

    getWithCredentials(
      sessionResyncUri,
      (responseText) => {
        console.log(`Received xhr HTTP session resynchronization message: ${responseText}`);

        // Make sure the heartbeat has not been restarted; especially important
        // if the uiId is reset after session expiration, to avoid multiple
        // heartbeat requests for different UIs.
        this.registry.getHeartbeat().setInterval(-1);

        const uiId = configuration.getUIId();
        const json = JSON.parse(responseText) as Record<string, unknown>;
        const newUiId = json[UI_ID] as number;
        if (newUiId !== uiId) {
          console.debug(`UI ID switched from ${uiId} to ${newUiId} after resynchronization`);
          configuration.setUIId(newUiId);
        }
        this.registry.reset();

        this.registry.getUILifecycle().setState(UIState.RUNNING);
        this.registry.getMessageHandler().handleMessage(json);

        if (this.registry.getPushConfiguration().isPushEnabled()) {
          // The push connection may have been closed in response to server
          // session expiration. Reconnect before recreating the web components
          // so connected events can reach the server. Deferred so the current
          // request completes and the Set-Cookie header is processed first.
          getScheduler().scheduleDeferred(() => {
            console.debug('Re-establish PUSH connection');
            this.registry.getMessageSender().setPushEnabled(true);
            getScheduler().scheduleDeferred(() => this.recreateWebComponents());
          });
        } else {
          getScheduler().scheduleDeferred(() => this.recreateWebComponents());
        }
      },
      (error) => {
        this.registry.getHeartbeat().setInterval(configuration.getHeartbeatInterval());
        this.handleError(error.message);
      }
    );
  }

  /** Shows the configured session-expired notification. Mirrors handleSessionExpiredError. */
  handleSessionExpiredError(details: string | null): void {
    const message = this.registry.getApplicationConfiguration().getSessionExpiredError();
    this.handleUnrecoverableError(
      message?.caption ?? null,
      message?.message ?? null,
      details,
      message?.url ?? null,
      null
    );
  }

  /**
   * Shows an error notification for an unrecoverable error. With no caption,
   * message or details, redirects to {@code url} (reloads when null) instead.
   * Clicking the notification or pressing Escape performs that same redirect.
   * Mirrors SystemErrorHandler.handleUnrecoverableError.
   */
  // eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java handleUnrecoverableError signature
  handleUnrecoverableError(
    caption: string | null,
    message: string | null,
    details: string | null,
    url: string | null,
    querySelector: string | null
  ): void {
    if (caption === null && message === null && details === null) {
      if (!this.isWebComponentMode()) {
        redirect(url);
      } else {
        this.resynchronizeSession();
      }
      return;
    }

    const systemErrorContainer = handleError(caption, message, details, querySelector, (text) =>
      this.handleError(text)
    );
    if (!this.isWebComponentMode()) {
      systemErrorContainer.addEventListener('click', () => redirect(url), false);
      document.addEventListener(
        'keydown',
        (event) => {
          if (event.key === 'Escape') {
            event.preventDefault();
            redirect(url);
          }
        },
        false
      );
    }
  }
}
