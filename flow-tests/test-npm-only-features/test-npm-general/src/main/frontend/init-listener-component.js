import { LitElement, html } from 'lit';

class InitListenerComponent extends LitElement {
  render() {
    return html` <div>Init Listener Component</div> `;
  }

  static get is() {
    return 'init-listener-component';
  }
}

customElements.define(InitListenerComponent.is, InitListenerComponent);
