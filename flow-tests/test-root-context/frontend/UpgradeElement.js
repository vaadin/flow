import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class MyTemplate extends Polymer.Element {
    static get is() { return 'upgrade-element' }
    
    static get template() {
        return html`
            <input id="input" value="{{text::input}}" on-change="valueUpdated">
    `;
      }
}

