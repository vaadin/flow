import { html, LitElement, css } from "lit";

class ReadyCallback extends LitElement {
  render() {
    return html`
      <div class="content">
        <vaadin-horizontal-layout style="width:100%; margin-top: 25px">
          <div id="container"></div>
        </vaadin-horizontal-layout>

        <div>Property: ${this.property}</div>
      </div>
    `;
  }

  static get is() {
    return "ready-callback";
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);

    div = document.createElement("div");
    div.innerText = "Created in ready()";
    this.renderRoot.querySelector("#container").appendChild(div);
    this.property = "Value set in ready()";
  }
}

customElements.define(ReadyCallback.is, ReadyCallback);
