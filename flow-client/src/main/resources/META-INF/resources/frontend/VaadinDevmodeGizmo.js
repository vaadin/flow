import {LitElement, html, css} from 'lit-element';

class VaadinDevmodeGizmo extends LitElement {

  static get styles() {
    return css`
       :host {
         --gizmo-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif;
         --gizmo-font-family-monospace: SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;

          --gizmo-font-size: 0.8125rem;

          --gizmo-text-color: rgba(255, 255, 255, .85);
          --gizmo-text-color-secondary: rgba(255, 255, 255, .65);
          --gizmo-text-color-emphasis: rgba(255, 255, 255, 1);

          --gizmo-background-color-inactive: rgba(50, 50, 50, .15);
          --gizmo-background-color-active: rgba(50, 50, 50, 0.98);

          --gizmo-border-radius: 0.5rem;
          --gizmo-box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.4);

          --gizmo-blue-hsl: 206, 100%, 70%;
          --gizmo-blue-color: hsl(var(--gizmo-blue-hsl));
          --gizmo-green-hsl: 145, 80%, 42%;
          --gizmo-green-color: hsl(var(--gizmo-green-hsl));
          --gizmo-grey-hsl: 0, 0%, 50%;
          --gizmo-grey-color: hsl(var(--gizmo-grey-hsl));
          --gizmo-yellow-hsl: 38, 98%, 64%;
          --gizmo-yellow-color: hsl(var(--gizmo-yellow-hsl));
          --gizmo-red-hsl: 355, 100%, 68%;
          --gizmo-red-color: hsl(var(--gizmo-red-hsl));

          /* Needs to be in ms, used in JavaScript as well */
          --gizmo-transition-duration: 200ms;

          direction: ltr;
          cursor: default;
          font: normal 500 var(--gizmo-font-size)/1.125rem var(--gizmo-font-family);
          color: var(--gizmo-text-color);
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;

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

      .gizmo {
          pointer-events: auto;
          display: flex;
          align-items: center;
          position: fixed;
          z-index: inherit;
          right: .25rem;
          bottom: .25rem;
          max-width: 1.75rem;
          border-radius: 1rem;
          padding: .375rem;
          box-sizing: border-box;
          background-color: var(--gizmo-background-color-inactive);
          color: var(--gizmo-text-color);
          transition: var(--gizmo-transition-duration) ease-in;
          white-space: nowrap;
          line-height: 1rem;
      }

      .gizmo:hover,
      .gizmo.active {
          max-width: calc(100% - 1rem);
          background-color: var(--gizmo-background-color-active);
          box-shadow: var(--gizmo-box-shadow);
      }

      .gizmo .vaadin-logo {
          flex: none;
          pointer-events: none;
          display: inline-block;
          width: 1rem;
          height: 1rem;
          fill: #fff;
          transition: var(--gizmo-transition-duration);
          transition-delay: var(--gizmo-transition-duration);
          margin: 0;
      }

      .gizmo:hover .vaadin-logo,
      .gizmo.active .vaadin-logo {
          opacity: 0;
          width: 0;
      }

      .gizmo .status-blip {
          flex: none;
          display: block;
          width: 1rem;
          height: 1rem;
          border-radius: 50%;
          z-index: 20001;
          background-color: var(--gizmo-green-color);
          transform: translate(-0.5rem, -0.5rem) scale(0.375);
          transition: var(--gizmo-transition-duration);
          transition-delay: var(--gizmo-transition-duration);
      }

      .gizmo:hover .status-blip,
      .gizmo.active .status-blip {
          transform: translate(0, 0) scale(1);
      }

      .gizmo.active .vaadin-logo,
      .gizmo.active .status-blip {
          transition-delay: 0s;
      }

      .gizmo .status-description {
          overflow: hidden;
          text-overflow: ellipsis;
      }

      .gizmo > * {
          margin-right: .5rem;
      }

      .switch {
          display: inline-flex;
          align-items: center;
          margin-left: auto;
          flex-shrink: 1;
          min-width: 28px;
      }

      .switch input {
          opacity: 0;
          width: 0;
          height: 0;
      }

      .switch .slider {
          display: block;
          flex: none;
          width: 28px;
          height: 18px;
          border-radius: 9px;
          background-color: rgba(255, 255, 255, 0.3);
          transition: var(--gizmo-transition-duration);
      }

      .switch .slider:hover {
          background-color: rgba(255, 255, 255, 0.35);
          transition: none;
      }

      .switch .slider::before {
          content: "";
          display: block;
          margin: 2px;
          width: 14px;
          height: 14px;
          background-color: #fff;
          transition: var(--gizmo-transition-duration);
          border-radius: 50%;
      }

      .switch input:checked + .slider {
          background-color: var(--gizmo-green-color);
      }

      .switch input:checked + .slider::before {
          transform: translateX(10px);
      }

     .switch input:disabled + .slider::before {
          background-color: var(--gizmo-grey-color);
      }

      .live-reload-text {
          font-weight: 600;
          margin-left: 0.5em;
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
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

      .window.visible ~ .gizmo {
          opacity: 0;
          pointer-events: none;
      }

      .window.hidden ~ .gizmo {
          opacity: 1;
          transition-delay: 0;
      }

      .window.hidden ~ .gizmo:hover,
      .window.hidden ~ .gizmo:hover .vaadin-logo,
      .window.hidden ~ .gizmo:hover .status-blip {
          transition-delay: var(--gizmo-transition-duration);
      }

      .window.visible ~ .gizmo .vaadin-logo,
      .window.visible ~ .gizmo .status-blip {
          transition: none;
          opacity: 0;
      }

      .window {
          border-radius: var(--gizmo-border-radius);
          overflow: hidden;
          margin: 0.5rem;
          width: 30rem;
          max-width: calc(100% - 1rem);
          max-height: calc(50vh - 1rem);
          flex-shrink: 0;
          background-color: var(--gizmo-background-color-active);
          color: var(--gizmo-text-color);
          transition: var(--gizmo-transition-duration);
          transform-origin: bottom right;
          display: flex;
          flex-direction: column;
          box-shadow: var(--gizmo-box-shadow);
      }

      .window-toolbar {
          display: flex;
          flex: none;
          align-items: center;
          border-bottom: 1px solid rgba(255, 255, 255, 0.14);
          padding: .25rem .75rem;
          white-space: nowrap;
      }

      .tab {
          color: var(--gizmo-text-color-secondary);
          font-weight: 600;
      }

      .ahreflike {
          font-weight: 600;
          color: var(--gizmo-text-color-secondary);
      }

      .ahreflike:hover {
          color: var(--gizmo-text-color-emphasis);
      }

      .minimize-button {
          flex: none;
          width: 1rem;
          height: 1rem;
          color: inherit;
          background-color: transparent;
          border: 0;
          padding: 0;
          margin: 0 -.375rem 0 1rem;
          opacity: .8;
          outline: none;
      }

      .minimize-button:hover {
          opacity: 1;
      }

      .minimize-button svg {
          max-width: 100%;
      }

      .message.information {
          --gizmo-notification-color: var(--gizmo-blue-color);
      }

      .message.warning {
          --gizmo-notification-color: var(--gizmo-yellow-color);
      }

      .message.error {
          --gizmo-notification-color: var(--gizmo-red-color);
      }

      .message {
          display: flex;
          padding: .125rem .75rem .125rem 2rem;
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

      .message .message-heading {
          position: relative;
          display: flex;
          align-items: center;
          margin: 0.125rem 0;
      }

      .message.has-details .message-heading {
          font-weight: 600;
      }

      .message .message-heading::before {
          position: absolute;
          margin-left: -1.5rem;
          display: inline-block;
          text-align: center;
          font-size: 0.875em;
          font-weight: 600;
          line-height: calc(1.25em - 2px);
          width: 1rem;
          height: 1rem;
          box-sizing: border-box;
          border: 1px solid transparent;
          border-radius: 50%;
      }

      .message.information .message-heading::before {
          content: "i";
          border-color: currentColor;
          color: var(--gizmo-notification-color);
      }

      .message.warning .message-heading::before,
      .message.error .message-heading::before {
          content: "!";
          color: var(--gizmo-background-color-active);
          background-color: var(--gizmo-notification-color);
      }

      .message .message-details {
          font-weight: 400;
          color: var(--gizmo-text-color-secondary);
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

      .message .ahreflike {
          color: var(--gizmo-notification-color, var(--gizmo-text-color));
          text-decoration: none;
          opacity: 0.9;
          font-weight: 500;
      }

      .message .ahreflike:hover {
          color: var(--gizmo-notification-color, var(--gizmo-text-color-emphasis));
          opacity: 1;
      }

      .message .persist {
          color: var(--gizmo-text-color-secondary);
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
          content: "";
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
          content: "";
          order: -1;
          position: absolute;
          width: 0.75em;
          height: 0.25em;
          border: 2px solid var(--gizmo-background-color-active);
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
          color: var(--gizmo-text-color-secondary);
      }

      .message .dismiss-message:hover {
          color: var(--gizmo-text-color);
      }

      .notification-tray {
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
          margin: 0.5rem;
      }

      .window.hidden + .notification-tray {
          margin-bottom: 3rem;
      }

      .notification-tray .message {
          pointer-events: auto;
          background-color: var(--gizmo-background-color-active);
          color: var(--gizmo-text-color);
          max-width: 30rem;
          box-sizing: border-box;
          border-radius: var(--gizmo-border-radius);
          margin-top: .5rem;
          transition: var(--gizmo-transition-duration);
          transform-origin: bottom right;
          animation: slideIn var(--gizmo-transition-duration);
          box-shadow: var(--gizmo-box-shadow);
          padding-top: 0.25rem;
          padding-bottom: 0.25rem;
      }

      .notification-tray .message.animate-out {
          animation: slideOut forwards var(--gizmo-transition-duration);
      }

      .notification-tray .message .message-details {
          max-height: 10em;
          overflow: hidden;
      }

      .message-tray {
          flex: auto;
          overflow: auto;
      }

      .message-tray .message {
          animation: appendList var(--gizmo-transition-duration) ease-in;
          padding-left: 2.25rem;
      }

      .message-tray .message.warning {
          background-color: hsla(var(--gizmo-yellow-hsl), 0.09);
      }

      .message-tray .message.error {
          background-color: hsla(var(--gizmo-red-hsl), 0.09);
      }

      .message-tray .message + .message {
          border-top: 1px solid rgba(255, 255, 255, 0.07);
      }

      .message-tray .message:last-child {
          padding-bottom: 0.375rem;
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

      @keyframes appendList {
          0% {
            font-size: 0;
            opacity: 0;
          }
          50% {
            font-size: 1em;
            opacity: 0;
          }
          100% {
            opacity: 1;
          }
      }

    `;
  }

