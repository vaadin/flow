// A custom element that isn't using Polymer
class CustomCustomElement extends HTMLElement {
  static get is() { return 'custom-custom-element' }

  constructor() {
    super();
    this.attachShadow({mode: "open"});
    this.property = "constructor";
  }

  set property(value) {
    this.shadowRoot.textContent = value;
  }

  get property() {
    return this.shadowRoot.textContent;
  }
}
customElements.define(CustomCustomElement.is, CustomCustomElement);
