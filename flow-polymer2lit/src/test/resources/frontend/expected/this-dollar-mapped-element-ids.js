import { html, LitElement, css } from "lit";

import "@vaadin/vaadin-grid/src/vaadin-grid.js";
import "@vaadin/vaadin-dialog/src/vaadin-dialog.js";

class ThisDollarMappedElement extends LitElement {
  render() {
    return html`
      <search-bar id="search" show-checkbox=""></search-bar>

      <vaadin-grid id="grid" theme="orders no-row-borders"></vaadin-grid>

      <vaadin-dialog
        id="dialog"
        theme="orders"
        @opened-changed="${this._onDialogOpen}"
      ></vaadin-dialog>
    `;
  }

  static get is() {
    return "this-dollar-mapped-element";
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);

    const grid = this.renderRoot.querySelector("#grid");
    console.log("Grid is ", grid);
  }

  // Workaround for styling the dialog content https://github.com/vaadin/vaadin-dialog-flow/issues/69
  _onDialogOpen(e) {
    if (!e.detail.value) {
      return;
    }
    var content = this.renderRoot.querySelector("#dialog").$.overlay.content;
    console.log("content is ", content);
  }
}

window.customElements.define(
  ThisDollarMappedElement.is,
  ThisDollarMappedElement
);
