import { html, LitElement, css } from 'lit';

class ComputedProperty extends LitElement {
  render() {
    return html`
      <vaadin-vertical-layout>
        <span>First: ${this.first}</span>
        <span>First: ${this.last}</span>
        <span>First: ${this.fullName}</span>
      </vaadin-vertical-layout>
    `;
  }

  static get properties() {
    return {
      first: String,
      last: String,
      fullName: {
        type: String
      }
    };
  }

  computeFullName(first, last) {
    return first + ' ' + last;
  }

  static get is() {
    return 'computed-property';
  }
  get fullName() {
    return this.computeFullName(this.first, this.last);
  }
}

customElements.define(ComputedProperty.is, ComputedProperty);
