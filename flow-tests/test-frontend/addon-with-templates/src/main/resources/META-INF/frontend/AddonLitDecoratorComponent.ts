import { html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';

// This add-on source is copied into Frontend/generated/jar-resources, which the
// generated tsconfig.json excludes. It deliberately uses TypeScript
// experimental decorators (@customElement/@property) so the bundle build must
// transpile them. Two things must work for this component to behave correctly:
//  1. Decorators must be transpiled, otherwise the browser cannot parse the
//     chunk ("Uncaught SyntaxError: Invalid or unexpected token").
//  2. The transpiled class fields must use `useDefineForClassFields: false`
//     semantics, otherwise the `label` field shadows the accessor Lit installs
//     for the reactive property and updates stop reaching the DOM.
// `label` is a reactive property set from the server, so asserting that the
// rendered value changes exercises both concerns end to end.
@customElement('addon-lit-decorator-component')
export class AddonLitDecoratorComponent extends LitElement {
  @property() label = 'Default';

  render() {
    return html`<span id="label">${this.label}</span>`;
  }
}
