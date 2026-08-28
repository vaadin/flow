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

// Implementations migrated from SystemErrorHandler.java.

import type { ErrorMessage } from './ApplicationConfiguration';
import { addGetParameters } from '../flow/shared/util/SharedUtil';
import { getScheduler } from './TrackingScheduler';
import { UIState } from './UILifecycle';
import { redirect } from './WidgetUtil';
import { Console } from './Console';

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
function recreateNodes(elementName: string): void {
  // Snapshot the live collection before mutating it.
  const elements = Array.from(document.getElementsByTagName(elementName)) as Array<
    Element & {
      $server: { disconnected: () => void };
    }
  >;
  for (const elem of elements) {
    // Mock the disconnected callback so it does not throw a TypeError.
    elem.$server.disconnected = () => {};
    // Java dereferences parentNode unguarded, so a detached element fails here
    // rather than silently keeping the stale node.
    elem.parentNode!.replaceChild(elem.cloneNode(false), elem);
  }
}

/** Invokes the native showPopover() of the element if it supports it. */
function showPopover(el: Element): void {
  const fn = el && (el as Element & { showPopover?: () => void }).showPopover;
  if (typeof fn === 'function') {
    fn.call(el);
  }
}

/** Returns the shadow root of the given host element, if any. */
function getShadowRootElement(host: Element): ShadowRoot | null {
  return host.shadowRoot;
}

// Builds and shows the system error notification for an unrecoverable error.
// Java documents none of this (its private overload has no Javadoc): each
// provided part becomes a labelled div and is also logged through Console, and
// when a querySelector is given the notification goes inside the matching
// element - its shadow root if it has one - rather than the body. The container
// is returned even when the selector matched nothing, as the caller relies on.

