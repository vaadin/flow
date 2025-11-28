import { css, html, LitElement } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';
import { handleLicenseMessage, licenseCheckOk, licenseInit, Product } from './License';
import { ConnectionStatus } from './connection';
import { LiveReloadConnection } from './live-reload-connection';
import { WebSocketConnection } from './websocket-connection';
import { preTrialStartFailed, updateLicenseDownloadStatus } from './pre-trial-splash-screen';

/**
 * Plugin API for the dev tools window.
 */
export interface DevToolsInterface {
  send(command: string, data: any): void;
}

export interface MessageHandler {
  handleMessage(message: ServerMessage): boolean;
}

export interface ServerMessage {
  /**
   * The command
   */
  command: string;
  /**
   * the data for the command
   */
  data: any;
}

/**
 * To create and register a plugin, use e.g.
 * @example
 * export class MyTab extends LitElement implements MessageHandler {
 *   render() {
 *     return html`<div>Here I am</div>`;
 *   }
 * }
 * customElements.define('my-tab', MyTab);
 *
 * const plugin: DevToolsPlugin = {
 *   init: function (devToolsInterface: DevToolsInterface): void {
 *     devToolsInterface.addTab('Tab title', 'my-tab')
 *   }
 * };
 *
 * (window as any).Vaadin.devToolsPlugins.push(plugin);
 */
export interface DevToolsPlugin {
  /**
   * Called once to initialize the plugin.
   *
   * @param devToolsInterface provides methods to interact with the dev tools
   */
  init(devToolsInterface: DevToolsInterface): void;
}

export enum MessageType {
  LOG = 'log',
  INFORMATION = 'information',
  WARNING = 'warning',
  ERROR = 'error'
}

interface Message {
  id: number;
  type: MessageType;
  message: string;
  details?: string;
  link?: string;
  persistentId?: string;
  dontShowAgain: boolean;
  dontShowAgainMessage?: string;
  deleted: boolean;
}

type DevToolsConf = {
  enable: boolean;
  url: string;
  contextRelativePath: string;
  backend?: string;
  liveReloadPort?: number;
  token?: string;
};

// @ts-ignore
const hmrClient: any = import.meta.hot ? import.meta.hot.hmrClient : undefined;

@customElement('vaadin-dev-tools')
export class VaadinDevTools extends LitElement {
  unhandledMessages: ServerMessage[] = [];
  conf: DevToolsConf = { enable: false, url: '', contextRelativePath: '', liveReloadPort: -1 };
  bodyShadowRoot: ShadowRoot | null = null;

