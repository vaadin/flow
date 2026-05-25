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

import { Console } from './Console';
import { WidgetUtil } from './WidgetUtil';

interface PopoverElement extends Element {
  showPopover?: () => void;
}

interface ServerHookedElement extends Element {
  $server?: { disconnected?: () => void };
}

interface ErrorMessageLike {
  caption?: string | null;
  message?: string | null;
  url?: string | null;
  querySelector?: string | null;
}

/**
 * Adapter shape that Java {@code DefaultRegistry} provides so the TS class
 * can reach back into still-Java services (heartbeat, push, message
 * handling) without depending on the Java {@code Registry} facade.
 */
export interface SystemErrorHandlerCallbacks {
  getServiceUrl(): string;
  isWebComponentMode(): boolean;
  isProductionMode(): boolean;
  getSessionExpiredError(): ErrorMessageLike;
  getExportedWebComponents(): string[];
  getHeartbeatInterval(): number;
  setHeartbeatInterval(seconds: number): void;
  isPushEnabled(): boolean;
  setPushEnabled(enabled: boolean): void;
  disablePushImmediately(): void;
  applyResyncResponse(responseText: string): void;
}

// Mirrors ApplicationConstants.REQUEST_TYPE_PARAMETER and
// ApplicationConstants.REQUEST_TYPE_WEBCOMPONENT_RESYNC. Kept inline so the TS
// module doesn't need a flow-shared bridge.
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_WEBCOMPONENT_RESYNC = 'webcomponent-resync';

/**
 * Handles system errors in the application. Migrated from
 * {@code com.vaadin.client.SystemErrorHandler}. Construction takes a
 * {@link SystemErrorHandlerCallbacks} adapter so the TS class does not need
 * to reach back through the Java {@code Registry} facade.
 */
export class SystemErrorHandler {
  private resyncInProgress = false;
  private readonly callbacks: SystemErrorHandlerCallbacks;

  constructor(callbacks: SystemErrorHandlerCallbacks) {
    this.callbacks = callbacks;
  }

  /** Shows the session expiration notification. */
  handleSessionExpiredError(details: string | null): void {
    const sessionExpired = this.callbacks.getSessionExpiredError();
    this.handleUnrecoverableError(
      sessionExpired.caption ?? null,
      sessionExpired.message ?? null,
      details,
      sessionExpired.url ?? null,
      null
    );
  }

  // eslint-disable-next-line @typescript-eslint/max-params
  handleUnrecoverableError(
    caption: string | null,
    message: string | null,
    details: string | null,
    url: string | null,
    querySelector: string | null
  ): void {
    if (caption === null && message === null && details === null) {
      if (!this.callbacks.isWebComponentMode()) {
        WidgetUtil.redirect(url);
      } else {
        this.resynchronizeSession();
      }
      return;
    }

    const systemErrorContainer = this.renderError(caption, message, details, querySelector);
    if (!this.callbacks.isWebComponentMode()) {
      systemErrorContainer.addEventListener('click', () => WidgetUtil.redirect(url), false);
      document.addEventListener(
        'keydown',
        (e: Event) => {
          if ((e as KeyboardEvent).key === 'Escape') {
            e.preventDefault();
            WidgetUtil.redirect(url);
          }
        },
        false
      );
    }
  }

  handleError(errorMessage: string | null | undefined): void {
    Console.error(errorMessage as unknown as object);
  }

  private resynchronizeSession(): void {
    if (this.resyncInProgress) {
      Console.debug('Web components resynchronization already in progress');
      return;
    }
    this.resyncInProgress = true;
    const serviceUrl = `${this.callbacks.getServiceUrl()}web-component/web-component-bootstrap.js`;

    // Stop heart beat to prevent requests during resynchronization.
    this.callbacks.setHeartbeatInterval(-1);
    if (this.callbacks.isPushEnabled()) {
      this.callbacks.disablePushImmediately();
    }

    const separator = serviceUrl.indexOf('?') === -1 ? '?' : '&';
    const sessionResyncUri = `${serviceUrl}${separator}${REQUEST_TYPE_PARAMETER}=${REQUEST_TYPE_WEBCOMPONENT_RESYNC}`;

    const xhr = new XMLHttpRequest();
    xhr.open('GET', sessionResyncUri, true);
    xhr.withCredentials = true;
    xhr.onreadystatechange = () => {
      if (xhr.readyState !== 4) {
        return;
      }
      if (xhr.status >= 200 && xhr.status < 300) {
        this.onResyncSuccess(xhr);
      } else {
        this.callbacks.setHeartbeatInterval(this.callbacks.getHeartbeatInterval());
        this.handleError(`Resynchronization failed: HTTP ${xhr.status}`);
        this.resyncInProgress = false;
      }
    };
    xhr.send();
  }

