import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';

class ReadyCallback extends PolymerElement {
  static get template() {
    return html`
      <div class="content">
        <vaadin-horizontal-layout style="width:100%; margin-top: 25px">
          <div id="container"></div>
        </vaadin-horizontal-layout>

        <div>Property: [[property]]</div>
      </div>
    `;
  }

  static get is() {
    return 'ready-callback';
  }

  ready() {
    super.ready();

    div = document.createElement('div');
    div.innerText = 'Created in ready()';
    this.$.container.appendChild(div);
    this.property = 'Value set in ready()';
  }
}

customElements.define(ReadyCallback.is, ReadyCallback);