  static get properties() {
    return {
      expanded: {type: Boolean},
      messages: {type: Array},
      splashMessage: {type: String},
      notifications: {type: Array},
      status: {type: String},
      reloadConnectionBaseUri: {type: String},
      liveReloadBackend: {type: String},
      springBootDevToolsPort: {type: Number}
    };
  }

  static get ACTIVE() {
    return 'active';
  }

  static get INACTIVE() {
    return 'inactive';
  }

  static get UNAVAILABLE() {
    return 'unavailable';
  }

  static get ERROR() {
    return 'error';
  }

  static get ENABLED_KEY_IN_LOCAL_STORAGE() {
    return 'vaadin.live-reload.enabled';
  }

  static get DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE() {
    return 'vaadin.live-reload.dismissedNotifications';
  }

  static get ACTIVE_KEY_IN_SESSION_STORAGE() {
    return 'vaadin.live-reload.active';
  }

  static get TRIGGERED_KEY_IN_SESSION_STORAGE() {
    return 'vaadin.live-reload.triggered';
  }

  static get TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE() {
    return 'vaadin.live-reload.triggeredCount';
  }

  static get HEARTBEAT_INTERVAL() {
    return 180000;
  }

  static get AUTO_DEMOTE_NOTIFICATION_DELAY() {
    return 5000;
  }