  static get styles() {
    return [
      css`
        :host {
          --dev-tools-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen-Sans, Ubuntu, Cantarell,
            'Helvetica Neue', sans-serif;
          --dev-tools-font-family-monospace: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
            monospace;

          --dev-tools-font-size: 0.8125rem;
          --dev-tools-font-size-small: 0.75rem;

          --dev-tools-text-color: rgba(255, 255, 255, 0.8);
          --dev-tools-text-color-secondary: rgba(255, 255, 255, 0.65);
          --dev-tools-text-color-emphasis: rgba(255, 255, 255, 0.95);
          --dev-tools-text-color-active: rgba(255, 255, 255, 1);

          --dev-tools-background-color-inactive: rgba(45, 45, 45, 0.25);
          --dev-tools-background-color-active: rgba(45, 45, 45, 0.98);
          --dev-tools-background-color-active-blurred: rgba(45, 45, 45, 0.85);

          --dev-tools-border-radius: 0.5rem;
          --dev-tools-box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.4);

          --dev-tools-blue-hsl: 206, 100%, 70%;
          --dev-tools-blue-color: hsl(var(--dev-tools-blue-hsl));
          --dev-tools-green-hsl: 145, 80%, 42%;
          --dev-tools-green-color: hsl(var(--dev-tools-green-hsl));
          --dev-tools-grey-hsl: 0, 0%, 50%;
          --dev-tools-grey-color: hsl(var(--dev-tools-grey-hsl));
          --dev-tools-yellow-hsl: 38, 98%, 64%;
          --dev-tools-yellow-color: hsl(var(--dev-tools-yellow-hsl));
          --dev-tools-red-hsl: 355, 100%, 68%;
          --dev-tools-red-color: hsl(var(--dev-tools-red-hsl));

          /* Needs to be in ms, used in JavaScript as well */
          --dev-tools-transition-duration: 180ms;

          all: initial;

          direction: ltr;
          cursor: default;
          font: normal 400 var(--dev-tools-font-size) / 1.125rem var(--dev-tools-font-family);
          color: var(--dev-tools-text-color);
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
          color-scheme: dark;

          position: fixed;
          z-index: 20000;
          pointer-events: none;
          bottom: 0;
          right: 0;
          width: 100%;
          height: 100%;
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
        }

        .dev-tools {
          pointer-events: auto;
          display: flex;
          align-items: center;
          position: fixed;
          z-index: inherit;
          right: 0.5rem;
          bottom: 0.5rem;
          min-width: 1.75rem;
          height: 1.75rem;
          max-width: 1.75rem;
          border-radius: 0.5rem;
          padding: 0.375rem;
          box-sizing: border-box;
          background-color: var(--dev-tools-background-color-inactive);
          box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          white-space: nowrap;
          line-height: 1rem;
        }

        .dev-tools:hover,
        .dev-tools.active {
          background-color: var(--dev-tools-background-color-active);
          box-shadow: var(--dev-tools-box-shadow);
        }

        .dev-tools.active {
          max-width: calc(100% - 1rem);
        }

        .dev-tools .status-description {
          overflow: hidden;
          text-overflow: ellipsis;
          padding: 0 0.25rem;
        }

        .dev-tools.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.15);
          animation: bounce 0.5s;
          animation-iteration-count: 2;
        }

        .window.hidden {
          opacity: 0;
          transform: scale(0);
          position: absolute;
        }

        .window.visible {
          transform: none;
          opacity: 1;
          pointer-events: auto;
        }

        .window.visible ~ .dev-tools {
          opacity: 0;
          pointer-events: none;
        }

        .window.visible ~ .dev-tools .dev-tools-icon,
        .window.visible ~ .dev-tools .status-blip {
          transition: none;
          opacity: 0;
        }

        .window {
          border-radius: var(--dev-tools-border-radius);
          overflow: auto;
          margin: 0.5rem;
          min-width: 30rem;
          max-width: calc(100% - 1rem);
          max-height: calc(100vh - 1rem);
          flex-shrink: 1;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          display: flex;
          flex-direction: column;
          box-shadow: var(--dev-tools-box-shadow);
          outline: none;
        }

        .window-toolbar {
          display: flex;
          flex: none;
          align-items: center;
          padding: 0.375rem;
          white-space: nowrap;
          order: 1;
          background-color: rgba(0, 0, 0, 0.2);
          gap: 0.5rem;
        }

        .ahreflike {
          font-weight: 500;
          color: var(--dev-tools-text-color-secondary);
          text-decoration: underline;
          cursor: pointer;
        }

        .ahreflike:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .button {
          all: initial;
          font-family: inherit;
          font-size: var(--dev-tools-font-size-small);
          line-height: 1;
          white-space: nowrap;
          background-color: rgba(0, 0, 0, 0.2);
          color: inherit;
          font-weight: 600;
          padding: 0.25rem 0.375rem;
          border-radius: 0.25rem;
        }

        .button:focus,
        .button:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .message.information {
          --dev-tools-notification-color: var(--dev-tools-blue-color);
        }

        .message.warning {
          --dev-tools-notification-color: var(--dev-tools-yellow-color);
        }

        .message.error {
          --dev-tools-notification-color: var(--dev-tools-red-color);
        }

        .message {
          display: flex;
          padding: 0.1875rem 0.75rem 0.1875rem 2rem;
          background-clip: padding-box;
        }

        .message.log {
          padding-left: 0.75rem;
        }

        .message-content {
          margin-right: 0.5rem;
          -webkit-user-select: text;
          -moz-user-select: text;
          user-select: text;
        }

        .message-heading {
          position: relative;
          display: flex;
          align-items: center;
          margin: 0.125rem 0;
        }

        .message.log {
          color: var(--dev-tools-text-color-secondary);
        }

        .message:not(.log) .message-heading {
          font-weight: 500;
        }

        .message.has-details .message-heading {
          color: var(--dev-tools-text-color-emphasis);
          font-weight: 600;
        }

        .message-heading::before {
          position: absolute;
          margin-left: -1.5rem;
          display: inline-block;
          text-align: center;
          font-size: 0.875em;
          font-weight: 600;
          line-height: calc(1.25em - 2px);
          width: 14px;
          height: 14px;
          box-sizing: border-box;
          border: 1px solid transparent;
          border-radius: 50%;
        }

        .message.information .message-heading::before {
          content: 'i';
          border-color: currentColor;
          color: var(--dev-tools-notification-color);
        }

        .message.warning .message-heading::before,
        .message.error .message-heading::before {
          content: '!';
          color: var(--dev-tools-background-color-active);
          background-color: var(--dev-tools-notification-color);
        }

        .features-tray {
          padding: 0.75rem;
          flex: auto;
          overflow: auto;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          user-select: text;
        }

        .features-tray p {
          margin-top: 0;
          color: var(--dev-tools-text-color-secondary);
        }

        .features-tray .feature {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding-bottom: 0.5em;
        }

        .message .message-details {
          font-weight: 400;
          color: var(--dev-tools-text-color-secondary);
          margin: 0.25rem 0;
        }

        .message .message-details[hidden] {
          display: none;
        }

        .message .message-details p {
          display: inline;
          margin: 0;
          margin-right: 0.375em;
          word-break: break-word;
        }

        .message .persist {
          color: var(--dev-tools-text-color-secondary);
          white-space: nowrap;
          margin: 0.375rem 0;
          display: flex;
          align-items: center;
          position: relative;
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
        }

        .message .persist::before {
          content: '';
          width: 1em;
          height: 1em;
          border-radius: 0.2em;
          margin-right: 0.375em;
          background-color: rgba(255, 255, 255, 0.3);
        }

        .message .persist:hover::before {
          background-color: rgba(255, 255, 255, 0.4);
        }

        .message .persist.on::before {
          background-color: rgba(255, 255, 255, 0.9);
        }

        .message .persist.on::after {
          content: '';
          order: -1;
          position: absolute;
          width: 0.75em;
          height: 0.25em;
          border: 2px solid var(--dev-tools-background-color-active);
          border-width: 0 0 2px 2px;
          transform: translate(0.05em, -0.05em) rotate(-45deg) scale(0.8, 0.9);
        }

        .message .dismiss-message {
          font-weight: 600;
          align-self: stretch;
          display: flex;
          align-items: center;
          padding: 0 0.25rem;
          margin-left: 0.5rem;
          color: var(--dev-tools-text-color-secondary);
        }

        .message .dismiss-message:hover {
          color: var(--dev-tools-text-color);
        }

        .notification-tray {
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
          margin: 0.5rem;
          flex: none;
        }

        .window.hidden + .notification-tray {
          margin-bottom: 3rem;
        }

        .notification-tray .message {
          pointer-events: auto;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          max-width: 30rem;
          box-sizing: border-box;
          border-radius: var(--dev-tools-border-radius);
          margin-top: 0.5rem;
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          animation: slideIn var(--dev-tools-transition-duration);
          box-shadow: var(--dev-tools-box-shadow);
          padding-top: 0.25rem;
          padding-bottom: 0.25rem;
        }

        .notification-tray .message.animate-out {
          animation: slideOut forwards var(--dev-tools-transition-duration);
        }

        .notification-tray .message .message-details {
          max-height: 10em;
          overflow: hidden;
        }

        .message-tray {
          flex: auto;
          overflow: auto;
          max-height: 20rem;
          user-select: text;
        }

        .message-tray .message {
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          padding-left: 2.25rem;
        }

        .message-tray .message.warning {
          background-color: hsla(var(--dev-tools-yellow-hsl), 0.09);
        }

        .message-tray .message.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.09);
        }

        .message-tray .message.error .message-heading {
          color: hsl(var(--dev-tools-red-hsl));
        }

        .message-tray .message.warning .message-heading {
          color: hsl(var(--dev-tools-yellow-hsl));
        }

        .message-tray .message + .message {
          border-top: 1px solid rgba(255, 255, 255, 0.07);
        }

        .message-tray .dismiss-message,
        .message-tray .persist {
          display: none;
        }

        @keyframes slideIn {
          from {
            transform: translateX(100%);
            opacity: 0;
          }
          to {
            transform: translateX(0%);
            opacity: 1;
          }
        }

        @keyframes slideOut {
          from {
            transform: translateX(0%);
            opacity: 1;
          }
          to {
            transform: translateX(100%);
            opacity: 0;
          }
        }

        @keyframes fade-in {
          0% {
            opacity: 0;
          }
        }

        @keyframes bounce {
          0% {
            transform: scale(0.8);
          }
          50% {
            transform: scale(1.5);
            background-color: hsla(var(--dev-tools-red-hsl), 1);
          }
          100% {
            transform: scale(1);
          }
        }

        @supports (backdrop-filter: blur(1px)) {
          .dev-tools,
          .window,
          .notification-tray .message {
            backdrop-filter: blur(8px);
          }

          .dev-tools:hover,
          .dev-tools.active,
          .window,
          .notification-tray .message {
            background-color: var(--dev-tools-background-color-active-blurred);
          }
        }
      `
    ];
  }

