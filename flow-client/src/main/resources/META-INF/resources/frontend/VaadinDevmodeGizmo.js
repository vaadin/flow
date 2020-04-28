import {LitElement, html, css} from 'lit-element';

class VaadinDevmodeGizmo extends LitElement {

  static get styles() {
    return css`
       :host {
          --gizmo-border-radius: 1rem;
          
          --gizmo-green-color: hsl(145, 80%, 42%);
          --gizmo-grey-color: hsl(0, 0%, 50%);
          --gizmo-yellow-color: hsl(36, 100%, 61%);
          --gizmo-red-color: hsl(3, 100%, 61%);
          direction: ltr;
       }

      .gizmo {
          display: flex;
          align-items: center;
          justify-content: center;
          position: fixed;
          right: 0;
          bottom: 1rem;
          height: 2rem;
          width: auto;
          border-top-left-radius: var(--gizmo-border-radius);
          border-bottom-left-radius: var(--gizmo-border-radius);
          padding-left: .5rem;
          background-color: rgba(50,50,50,.15);
          color: rgba(255,255,255,.8);
          transform: translateX(calc(100% - 2rem));
          transition: 400ms;
          z-index: 20000;
      }

      .gizmo:hover,
      .gizmo.active {
          transform: translateX(0);
          background-color: rgba(50,50,50,1);
      }
      
      .gizmo .vaadin-logo {
          pointer-events: none;
          display: inline-block;
          width: 16px;
          height: 16px;
          fill: #fff;
          opacity: 1;
          transition: 400ms;
      }
      
      .gizmo:hover .vaadin-logo,
      .gizmo.active .vaadin-logo {
          opacity: 0;
          width: 0px;
          margin-right: 0.25em;
      }
      
      .gizmo .status-blip {
          display: block;
          width: 0.75rem;
          height: 0.75rem;
          border-radius: 50%;
          z-index: 20001;
          background-color: var(--gizmo-green-color);
          transform: translate(-14px, 7px) scale(.5);
          transition: 400ms;
      }

      .gizmo:hover .status-blip,
      .gizmo.active .status-blip {
          transform: translate(0, 0) scale(1);
      }
      
      .gizmo > * {
          margin-right: .5rem;
      }

      .switch {
          position: relative;
          display: inline-block;
          margin-top: auto;
          margin-bottom: auto;
          width: 28px;
          height: 18px;
      }

      .switch input {
          opacity: 0;
          width: 0;
          height: 0;
      }

      .switch .slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        border-radius: 18px;
        background-color: rgba(255, 255, 255, 0.3);
        -webkit-transition: .4s;
        transition: .4s;
      }
      
      .switch .slider:hover {
        background-color: rgba(255, 255, 255, 0.35);
        transition: 0ms;
      }

      .switch .slider:before {
        position: absolute;
        content: "";
        height: 14px;
        width: 14px;
        left: 2px;
        bottom: 2px;
        background-color: white;
        -webkit-transition: .4s;
        transition: .4s;
        border-radius: 50%;
      }

      .switch input:checked + .slider {
        background-color: var(--gizmo-green-color);
      }

      .switch input:checked + .slider:before {
        -webkit-transform: translateX(10px);
        -ms-transform: translateX(10px);
        transform: translateX(10px);
      }

      .window.hidden {
          opacity: 0;
          transform: scale(0.1,0.4);
      }

      .window.visible {
          transform: scale(1,1);
          opacity: 1;
      }

      .window.visible ~ .gizmo {
          opacity: 0;
      }

      .window.hidden ~ .gizmo {
          opacity: 1;
      }

      .window {
          position: fixed;
          right: 1rem;
          bottom: 1rem;
          border-radius: .5rem;
          width: 480px;
          font-size: 14px;
          background-color: rgba(50,50,50,1);
          color: #fff;
          transition: 400ms;
          transform-origin: bottom right;
      }

      .window-header {
          display: flex;
          justify-content: flex-end;
          align-items: center;
          background-color: rgba(40,40,40,1);
          border-radius: .5rem .5rem 0px 0px;
          border-bottom: 1px solid rgba(70,70,70,1);
          padding: .25rem .5rem;
          text-align: right;
      }
            
      .ahreflike {
          cursor: pointer;
          font-weight: 600;
      }
      
      a {
          font-weight: 600;
      }

      .live-reload-text {
          padding: 0 .5rem 0 .25rem;
      }
      
      .minimize-button {
          width: 17px;
          height: 17px;
          color: #fff;
          background-color: transparent;
          border: 0;
          padding: 0;
          margin-left: .25rem;
          opacity: .8;
      }
      
      .minimize-button:hover {
          opacity: 1;
      }
      
      .message.warning {
          --gizmo-notification-color: var(--gizmo-yellow-color);
      }
      
      .message.error {
          --gizmo-notification-color: var(--gizmo-red-color);
      }
      
      .message .message-content {
          display: flex;
          align-items: center;
      }
      
      .message .message-content:before {
          display: inline-block;
          color: #000;
          text-align: center;
          font-size: 0.875em;
          font-weight: 600;
          line-height: 1.25em;
          width: 1.25em;
          height: 1.25em;
          border-radius: 50%;
          margin-right: .5rem;
      }

      .message.information .message-content:before {
          content: "i";
          color: var(--gizmo-grey-color);
          box-shadow: inset 0 0 0 1px var(--gizmo-grey-color);
      }
            
      .message.warning .message-content:before {
          content: "!";
          background-color: var(--gizmo-notification-color);
      }
      
      .message.error .message-content:before {
          content: "!";
          background-color: var(--gizmo-notification-color);
      }
      
      .message .message-details {
          font-size: .875em;
          min-width: 200px;
          padding-bottom: 4px;
          padding-left: 1.625rem;
      }
      
      .message .message-actions {
          display: flex;
          flex-direction: row;
          justify-content: flex-end;
          font-size: 1em;
      }
      
      .message .message-actions > *:not(:first-child) {
          margin-left: .5rem;
      }
      
      .message .message-actions a,
      .message .message-actions .ahreflike {
          color: var(--gizmo-notification-color, rgba(255,255,255,.9));
          text-decoration: none;
      }
      
      .message .message-actions a:hover,
      .message .message-actions .ahreflike:hover {
          color: var(--gizmo-notification-color, rgba(255,255,255,1));
          text-decoration: none;
      }
      
      .message .message-actions .dismiss-message {
          color: rgba(255,255,255,.6);
      }
      
      .message .message-actions .dismiss-message:hover {
          color: var(--gizmo-red-color);
      }
      
      .notification-tray {
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
          justify-content: center;
          position: fixed;
          right: 0;
          bottom: 4rem;
          height: auto;
          width: auto;
          padding-left: .5rem;
          z-index: 20000;
      }
      
      .notification-tray .message {
          background-color: rgba(50,50,50);
          color: rgba(255,255,255,.9);
          width: auto;
          max-width: 400px;
          border-top-left-radius: var(--gizmo-border-radius);
          border-bottom-left-radius: var(--gizmo-border-radius);
          padding-top: .25rem;
          padding-bottom: .25rem;
          padding-left: .75rem;
          padding-right: .75rem;
          margin-top: .5rem;
          transition: 400ms;
          transform-origin: bottom right;
          animation: slideIn 400ms;
      }
      
      .notification-tray .message.animate-out {
        animation: slideOut forwards 400ms;
      }
      
      .notification-tray .message .message-details {
          min-width: 200px;
          padding-bottom: 4px;
      }
      
      .message-tray .message {
          padding: .25em .75em;
          animation: appendList 400ms ease-in;
      }
      
      .message-tray .message .message-details {
          font-size: 1em;
      }

      .message-tray .message:not(:last-of-type) {
          border-bottom: 1px solid rgba(70,70,70,1);
      }
      
      .message-tray .dismiss-message {
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
      serviceurl: {type: String},
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

  static get INFORMATION() {
    return 'information';
  }

  static get WARNING() {
    return 'warning';
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
    const hostname = window.location.hostname;
    // try Spring Boot Devtools first, if port is set
    if (this.liveReloadBackend === VaadinDevmodeGizmo.SPRING_BOOT_DEVTOOLS && this.springBootDevToolsPort) {
      const self = this;
      const wsProtocol = window.location.protocol == 'https:' ? 'wss' : 'ws';
      if (hostname.endsWith('gitpod.io')) {
        // Gitpod uses `port-url` instead of `url:port`
        const hostnameWithoutPort = hostname.replace(/.*?-/, '');
        self.connection = new WebSocket(
            wsProtocol + '://' + this.springBootDevToolsPort + '-' + hostnameWithoutPort);
      } else {
        self.connection = new WebSocket(
            wsProtocol + '://' + hostname + ':' + this.springBootDevToolsPort);
      }
    } else if (this.liveReloadBackend) {
      this.openDedicatedWebSocketConnection();
    } else {
      this.showNotification(VaadinDevmodeGizmo.WARNING,
        'Live reload unavailable',
        'Live reload is currently not set up. Find out how to make use of this functionality to boost your workflow.',
        'https://github.com/vaadin/flow-and-components-documentation/blob/master/documentation/workflow/workflow-overview.asciidoc',
        'liveReloadUnavailable');
    }
    if (this.connection) {
      this.connection.onmessage = msg => this.handleMessage(msg);
      this.connection.onerror = err => this.handleError(err);
      this.connection.onclose = _ => {
        self.status = VaadinDevmodeGizmo.UNAVAILABLE;
        self.connection = null;
      };
    }
  }

  openDedicatedWebSocketConnection() {
    const url = this.serviceurl ? this.serviceurl : window.location.toString();
    if (!url.startsWith('http://')) {
      console.warn('The protocol of the url should be http for live reload to work.');
      return;
    }
    const wsUrl = url.replace(/^http:/, 'ws:') + '?refresh_connection';
    const self = this;
    this.connection = new WebSocket(wsUrl);
    setInterval(function() {
      if (self.connection !== null) {
        self.connection.send('');
      }
    }, VaadinDevmodeGizmo.HEARTBEAT_INTERVAL);
  }

  handleMessage(msg) {
    const json = JSON.parse(msg.data);
    const command = json['command'];
    switch (command) {
      case 'hello': {
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
          this.showSplashMessage('Reloading...');
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
    // automatically move notification to message tray after a certain amount of time
    if (this.splashMessage != null) {
      setTimeout(() => {
        this.demoteSplashMessage();
      }, VaadinDevmodeGizmo.AUTO_DEMOTE_NOTIFICATION_DELAY);
    }
  }

  demoteSplashMessage() {
    if (this.splashMessage) {
      this.showMessage(VaadinDevmodeGizmo.INFORMATION, this.splashMessage);
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
        persistentId: persistentId
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

  dismissNotification(id, persistently = false) {
    const index = this.notifications.findIndex(notification => notification.id === id);
    if (index !== -1 && !this.notifications[index].deleted) {
      const notification = this.notifications[index];

      // user is explicitly dismissing a notification---after that we won't bug them with it
      if (persistently && notification.persistentId && !VaadinDevmodeGizmo.notificationDismissed(notification.persistentId)) {
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

      // give some time for the animation
      setTimeout(() => {
        const index = this.notifications.findIndex(notification => notification.id === id);
        if (index != -1) {
          this.notifications.splice(index, 1);
          this.requestUpdate();
        }
      }, 400);
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
      <div class="message ${messageObject.type} ${messageObject.deleted ? 'animate-out' : ''}" @click=${e => this.dismissNotification(messageObject.id)}>
        <div class="message-content">${messageObject.message}</div>
        ${messageObject.details ? html`<div class="message-details">${messageObject.details}</div>` : ''}
        <div class="message-actions">
            ${messageObject.link ? html`<a href="${messageObject.link}" target="_blank">Read more</a>` : ''}
            ${messageObject.persistentId ? html`<span class="ahreflike dismiss-message" @click=${e => this.dismissNotification(messageObject.id, true)}>Don't show again</span>` : ''}
        </div>
      </div>
      `;
  }

  render() {
    return html`
      <div class="vaadin-live-reload">

      <div class="window ${this.expanded ? 'visible' : 'hidden'}">
        <div class="window-header">
            <label class="switch">
                <input id="toggle" type="checkbox"
                    ?disabled=${this.status === VaadinDevmodeGizmo.UNAVAILABLE || this.status === VaadinDevmodeGizmo.ERROR}
                    ?checked="${this.status === VaadinDevmodeGizmo.ACTIVE}"
                    @change=${e => this.setActive(e.target.checked)}/>
                <span class="slider"></span>
             </label>
             <span class="live-reload-text">Live-reload</span>
             <button class="minimize-button" @click=${e => this.toggleExpanded()}>
               <svg xmlns="http://www.w3.org/2000/svg" style="width: 18px; height: 18px;">
                 <g stroke="#fff" stroke-width="1.25">
                   <rect rx="5" x="0.5" y="0.5" height="16" width="16" fill-opacity="0"/>
                   <line y2="12.1" x2="12.3" y1="3.4" x1="3" />
                   <line y2="8.5" x2="12.1" y1="12.4" x1="12.8" />
                   <line y2="12.1" x2="8.7" y1="12.1" x1="12.8" />
                 </g>
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
          <g><title>vaadin-logo</title>
          <path d="M15.21 0.35c-0.436 0-0.79 0.354-0.79 0.79v0 0.46c0 0.5-0.32 0.85-1.070 0.85h-3.55c-1.61 0-1.73 1.19-1.8 1.83v0c-0.060-0.64-0.18-1.83-1.79-1.83h-3.57c-0.75 0-1.090-0.37-1.090-0.86v-0.45c0-0.006 0-0.013 0-0.020 0-0.425-0.345-0.77-0.77-0.77-0 0-0 0-0 0h0c-0 0-0 0-0 0-0.431 0-0.78 0.349-0.78 0.78 0 0.004 0 0.007 0 0.011v-0.001 1.32c0 1.54 0.7 2.31 2.34 2.31h3.66c1.090 0 1.19 0.46 1.19 0.9 0 0 0 0.090 0 0.13 0.048 0.428 0.408 0.758 0.845 0.758s0.797-0.33 0.845-0.754l0-0.004s0-0.080 0-0.13c0-0.44 0.1-0.9 1.19-0.9h3.61c1.61 0 2.32-0.77 2.32-2.31v-1.32c0-0.436-0.354-0.79-0.79-0.79v0z"></path>
          <path d="M11.21 7.38c-0.012-0-0.026-0.001-0.040-0.001-0.453 0-0.835 0.301-0.958 0.714l-0.002 0.007-2.21 4.21-2.3-4.2c-0.122-0.425-0.507-0.731-0.963-0.731-0.013 0-0.026 0-0.039 0.001l0.002-0c-0.012-0-0.025-0.001-0.039-0.001-0.58 0-1.050 0.47-1.050 1.050 0 0.212 0.063 0.41 0.171 0.575l-0.002-0.004 3.29 6.1c0.15 0.333 0.478 0.561 0.86 0.561s0.71-0.228 0.858-0.555l0.002-0.006 3.34-6.1c0.090-0.152 0.144-0.335 0.144-0.53 0-0.58-0.47-1.050-1.050-1.050-0.005 0-0.010 0-0.014 0h0.001z"></path>
          </g>
        </svg>
        <span class="status-blip" style="background-color: ${this.getStatusColor()}"></span>
          ${this.splashMessage !== null
      ? html`<span class="status-description">${this.splashMessage}</span></div>`
      : html`<span class="status-description">Live-reload ${this.status} </span><span class="ahreflike">Details</span></div>`
    }
      </div>
    </div>`;
  }
}

const init = function(serviceUrl, liveReloadBackend, springBootDevToolsPort) {
  if ('false' !== window.localStorage.getItem(VaadinDevmodeGizmo.ENABLED_KEY_IN_LOCAL_STORAGE)) {
    customElements.define('vaadin-devmode-gizmo', VaadinDevmodeGizmo);
    const devmodeGizmo = document.createElement('vaadin-devmode-gizmo');
    if (serviceUrl) {
      devmodeGizmo.setAttribute('serviceurl', serviceUrl);
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

export {init};
