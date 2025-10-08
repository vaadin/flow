import { unsafeCSS } from 'lit';
import { html, LitElement, css } from 'lit';

import '@vaadin/vaadin-horizontal-layout';
import '@vaadin/vaadin-vertical-layout';

class InlineStyles extends LitElement {
  static get styles() {
    const includedStyles = {};
    includedStyles['shared-styles'] =
      document.querySelector("dom-module[id='shared-styles']")?.firstElementChild?.content?.firstElementChild
        ?.innerText ?? '';
    includedStyles['something-else'] =
      document.querySelector("dom-module[id='something-else']")?.firstElementChild?.content?.firstElementChild
        ?.innerText ?? '';
    return [
      unsafeCSS(includedStyles['shared-styles']),
      unsafeCSS(includedStyles['something-else']),
      css`
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
      `
    ];
  }
  render() {
    return html`
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
    return 'inline-styles';
  }
}

customElements.define(InlineStyles.is, InlineStyles);
