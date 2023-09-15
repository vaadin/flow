import 'construct-style-sheets-polyfill';
import { css, html, LitElement, nothing, PropertyValueMap, render, TemplateResult } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';
import { classMap } from 'lit/directives/class-map.js';
import { Overlay, OverlayOutsideClickEvent } from '@vaadin/overlay';
import { ComponentPicker } from './component-picker';
import { ComponentReference, deepContains } from './component-util';
import './theme-editor/editor';
import { ThemeEditorState } from './theme-editor/model';
import { licenseCheckFailed, licenseInit, Product } from './License';
import { Connection, ConnectionStatus } from './connection';
import { popupStyles } from './styles';
import './vaadin-dev-tools-log';
import './vaadin-dev-tools-info';

/**
 * Plugin API for the dev tools window.
 */
export interface DevToolsInterface {
  send(command: string, data: any): void;
  addTab(id: string, tag: string): void;
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

interface Feature {
  id: string;
  title: string;
  moreInfoLink: string;
  requiresServerRestart: boolean;
  enabled: boolean;
}

interface Tab {
  id: string;
  title: string;
  render: (() => TemplateResult) | string;
  element?: HTMLElement;
  activate?: () => void;
  handleMessage?: MessageHandler;
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
  deleted: boolean;
}

@customElement('vaadin-dev-tools')
export class VaadinDevTools extends LitElement {
  static MAX_LOG_ROWS = 1000;
  unhandledMessages: ServerMessage[] = [];

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

        .dev-tools .dev-tools-icon {
          flex: none;
          pointer-events: none;
          display: inline-block;
          width: 1rem;
          height: 1rem;
          fill: #fff;
          transition: var(--dev-tools-transition-duration);
          margin: 0;
        }

        .dev-tools.active .dev-tools-icon {
          opacity: 0;
          position: absolute;
          transform: scale(0.5);
        }

