import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { LumoEditor } from '../lumo-editor.js'
export class EditorModule extends PolymerElement {
  _notifyPropertyChange(entry) {
    this.dispatchEvent(new CustomEvent(LumoEditor.PROPERTY_CHANGED, {
      detail: entry,
      bubbles: true,
      composed: true,
    }));
  }
}
