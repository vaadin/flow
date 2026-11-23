// Fetches a data: URL the way vaadin-icon fetches the src of an SvgIcon, so
// that the view shows whether connect-src of the policy allows it.
customElements.define(
  'csp-data-url-element',
  class extends HTMLElement {
    async connectedCallback() {
      try {
        const response = await fetch(
          'data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%2F%3E'
        );
        const svg = await response.text();
        this.textContent = svg.startsWith('<svg') ? 'Fetched' : 'Unexpected content';
      } catch (e) {
        this.textContent = `Failed: ${e.message}`;
      }
    }
  }
);
