import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class MyTemplate extends PolymerElement {
  static get is() { return 'default-property' }

  static get template() {
    return html`
        <div id="text">[[text]]</div>
        <div id="name">[[name]]</div>
        <div id="message">[[message]]</div>
        <div id="email">[[email]]</div>
        `;
  }

  static get properties(){
    return {
      text: {
        type: String,
        value: ""
      },
      name :{
        type: String,
        value: "bar"
      },
      message :{
        type: String,
        value: "msg",
        notify: true
      },
      email :{
        type: String,
        value: "foo@example.com",
        notify: true
      }
    };
  }
}
customElements.define(MyTemplate.is, MyTemplate);