  static DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE = 'vaadin.live-reload.dismissedNotifications';
  static ACTIVE_KEY_IN_SESSION_STORAGE = 'vaadin.live-reload.active';
  static TRIGGERED_KEY_IN_SESSION_STORAGE = 'vaadin.live-reload.triggered';
  static TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE = 'vaadin.live-reload.triggeredCount';

  static AUTO_DEMOTE_NOTIFICATION_DELAY = 5000;

  static HOTSWAP_AGENT = 'HOTSWAP_AGENT';
  static JREBEL = 'JREBEL';
  static SPRING_BOOT_DEVTOOLS = 'SPRING_BOOT_DEVTOOLS';
  static BACKEND_DISPLAY_NAME: Record<string, string> = {
    HOTSWAP_AGENT: 'HotswapAgent',
    JREBEL: 'JRebel',
    SPRING_BOOT_DEVTOOLS: 'Spring Boot Devtools'
  };

  static get isActive() {
    const active = window.sessionStorage.getItem(VaadinDevTools.ACTIVE_KEY_IN_SESSION_STORAGE);
    return active === null || active !== 'false';
  }

  @property({ type: String, attribute: false })
  frontendStatus: ConnectionStatus = ConnectionStatus.UNAVAILABLE;

  @property({ type: String, attribute: false })
  javaStatus: ConnectionStatus = ConnectionStatus.UNAVAILABLE;

