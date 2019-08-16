import {PolymerElement, html} from '@polymer/polymer';
import {mixinBehaviors} from '@polymer/polymer/lib/legacy/class.js';
import {IronResizableBehavior} from '@polymer/iron-resizable-behavior/iron-resizable-behavior.js';

class MixinInjects extends mixinBehaviors([IronResizableBehavior], PolymerElement) {
  static get is() {
    return 'mixin-injects'
  }

  static get template() {
    return html`
       <div id="injected"></div>
    `;
  }
}
customElements.define(MixinInjects.is, MixinInjects);
