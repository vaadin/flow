import {
  columnBodyRenderer,
  columnFooterRenderer,
  columnHeaderRenderer,
} from "@vaadin/grid/lit.js";
import { html, LitElement, css } from "lit";

import "@vaadin/vaadin-grid";
import "@vaadin/vaadin-grid/vaadin-grid-selection-column";

class GridColumns extends LitElement {
  render() {
    return html`
      <vaadin-grid
        aria-label="Basic example"
        column-reordering-allowed
        multi-sort
        .items="${this.users}"
      >
        <vaadin-grid-selection-column
          auto-select
        ></vaadin-grid-selection-column>

        <vaadin-grid-column
          width="9em"
          ${columnHeaderRenderer(
            (column) =>
              html`
                <vaadin-grid-sorter path="firstName"
                  >First Name</vaadin-grid-sorter
                >
              `
          )}
          ${columnBodyRenderer((item) => html`${item.firstName}`)}
          ${columnFooterRenderer((column) => html`First Name`)}
        >
        </vaadin-grid-column>

        <vaadin-grid-column
          width="9em"
          ${columnHeaderRenderer(
            (column) =>
              html`
                <vaadin-grid-sorter path="lastName"
                  >Last Name</vaadin-grid-sorter
                >
              `
          )}
          ${columnBodyRenderer((item) => html`${item.lastName}`)}
          ${columnFooterRenderer((column) => html`Last Name`)}
        >
        </vaadin-grid-column>

        <vaadin-grid-column
          width="15em"
          flex-grow="2"
          ${columnHeaderRenderer(
            (column) =>
              html`
                <vaadin-grid-sorter path="address.street"
                  >Address</vaadin-grid-sorter
                >
              `
          )}
          ${columnBodyRenderer(
            (item) => html`${item.address?.street}, ${item.address?.city}`
          )}
          ${columnFooterRenderer((column) => html`Address`)}
        >
        </vaadin-grid-column>
      </vaadin-grid>
    `;
  }

  static get is() {
    return "grid-columns";
  }
  static get properties() {
    return {
      users: {
        type: Array,
      },
    };
  }
  constructor() {
    super();
    this.users = [
      {
        firstName: "John",
        lastName: "Short",
        address: { street: "Homestreet 1", city: "Boston" },
      },
      {
        firstName: "Lea",
        lastName: "Green",
        address: { street: "Faraway 22", city: "Cairo" },
      },
    ];
  }
}

customElements.define(GridColumns.is, GridColumns);