  @query('.window')
  private root!: HTMLElement;

  @state()
  componentPickActive: boolean = false;

  private javaConnection?: LiveReloadConnection;
  private frontendConnection?: WebSocketConnection;

  private nextMessageId: number = 1;

  private transitionDuration: number = 0;

  elementTelemetry() {
    let data = {};
    try {
      // localstorage data is collected by vaadin-usage-statistics.js
      const localStorageStatsString = localStorage.getItem('vaadin.statistics.basket');
      if (!localStorageStatsString) {
        // Do not send empty data
        return;
      }
      data = JSON.parse(localStorageStatsString);
    } catch (e) {
      // In case of parse errors don't send anything
      return;
    }

    if (this.frontendConnection) {
      this.frontendConnection.send('reportTelemetry', { browserData: data });
    }
  }

  openWebSocketConnection() {
    this.frontendStatus = ConnectionStatus.UNAVAILABLE;
    this.javaStatus = ConnectionStatus.UNAVAILABLE;
    if (!this.conf.token) {
      console.error(
        'Dev tools functionality denied for this host. See Vaadin documentation on how to configure devmode.hostsAllowed property: https://vaadin.com/docs/latest/configuration/properties#properties'
      );
      return;
    }
    const onConnectionError = (msg: string) => console.error(msg);
    const onReload = (strategy: string = 'reload') => {
      if (strategy === 'refresh' || strategy === 'full-refresh') {
        const anyVaadin = window.Vaadin as any;
        // TODO: do it in Flow client. Maybe raise a custom vaadin-refresh-ui event
        //  and handle it in Flow client?
        Object.keys(anyVaadin.Flow.clients)
          .filter((key) => key !== 'TypeScript')
          .map((id) => anyVaadin.Flow.clients[id])
          .forEach((client) => {
            if (client.sendEventMessage) {
              client.sendEventMessage(1, 'ui-refresh', {
                fullRefresh: strategy === 'full-refresh'
              });
            } else {
              console.warn('Ignoring ui-refresh event for application ', id);
            }
          });
      } else {
        const lastReload = window.sessionStorage.getItem(VaadinDevTools.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE);
        const nextReload = lastReload ? parseInt(lastReload, 10) + 1 : 1;
        window.sessionStorage.setItem(VaadinDevTools.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE, nextReload.toString());
        window.sessionStorage.setItem(VaadinDevTools.TRIGGERED_KEY_IN_SESSION_STORAGE, 'true');
        window.location.reload();
      }
    };
    const onUpdate = (path: string, content: string) => {
      const contextPathProtocol = 'context://';
      const pathWithNoProtocol = path.substring(contextPathProtocol.length);
      if (path.startsWith(contextPathProtocol)) {
        path = this.conf.contextRelativePath + pathWithNoProtocol;
      }

      if (content) {
        // change or add a new stylesheet
        let styleTag = document.head.querySelector(`style[data-file-path='${path}']`) as HTMLStyleElement | null;
        if (!styleTag) {
          styleTag = document.createElement('style');
          styleTag.setAttribute('data-file-path', path);
          document.head.appendChild(styleTag);
          this.removeOldLinks(pathWithNoProtocol);
        }
        styleTag.textContent = content;
        document.dispatchEvent(new CustomEvent('vaadin-theme-updated'));
      } else if (content === '') {
        // remove inlined stylesheets or initial links with the given path
        const styleTag = document.head.querySelector(`style[data-file-path='${path}']`) as HTMLStyleElement | null;
        if (styleTag) {
          styleTag.remove();
        } else {
          this.removeOldLinks(pathWithNoProtocol);
        }
        document.dispatchEvent(new CustomEvent('vaadin-theme-updated'));
      }
    };

    const frontendConnection = new WebSocketConnection(this.getDedicatedWebSocketUrl());
    frontendConnection.onHandshake = () => {
      if (!VaadinDevTools.isActive) {
        frontendConnection.setActive(false);
      }
      this.elementTelemetry();
    };
    frontendConnection.onConnectionError = onConnectionError;
    frontendConnection.onReload = onReload;
    frontendConnection.onUpdate = onUpdate;
    frontendConnection.onStatusChange = (status: ConnectionStatus) => {
      this.frontendStatus = status;
    };
    frontendConnection.onMessage = (message: any) => this.handleFrontendMessage(message);
    this.frontendConnection = frontendConnection;

    if (this.conf.backend === VaadinDevTools.SPRING_BOOT_DEVTOOLS && this.conf.liveReloadPort) {
      this.javaConnection = new LiveReloadConnection(this.getSpringBootWebSocketUrl(window.location));
      this.javaConnection.onHandshake = () => {
        if (!VaadinDevTools.isActive) {
          this.javaConnection!.setActive(false);
        }
      };
      this.javaConnection.onReload = onReload;
      this.javaConnection.onConnectionError = onConnectionError;
      this.javaConnection.onStatusChange = (status) => {
        this.javaStatus = status;
      };
    }
  }

