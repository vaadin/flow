import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class PropertiesUpdatedBeforeChangeEvents extends PolymerElement {
  static get is() { return 'properties-updated-before-change-events' }

  static get template() {
    return html`
      <input id="first-prop-input" value="{{firstProp::input}}">
      <div id="second-prop-div">[[secondProp]]</div>
      <div id="text-div">[[text]]</div>
      `;
  }

  static get properties(){
    return {
      firstProp: {
        type: String,
        value: "",
        observer: '_firstPropChanged',
        notify: true
      }
    }
  }

  _firstPropChanged(newVal, oldVal) {
    this.secondProp = newVal;
  }
}
customElements.define(PropertiesUpdatedBeforeChangeEvents.is, PropertiesUpdatedBeforeChangeEvents);
