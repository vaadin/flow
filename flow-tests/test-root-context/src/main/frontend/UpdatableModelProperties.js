import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class UpdatableProperties extends PolymerElement {
  static get is() { return 'updatable-model-properties' }

  static get properties() {
    return {
      name: {
        type: String,
        value:'bar',
        notify: true
      },
      age: {
        type: Number,
        value: 11,
        notify: true
      },
      email: {
        type: String,
        value: 'baz@example.com',
        notify: true
      },
      text: {
        type: String,
        value: 'baz',
        notify: true
      }
    }
  }

  updateName(){
    this.name='foo';
    this.$server.updateStatus();
  }

  updateAge(){
    if (this.age < 29){
     this.age = 29;
    } else {
      this.age = this.age+1;
    }
    this.$server.updateStatus();
  }

  updateEmail(){
    this.email="foo@bar.com";
    this.$server.updateStatus();
  }

  updateText(){
    this.text='bar';
    this.$server.updateStatus();
  }

  clearStatus(){
    this.updateStatus='';
  }

  static get template() {
    return html`
        <div id="name" on-click='updateName'>{{name}}</div>
        <div id="age" on-click='updateAge'>[[age]]</div>
        <div id="email" on-click='updateEmail'>[[email]]</div>
        <div id="text" on-click='updateText'>[[text]]</div>
        <button id="syncAge" on-click="syncAge">Synchronize Age</button>
        <label id='updateStatus' on-click='clearStatus'>[[updateStatus]]</label>
        <slot></slot>
        `;
  }
}
customElements.define(UpdatableProperties.is, UpdatableProperties);
