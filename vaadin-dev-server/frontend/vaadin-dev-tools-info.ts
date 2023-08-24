// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import { copy } from './copy-to-clipboard.js';

import { LitElement, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { ConnectionStatus } from './connection';

import { MessageType, VaadinDevTools } from './vaadin-dev-tools.js';

interface ServerInfo {
  vaadinVersion: string;
  flowVersion: string;
  javaVersion: string;
  osVersion: string;
  productName: string;
}

@customElement('vaadin-dev-tools-info')
export class InfoTab extends LitElement {
  @property({ type: Object })
  private _devTools!: VaadinDevTools;

  @state()
  private serverInfo: ServerInfo = {
    flowVersion: '',
    vaadinVersion: '',
    javaVersion: '',
    osVersion: '',
    productName: ''
  };

  protected createRenderRoot(): Element | ShadowRoot {
    return this;
  }

  render() {
    return html` <div class="info-tray">
      <button class="button copy" @click=${this.copyInfoToClipboard}>Copy</button>
      <dl>
        <dt>${this.serverInfo.productName}</dt>
        <dd>${this.serverInfo.vaadinVersion}</dd>
        <dt>Flow</dt>
        <dd>${this.serverInfo.flowVersion}</dd>
        <dt>Java</dt>
        <dd>${this.serverInfo.javaVersion}</dd>
        <dt>OS</dt>
        <dd>${this.serverInfo.osVersion}</dd>
        <dt>Browser</dt>
        <dd>${navigator.userAgent}</dd>
        <dt>
          Live reload
          <label class="switch">
            <input
              id="toggle"
              type="checkbox"
              ?disabled=${this._devTools.liveReloadDisabled ||
              ((this._devTools.frontendStatus === ConnectionStatus.UNAVAILABLE ||
                this._devTools.frontendStatus === ConnectionStatus.ERROR) &&
                (this._devTools.javaStatus === ConnectionStatus.UNAVAILABLE ||
                  this._devTools.javaStatus === ConnectionStatus.ERROR))}
              ?checked="${this._devTools.frontendStatus === ConnectionStatus.ACTIVE ||
              this._devTools.javaStatus === ConnectionStatus.ACTIVE}"
              @change=${(e: InputEvent) => this._devTools.setActive((e.target as HTMLInputElement).checked)}
            />
            <span class="slider"></span>
          </label>
        </dt>
        <dd
          class="live-reload-status"
          style="--status-color: ${this._devTools.getStatusColor(this._devTools.javaStatus)}"
        >
          Java ${this._devTools.javaStatus}
          ${this._devTools.backend ? `(${VaadinDevTools.BACKEND_DISPLAY_NAME[this._devTools.backend]})` : ''}
        </dd>
        <dd
          class="live-reload-status"
          style="--status-color: ${this._devTools.getStatusColor(this._devTools.frontendStatus)}"
        >
          Front end ${this._devTools.frontendStatus}
        </dd>
      </dl>
    </div>`;
  }

  handleMessage(message: any) {
    if (message?.command === 'serverInfo') {
      this.serverInfo = message.data as ServerInfo;
      return true;
    }
    return false;
  }

  copyInfoToClipboard() {
    const items = this.renderRoot.querySelectorAll('.info-tray dt, .info-tray dd');
    const text = Array.from(items)
      .map((message) => (message.localName === 'dd' ? ': ' : '\n') + message.textContent!.trim())
      .join('')
      .replace(/^\n/, '');
    copy(text);
    this._devTools.showNotification(
      MessageType.INFORMATION,
      'Environment information copied to clipboard',
      undefined,
      undefined,
      'versionInfoCopied'
    );
  }
}
