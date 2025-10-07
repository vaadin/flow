import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@vaadin/vaadin-vertical-layout';
import '@polymer/polymer/lib/elements/dom-repeat.js';

class DomRepeatTest extends PolymerElement {
  static get template() {
    return html`
      <div>Employee list:</div>
      <template is="dom-repeat" items="{{managers}}">
        <div><br /># [[index]]</div>
        <div>Given name: <span>[[item.given]]</span></div>
        <div>Family name: <span>[[item.family]]</span></div>
        <template is="dom-repeat" items="{{item.employees}}">
          <div><br />Employee # [[index]]</div>
          <div>Employee name: <span>[[item.given]] [[item.family]]</span></div>
        </template>
      </template>
    `;
  }

  static get properties() {
    return {
      employees: {
        type: Array,
        value() {
          return [
            { given: 'Kamil', family: 'Smith' },
            { given: 'Sally', family: 'Johnson' }
          ];
        }
      }
    };
  }

  static get is() {
    return 'dom-repeat-test';
  }
}

customElements.define(DomRepeatTest.is, DomRepeatTest);