  static get HOTSWAP_AGENT() {
    return 'HOTSWAP_AGENT';
  }

  static get JREBEL() {
    return 'JREBEL';
  }

  static get SPRING_BOOT_DEVTOOLS() {
    return 'SPRING_BOOT_DEVTOOLS';
  }

  static get BACKEND_DISPLAY_NAME() {
    return {
      HOTSWAP_AGENT: 'HotswapAgent',
      JREBEL: 'JRebel',
      SPRING_BOOT_DEVTOOLS: 'Spring Boot Devtools'
    };
  }

  static get LOG() {
    return 'log';
  }

  static get INFORMATION() {
    return 'information';
  }

  static get WARNING() {
    return 'warning';
  }

  static get WEBSOCKET_ERROR_MESSAGE() {
    return 'Error in WebSocket connection to ';
  }

  static get isEnabled() {
    const enabled = window.localStorage.getItem(VaadinDevmodeGizmo.ENABLED_KEY_IN_LOCAL_STORAGE);
    return enabled === null || enabled !== 'false';
  }

  static get isActive() {
    const active = window.sessionStorage.getItem(VaadinDevmodeGizmo.ACTIVE_KEY_IN_SESSION_STORAGE);
    return active === null || active !== 'false';
  }

  static notificationDismissed(persistentId) {
    const shown = window.localStorage.getItem(VaadinDevmodeGizmo.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);
    return shown !== null && shown.includes(persistentId);
  }

