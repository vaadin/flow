import type {
  DevToolsInterface,
  DevToolsPlugin,
  ServerMessage
} from 'Frontend/generated/jar-resources/vaadin-dev-tools/vaadin-dev-tools';
import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

let pendingMessages: string[] = [];

@customElement('my-tool')
export class MyTool extends LitElement {
  @property({ type: Object })
  private devToolsInterface!: DevToolsInterface;

  @property({ type: Array })
  messages: string[] = [];

  connectedCallback(): void {
    super.connectedCallback();
    this.messages = pendingMessages;
    pendingMessages = [];
  }
  render() {
    return html`<div>
      <button @click=${this.modifyUI}>Tell server to add a component</button>
      ${this.messages.map((msg) => html`<div class="plugin-log">${msg}</div>`)}
    </div>`;
  }

  private modifyUI() {
    const realClients: any[] = Object.values((window as any).Vaadin.Flow.clients).filter(
      (client: any) => !!client.getUIId
    );

    this.devToolsInterface.send('plugin-query', {
      uiId: realClients[0].getUIId(),
      text: 'Hello from dev tools plugin'
    });
  }
}

const plugin: DevToolsPlugin = {
  init: function (devToolsInterface: DevToolsInterface): void {
    devToolsInterface.addTab('Hello', () => html`<my-tool .devToolsInterface=${devToolsInterface}></my-tool>`);
    devToolsInterface.addMessageListener((message: ServerMessage): boolean => {
      // It is not clear that the tab has been rendered
      const content = devToolsInterface.getContent();
      let myTool: MyTool | undefined;
      if (content && content.tagName === 'MY-TOOL') {
        myTool = content as MyTool;
      }

      let target;
      if (myTool) {
        target = myTool.messages;
      } else {
        target = pendingMessages;
      }

      if (message.command === 'plugin-init') {
        target.push('plugin-init');
        if (myTool) {
          myTool.requestUpdate();
        }
        return true;
      } else if (message.command === 'plugin-response') {
        target.push(message.data.text);
        if (myTool) {
          myTool.requestUpdate();
        }
        return true;
      }
      return false;
    });
  }
};

(window as any).Vaadin.devToolsPlugins.push(plugin);
