// Imported only by LazyChunkView, so the production build puts it in a chunk
// that is loaded when the view is first navigated to.
customElements.define(
  'csp-lazy-element',
  class extends HTMLElement {
    connectedCallback() {
      this.textContent = 'Loaded from a lazy chunk';
    }
  }
);
