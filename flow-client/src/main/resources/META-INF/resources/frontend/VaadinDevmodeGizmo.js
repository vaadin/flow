import {LitElement, html, css} from 'lit-element';

class VaadinDevmodeGizmo extends LitElement {

  static get styles() {
    return css`
      .vaadin-live-reload > div {
         box-shadow: 2px 2px 2px grey;
         background-color: #333; 
         color: #DDD;
      }
      
      .gizmo { 
          position: fixed;
          right: 15px;
          top: 10px;
          z-index: 20000;
      }
      
      .vaadin-logo {
          font-size: 20px;
          font-weight: bold;
          text-align: center;
          vertical-align: middle; 
          line-height: 50px; 
          transform: rotate(90deg);
          border-radius: 50%;
          width: 50px;
          height: 50px;
      }
      
      .notification {
        padding: 4px 20px 4px 10px;
        border-radius: 8px;
      }
      
      .status-blip {
          position: fixed;
          right: 15px;
          top: 10px;
          background-color: green;
          border: 3px solid black;
          border-radius: 50%;
          width: 10px;
          height: 10px;
          z-index: 20001;
      }
      
      .window {
          position: fixed;
          right: 10px;
          top: 65px;
          border-radius: 8px;
          border: #AAA;
          width: 30%;
          font-size: 14px;
      }
      
      .window-header {
          background-color: #222;
          border-radius: 8px 8px 0px 0px;
          border-color: #AAA;
          padding: 4px;
          text-align: right;
      }
      
      .message-tray > div {
          padding: 6px;
          border-top: 1px solid #444;
      }
      
      .message-tray > div:before {
          content: "ⓘ";
          margin-right: 4px;
      }
    `;
  }

  static get properties() {
    return {
      expanded: {type: Boolean},
      messages: {type: Array},
      status: {type: String},
      notification: {type: String},
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

  static get isEnabled() {
    const enabled = window.localStorage.getItem(VaadinDevmodeGizmo.ENABLED_KEY_IN_LOCAL_STORAGE);
    return enabled === null || !(enabled === 'false');
  }

  static get isActive() {
    const active = window.sessionStorage.getItem(VaadinDevmodeGizmo.ACTIVE_KEY_IN_SESSION_STORAGE);
    return active === null || active !== 'false';
  }

  constructor() {
    super();
    this.messages = [];
    this.expanded = false;
    this.status = VaadinDevmodeGizmo.UNAVAILABLE;
    this.notification = null;
    this.connection = null;
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
      self.connection = new WebSocket(
        'ws://' + hostname + ':' + this.springBootDevToolsPort);
    } else if (this.liveReloadBackend) {
      this.openDedicatedWebSocketConnection();
    } else {
      this.showMessage('Live reload unavailable');
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
          this.showMessage('Live reload available: ' + backend);
        } else {
          this.status = VaadinDevmodeGizmo.INACTIVE;
        }
        break;
      }

      case 'reload':
        if (this.status === VaadinDevmodeGizmo.ACTIVE) {
          this.showNotification('Reloading...');
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

    // when focus or clicking anywhere, move the notification to the message tray
    this.disableEventListener = e => this.demoteNotification();
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
      this.showNotification('Automatic reload #' + count + ' finished at ' + reloaded);
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
    this.expanded = !this.expanded;
  }

  showNotification(msg) {
    this.notification = msg;
  }

  showMessage(msg) {
    this.messages.push(msg);
  }

  demoteNotification() {
    if (this.notification) {
      this.showMessage(this.notification);
    }
    this.showNotification(null);
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
      return 'green';
    } else if (this.status === VaadinDevmodeGizmo.INACTIVE) {
      return 'grey';
    } else if (this.status === VaadinDevmodeGizmo.UNAVAILABLE) {
      return 'yellow';
    } else if (this.status === VaadinDevmodeGizmo.ERROR) {
      return 'red';
    } else {
      return 'none';
    }
  }

  render() {
    return html`
            <div class="vaadin-live-reload">
                ${this.notification !== null
    ? html`<div class="gizmo notification" @click=${e => this.toggleExpanded()}>${this.notification}</div>`
    : html`<div class="gizmo vaadin-logo" @click=${e => this.toggleExpanded()}>}&gt;</div>`}

                <span class="status-blip" style="background-color: ${this.getStatusColor()}"></span>
                <div class="window" style="visibility: ${this.expanded ? 'visible' : 'hidden'}">
                    <div class="window-header">
                        <button id="disable" @click=${e => this.disableLiveReload()}>Disable</button>
                        <input id="toggle" type="checkbox" 
                            ?disabled=${this.status === VaadinDevmodeGizmo.UNAVAILABLE || this.status === VaadinDevmodeGizmo.ERROR} 
                            ?checked="${this.status === VaadinDevmodeGizmo.ACTIVE}" 
                        @change=${e => this.setActive(e.target.checked)}>Live-reload</input>
                    </div>
                    <div class="message-tray">
                         ${this.messages.map(i => html`<div>${i}</div>`)}
                    </div>
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
  }
};

export {init};
