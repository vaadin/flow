import { SimpleLitTemplateShadowRoot } from './simple-lit-template-shadow-root.js';

export class SimpleLitTemplateNoShadowRoot extends SimpleLitTemplateShadowRoot {
  createRenderRoot() {
    return this;
  }
}
customElements.define('simple-lit-template-no-shadow-root', SimpleLitTemplateNoShadowRoot);
