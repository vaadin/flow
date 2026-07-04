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

import { html, LitElement, type PropertyValues } from 'lit';
import { property, state } from 'lit/decorators.js';
import { classMap } from 'lit/directives/class-map.js';
import { ConnectionState, type ConnectionStateStore } from './ConnectionState';

const DEFAULT_STYLE_ID = 'css-loading-indicator';

/**
 * The loading indicator states
 */
export const enum LoadingBarState {
  IDLE = '',
  FIRST = 'first',
  SECOND = 'second',
  THIRD = 'third'
}

declare global {
  interface HTMLElementTagNameMap {
    'vaadin-connection-indicator': ConnectionIndicator;
  }
}

/**
 * Component showing loading and connection indicator. When added to DOM,
 * listens for changes on `window.Vaadin.connectionState` ConnectionStateStore.
 */
export class ConnectionIndicator extends LitElement {
  static get instance(): ConnectionIndicator {
    return ConnectionIndicator.create();
  }

  /**
   * Initialize global connection indicator instance at
   * window.Vaadin.connectionIndicator and add instance to the document body.
   */
  static create(): ConnectionIndicator {
    const $wnd = window as any;
    if (!$wnd.Vaadin?.connectionIndicator) {
      $wnd.Vaadin ??= {};
      $wnd.Vaadin.connectionIndicator = document.createElement('vaadin-connection-indicator');
      document.body.appendChild($wnd.Vaadin.connectionIndicator);
    }
    return $wnd.Vaadin?.connectionIndicator as ConnectionIndicator;
  }

  /**
   * The delay before showing the loading indicator, in ms.
   */
  @property({ type: Number })
  accessor firstDelay = 450;

  /**
   * The delay before the loading indicator goes into "second" state, in ms.
   */
  @property({ type: Number })
  accessor secondDelay = 1500;

  /**
   * The delay before the loading indicator goes into "third" state, in ms.
   */
  @property({ type: Number })
  accessor thirdDelay = 5000;

  /**
   * The duration for which the connection state change message is visible,
   * in ms.
   */
  @property({ type: Number })
  accessor expandedDuration = 2000;

  /**
   * The message shown when the connection goes to connected state.
   */
  @property({ type: String })
  accessor onlineText = 'Online';

  /**
   * The message shown when the connection goes to lost state.
   */
  @property({ type: String })
  accessor offlineText = 'Connection lost';

  /**
   * The message shown when the connection goes to reconnecting state.
   */
  @property({ type: String })
  accessor reconnectingText = 'Connection lost, trying to reconnect...';

  @property({ type: Boolean, reflect: true })
  accessor offline = false;

  @property({ type: Boolean, reflect: true })
  accessor reconnecting = false;

  @property({ type: Boolean, reflect: true })
  accessor expanded = false;

  @property({ type: Boolean, reflect: true })
  accessor loading = false;

  @state()
  accessor #loadingBarState: LoadingBarState = LoadingBarState.IDLE;

  accessor #isPopover: boolean = false;

  #applyDefaultThemeState = true;

  #firstTimeout = 0;

  #secondTimeout = 0;

  #thirdTimeout = 0;

  #expandedTimeout = 0;

  #connectionStateStore?: ConnectionStateStore;

  readonly connectionStateListener: () => void;

  #lastMessageState: ConnectionState = ConnectionState.CONNECTED;

