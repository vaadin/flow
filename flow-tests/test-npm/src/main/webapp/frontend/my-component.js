import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class MyComponentElement extends PolymerElement {
  static get template() {
    return html`
        <div id="button">Click</div>
        <div id="content"></div>
`;
  }

  static get is() {
      return 'my-component'
  }
}
customElements.define(MyComponentElement.is, MyComponentElement);
