import { html, LitElement, css } from 'lit';

import '@vaadin/vaadin-vertical-layout';

class DomRepeatTest extends LitElement {
  render() {
    return html`
      <div>Employee list:</div>
      ${(this.employees?.list ?? []).map(
        (item, index) => html`
          <div><br /># ${index}</div>
          <div>Given name: <span>${item.given}</span></div>
          <div>Family name: <span>${item.family}</span></div>
        `
      )}
      ${(this.employees?.list ?? []).map(
        (item, index) => html`
          <div><br /># ${index}</div>
          <div>Given name: <span>${item.given}</span></div>
          <div>Family name: <span>${item.family}</span></div>
        `
      )}
    `;
  }

  static get properties() {
    return {
      employees: {
        type: Object
      }
    };
  }

  static get is() {
    return 'dom-repeat-test';
  }
  constructor() {
    super();
    this.employees = {
      list: [
        { given: 'Kamil', family: 'Smith' },
        { given: 'Sally', family: 'Johnson' }
      ]
    };
  }
}

customElements.define(DomRepeatTest.is, DomRepeatTest);
