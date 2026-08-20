import { html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';

// This add-on source is copied into the jar-resources folder, which the
// project tsconfig.json excludes. It deliberately uses TypeScript experimental
// decorators, so that the bundle build has to transpile them: browsers cannot
// parse a raw decorator and the chunk fails to load with a SyntaxError. The
// initializer of the reactive property has to end up in the constructor as
// well, otherwise a native class field shadows the accessor Lit installs and
// the element stops rendering updates.
@customElement('decorator-element')
export class DecoratorElement extends LitElement {
  @property() label = 'Default';

  render() {
    return html`<span id="label">${this.label}</span>`;
  }
}
