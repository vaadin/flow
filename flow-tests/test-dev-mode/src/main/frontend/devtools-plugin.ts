import type {
  DevToolsInterface,
  DevToolsPlugin,
  MessageHandler,
  ServerMessage
} from 'Frontend/generated/jar-resources/vaadin-dev-tools/vaadin-dev-tools';
import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

let devTools: DevToolsInterface;

@customElement('my-tool')
export class MyTool extends LitElement implements MessageHandler {
  @property({ type: Array })
  messages: string[] = [];

  render() {
    return html`<div>
      <button @click=${this.modifyUI}>Tell server to add a component</button>
      ${this.messages.map((msg) => html`<div class="plugin-log">${msg}</div>`)}
    </div>`;
  }

  handleMessage(message: ServerMessage): boolean {
    if (message.command === 'plugin-init') {
      this.messages.push('plugin-init');
      this.requestUpdate();
      return true;
    } else if (message.command === 'plugin-response') {
      this.messages.push(message.data.text);
      this.requestUpdate();
      return true;
    }
    return false;
  }

  private modifyUI() {
    const realClients: any[] = Object.values((window as any).Vaadin.Flow.clients).filter(
      (client: any) => !!client.getUIId
    );

    devTools.send('plugin-query', {
      uiId: realClients[0].getUIId(),
      text: 'Hello from dev tools plugin'
    });
  }
  activate() {
    this.messages.push('activate called');
  }
  deactivate() {
    this.messages.push('deactivate called');
  }
}

const plugin: DevToolsPlugin = {
  init: function (devToolsInterface: DevToolsInterface): void {
    devTools = devToolsInterface;
    devTools.addTab('Hello', 'my-tool');
  }
};

(window as any).Vaadin.devToolsPlugins.push(plugin);
