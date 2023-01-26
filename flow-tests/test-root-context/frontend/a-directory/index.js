import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

export class ADirectoryComponent extends PolymerElement {
  static get template() {
    return html`Directory import ok`;
  }
  static get is() {
    return 'a-directory-component'
  }
}
customElements.define(ADirectoryComponent.is, ADirectoryComponent);