  constructor() {
    super();
    this.messages = [];
    this.splashMessage = null;
    this.notifications = [];
    this.expanded = false;
    this.status = VaadinDevmodeGizmo.UNAVAILABLE;
    this.connection = null;
    this.nextMessageId = 1;
  }

  openWebSocketConnection() {
    if (this.connection !== null) {
      this.connection.close();
      this.connection = null;
    }
    // try Spring Boot Devtools first, if port is set
    if (this.liveReloadBackend === VaadinDevmodeGizmo.SPRING_BOOT_DEVTOOLS && this.springBootDevToolsPort) {
      this.connection = new WebSocket(this.getSpringBootWebSocketUrl(window.location));
    } else if (this.liveReloadBackend) {
      this.openDedicatedWebSocketConnection();
    } else {
      this.showNotification(VaadinDevmodeGizmo.WARNING,
        'Live reload unavailable',
        'Live reload is currently not set up. Find out how to make use of this functionality to boost your workflow.',
        'https://vaadin.com/docs/live-reload',
        'liveReloadUnavailable');
    }
    if (this.connection) {
      this.connection.onmessage = msg => this.handleMessage(msg);
      this.connection.onerror = err => this.handleError(err);
      const self = this;
      this.connection.onclose = _ => {
        if (self.status !== VaadinDevmodeGizmo.ERROR) {
          self.status = VaadinDevmodeGizmo.UNAVAILABLE;
        }
        self.connection = null;
      };
    }
  }

  openDedicatedWebSocketConnection() {
    const wsUrl = this.getDedicatedWebSocketUrl();
    if (wsUrl) {
      this.connection = new WebSocket(wsUrl);
      const self = this;
      setInterval(function() {
        if (self.connection !== null) {
          self.connection.send('');
        }
      }, VaadinDevmodeGizmo.HEARTBEAT_INTERVAL);
    }
  }

  getDedicatedWebSocketUrl() {
    if (!this.reloadConnectionBaseUri) {
      console.warn('This live reload backend requires a dedicated WS connection, but no base URL is given');
      return null;
    }
    if (!this.reloadConnectionBaseUri.startsWith('http://') && !this.reloadConnectionBaseUri.startsWith('https://')) {
      console.warn('The protocol of the url should be http or https for live reload to work.');
      return null;
    }
    return this.reloadConnectionBaseUri.replace(/^http/, 'ws') + '?v-r=push&refresh_connection';
  }