  removeOldLinks(path: string) {
    // removes initially added links that are outdated after hot-reload and replaced by inlined styles
    const links = Array.from(document.head.querySelectorAll('link[rel="stylesheet"]')) as HTMLLinkElement[];
    links.forEach((link) => {
      const filePath = link.getAttribute('data-file-path');
      if (filePath && filePath.includes(path)) {
        link.remove();
      }
    });
  }

  tabHandleMessage(tabElement: HTMLElement, message: ServerMessage): boolean {
    const handler = tabElement as any as MessageHandler;
    return handler.handleMessage && handler.handleMessage.call(tabElement, message);
  }

  handleFrontendMessage(message: ServerMessage) {
    if (message.command === 'featureFlags') {
    } else if (handleLicenseMessage(message, this.bodyShadowRoot) || this.handleHmrMessage(message)) {
    } else {
      this.unhandledMessages.push(message);
    }
  }

  handleHmrMessage(message: ServerMessage): boolean {
    if (message.command !== 'hmr') {
      return false;
    }
    if (hmrClient) {
      hmrClient.notifyListeners(message.data.event, message.data.eventData);
    }
    return true;
  }

  getDedicatedWebSocketUrl(): string | undefined {
    function getAbsoluteUrl(relative: string) {
      // Use innerHTML to obtain an absolute URL
      const div = document.createElement('div');
      div.innerHTML = `<a href="${relative}"/>`;
      return (div.firstChild as HTMLLinkElement).href;
    }

    if (this.conf.url === undefined) {
      return undefined;
    }
    const connectionBaseUrl = getAbsoluteUrl(this.conf.url!);

    if (!connectionBaseUrl.startsWith('http://') && !connectionBaseUrl.startsWith('https://')) {
      // eslint-disable-next-line no-console
      console.error('The protocol of the url should be http or https for live reload to work.');
      return undefined;
    }
    return `${connectionBaseUrl}?v-r=push&debug_window&token=${this.conf.token}`;
  }

