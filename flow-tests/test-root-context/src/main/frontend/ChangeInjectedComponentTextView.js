import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ChangeInjectedComponentTextView extends PolymerElement {
  static get template() {
    return html`
       <div>
        <div id='injected'>
            Foo
            <label>Bar</label>
            Baz
        </div>
    </div>
    `;
  }
    
  static get is() {
    return 'update-injected-component-text'
  }
}

customElements.define(ChangeInjectedComponentTextView.is, ChangeInjectedComponentTextView);
