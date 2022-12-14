import { PolymerElement } from "@polymer/polymer/polymer-element.js";
import "@vaadin/vaadin-grid/src/vaadin-grid.js";
import "@vaadin/vaadin-dialog/src/vaadin-dialog.js";
import { html } from "@polymer/polymer/lib/utils/html-tag.js";

class ThisDollarMappedElement extends PolymerElement {
  static get template() {
    return html`
      <search-bar id="search" show-checkbox=""></search-bar>

      <vaadin-grid id="grid" theme="orders no-row-borders"></vaadin-grid>

      <vaadin-dialog
        id="dialog"
        theme="orders"
        on-opened-changed="_onDialogOpen"
      ></vaadin-dialog>
    `;
  }

  static get is() {
    return "this-dollar-mapped-element";
  }

  ready() {
    super.ready();

    const grid = this.$.grid;
    console.log("Grid is ", grid);
  }

  // Workaround for styling the dialog content https://github.com/vaadin/vaadin-dialog-flow/issues/69
  _onDialogOpen(e) {
    if (!e.detail.value) {
      return;
    }
    var content = this.$.dialog.$.overlay.content;
    console.log("content is ", content);
  }
}

window.customElements.define(
  ThisDollarMappedElement.is,
  ThisDollarMappedElement
);
