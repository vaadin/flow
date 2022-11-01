import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";
import "@vaadin/vaadin-horizontal-layout";
import "@vaadin/vaadin-vertical-layout";

class InlineStyles extends PolymerElement {
  static get template() {
    return html`
      <style include="shared-styles something-else">
        :host {
          display: block;
          height: 100%;
        }

        .details {
          color: red;
        }

        .title {
          color: blue;
        }
      </style>
      <div class="content">
        <vaadin-horizontal-layout class="cards-holder">
          <vaadin-vertical-layout class="inner-content">
            <vaadin-vertical-layout class="card-header">
              <span class="title">Trial</span>
              <span class="details">30 Days Trial</span>
            </vaadin-vertical-layout>
          </vaadin-vertical-layout>
        </vaadin-horizontal-layout>
      </div>
    `;
  }

  static get is() {
    return "inline-styles";
  }
}

customElements.define(InlineStyles.is, InlineStyles);