function handleError(
  caption: string | null,
  message: string | null,
  details: string | null,
  querySelector: string | null
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
      Console.error(text);
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

/** The slice of Registry SystemErrorHandler uses. */
interface SystemErrorRegistry {
  getApplicationConfiguration(): {
    isWebComponentMode(): boolean;
    getExportedWebComponents(): string[];
    getSessionExpiredError(): ErrorMessage | null;
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

/**
 * Handles system errors in the application.
 */
export class SystemErrorHandler {
  readonly #registry: SystemErrorRegistry;

  #resyncInProgress = false;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: SystemErrorRegistry) {
    this.#registry = registry;
  }

  /**
   * Shows the given error message if not running in production mode and logs
   * it to the console if running in production mode.
   *
   * @param errorMessage - the error message to show
   */
  handleError(errorMessage: string): void {
    Console.error(errorMessage);
  }

  /**
   * Shows the given error message if not running in production mode and logs it
   * to the console.
   *
   * @param error - the throwable which occurred
   */
  handleErrorObject(error: unknown): void {
    // Java distinguishes an AssertionError, whose message alone does not say
    // what kind of failure it was.
    if (error instanceof Error && error.name === 'AssertionError') {
      this.handleError(`Assertion error: ${error.message}`);
    } else {
      this.handleError(error instanceof Error ? error.message : String(error));
    }
  }

  /** Whether the application runs in web-component (embedded) mode. */
  #isWebComponentMode(): boolean {
    return this.#registry.getApplicationConfiguration().isWebComponentMode();
  }

  /** Recreates every exported web component's elements (detaching stale ones). */
  #recreateWebComponents(): void {
    for (const elementName of this.#registry.getApplicationConfiguration().getExportedWebComponents()) {
      recreateNodes(elementName);
    }
    this.#resyncInProgress = false;
  }

  /**
   * Resynchronizes a web-component (embedded) session after server-side session
   * expiration: requests a fresh JSESSIONID, resets the registry, replays the
   * returned UIDL and re-establishes push, then recreates the exported web
   * components. Mirrors SystemErrorHandler.resynchronizeSession.
   */
  #resynchronizeSession(): void {
    if (this.#resyncInProgress) {
      Console.debug('Web components resynchronization already in progress');
      return;
    }
    this.#resyncInProgress = true;

    const configuration = this.#registry.getApplicationConfiguration();
    const serviceUrl = `${configuration.getServiceUrl()}web-component/web-component-bootstrap.js`;

    // Stop the heartbeat to prevent requests during resynchronization.
    this.#registry.getHeartbeat().setInterval(-1);
    if (this.#registry.getPushConfiguration().isPushEnabled()) {
      this.#registry.getMessageSender().setPushEnabled(false, false);
    }

    const sessionResyncUri = addGetParameters(
      serviceUrl,
      `${REQUEST_TYPE_PARAMETER}=${REQUEST_TYPE_WEBCOMPONENT_RESYNC}`
    );

    getWithCredentials(
      sessionResyncUri,
      (responseText) => {
        Console.log(`Received xhr HTTP session resynchronization message: ${responseText}`);

        // Make sure the heartbeat has not been restarted; especially important
        // if the uiId is reset after session expiration, to avoid multiple
        // heartbeat requests for different UIs.
        this.#registry.getHeartbeat().setInterval(-1);

        const uiId = configuration.getUIId();
        const json = JSON.parse(responseText) as Record<string, unknown>;
        const newUiId = json[UI_ID] as number;
        if (newUiId !== uiId) {
          Console.debug(`UI ID switched from ${uiId} to ${newUiId} after resynchronization`);
          configuration.setUIId(newUiId);
        }
        this.#registry.reset();

        this.#registry.getUILifecycle().setState(UIState.RUNNING);
        this.#registry.getMessageHandler().handleMessage(json);

        if (this.#registry.getPushConfiguration().isPushEnabled()) {
          // The push connection may have been closed in response to server
          // session expiration. Reconnect before recreating the web components
          // so connected events can reach the server. Deferred so the current
          // request completes and the Set-Cookie header is processed first.
          getScheduler().scheduleDeferred(() => {
            Console.debug('Re-establish PUSH connection');
            this.#registry.getMessageSender().setPushEnabled(true);
            getScheduler().scheduleDeferred(() => this.#recreateWebComponents());
          });
        } else {
          getScheduler().scheduleDeferred(() => this.#recreateWebComponents());
        }
      },
      (error) => {
        this.#registry.getHeartbeat().setInterval(configuration.getHeartbeatInterval());
        this.handleError(error.message);
      }
    );
  }

  /**
   * Shows the session expiration notification.
   *
   * @param details - message details or null if there are no details
   */
  handleSessionExpiredError(details: string | null): void {
    this.handleUnrecoverableErrorFor(details, this.#registry.getApplicationConfiguration().getSessionExpiredError());
  }

  /**
   * Shows an error notification for an error which is unrecoverable.
   *
   * Named apart from the caption/message/details overloads, which JavaScript
   * cannot distinguish by arity alone.
   *
   * @param details - message details or null if there are no details
   * @param message - an ErrorMessage describing the error
   */
  protected handleUnrecoverableErrorFor(details: string | null, message: ErrorMessage | null): void {
    // Java dereferences the message unguarded, so a missing session-expired
    // message fails here rather than silently reloading the page.
    const errorMessage = message!;
    this.handleUnrecoverableError(
      errorMessage.caption ?? null,
      errorMessage.message ?? null,
      details,
      errorMessage.url ?? null,
      null
    );
  }

  /**
   * Shows an error notification for an error which is unrecoverable, using the
   * given parameters.
   *
   * @param caption - the caption of the message
   * @param message - the message body
   * @param details - message details or `null` if there are no details
   * @param url - a URL to redirect to when the user clicks the message or
   *          `null` to refresh on click
   * @param querySelector - query selector to find the element under which the
   *          error will be added . If element is not found or the selector is
   *          `null`, body will be used
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
      if (!this.#isWebComponentMode()) {
        redirect(url);
      } else {
        this.#resynchronizeSession();
      }
      return;
    }

    const systemErrorContainer = handleError(caption, message, details, querySelector);
    if (!this.#isWebComponentMode()) {
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
