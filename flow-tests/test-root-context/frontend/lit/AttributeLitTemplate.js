
import { LitElement, html } from "lit-element";

export class AttributeLitTemplate extends LitElement {
  render() {
    return html`
       <div style="padding: 10px; border: 1px solid black">
       <div id="div" title="foo" foo="bar" baz></div>
       <div id="disabled" disabled></div>
       <div id="hasText">foo</div>
       <div id="hasTextAndChild">foo <label>bar</label> baz</div>
       </div>
       <slot>
    `;
  }
}
customElements.define("attribute-lit-template", AttributeLitTemplate);
