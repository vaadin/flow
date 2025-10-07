import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';

class ComputedProperty extends PolymerElement {
  static get template() {
    return html`
      <vaadin-vertical-layout>
        <span>First: [[first]]</span>
        <span>First: [[last]]</span>
        <span>First: [[fullName]]</span>
      </vaadin-vertical-layout>
    `;
  }

  static get properties() {
    return {
      first: String,
      last: String,
      fullName: {
        type: String,
        computed: 'computeFullName(first, last)'
      }
    };
  }

  computeFullName(first, last) {
    return first + ' ' + last;
  }

  static get is() {
    return 'computed-property';
  }
}

customElements.define(ComputedProperty.is, ComputedProperty);
