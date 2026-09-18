import {html, PolymerElement} from '@polymer/polymer/polymer-element.js';

class MyTestElement extends PolymerElement {
  static get template() {
    return html`
      <h1>[[title]]</h1>
    `;
  }

  static get properties() {
    return {title: {type: String}};
  }

  setTitle(text) {
    this.title = text;
  }

  constructor() {
    super();
    this.setTitle('untitled');
  }
}

customElements.define('my-test-element', MyTestElement);