  constructor() {
    super();

    this.connectionStateListener = () => {
      this.expanded = this.#updateConnectionState();
      this.#expandedTimeout = this.#timeoutFor(
        this.#expandedTimeout,
        this.expanded,
        () => {
          this.expanded = false;
        },
        this.expandedDuration
      );
    };
  }

  protected override render() {
    return html`
      <div class="v-loading-indicator ${this.#loadingBarState}" style=${this.#getLoadingBarStyle()}></div>

      <div
        class="v-status-message ${classMap({
          active: this.reconnecting
        })}"
      >
        <span class="text"> ${this.#renderMessage()} </span>
      </div>
    `;
  }

  override connectedCallback() {
    super.connectedCallback();

    this.#initPopover();

    const $wnd = window as any;
    if ($wnd.Vaadin?.connectionState) {
      this.#connectionStateStore = $wnd.Vaadin.connectionState as ConnectionStateStore;
      this.#connectionStateStore.addStateChangeListener(this.connectionStateListener);
      this.#updateConnectionState();
    }

    this.#updateTheme();
  }

  override disconnectedCallback() {
    super.disconnectedCallback();

    if (this.#connectionStateStore) {
      this.#connectionStateStore.removeStateChangeListener(this.connectionStateListener);
    }

    this.#updateTheme();
    this.#isPopover = false;
  }

  protected override updated(props: PropertyValues): void {
    if (['loading', 'offline', 'reconnecting', 'expanded'].some((p) => props.has(p))) {
      this.#updatePopoverState();
    }
  }

  get applyDefaultTheme() {
    return this.#applyDefaultThemeState;
  }

  @property({ type: Boolean, reflect: true })
  set applyDefaultTheme(applyDefaultTheme: boolean) {
    if (applyDefaultTheme !== this.#applyDefaultThemeState) {
      this.#applyDefaultThemeState = applyDefaultTheme;
      this.#updateTheme();
    }
  }

  protected override createRenderRoot() {
    return this;
  }

  #initPopover() {
    // Allow showing the indicator as popover
    this.setAttribute('popover', 'manual');
    // Override user agent styles for popover
    this.style.display = 'contents';
    this.style.width = 'auto';
    this.style.height = 'auto';
    this.style.top = '0';
    this.style.right = '0';
    this.style.bottom = 'auto';
    this.style.left = '0';
    this.style.margin = '0';
    this.style.padding = '0';
    this.style.background = 'none';
    this.style.border = 'none';
  }

  /**
   * Update state flags.
   *
   * @returns true if the connection message changes, and therefore a new
   * message should be shown
   */
  #updateConnectionState(): boolean {
    const connectionState = this.#connectionStateStore?.state;
    this.offline = connectionState === ConnectionState.CONNECTION_LOST;
    this.reconnecting = connectionState === ConnectionState.RECONNECTING;
    this.#updateLoading(connectionState === ConnectionState.LOADING);
    if (this.loading) {
      // Entering loading state, do not show message
      return false;
    }

    if (connectionState !== this.#lastMessageState) {
      this.#lastMessageState = connectionState!;
      // Message changes, show new message
      return true;
    }

    // Message did not change
    return false;
  }

  #updateLoading(loading: boolean) {
    this.loading = loading;
    this.#loadingBarState = LoadingBarState.IDLE;

    this.#firstTimeout = this.#timeoutFor(
      this.#firstTimeout,
      loading,
      () => {
        this.#loadingBarState = LoadingBarState.FIRST;
      },
      this.firstDelay
    );

    this.#secondTimeout = this.#timeoutFor(
      this.#secondTimeout,
      loading,
      () => {
        this.#loadingBarState = LoadingBarState.SECOND;
      },
      this.secondDelay
    );

    this.#thirdTimeout = this.#timeoutFor(
      this.#thirdTimeout,
      loading,
      () => {
        this.#loadingBarState = LoadingBarState.THIRD;
      },
      this.thirdDelay
    );
  }

  #updatePopoverState() {
    const showPopover = this.loading || this.offline || this.reconnecting || this.expanded;

    // Always close the popover first on state changes. This way, on every state change,
    // showPopover is called again, resulting in the connection indicator being shown on
    // top of other popovers that might have been added, for example after a reconnect.
    if (this.#isPopover) {
      this.hidePopover();
    }
    if (showPopover) {
      this.showPopover();
    }
    this.#isPopover = showPopover;
  }

  #renderMessage() {
    if (this.reconnecting) {
      return this.reconnectingText;
    }

    if (this.offline) {
      return this.offlineText;
    }

    return this.onlineText;
  }

  #updateTheme() {
    if (this.#applyDefaultThemeState && this.isConnected) {
      if (!document.getElementById(DEFAULT_STYLE_ID)) {
        const style = document.createElement('style');
        style.id = DEFAULT_STYLE_ID;
        style.textContent = this.#getDefaultStyle();
        document.head.appendChild(style);
      }
    } else {
      const style = document.getElementById(DEFAULT_STYLE_ID);
      if (style) {
        document.head.removeChild(style);
      }
    }
  }

  #getDefaultStyle(): string {
    return `
      @keyframes v-progress-start {
        0% {
          width: 0%;
        }
        100% {
          width: 50%;
        }
      }
      @keyframes v-progress-delay {
        0% {
          width: 50%;
        }
        100% {
          width: 90%;
        }
      }
      @keyframes v-progress-wait {
        0% {
          width: 90%;
          height: 4px;
        }
        3% {
          width: 91%;
          height: 7px;
        }
        100% {
          width: 96%;
          height: 7px;
        }
      }
      @keyframes v-progress-wait-pulse {
        0% {
          opacity: 1;
        }
        50% {
          opacity: 0.1;
        }
        100% {
          opacity: 1;
        }
      }
      .v-loading-indicator,
      .v-status-message {
        box-sizing: border-box;
        position: fixed;
        left: 0;
        right: 0;
        top: 0;
        background-color: var(--lumo-primary-color, var(--material-primary-color, blue));
        transition: none;
      }
      .v-loading-indicator {
        right: auto;
        width: 50%;
        height: 4px;
        opacity: 1;
        pointer-events: none;
        animation: v-progress-start 1000ms 200ms both;
      }
      .v-loading-indicator[style*='none'] {
        display: block !important;
        width: 100%;
        opacity: 0;
        animation: none;
        transition: opacity 500ms 300ms, width 300ms;
      }
      .v-loading-indicator.second {
        width: 90%;
        animation: v-progress-delay 3.8s forwards;
      }
      .v-loading-indicator.third {
        width: 96%;
        animation: v-progress-wait 5s forwards, v-progress-wait-pulse 1s 4s infinite backwards;
      }

      vaadin-connection-indicator[offline] .v-loading-indicator,
      vaadin-connection-indicator[reconnecting] .v-loading-indicator {
        display: none;
      }

      .v-status-message {
        opacity: 0;
        pointer-events: none;
        max-height: var(--status-height-collapsed, 8px);
        overflow: hidden;
        background-color: var(--status-bg-color-online, var(--lumo-primary-color, var(--material-primary-color, blue)));
        color: var(
          --status-text-color-online,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        font-size: 0.75rem;
        font-weight: 600;
        line-height: 1;
        transition: all 0.5s;
        padding: 0 0.5em;
      }

      vaadin-connection-indicator[offline] .v-status-message,
      vaadin-connection-indicator[reconnecting] .v-status-message {
        opacity: 1;
        pointer-events: auto;
        background-color: var(--status-bg-color-offline, var(--lumo-shade, #333));
        color: var(
          --status-text-color-offline,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        background-image: repeating-linear-gradient(
          45deg,
          rgba(255, 255, 255, 0),
          rgba(255, 255, 255, 0) 10px,
          rgba(255, 255, 255, 0.1) 10px,
          rgba(255, 255, 255, 0.1) 20px
        );
      }

      vaadin-connection-indicator[reconnecting] .v-status-message {
        animation: show-reconnecting-status 2s;
      }

      vaadin-connection-indicator[offline] .v-status-message:hover,
      vaadin-connection-indicator[reconnecting] .v-status-message:hover,
      vaadin-connection-indicator[expanded] .v-status-message {
        max-height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[expanded] .v-status-message {
        opacity: 1;
        pointer-events: auto;
      }

      .v-status-message span {
        display: flex;
        align-items: center;
        justify-content: center;
        height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[reconnecting] .v-status-message span::before {
        content: '';
        width: 1em;
        height: 1em;
        border-top: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-left: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-right: 2px solid transparent;
        border-bottom: 2px solid transparent;
        border-radius: 50%;
        box-sizing: border-box;
        animation: v-spin 0.4s linear infinite;
        margin: 0 0.5em;
      }

      @keyframes v-spin {
        100% {
          transform: rotate(360deg);
        }
      }
    `;
  }

  #getLoadingBarStyle(): string {
    switch (this.#loadingBarState) {
      case LoadingBarState.IDLE:
        return 'display: none';
      case LoadingBarState.FIRST:
      case LoadingBarState.SECOND:
      case LoadingBarState.THIRD:
        return 'display: block';
      default:
        return '';
    }
  }

  #timeoutFor(timeoutId: number, enabled: boolean, handler: () => void, delay: number): number {
    if (timeoutId !== 0) {
      window.clearTimeout(timeoutId);
    }

    return enabled ? window.setTimeout(handler, delay) : 0;
  }
}

if (customElements.get('vaadin-connection-indicator') === undefined) {
  customElements.define('vaadin-connection-indicator', ConnectionIndicator);
}

/**
 * The global connection indicator object. Its appearance and behavior can be
 * configured via properties:
 *
 * connectionIndicator.firstDelay = 0;
 * connectionIndicator.onlineText = 'The application is online';
 *
 * To avoid altering the appearance while the indicator is active, apply the
 * configuration in your application 'frontend/index.ts' file.
 */
export const connectionIndicator = ConnectionIndicator.instance;
