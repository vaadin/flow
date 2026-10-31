// A minimal slider that isn't using Polymer, only to verify that the
// @JsModule of a Composite's content is loaded
class CompositeSlider extends HTMLElement {
  static get is() {
    return 'composite-slider';
  }

  constructor() {
    super();
    this.max = 100;
  }
}
customElements.define(CompositeSlider.is, CompositeSlider);