  getSpringBootWebSocketUrl(location: any) {
    const { hostname } = location;
    const wsProtocol = location.protocol === 'https:' ? 'wss' : 'ws';
    if (hostname.endsWith('gitpod.io')) {
      // Gitpod uses `port-url` instead of `url:port`
      const hostnameWithoutPort = hostname.replace(/.*?-/, '');
      return `${wsProtocol}://${this.conf.liveReloadPort}-${hostnameWithoutPort}`;
    } else {
      return `${wsProtocol}://${hostname}:${this.conf.liveReloadPort}`;
    }
  }

  connectedCallback() {
    super.connectedCallback();

    this.bodyShadowRoot = document.body.attachShadow({ mode: 'closed' });
    this.bodyShadowRoot.innerHTML = '<slot></slot>';

    this.conf = (window.Vaadin as any).devToolsConf || this.conf;

    const lastReload = window.sessionStorage.getItem(VaadinDevTools.TRIGGERED_KEY_IN_SESSION_STORAGE);
    if (lastReload) {
      const now = new Date();
      const reloaded = `${`0${now.getHours()}`.slice(-2)}:${`0${now.getMinutes()}`.slice(
        -2
      )}:${`0${now.getSeconds()}`.slice(-2)}`;
      window.sessionStorage.removeItem(VaadinDevTools.TRIGGERED_KEY_IN_SESSION_STORAGE);
    }

    this.transitionDuration = parseInt(
      window.getComputedStyle(this).getPropertyValue('--dev-tools-transition-duration'),
      10
    );

    const windowAny = window as any;
    windowAny.Vaadin = windowAny.Vaadin || {};
    windowAny.Vaadin.devTools = Object.assign(this, windowAny.Vaadin.devTools);

    const anyVaadin = window.Vaadin as any;
    if (anyVaadin.devToolsPlugins) {
      Array.from(anyVaadin.devToolsPlugins as DevToolsPlugin[]).forEach((plugin) => this.initPlugin(plugin));
      anyVaadin.devToolsPlugins = { push: (plugin: DevToolsPlugin) => this.initPlugin(plugin) };
    }

    this.openWebSocketConnection();
    licenseInit();
  }

  async initPlugin(plugin: DevToolsPlugin) {
    const devTools = this;
    plugin.init({
      send: function (command: string, data: any): void {
        devTools.frontendConnection!.send(command, data);
      }
    });
  }

  format(o: any): string {
    return o.toString();
  }

  checkLicense(productInfo: Product) {
    if (this.frontendConnection) {
      this.frontendConnection.send('checkLicense', productInfo);
    } else {
      licenseCheckOk(productInfo);
    }
  }

  startPreTrial() {
    if (this.frontendConnection) {
      this.frontendConnection.send('startPreTrialLicense', {});
    } else {
      console.error('Cannot start pre-trial: no connection');
      preTrialStartFailed(false, this.bodyShadowRoot);
    }
  }

  downloadLicense(productInfo: Product) {
    if (this.frontendConnection) {
      this.frontendConnection.send('downloadLicense', productInfo);
    } else {
      updateLicenseDownloadStatus('failed', this.bodyShadowRoot);
    }
  }

  setActive(yes: boolean) {
    this.frontendConnection?.setActive(yes);
    this.javaConnection?.setActive(yes);
    window.sessionStorage.setItem(VaadinDevTools.ACTIVE_KEY_IN_SESSION_STORAGE, yes ? 'true' : 'false');
  }

  /* eslint-disable lit/no-template-map */
  render() {
    return html` <div style="display: none" class="dev-tools"></div>`;
  }

  setJavaLiveReloadActive(active: boolean) {
    // Java reload either goes through the direct connection to live reload, or then through the shared websocket connection
    if (this.javaConnection) {
      this.javaConnection.setActive(active);
    } else {
      this.frontendConnection?.setActive(active);
    }
  }
}
