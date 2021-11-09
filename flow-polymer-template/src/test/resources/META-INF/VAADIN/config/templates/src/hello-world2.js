import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import '@polymer/paper-input/paper-input.js';

class HelloWorld extends PolymerElement {
  static get template() {
    return html`
            <div>
                <paper-input id="inputId" value="{{userInput}}"></paper-input>
                <button id="helloButton" on-click="sayHello">Say hello</button>
                <div id="greeting">[[greeting]]</div>
            </div>`;
  }

  static get is() {
    return 'hello-world';
  }

}

customElements.define(HelloWorld.is, HelloWorld);
