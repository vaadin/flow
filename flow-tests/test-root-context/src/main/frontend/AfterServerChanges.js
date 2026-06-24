import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class AfterServerChanges extends PolymerElement {
  static get template() {
    return html`
      <div id="text">[[text]]</div>
      <div id="count">[[count]]</div>
      <div id="delta">[[delta]]</div>
    `;
  }
  
  static get properties(){
    return {
       count: {
         type: Number,
         value: 0
       },
       old: {
         type: String,
         value: ""
       },
       delta: {
         type: Boolean,
         value: false
       }
    }
  }
  
  static get is() {
    return 'after-server-changes'
  }
  
  afterServerUpdate(){
    this.delta = this.old != this.text;
    this.count++;
    this.old = this.text;
  }
    
}
  
customElements.define(AfterServerChanges.is, AfterServerChanges);