  getSpringBootWebSocketUrl(location) {
    const hostname = location.hostname;
    const wsProtocol = location.protocol == 'https:' ? 'wss' : 'ws';
    if (hostname.endsWith('gitpod.io')) {
      // Gitpod uses `port-url` instead of `url:port`
      const hostnameWithoutPort = hostname.replace(/.*?-/, '');
      return wsProtocol + '://' + this.springBootDevToolsPort + '-' + hostnameWithoutPort;
    } else {
      return wsProtocol + '://' + hostname + ':' + this.springBootDevToolsPort;
    }
  }

  handleMessage(msg) {
    let json;
    try {
      json = JSON.parse(msg.data);
    } catch (e) {
      this.handleError(`[${e.name}: ${e.message}`);
      return;
    }
    const command = json['command'];
    switch (command) {
      case 'hello': {
        this.showMessage(VaadinDevmodeGizmo.LOG, 'Vaadin development mode initialized');
        if (this.liveReloadBackend) {
          if (VaadinDevmodeGizmo.isActive) {
            this.status = VaadinDevmodeGizmo.ACTIVE;
          } else {
            this.status = VaadinDevmodeGizmo.INACTIVE;
          }
          const backend = VaadinDevmodeGizmo.BACKEND_DISPLAY_NAME[this.liveReloadBackend];
          this.showMessage(VaadinDevmodeGizmo.INFORMATION, 'Live reload available: ' + backend);
        } else {
          this.status = VaadinDevmodeGizmo.INACTIVE;
        }
        break;
      }

      case 'reload':
        if (this.status === VaadinDevmodeGizmo.ACTIVE) {
          this.showSplashMessage('Reloading…');
          const lastReload = window.sessionStorage.getItem(VaadinDevmodeGizmo.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE);
          const nextReload = lastReload ? (parseInt(lastReload) + 1) : 1;
          window.sessionStorage.setItem(VaadinDevmodeGizmo.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE, nextReload.toString());
          window.sessionStorage.setItem(VaadinDevmodeGizmo.TRIGGERED_KEY_IN_SESSION_STORAGE, 'true');
          window.location.reload();
        }
        break;

      default:
        console.warn('Unknown command received from the live reload server:', command);
    }
  }

  handleError(msg) {
    console.error(msg);
    this.status = VaadinDevmodeGizmo.ERROR;
    if (msg instanceof Event) {
      this.showMessage(VaadinDevmodeGizmo.ERROR, VaadinDevmodeGizmo.WEBSOCKET_ERROR_MESSAGE + this.connection.url);
    } else {
      this.showMessage(VaadinDevmodeGizmo.ERROR, msg);
    }
  }

  connectedCallback() {
    super.connectedCallback();

    // when focus or clicking anywhere, move the splash message to the message tray
    this.disableEventListener = e => this.demoteSplashMessage();
    document.body.addEventListener('focus', this.disableEventListener);
    document.body.addEventListener('click', this.disableEventListener);
    this.openWebSocketConnection();

    const lastReload = window.sessionStorage.getItem(VaadinDevmodeGizmo.TRIGGERED_KEY_IN_SESSION_STORAGE);
    if (lastReload) {
      const count = window.sessionStorage.getItem(VaadinDevmodeGizmo.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE);
      const now = new Date();
      const reloaded = ('0' + now.getHours()).slice(-2) + ':'
        + ('0' + now.getMinutes()).slice(-2) + ':'
        + ('0' + now.getSeconds()).slice(-2);
      this.showSplashMessage('Automatic reload #' + count + ' finished at ' + reloaded);
      window.sessionStorage.removeItem(VaadinDevmodeGizmo.TRIGGERED_KEY_IN_SESSION_STORAGE);
    }

    this.__transitionDuration = parseInt(window.getComputedStyle(this).getPropertyValue('--gizmo-transition-duration'));
  }

  disconnectedCallback() {
    document.body.removeEventListener('focus', this.disableEventListener);
    document.body.removeEventListener('click', this.disableEventListener);
    super.disconnectedCallback();
  }

