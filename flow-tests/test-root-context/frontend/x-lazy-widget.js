import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import {GestureEventListeners} from '@polymer/polymer/lib/mixins/gesture-event-listeners.js';
import '@polymer/polymer/lib/elements/dom-if.js';

console.timeStamp('LazyWidget script start');
class LazyWidget extends GestureEventListeners(PolymerElement) {
  static get is() {
    return 'x-lazy-widget'
  }

  static get template() {
    return html`
        <style>
        :host {
            display: block;
            border: 1px solid grey;
            background: #ffecab;;
        }
        </style>
        <h2>I'm a lazy widget</h2>
        <input id="input" type="text" placeholder="Write your name...">
        <button id="button" on-tap="_onButtonTapped">Tap me!</button>
        <template is="dom-if" if="[[hasGreeting]]">
            <p id="greeting">[[greeting]]</p>
        </template>
    `;
  }

  _onButtonTapped(event) {
    this.hasGreeting = false;
    this.$server.greet(this.$.input.value);
  }
}

(function sleep(milliseconds) {
  var start = new Date().getTime();
  for (var i = 0; i < 1e7; i++) {
    if ((new Date().getTime() - start) > milliseconds){
      break;
    }
  }
})(1000);
customElements.define(LazyWidget.is, LazyWidget);
