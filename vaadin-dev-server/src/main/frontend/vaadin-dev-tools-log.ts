import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { VaadinDevTools } from './vaadin-dev-tools.js';

@customElement('vaadin-dev-tools-log')
export class VaadinDevToolsLog extends LitElement {
  @property({ type: Object })
  _devTools!: VaadinDevTools;

  protected createRenderRoot(): Element | ShadowRoot {
    return this;
  }

  activate() {
    this._devTools.unreadErrors = false;
    this.updateComplete.then(() => {
      const lastMessage = this.renderRoot.querySelector('.message-tray .message:last-child');
      if (lastMessage) {
        lastMessage.scrollIntoView();
      }
    });
  }
  render() {
    return html`<div class="message-tray">
      ${this._devTools.messages.map((msg) => this._devTools.renderMessage(msg))}
    </div>`;
  }
}