  disableLiveReload() {
    if (this.connection !== null) {
      this.connection.close();
      this.connection = null;
    }
    window.localStorage.setItem(VaadinDevmodeGizmo.ENABLED_KEY_IN_LOCAL_STORAGE, 'false');
    this.remove();
  }

  toggleExpanded() {
    this.notifications.slice().forEach(notification => this.dismissNotification(notification.id));
    this.expanded = !this.expanded;
  }

  showSplashMessage(msg) {
    this.splashMessage = msg;
    if (this.splashMessage != null) {
      if (this.expanded) {
        this.demoteSplashMessage();
      } else {
        // automatically move notification to message tray after a certain amount of time
        setTimeout(() => {
          this.demoteSplashMessage();
        }, VaadinDevmodeGizmo.AUTO_DEMOTE_NOTIFICATION_DELAY);
      }
    }
  }

  demoteSplashMessage() {
    if (this.splashMessage) {
      this.showMessage(VaadinDevmodeGizmo.LOG, this.splashMessage);
    }
    this.showSplashMessage(null);
  }

  showMessage(type, msg, details = null, link = null) {
    const id = this.nextMessageId++;
    this.messages.push({
      id: id,
      type: type,
      message: msg,
      details: details,
      link: link
    });
    this.requestUpdate();
    this.updateComplete.then(() => {
      // Scroll into view
      setTimeout(() => {
        this.shadowRoot.querySelector('.message-tray .message:last-child').scrollIntoView({behavior: 'smooth'});
      }, this.__transitionDuration);
    });
  }

  showNotification(type, msg, details = null, link = null, persistentId = null) {
    if (persistentId === null || !VaadinDevmodeGizmo.notificationDismissed(persistentId)) {
      const id = this.nextMessageId++;
      this.notifications.push({
        id: id,
        type: type,
        message: msg,
        details: details,
        link: link,
        persistentId: persistentId,
        dontShowAgain: false
      });
      // automatically move notification to message tray after a certain amount of time unless it contains a link
      if (link === null) {
        setTimeout(() => {
          this.dismissNotification(id);
        }, VaadinDevmodeGizmo.AUTO_DEMOTE_NOTIFICATION_DELAY);
      }
      this.requestUpdate();
    } else {
      this.showMessage(type, msg, details, link);
    }
  }

  dismissNotification(id) {
    const index = this.findNotificationIndex(id);
    if (index !== -1 && !this.notifications[index].deleted) {
      const notification = this.notifications[index];

      // user is explicitly dismissing a notification---after that we won't bug them with it
      if (notification.dontShowAgain && notification.persistentId && !VaadinDevmodeGizmo.notificationDismissed(notification.persistentId)) {
        let dismissed = window.localStorage.getItem(VaadinDevmodeGizmo.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);
        if (dismissed === null) {
          dismissed = notification.persistentId;
        } else {
          dismissed = dismissed + ',' + notification.persistentId;
        }
        window.localStorage.setItem(VaadinDevmodeGizmo.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE, dismissed);
      }

      notification.deleted = true;
      this.showMessage(notification.type, notification.message, notification.details, notification.link);

      const self = this;
      // give some time for the animation
      setTimeout(() => {
        const index = self.findNotificationIndex(id);
        if (index != -1) {
          this.notifications.splice(index, 1);
          this.requestUpdate();
        }
      }, this.__transitionDuration);
    }
  }

  findNotificationIndex(id) {
      let index = -1;
      this.notifications.some((notification, idx) => {
          if (notification.id === id) {
              index = idx;
              return true;
          }
      });
      return index;
  }

  toggleDontShowAgain(id) {
    const index = this.notifications.findIndex(notification => notification.id === id);
    if (index !== -1 && !this.notifications[index].deleted) {
      const notification = this.notifications[index];
      notification.dontShowAgain = !notification.dontShowAgain;
      this.requestUpdate();
    }
  }

