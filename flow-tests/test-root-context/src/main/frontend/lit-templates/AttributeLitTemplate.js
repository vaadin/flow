import { LitElement, html } from 'lit';

export class AttributeLitTemplate extends LitElement {
  render() {
    return html`
      <div style="padding: 10px; border: 1px solid black">
        <div id="div" title="foo" foo="bar" baz></div>
      </div>
      <slot></slot>
    `;
  }
}
customElements.define('attribute-lit-template', AttributeLitTemplate);
