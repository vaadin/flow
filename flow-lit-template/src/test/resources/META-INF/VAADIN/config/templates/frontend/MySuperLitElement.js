// Import an element
import { LitElement, html } from 'lit';
import { SimpleLitTemplateShadowRoot } from './MyLitElement.js';
export class MySuperLitElement extends MyLitElement {
  createRenderRoot() {
    return this;
  }
}
customElements.define('my-super-lit-element', MySuperLitElement);