  setActive(yes) {
    if (yes) {
      window.sessionStorage.setItem(VaadinDevmodeGizmo.ACTIVE_KEY_IN_SESSION_STORAGE, 'true');
      this.status = VaadinDevmodeGizmo.ACTIVE;
    } else {
      window.sessionStorage.setItem(VaadinDevmodeGizmo.ACTIVE_KEY_IN_SESSION_STORAGE, 'false');
      this.status = VaadinDevmodeGizmo.INACTIVE;
    }
  }

  getStatusColor() {
    if (this.status === VaadinDevmodeGizmo.ACTIVE) {
      return 'var(--gizmo-green-color)';
    } else if (this.status === VaadinDevmodeGizmo.INACTIVE) {
      return 'var(--gizmo-grey-color)';
    } else if (this.status === VaadinDevmodeGizmo.UNAVAILABLE) {
      return 'var(--gizmo-yellow-color)';
    } else if (this.status === VaadinDevmodeGizmo.ERROR) {
      return 'var(--gizmo-red-color)';
    } else {
      return 'none';
    }
  }

  renderMessage(messageObject) {
    return html`
      <div class="message ${messageObject.type} ${messageObject.deleted ? 'animate-out' : ''} ${messageObject.details || messageObject.link ? 'has-details' : ''}">
        <div class="message-content">
          <div class="message-heading">${messageObject.message}</div>
          <div class="message-details" ?hidden="${!messageObject.details && !messageObject.link}">
            ${messageObject.details ? html`<p>${messageObject.details}</p>` : ''}
            ${messageObject.link ? html`<a class="ahreflike" href="${messageObject.link}" target="_blank">Read more</a>` : ''}
          </div>
          ${messageObject.persistentId ? html`<div class="persist ${messageObject.dontShowAgain ? 'on' : 'off'}" @click=${e => this.toggleDontShowAgain(messageObject.id)}>Don’t show again</div>` : ''}
        </div>
        <div class="dismiss-message" @click=${e => this.dismissNotification(messageObject.id)}>Dismiss</div>
      </div>
      `;
  }

