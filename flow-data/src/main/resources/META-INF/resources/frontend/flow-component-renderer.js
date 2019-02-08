import '@polymer/polymer/polymer-legacy.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { Debouncer } from '@polymer/polymer/lib/utils/debounce.js';
import { idlePeriod } from '@polymer/polymer/lib/utils/async.js';
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
class FlowComponentRenderer extends PolymerElement {
  static get template() {
    return html`
    <slot></slot> 
`;
  }

  static get is() { return 'flow-component-renderer'; }
  static get properties() {
    return {
      nodeid: Number,
      appid: String
    };
  }
  static get observers() {
    return [
      '_attachRenderedComponentIfAble(appid, nodeid)'
    ]
  }

  ready(){
      super.ready();
      this.addEventListener("click", function(event){
          if (this.firstChild && 
                  typeof this.firstChild.click === "function" && 
                      event.target === this ){
              this.firstChild.click();
          }
      });
  }

  _asyncAttachRenderedComponentIfAble() {
    this._debouncer = Debouncer.debounce(
      this._debouncer,
      idlePeriod,
      () => this._attachRenderedComponentIfAble()
    );
  }

  _attachRenderedComponentIfAble() {
    if (!this.nodeid || !this.appid) {
      return;
    }
    const renderedComponent = this._getRenderedComponent();
    if (this.firstChild) {
      if (!renderedComponent) {
        this._clear();
        this._asyncAttachRenderedComponentIfAble();
      } else if (this.firstChild !== renderedComponent){
        this.replaceChild(renderedComponent, this.firstChild);
        this.onComponentRendered();
      } else {
        this.onComponentRendered();
      }
    } else {
      if (renderedComponent) {
        this.appendChild(renderedComponent);
        this.onComponentRendered();
      } else {
        this._asyncAttachRenderedComponentIfAble();
      }
    }
  }

  _getRenderedComponent() {
    try {
      return window.Vaadin.Flow.clients[this.appid].getByNodeId(this.nodeid);
    } catch (error) {
      console.error("Could not get node %s from app %s", this.nodeid, this.appid);
      console.error(error);
    }
    return null;
  }

  _clear() {
    while (this.firstChild) {
      this.removeChild(this.firstChild);
    }
  }

  onComponentRendered(){
    // subclasses can override this method to execute custom logic on resize
  }
}
window.customElements.define(FlowComponentRenderer.is, FlowComponentRenderer);
