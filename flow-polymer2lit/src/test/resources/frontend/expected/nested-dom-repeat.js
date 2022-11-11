import { html, LitElement, css } from "lit";

import "@vaadin/vaadin-vertical-layout";

class DomRepeatTest extends LitElement {
  render() {
    return html`
      <div>Employee list:</div>
      ${(this.managers ?? []).map(
        (item, index) => html`
          <div><br /># ${index}</div>
          <div>Given name: <span>${item.given}</span></div>
          <div>Family name: <span>${item.family}</span></div>
          ${(item.employees ?? []).map(
            (item, index) => html`
              <div><br />Employee # ${index}</div>
              <div>
                Employee name: <span>${item.given} ${item.family}</span>
              </div>
            `
          )}
        `
      )}
    `;
  }

  static get properties() {
    return {
      employees: {
        type: Array,
      },
    };
  }

  static get is() {
    return "dom-repeat-test";
  }
  constructor() {
    super();
    this.employees = [
      { given: "Kamil", family: "Smith" },
      { given: "Sally", family: "Johnson" },
    ];
  }
}

customElements.define(DomRepeatTest.is, DomRepeatTest);