  private onResyncSuccess(xhr: XMLHttpRequest): void {
    Console.log(`Received xhr HTTP session resynchronization message: ${xhr.responseText}`);

    // Make sure heartbeat has not been restarted. This is especially
    // important if the uiId gets reset after session expiration, to prevent
    // multiple heartbeat requests for different UIs.
    this.callbacks.setHeartbeatInterval(-1);

    this.callbacks.applyResyncResponse(xhr.responseText);

    const pushEnabled = this.callbacks.isPushEnabled();
    if (pushEnabled) {
      // PUSH connection might have been closed in response to server session
      // expiration. If PUSH is required, reconnect before recreating web
      // components to make sure the connected events can be propagated to the
      // server. PUSH reconnection is deferred to allow the current request to
      // complete and process the Set-Cookie header. setTimeout(fn, 0) matches
      // the original GWT Scheduler.scheduleDeferred semantics.
      setTimeout(() => {
        Console.debug('Re-establish PUSH connection');
        this.callbacks.setPushEnabled(true);
        setTimeout(() => this.recreateWebComponents(), 0);
      }, 0);
    } else {
      setTimeout(() => this.recreateWebComponents(), 0);
    }
  }

  private recreateWebComponents(): void {
    const exported = this.callbacks.getExportedWebComponents();
    for (const elementName of exported) {
      recreateNodes(elementName);
    }
    this.resyncInProgress = false;
  }

  private renderError(
    caption: string | null,
    message: string | null,
    details: string | null,
    querySelector: string | null
  ): Element {
    const systemErrorContainer = document.createElement('div');
    // Set the popover attribute for native popovers.
    systemErrorContainer.setAttribute('popover', 'manual');
    systemErrorContainer.className = 'v-system-error';

    if (caption !== null) {
      const captionDiv = document.createElement('div');
      captionDiv.className = 'caption';
      captionDiv.textContent = caption;
      systemErrorContainer.appendChild(captionDiv);
      Console.error(caption);
    }
    if (message !== null) {
      const messageDiv = document.createElement('div');
      messageDiv.className = 'message';
      messageDiv.textContent = message;
      systemErrorContainer.appendChild(messageDiv);
      Console.error(message);
    }
    if (details !== null) {
      const detailsDiv = document.createElement('div');
      detailsDiv.className = 'details';
      detailsDiv.textContent = details;
      systemErrorContainer.appendChild(detailsDiv);
      Console.error(details);
    }
    if (querySelector !== null) {
      const baseElement = document.querySelector(querySelector);
      if (baseElement !== null) {
        const target = (baseElement as Element & { shadowRoot: ShadowRoot | null }).shadowRoot ?? baseElement;
        target.appendChild(systemErrorContainer);
      }
    } else {
      document.body.appendChild(systemErrorContainer);
    }
    showPopover(systemErrorContainer);
    return systemErrorContainer;
  }
}

function recreateNodes(elementName: string): void {
  const elements = document.getElementsByTagName(elementName);
  for (const elem of Array.from(elements) as ServerHookedElement[]) {
    if (elem.$server) {
      // Mock disconnected callback to avoid TypeError when the placeholder is
      // replaced before the real $server has been installed.
      elem.$server.disconnected = () => {};
    }
    elem.parentNode?.replaceChild(elem.cloneNode(false), elem);
  }
}

function showPopover(el: Element | null | undefined): void {
  const candidate = el as PopoverElement | null | undefined;
  if (candidate && typeof candidate.showPopover === 'function') {
    candidate.showPopover();
  }
}
