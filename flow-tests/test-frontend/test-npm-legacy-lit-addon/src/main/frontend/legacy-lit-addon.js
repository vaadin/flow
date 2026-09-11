import { LitElement, html } from 'lit';

/**
 * Mimics an add-on that is built on Lit, and renders through a reactive
 * property so that the property initializers Lit installs are exercised.
 */
class LegacyLitAddonElement extends LitElement {
  static get properties() {
    return {
      page: { type: Number }
    };
  }

  constructor() {
    super();
    this.page = 1;
  }

  render() {
    return html`
      <button id="previousPage" @click="${() => this._movePage(-1)}">Previous</button>
      <span id="currentPage">${this.page}</span>
      <button id="nextPage" @click="${() => this._movePage(1)}">Next</button>
    `;
  }

  _movePage(delta) {
    this.page = Math.max(1, this.page + delta);
    this.dispatchEvent(new CustomEvent('page-change', { detail: { page: this.page } }));
  }
}

customElements.define('legacy-lit-addon', LegacyLitAddonElement);
