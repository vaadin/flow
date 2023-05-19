import {PolymerElement,html} from '@polymer/polymer/polymer-element.js';

class MyButton extends PolymerElement {

    static get template() {
        return html`<button>Click me</button>`;
    }

    static get is() {
          return 'my-button';
    }
}

customElements.define(MyButton.is, MyButton);