  render() {
    return html`
      <div class="window ${this.expanded ? 'visible' : 'hidden'}">
        <div class="window-toolbar">
          <span class="tab">Log</span>
          <label class="switch">
              <input id="toggle" type="checkbox"
                  ?disabled=${this.status === VaadinDevmodeGizmo.UNAVAILABLE || this.status === VaadinDevmodeGizmo.ERROR}
                  ?checked="${this.status === VaadinDevmodeGizmo.ACTIVE}"
                  @change=${e => this.setActive(e.target.checked)}/>
              <span class="slider"></span>
              <span class="live-reload-text">Live reload</span>
          </label>
          <button class="minimize-button" title="Minimize" @click=${e => this.toggleExpanded()}>
            <svg viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg" fill="currentColor">
              <path d="M7.25 0.75C7.25 0.335786 7.58579 0 8 0H12.25C14.3211 0 16 1.67893 16 3.75V12.25C16 14.3211 14.3211 16 12.25 16H3.75C1.67893 16 0 14.3211 0 12.25V8C0 7.58579 0.335786 7.25 0.75 7.25C1.16421 7.25 1.5 7.58579 1.5 8V12.25C1.5 13.4926 2.50736 14.5 3.75 14.5H12.25C13.4926 14.5 14.5 13.4926 14.5 12.25V3.75C14.5 2.50736 13.4926 1.5 12.25 1.5H8C7.58579 1.5 7.25 1.16421 7.25 0.75Z"/>
              <path d="M2.96967 2.96967C3.26256 2.67678 3.73744 2.67678 4.03033 2.96967L9.5 8.43934V5.75C9.5 5.33579 9.83579 5 10.25 5C10.6642 5 11 5.33579 11 5.75V10.25C11 10.6642 10.6642 11 10.25 11H5.75C5.33579 11 5 10.6642 5 10.25C5 9.83579 5.33579 9.5 5.75 9.5H8.43934L2.96967 4.03033C2.67678 3.73744 2.67678 3.26256 2.96967 2.96967Z"/>
            </svg>
          </button>
        </div>
        <div class="message-tray">
          ${this.messages.map(msg => this.renderMessage(msg))}
        </div>
      </div>

      <div class="notification-tray">
        ${this.notifications.map(msg => this.renderMessage(msg))}
      </div>

      <div class="gizmo ${this.splashMessage !== null ? 'active' : ''}" @click=${e => this.toggleExpanded()}>
        <svg viewBox="0 0 16 16" preserveAspectRatio="xMidYMid meet" focusable="false" class="vaadin-logo">
          <path d="M15.21 0.35c-0.436 0-0.79 0.354-0.79 0.79v0 0.46c0 0.5-0.32 0.85-1.070 0.85h-3.55c-1.61 0-1.73 1.19-1.8 1.83v0c-0.060-0.64-0.18-1.83-1.79-1.83h-3.57c-0.75 0-1.090-0.37-1.090-0.86v-0.45c0-0.006 0-0.013 0-0.020 0-0.425-0.345-0.77-0.77-0.77-0 0-0 0-0 0h0c-0 0-0 0-0 0-0.431 0-0.78 0.349-0.78 0.78 0 0.004 0 0.007 0 0.011v-0.001 1.32c0 1.54 0.7 2.31 2.34 2.31h3.66c1.090 0 1.19 0.46 1.19 0.9 0 0 0 0.090 0 0.13 0.048 0.428 0.408 0.758 0.845 0.758s0.797-0.33 0.845-0.754l0-0.004s0-0.080 0-0.13c0-0.44 0.1-0.9 1.19-0.9h3.61c1.61 0 2.32-0.77 2.32-2.31v-1.32c0-0.436-0.354-0.79-0.79-0.79v0z"></path>
          <path d="M11.21 7.38c-0.012-0-0.026-0.001-0.040-0.001-0.453 0-0.835 0.301-0.958 0.714l-0.002 0.007-2.21 4.21-2.3-4.2c-0.122-0.425-0.507-0.731-0.963-0.731-0.013 0-0.026 0-0.039 0.001l0.002-0c-0.012-0-0.025-0.001-0.039-0.001-0.58 0-1.050 0.47-1.050 1.050 0 0.212 0.063 0.41 0.171 0.575l-0.002-0.004 3.29 6.1c0.15 0.333 0.478 0.561 0.86 0.561s0.71-0.228 0.858-0.555l0.002-0.006 3.34-6.1c0.090-0.152 0.144-0.335 0.144-0.53 0-0.58-0.47-1.050-1.050-1.050-0.005 0-0.010 0-0.014 0h0.001z"></path>
        </svg>
        <span class="status-blip" style="background-color: ${this.getStatusColor()}"></span>
          ${this.splashMessage !== null
      ? html`<span class="status-description">${this.splashMessage}</span></div>`
      : html`<span class="status-description">Live reload ${this.status} </span><span class="ahreflike">Details</span></div>`
    }
    </div>`;
  }
}

const init = function(reloadConnectionBaseUri, liveReloadBackend, springBootDevToolsPort) {
  if ('false' !== window.localStorage.getItem(VaadinDevmodeGizmo.ENABLED_KEY_IN_LOCAL_STORAGE)) {
    if (customElements.get('vaadin-devmode-gizmo') === undefined) {
      customElements.define('vaadin-devmode-gizmo', VaadinDevmodeGizmo);
    }
    const devmodeGizmo = document.createElement('vaadin-devmode-gizmo');
    if (reloadConnectionBaseUri) {
      devmodeGizmo.setAttribute('reloadConnectionBaseUri', reloadConnectionBaseUri);
    }
    if (liveReloadBackend) {
      devmodeGizmo.setAttribute('liveReloadBackend', liveReloadBackend);
    }
    if (springBootDevToolsPort) {
      devmodeGizmo.setAttribute('springBootDevToolsPort', springBootDevToolsPort);
    }
    document.body.appendChild(devmodeGizmo);
    return devmodeGizmo;
  } else {
    return undefined;
  }
};

if (window.Vaadin && window.Vaadin.Flow) {
  // expose the init function globally for access by GWT ApplicationConnection
  window.Vaadin.Flow.initDevModeGizmo = init;
}