        .dev-tools .status-blip {
          flex: none;
          display: block;
          width: 6px;
          height: 6px;
          border-radius: 50%;
          z-index: 20001;
          background: var(--dev-tools-grey-color);
          position: absolute;
          top: -1px;
          right: -1px;
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

        .switch {
          display: inline-flex;
          align-items: center;
        }

        .switch input {
          opacity: 0;
          width: 0;
          height: 0;
          position: absolute;
        }

        .switch .slider {
          display: block;
          flex: none;
          width: 28px;
          height: 18px;
          border-radius: 9px;
          background-color: rgba(255, 255, 255, 0.3);
          transition: var(--dev-tools-transition-duration);
          margin-right: 0.5rem;
        }

        .switch:focus-within .slider,
        .switch .slider:hover {
          background-color: rgba(255, 255, 255, 0.35);
          transition: none;
        }

        .switch input:focus-visible ~ .slider {
          box-shadow: 0 0 0 2px var(--dev-tools-background-color-active), 0 0 0 4px var(--dev-tools-blue-color);
        }

        .switch .slider::before {
          content: '';
          display: block;
          margin: 2px;
          width: 14px;
          height: 14px;
          background-color: #fff;
          transition: var(--dev-tools-transition-duration);
          border-radius: 50%;
        }

        .switch input:checked + .slider {
          background-color: var(--dev-tools-green-color);
        }

        .switch input:checked + .slider::before {
          transform: translateX(10px);
        }

        .switch input:disabled + .slider::before {
          background-color: var(--dev-tools-grey-color);
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

        .tab {
          color: var(--dev-tools-text-color-secondary);
          font: inherit;
          font-size: var(--dev-tools-font-size-small);
          font-weight: 500;
          line-height: 1;
          padding: 0.25rem 0.375rem;
          background: none;
          border: none;
          margin: 0;
          border-radius: 0.25rem;
          transition: var(--dev-tools-transition-duration);
        }

        .tab:hover,
        .tab.active {
          color: var(--dev-tools-text-color-active);
        }

        .tab.active {
          background-color: rgba(255, 255, 255, 0.12);
        }

        .tab.unreadErrors::after {
          content: '•';
          color: hsl(var(--dev-tools-red-hsl));
          font-size: 1.5rem;
          position: absolute;
          transform: translate(0, -50%);
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

        .minimize-button {
          flex: none;
          width: 1rem;
          height: 1rem;
          color: inherit;
          background-color: transparent;
          border: 0;
          padding: 0;
          margin: 0 0 0 auto;
          opacity: 0.8;
        }

        .minimize-button:hover {
          opacity: 1;
        }

        .minimize-button svg {
          max-width: 100%;
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

        .info-tray {
          padding: 0.75rem;
          position: relative;
          flex: auto;
          overflow: auto;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          user-select: text;
        }

        .info-tray dl {
          margin: 0;
          display: grid;
          grid-template-columns: max-content 1fr;
          column-gap: 0.75rem;
          position: relative;
        }

        .info-tray dt {
          grid-column: 1;
          color: var(--dev-tools-text-color-emphasis);
        }

        .info-tray dt:not(:first-child)::before {
          content: '';
          width: 100%;
          position: absolute;
          height: 1px;
          background-color: rgba(255, 255, 255, 0.1);
          margin-top: -0.375rem;
        }

        .info-tray dd {
          grid-column: 2;
          margin: 0;
        }

        .info-tray :is(dt, dd):not(:last-child) {
          margin-bottom: 0.75rem;
        }

        .info-tray dd + dd {
          margin-top: -0.5rem;
        }

        .info-tray .live-reload-status::before {
          content: '•';
          color: var(--status-color);
          width: 0.75rem;
          display: inline-block;
          font-size: 1rem;
          line-height: 0.5rem;
        }

        .info-tray .copy {
          position: fixed;
          z-index: 1;
          top: 0.5rem;
          right: 0.5rem;
        }

        .info-tray .switch {
          vertical-align: -4px;
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
      `,
      popupStyles
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

  static notificationDismissed(persistentId: string) {
    const shown = window.localStorage.getItem(VaadinDevTools.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);
    return shown !== null && shown.includes(persistentId);
  }

  @property({ type: String })
  url?: string;

  @property({ type: Boolean, attribute: true })
  liveReloadDisabled?: boolean;

  @property({ type: String })
  backend?: string;

  @property({ type: Number })
  springBootLiveReloadPort?: number;

  @property({ type: Boolean, attribute: false })
  expanded: boolean = false;

  @property({ type: Array, attribute: false })
  messages: Message[] = [];

  @property({ type: String, attribute: false })
  splashMessage?: string;

  @property({ type: Array, attribute: false })
  notifications: Message[] = [];

  @property({ type: String, attribute: false })
  frontendStatus: ConnectionStatus = ConnectionStatus.UNAVAILABLE;

  @property({ type: String, attribute: false })
  javaStatus: ConnectionStatus = ConnectionStatus.UNAVAILABLE;

  @state()
  private tabs: Tab[] = [
    { id: 'log', title: 'Log', render: 'vaadin-dev-tools-log' },
    { id: 'info', title: 'Info', render: 'vaadin-dev-tools-info' },
    { id: 'features', title: 'Feature Flags', render: () => this.renderFeatures() }
  ];

  @state()
  private activeTab: string = 'log';

  @state()
  private features: Feature[] = [];

  @state()
  unreadErrors = false;

  @query('.window')
  private root!: HTMLElement;

  @query('vaadin-dev-tools-component-picker')
  private componentPicker!: ComponentPicker;

  @state()
  componentPickActive: boolean = false;

  @state()
  themeEditorState: ThemeEditorState = ThemeEditorState.disabled;

  private javaConnection?: Connection;
  private frontendConnection?: Connection;

  private nextMessageId: number = 1;

  private disableEventListener?: EventListener;

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
      this.frontendConnection.sendTelemetry(data);
    }
  }

  openWebSocketConnection() {
    this.frontendStatus = ConnectionStatus.UNAVAILABLE;
    this.javaStatus = ConnectionStatus.UNAVAILABLE;

    const onConnectionError = (msg: string) => this.log(MessageType.ERROR, msg);
    const onReload = () => {
      this.showSplashMessage('Reloading…');
      const lastReload = window.sessionStorage.getItem(VaadinDevTools.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE);
      const nextReload = lastReload ? parseInt(lastReload, 10) + 1 : 1;
      window.sessionStorage.setItem(VaadinDevTools.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE, nextReload.toString());
      window.sessionStorage.setItem(VaadinDevTools.TRIGGERED_KEY_IN_SESSION_STORAGE, 'true');
      window.location.reload();
    };
    const onUpdate = (path: string, content: string) => {
      let styleTag = document.head.querySelector(`style[data-file-path='${path}']`);
      if (styleTag) {
        this.log(MessageType.INFORMATION, 'Hot update of ' + path);
        styleTag.textContent = content;
        document.dispatchEvent(new CustomEvent('vaadin-theme-updated'));
      } else {
        onReload();
      }
    };

    const frontendConnection = new Connection(this.getDedicatedWebSocketUrl());
    frontendConnection.onHandshake = () => {
      this.log(MessageType.LOG, 'Vaadin development mode initialized');
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

    let javaConnection: Connection;
    if (this.backend === VaadinDevTools.SPRING_BOOT_DEVTOOLS && this.springBootLiveReloadPort) {
      javaConnection = new Connection(this.getSpringBootWebSocketUrl(window.location));
      javaConnection.onHandshake = () => {
        if (!VaadinDevTools.isActive) {
          javaConnection.setActive(false);
        }
      };
      javaConnection.onReload = onReload;
      javaConnection.onConnectionError = onConnectionError;
    } else if (this.backend === VaadinDevTools.JREBEL || this.backend === VaadinDevTools.HOTSWAP_AGENT) {
      javaConnection = frontendConnection;
    } else {
      javaConnection = new Connection(undefined);
    }
    const prevOnStatusChange = javaConnection.onStatusChange;
    javaConnection.onStatusChange = (status) => {
      prevOnStatusChange(status);
      this.javaStatus = status;
    };
    const prevOnHandshake = javaConnection.onHandshake;
    javaConnection.onHandshake = () => {
      prevOnHandshake();
      if (this.backend) {
        this.log(
          MessageType.INFORMATION,
          `Java live reload available: ${VaadinDevTools.BACKEND_DISPLAY_NAME[this.backend]}`
        );
      }
    };
    this.javaConnection = javaConnection;

    if (!this.backend) {
      this.showNotification(
        MessageType.WARNING,
        'Java live reload unavailable',
        'Live reload for Java changes is currently not set up. Find out how to make use of this functionality to boost your workflow.',
        'https://vaadin.com/docs/latest/flow/configuration/live-reload',
        'liveReloadUnavailable'
      );
    }
  }

  tabHandleMessage(tabElement: HTMLElement, message: ServerMessage): boolean {
    const handler = tabElement as any as MessageHandler;
    return handler.handleMessage && handler.handleMessage.call(tabElement, message);
  }
  handleFrontendMessage(message: ServerMessage) {
    for (const tab of this.tabs) {
      if (tab.element && this.tabHandleMessage(tab.element, message)) {
        // Fully handled
        return;
      }
    }

    if (message?.command === 'featureFlags') {
      this.features = message.data.features as Feature[];
    } else if (message?.command === 'themeEditorState') {
      const isFlowApp = !!(window as any).Vaadin.Flow;
      this.themeEditorState = message.data;
      if (isFlowApp && this.themeEditorState !== ThemeEditorState.disabled) {
        this.tabs.push({
          id: 'theme-editor',
          title: 'Theme Editor (Preview)',
          render: () => this.renderThemeEditor()
        });
        this.requestUpdate();
      }
    } else {
      this.unhandledMessages.push(message);
    }
  }

  getDedicatedWebSocketUrl(): string | undefined {
    function getAbsoluteUrl(relative: string) {
      // Use innerHTML to obtain an absolute URL
      const div = document.createElement('div');
      div.innerHTML = `<a href="${relative}"/>`;
      return (div.firstChild as HTMLLinkElement).href;
    }
    if (this.url === undefined) {
      return undefined;
    }
    const connectionBaseUrl = getAbsoluteUrl(this.url!);

    if (!connectionBaseUrl.startsWith('http://') && !connectionBaseUrl.startsWith('https://')) {
      // eslint-disable-next-line no-console
      console.error('The protocol of the url should be http or https for live reload to work.');
      return undefined;
    }
    return `${connectionBaseUrl.replace(/^http/, 'ws')}?v-r=push&debug_window`;
  }

  getSpringBootWebSocketUrl(location: any) {
    const { hostname } = location;
    const wsProtocol = location.protocol === 'https:' ? 'wss' : 'ws';
    if (hostname.endsWith('gitpod.io')) {
      // Gitpod uses `port-url` instead of `url:port`
      const hostnameWithoutPort = hostname.replace(/.*?-/, '');
      return `${wsProtocol}://${this.springBootLiveReloadPort}-${hostnameWithoutPort}`;
    } else {
      return `${wsProtocol}://${hostname}:${this.springBootLiveReloadPort}`;
    }
  }

  constructor() {
    super();

    if ((window as any).Vaadin.Flow) {
      this.tabs.push({ id: 'code', title: 'Code', render: () => this.renderCode() });
    }
  }
  connectedCallback() {
    super.connectedCallback();
    this.catchErrors();

    // when focus or clicking anywhere, move the splash message to the message tray
    this.disableEventListener = (_: any) => this.demoteSplashMessage();
    document.body.addEventListener('focus', this.disableEventListener);
    document.body.addEventListener('click', this.disableEventListener);

    const lastReload = window.sessionStorage.getItem(VaadinDevTools.TRIGGERED_KEY_IN_SESSION_STORAGE);
    if (lastReload) {
      const now = new Date();
      const reloaded = `${`0${now.getHours()}`.slice(-2)}:${`0${now.getMinutes()}`.slice(
        -2
      )}:${`0${now.getSeconds()}`.slice(-2)}`;
      this.showSplashMessage(`Page reloaded at ${reloaded}`);
      window.sessionStorage.removeItem(VaadinDevTools.TRIGGERED_KEY_IN_SESSION_STORAGE);
    }

    this.transitionDuration = parseInt(
      window.getComputedStyle(this).getPropertyValue('--dev-tools-transition-duration'),
      10
    );

    const windowAny = window as any;
    windowAny.Vaadin = windowAny.Vaadin || {};
    windowAny.Vaadin.devTools = Object.assign(this, windowAny.Vaadin.devTools);

    // Prevent application overlays from closing when interacting with the dev tools
    document.documentElement.addEventListener('vaadin-overlay-outside-click', (event: Event) => {
      // We don't want to prevent closing the overlay if the overlay owner is
      // part of the dev tools, for example when using the color picker of the
      // theme editor
      const outsideClickEvent = event as OverlayOutsideClickEvent;
      const overlayOwner = (outsideClickEvent.target as Overlay).owner;
      const containsOverlayOwner = overlayOwner ? deepContains(this, overlayOwner) : false;

      if (containsOverlayOwner) {
        return;
      }
      // Otherwise prevent closing the overlay if click is within the dev tools
      const sourceEvent = outsideClickEvent.detail.sourceEvent;
      const composedPath = sourceEvent.composedPath();
      if (composedPath.includes(this)) {
        event.preventDefault();
      }
    });

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
      addTab: (title, tag) => {
        devTools.tabs.push({ id: title, title, render: tag });
      },
      send: function (command: string, data: any): void {
        devTools.frontendConnection!.send(command, data);
      }
    });
  }

  format(o: any): string {
    return o.toString();
  }
  catchErrors() {
    // Process stored messages
    const queue = (window as any).Vaadin.ConsoleErrors as any[];
    if (queue) {
      queue.forEach((args: any[]) => {
        this.log(MessageType.ERROR, args.map((o) => this.format(o)).join(' '));
      });
    }
    // Install new handler that immediately processes messages
    (window as any).Vaadin.ConsoleErrors = {
      push: (args: any[]) => {
        this.log(MessageType.ERROR, args.map((o) => this.format(o)).join(' '));
      }
    };
  }

  disconnectedCallback() {
    if (this.disableEventListener) {
      document.body.removeEventListener('focus', this.disableEventListener!);
      document.body.removeEventListener('click', this.disableEventListener!);
    }
    super.disconnectedCallback();
  }

  toggleExpanded() {
    this.notifications.slice().forEach((notification) => this.dismissNotification(notification.id));
    this.expanded = !this.expanded;
    if (this.expanded) {
      this.root.focus();
    }
  }

  showSplashMessage(msg: string | undefined) {
    this.splashMessage = msg;
    if (this.splashMessage) {
      if (this.expanded) {
        this.demoteSplashMessage();
      } else {
        // automatically move notification to message tray after a certain amount of time
        setTimeout(() => {
          this.demoteSplashMessage();
        }, VaadinDevTools.AUTO_DEMOTE_NOTIFICATION_DELAY);
      }
    }
  }

  demoteSplashMessage() {
    if (this.splashMessage) {
      this.log(MessageType.LOG, this.splashMessage);
    }
    this.showSplashMessage(undefined);
  }

  checkLicense(productInfo: Product) {
    if (this.frontendConnection) {
      this.frontendConnection.sendLicenseCheck(productInfo);
    } else {
      licenseCheckFailed({ message: 'Internal error: no connection', product: productInfo });
    }
  }

  log(type: MessageType, message: string, details?: string, link?: string) {
    const id = this.nextMessageId;
    this.nextMessageId += 1;
    this.messages.push({
      id,
      type,
      message,
      details,
      link,
      dontShowAgain: false,
      deleted: false
    });
    while (this.messages.length > VaadinDevTools.MAX_LOG_ROWS) {
      this.messages.shift();
    }
    this.requestUpdate();
    this.updateComplete.then(() => {
      // Scroll into view
      const lastMessage = this.renderRoot.querySelector('.message-tray .message:last-child');
      if (this.expanded && lastMessage) {
        setTimeout(() => lastMessage.scrollIntoView({ behavior: 'smooth' }), this.transitionDuration);
        this.unreadErrors = false;
      } else if (type === MessageType.ERROR) {
        this.unreadErrors = true;
      }
    });
  }

  showNotification(type: MessageType, message: string, details?: string, link?: string, persistentId?: string) {
    if (persistentId === undefined || !VaadinDevTools.notificationDismissed(persistentId!)) {
      // Do not open persistent message if another is already visible with the same persistentId
      const matchingVisibleNotifications = this.notifications
        .filter((notification) => notification.persistentId === persistentId)
        .filter((notification) => !notification.deleted);
      if (matchingVisibleNotifications.length > 0) {
        return;
      }
      const id = this.nextMessageId;
      this.nextMessageId += 1;
      this.notifications.push({
        id,
        type,
        message,
        details,
        link,
        persistentId,
        dontShowAgain: false,
        deleted: false
      });
      // automatically move notification to message tray after a certain amount of time unless it contains a link
      if (link === undefined) {
        setTimeout(() => {
          this.dismissNotification(id);
        }, VaadinDevTools.AUTO_DEMOTE_NOTIFICATION_DELAY);
      }
      this.requestUpdate();
    } else {
      this.log(type, message, details, link);
    }
  }

  dismissNotification(id: number) {
    const index = this.findNotificationIndex(id);
    if (index !== -1 && !this.notifications[index].deleted) {
      const notification = this.notifications[index];

      // user is explicitly dismissing a notification---after that we won't bug them with it
      if (
        notification.dontShowAgain &&
        notification.persistentId &&
        !VaadinDevTools.notificationDismissed(notification.persistentId)
      ) {
        let dismissed = window.localStorage.getItem(VaadinDevTools.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);
        dismissed = dismissed === null ? notification.persistentId : `${dismissed},${notification.persistentId}`;
        window.localStorage.setItem(VaadinDevTools.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE, dismissed);
      }

      notification.deleted = true;
      this.log(notification.type, notification.message, notification.details, notification.link);

      // give some time for the animation
      setTimeout(() => {
        const idx = this.findNotificationIndex(id);
        if (idx !== -1) {
          this.notifications.splice(idx, 1);
          this.requestUpdate();
        }
      }, this.transitionDuration);
    }
  }

  findNotificationIndex(id: number): number {
    let index = -1;
    this.notifications.some((notification, idx) => {
      if (notification.id === id) {
        index = idx;
        return true;
      } else {
        return false;
      }
    });
    return index;
  }

  toggleDontShowAgain(id: number) {
    const index = this.findNotificationIndex(id);
    if (index !== -1 && !this.notifications[index].deleted) {
      const notification = this.notifications[index];
      notification.dontShowAgain = !notification.dontShowAgain;
      this.requestUpdate();
    }
  }

  setActive(yes: boolean) {
    this.frontendConnection?.setActive(yes);
    this.javaConnection?.setActive(yes);
    window.sessionStorage.setItem(VaadinDevTools.ACTIVE_KEY_IN_SESSION_STORAGE, yes ? 'true' : 'false');
  }

  getStatusColor(status: ConnectionStatus | undefined) {
    if (status === ConnectionStatus.ACTIVE) {
      return 'var(--dev-tools-green-color)';
    } else if (status === ConnectionStatus.INACTIVE) {
      return 'var(--dev-tools-grey-color)';
    } else if (status === ConnectionStatus.UNAVAILABLE) {
      return 'var(--dev-tools-yellow-hsl)';
    } else if (status === ConnectionStatus.ERROR) {
      return 'var(--dev-tools-red-color)';
    } else {
      return 'none';
    }
  }

  /* eslint-disable lit/no-template-arrow */
  renderMessage(messageObject: Message) {
    return html`
      <div
        class="message ${messageObject.type} ${messageObject.deleted ? 'animate-out' : ''} ${messageObject.details ||
        messageObject.link
          ? 'has-details'
          : ''}"
      >
        <div class="message-content">
          <div class="message-heading">${messageObject.message}</div>
          <div class="message-details" ?hidden="${!messageObject.details && !messageObject.link}">
            ${messageObject.details ? html`<p>${messageObject.details}</p>` : ''}
            ${messageObject.link
              ? html`<a class="ahreflike" href="${messageObject.link}" target="_blank">Learn more</a>`
              : ''}
          </div>
          ${messageObject.persistentId
            ? html`<div
                class="persist ${messageObject.dontShowAgain ? 'on' : 'off'}"
                @click=${() => this.toggleDontShowAgain(messageObject.id)}
              >
                Don’t show again
              </div>`
            : ''}
        </div>
        <div class="dismiss-message" @click=${() => this.dismissNotification(messageObject.id)}>Dismiss</div>
      </div>
    `;
  }

  /* eslint-disable lit/no-template-map */
  render() {
    return html` <div
        class="window ${this.expanded && !this.componentPickActive ? 'visible' : 'hidden'}"
        tabindex="0"
        @keydown=${(e: KeyboardEvent) => e.key === 'Escape' && this.expanded && this.toggleExpanded()}
      >
        <div class="window-toolbar">
          ${this.tabs.map(
            (tab) =>
              html`<button
                class=${classMap({
                  tab: true,
                  active: this.activeTab === tab.id,
                  unreadErrors: tab.id === 'log' && this.unreadErrors
                })}
                id="${tab.id}"
                @click=${() => {
                  const currentTab = this.tabs.find((tab) => tab.id === this.activeTab);
                  if (currentTab && currentTab.element) {
                    const deactivateMethod = (currentTab.element as any)?.deactivate;
                    if (deactivateMethod) {
                      deactivateMethod.call(currentTab.element);
                    }
                  }
                  this.activeTab = tab.id;
                  const activateMethod = (tab.element as any).activate;
                  if (activateMethod) {
                    activateMethod.call(tab.element);
                  }
                }}
              >
                ${tab.title}
              </button> `
          )}
          <button class="minimize-button" title="Minimize" @click=${() => this.toggleExpanded()}>
            <svg fill="none" height="16" viewBox="0 0 16 16" width="16" xmlns="http://www.w3.org/2000/svg">
              <g fill="#fff" opacity=".8">
                <path
                  d="m7.25 1.75c0-.41421.33579-.75.75-.75h3.25c2.0711 0 3.75 1.67893 3.75 3.75v6.5c0 2.0711-1.6789 3.75-3.75 3.75h-6.5c-2.07107 0-3.75-1.6789-3.75-3.75v-3.25c0-.41421.33579-.75.75-.75s.75.33579.75.75v3.25c0 1.2426 1.00736 2.25 2.25 2.25h6.5c1.2426 0 2.25-1.0074 2.25-2.25v-6.5c0-1.24264-1.0074-2.25-2.25-2.25h-3.25c-.41421 0-.75-.33579-.75-.75z"
                />
                <path
                  d="m2.96967 2.96967c.29289-.29289.76777-.29289 1.06066 0l5.46967 5.46967v-2.68934c0-.41421.33579-.75.75-.75.4142 0 .75.33579.75.75v4.5c0 .4142-.3358.75-.75.75h-4.5c-.41421 0-.75-.3358-.75-.75 0-.41421.33579-.75.75-.75h2.68934l-5.46967-5.46967c-.29289-.29289-.29289-.76777 0-1.06066z"
                />
              </g>
            </svg>
          </button>
        </div>
        <div id="tabContainer"></div>
      </div>

      <div class="notification-tray">${this.notifications.map((msg) => this.renderMessage(msg))}</div>
      <vaadin-dev-tools-component-picker
        .active=${this.componentPickActive}
        @component-picker-opened=${() => {
          this.componentPickActive = true;
        }}
        @component-picker-closed=${() => {
          this.componentPickActive = false;
        }}
      ></vaadin-dev-tools-component-picker>
      <div
        class="dev-tools ${this.splashMessage ? 'active' : ''}${this.unreadErrors ? ' error' : ''}"
        @click=${() => this.toggleExpanded()}
      >
        ${this.unreadErrors
          ? html`<svg
              fill="none"
              height="16"
              viewBox="0 0 16 16"
              width="16"
              xmlns="http://www.w3.org/2000/svg"
              xmlns:xlink="http://www.w3.org/1999/xlink"
              class="dev-tools-icon error"
            >
              <clipPath id="a"><path d="m0 0h16v16h-16z" /></clipPath>
              <g clip-path="url(#a)">
                <path
                  d="m6.25685 2.09894c.76461-1.359306 2.72169-1.359308 3.4863 0l5.58035 9.92056c.7499 1.3332-.2135 2.9805-1.7432 2.9805h-11.1606c-1.529658 0-2.4930857-1.6473-1.743156-2.9805z"
                  fill="#ff5c69"
                />
                <path
                  d="m7.99699 4c-.45693 0-.82368.37726-.81077.834l.09533 3.37352c.01094.38726.32803.69551.71544.69551.38741 0 .70449-.30825.71544-.69551l.09533-3.37352c.0129-.45674-.35384-.834-.81077-.834zm.00301 8c.60843 0 1-.3879 1-.979 0-.5972-.39157-.9851-1-.9851s-1 .3879-1 .9851c0 .5911.39157.979 1 .979z"
                  fill="#fff"
                />
              </g>
            </svg>`
          : html`<svg
              fill="none"
              height="17"
              viewBox="0 0 16 17"
              width="16"
              xmlns="http://www.w3.org/2000/svg"
              class="dev-tools-icon logo"
            >
              <g fill="#fff">
                <path
                  d="m8.88273 5.97926c0 .04401-.0032.08898-.00801.12913-.02467.42848-.37813.76767-.8117.76767-.43358 0-.78704-.34112-.81171-.76928-.00481-.04015-.00801-.08351-.00801-.12752 0-.42784-.10255-.87656-1.14434-.87656h-3.48364c-1.57118 0-2.315271-.72849-2.315271-2.21758v-1.26683c0-.42431.324618-.768314.748261-.768314.42331 0 .74441.344004.74441.768314v.42784c0 .47924.39576.81265 1.11293.81265h3.41538c1.5542 0 1.67373 1.156 1.725 1.7679h.03429c.05095-.6119.17048-1.7679 1.72468-1.7679h3.4154c.7172 0 1.0145-.32924 1.0145-.80847l-.0067-.43202c0-.42431.3227-.768314.7463-.768314.4234 0 .7255.344004.7255.768314v1.26683c0 1.48909-.6181 2.21758-2.1893 2.21758h-3.4836c-1.04182 0-1.14437.44872-1.14437.87656z"
                />
                <path
                  d="m8.82577 15.1648c-.14311.3144-.4588.5335-.82635.5335-.37268 0-.69252-.2249-.83244-.5466-.00206-.0037-.00412-.0073-.00617-.0108-.00275-.0047-.00549-.0094-.00824-.0145l-3.16998-5.87318c-.08773-.15366-.13383-.32816-.13383-.50395 0-.56168.45592-1.01879 1.01621-1.01879.45048 0 .75656.22069.96595.6993l2.16882 4.05042 2.17166-4.05524c.2069-.47379.513-.69448.9634-.69448.5603 0 1.0166.45711 1.0166 1.01879 0 .17579-.0465.35029-.1348.50523l-3.1697 5.8725c-.00503.0096-.01006.0184-.01509.0272-.00201.0036-.00402.0071-.00604.0106z"
                />
              </g>
            </svg>`}

        <span
          class="status-blip"
          style="background: linear-gradient(to right, ${this.getStatusColor(
            this.frontendStatus
          )} 50%, ${this.getStatusColor(this.javaStatus)} 50%)"
        ></span>
        ${this.splashMessage ? html`<span class="status-description">${this.splashMessage}</span></div>` : nothing}
      </div>`;
  }

  protected updated(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>): void {
    super.updated(_changedProperties);

    const tabContainer = this.renderRoot.querySelector('#tabContainer')! as HTMLElement;

    const newTabElements: HTMLElement[] = [];

    // Ensure each tab has an element, either a container <div> for render functions or the requested tag
    this.tabs.forEach((tab) => {
      if (tab.element) {
        return;
      }

      if (typeof tab.render === 'function') {
        tab.element = document.createElement('div');
      } else {
        tab.element = document.createElement(tab.render);
        (tab.element as any)._devTools = this;
      }
      newTabElements.push(tab.element);
    });

    // Ensure tabs are in the correct order in the tab container
    if (tabContainer?.childElementCount !== this.tabs.length) {
      for (let i = 0; i < this.tabs.length; i++) {
        const tab = this.tabs[i];
        if (tabContainer.childElementCount > i && tabContainer.children[i] === tab.element) {
          // ok
        } else {
          // insert at i
          tabContainer.insertBefore(tab.element!, tabContainer.children[i]);
        }
      }
      // Remove extra elements
      while (tabContainer?.childElementCount > this.tabs.length) {
        tabContainer.lastElementChild?.remove();
      }
    }

    // Render or request an update for each tab
    for (const tab of this.tabs) {
      if (typeof tab.render === 'function') {
        render(tab.render(), tab.element!);
      } else if ((tab.element as any).requestUpdate) {
        // To re-render log when messages are updated
        (tab.element as any).requestUpdate();
      }
      const active = tab.id === this.activeTab;
      tab.element!.hidden = !active;
    }

    // Send any unhandled messages
    for (const tabElement of newTabElements) {
      for (var i = 0; i < this.unhandledMessages.length; i++) {
        if (this.tabHandleMessage(tabElement, this.unhandledMessages[i])) {
          this.unhandledMessages.splice(i, 1);
          i--;
        }
      }
    }
  }

  renderCode() {
    return html`<div class="info-tray">
      <div>
        <select id="locationType">
          <option value="create" selected>Create</option>
          <option value="attach">Attach</option>
        </select>
        <button
          class="button pick"
          @click=${async () => {
            await import('./component-picker.js');
            this.componentPicker.open({
              infoTemplate: html`
                <div>
                  <h3>Locate a component in source code</h3>
                  <p>Use the mouse cursor to highlight components in the UI.</p>
                  <p>Use arrow down/up to cycle through and highlight specific components under the cursor.</p>
                  <p>
                    Click the primary mouse button to open the corresponding source code line of the highlighted
                    component in your IDE.
                  </p>
                </div>
              `,
              pickCallback: (component) => {
                const serializableComponentRef: ComponentReference = { nodeId: component.nodeId, uiId: component.uiId };
                const locationType = (this.renderRoot.querySelector('#locationType') as HTMLSelectElement).value;
                if (locationType === 'create') {
                  this.frontendConnection!.sendShowComponentCreateLocation(serializableComponentRef);
                } else {
                  this.frontendConnection!.sendShowComponentAttachLocation(serializableComponentRef);
                }
              }
            });
          }}
        >
          Find component in code
        </button>
      </div>
      </div>
    </div>`;
  }

  private renderFeatures() {
    return html`<div class="features-tray">
      ${this.features.map(
        (feature) => html`<div class="feature">
          <label class="switch">
            <input
              class="feature-toggle"
              id="feature-toggle-${feature.id}"
              type="checkbox"
              ?checked=${feature.enabled}
              @change=${(e: InputEvent) => this.toggleFeatureFlag(e, feature)}
            />
            <span class="slider"></span>
            ${feature.title}
          </label>
          <a class="ahreflike" href="${feature.moreInfoLink}" target="_blank">Learn more</a>
        </div>`
      )}
    </div>`;
  }

  disableJavaLiveReload() {
    this.javaConnection?.setActive(false);
  }

  enableJavaLiveReload() {
    this.javaConnection?.setActive(true);
  }

  renderThemeEditor() {
    return html` <vaadin-dev-tools-theme-editor
      .expanded=${this.expanded}
      .themeEditorState=${this.themeEditorState}
      .pickerProvider=${() => this.componentPicker}
      .connection=${this.frontendConnection}
      @before-open=${this.disableJavaLiveReload}
      @after-close=${this.enableJavaLiveReload}
    ></vaadin-dev-tools-theme-editor>`;
  }

  toggleFeatureFlag(e: Event, feature: Feature) {
    const enabled = (e.target! as HTMLInputElement).checked;
    if (this.frontendConnection) {
      this.frontendConnection.setFeature(feature.id, enabled);
      this.showNotification(
        MessageType.INFORMATION,
        `“${feature.title}” ${enabled ? 'enabled' : 'disabled'}`,
        feature.requiresServerRestart ? 'This feature requires a server restart' : undefined,
        undefined,
        `feature${feature.id}${enabled ? 'Enabled' : 'Disabled'}`
      );
    } else {
      this.log(MessageType.ERROR, `Unable to toggle feature ${feature.title}: No server connection available`);
    }
  }
}
