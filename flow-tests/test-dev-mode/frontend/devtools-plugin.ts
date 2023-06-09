
import type {
  DevToolsInterface,
  DevToolsPlugin //@ts-ignore
} from 'Frontend/generated/jar-resources/vaadin-dev-tools/vaadin-dev-tools';
import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

@customElement('my-tool')
export class MyTool extends LitElement {
  @property({ type: Object })
  private devToolsInterface!: DevToolsInterface;

  render() {
    return html`<div>
      <button @click=${this.modifyUI}>Tell server to add a component</button>
    </div>`;
  }

  private modifyUI() {
    const realClients: any[] = Object.values((window as any).Vaadin.Flow.clients).filter(
      (client: any) => !!client.getUIId
    );

    this.devToolsInterface.send('modifyUI', {
      uiId: realClients[0].getUIId(),
      text: 'Hello from dev tools plugin'
    });
  }
}

const plugin: DevToolsPlugin = {
  init: function (devToolsInterface: DevToolsInterface): void {
    devToolsInterface.addTab('Hello', () => html`<my-tool .devToolsInterface=${devToolsInterface}></my-tool>`);
  }
};

(window as any).Vaadin.devToolsPlugins.push(plugin);
