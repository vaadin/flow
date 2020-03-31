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
      serviceurl: {type: String}
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

  static get SPRING_DEV_TOOLS_PORT() {
    return 35729;
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
    const self = this;
    const hostname = window.location.hostname;
    // try Spring Boot Devtools first
    self.connection = new WebSocket(
      'ws://' + hostname + ':' + VaadinDevmodeGizmo.SPRING_DEV_TOOLS_PORT);
    self.connection.onmessage = msg => self.handleMessage(msg);
    self.connection.onclose = _ => {
      self.status = VaadinDevmodeGizmo.UNAVAILABLE;
    };
    self.connection.onerror = err => {
      if (self.status === VaadinDevmodeGizmo.UNAVAILABLE) {
        // no Spring, try the dedicated push channel
        const url = self.serviceurl ? self.serviceurl : window.location.toString();
        if (!url.startsWith('http://')) {
          console.warn('The protocol of the url should be http for live reload to work.');
          return;
        }
        const wsUrl = url.replace(/^http:/, 'ws:') + '?refresh_connection';
        self.connection = new WebSocket(wsUrl);
        self.connection.onmessage = msg => self.handleMessage(msg);
        self.connection.onerror = err => self.handleError(err);
        self.connection.onclose = _ => {
          self.status = VaadinDevmodeGizmo.UNAVAILABLE;
        };
      } else {
        self.handleError(err);
      }
    };
  }

  handleMessage(msg) {
    const json = JSON.parse(msg.data);
    const command = json['command'];
    switch (command) {
      case 'hello':
        if (VaadinDevmodeGizmo.isActive) {
          this.status = VaadinDevmodeGizmo.ACTIVE;
        } else {
          this.status = VaadinDevmodeGizmo.INACTIVE;
        }
        this.showMessage('Live reload available');
        this.connection.onerror = e => self.handleError(e);
        break;

      case 'reload':
        if (this.status === VaadinDevmodeGizmo.ACTIVE) {
          this.showNotification('Reloading...');
          const now = new Date();
          const reloaded = ('0' + now.getHours()).slice(-2) + ':'
            + ('0' + now.getMinutes()).slice(-2) + ':'
            + ('0' + now.getSeconds()).slice(-2);
          window.sessionStorage.setItem(VaadinDevmodeGizmo.TRIGGERED_KEY_IN_SESSION_STORAGE, reloaded);
          window.location.reload();
        }
        break;

      default:
        console.warn('unknown command:', command);
    }
  }

  handleError(msg) {
    console.error(msg);
    this.status = VaadinDevmodeGizmo.ERROR;
  }

  connectedCallback() {
    super.connectedCallback();
    this.disableEventListener = e => this.demoteNotification();
    document.body.addEventListener('focus', this.disableEventListener);
    document.body.addEventListener('click', this.disableEventListener);
    this.openWebSocketConnection();

    const lastReload = window.sessionStorage.getItem(VaadinDevmodeGizmo.TRIGGERED_KEY_IN_SESSION_STORAGE);
    if (lastReload) {
      this.showNotification('Last automatic reload on ' + lastReload);
      window.sessionStorage.removeItem(VaadinDevmodeGizmo.TRIGGERED_KEY_IN_SESSION_STORAGE);
    }
  }

  disconnectedCallback() {
    document.body.removeEventListener('focus', this.disableEventListener);
    document.body.removeEventListener('click', this.disableEventListener);
    super.disconnectedCallback();
  }

  disableLiveReload() {
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
                        <input id="toggle" type="checkbox" ?checked="${this.status === VaadinDevmodeGizmo.ACTIVE}" 
                        @change=${e => this.setActive(e.target.checked)}>Live-reload</input>
                    </div>
                    <div class="message-tray">
                         ${this.messages.map(i => html`<div>${i}</div>`)}
                    </div>
                </div>
            </div>`;
  }
}

const init = function(serviceUrl) {
  if ('false' !== window.localStorage.getItem(VaadinDevmodeGizmo.ENABLED_KEY_IN_LOCAL_STORAGE)) {
    customElements.define('vaadin-devmode-gizmo', VaadinDevmodeGizmo);
    const devmodeGizmo = document.createElement('vaadin-devmode-gizmo');
    if (serviceUrl) {
      devmodeGizmo.setAttribute('serviceurl', serviceUrl);
    }
    document.body.appendChild(devmodeGizmo);
  }
};

export {init};
