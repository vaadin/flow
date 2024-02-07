import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class MultiplePropsMutation extends PolymerElement {
  static get is() { return 'multiple-props-mutation' }

  static get template() {
    return html`
       <label id='name'>[[name]]</label>
       <label id='msg'>[[message]]</label>
       <slot></slot>
  `;
  }

  static get properties() {
    return {
      name :{
        type: String,
        notify: true
      },
      message :{
        type: String,
        notify: true
      },
    };
  }
}
customElements.define(MultiplePropsMutation.is, MultiplePropsMutation);
