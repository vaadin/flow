import '@polymer/polymer/polymer-legacy.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { Debouncer } from '@polymer/polymer/lib/utils/debounce.js';
import { animationFrame, idlePeriod } from '@polymer/polymer/lib/utils/async.js';
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
class FlowComponentRenderer extends PolymerElement {
  static get template() {
    return html`
    <style>
      @keyframes flow-component-renderer-appear {
        to {
          opacity: 1;
        }
      }
      :host {
        animation: 1ms flow-component-renderer-appear;
      }
    </style>
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

  connectedCallback() {
    super.connectedCallback();
    this._runChrome72ShadowDomBugWorkaround();
  }

  /* workaround for issue vaadin/flow#5025 */
  _runChrome72ShadowDomBugWorkaround() {
    const userAgent = navigator.userAgent;
    if (userAgent && userAgent.match('Chrome\/')) {
      // example: ... Chrome/72.0.3626.96 ...
      const majorVersionString = userAgent.split('Chrome\/')[1].split('.')[0];
      if (majorVersionString && parseInt(majorVersionString) > 71) {
        const debouncedNotifyResize = this._getDebouncedNotifyResizeFunction();

        // if there is no notifyResize function, then just skip

        if (debouncedNotifyResize) {
          this.style.visibility = 'hidden';
          // need to use animation frame instead of timeout or focusing won't work
          requestAnimationFrame(() => {
            this.style.visibility = '';
            debouncedNotifyResize();
          });
        }
      }
    }
  }

  _getDebouncedNotifyResizeFunction() {
    // 1. dig out the web component that might have the notifyResize function
    let component = this.parentElement;
    while (true) {
      if (!component) {
        return;
      }
      if (component.notifyResize) {
        break;
      } else {
        component = component.parentElement;
      }
    }
    // 2. assign a debounced proxy to notifyResize, if not yet there
    if (!component._debouncedNotifyResize) {
      component._debouncedNotifyResize = function () {
        component.__debouncedNotifyResize =
            Debouncer.debounce(
                component.__debouncedNotifyResize, // initially undefined
                animationFrame,
                component.notifyResize);
      }
    }
    return component._debouncedNotifyResize;
  }

  ready(){
      super.ready();
      this.addEventListener("click", function(event){
          if (this.firstChild &&
                  typeof this.firstChild.click === "function" &&
                      event.target === this ){
              event.stopPropagation();
              this.firstChild.click();
          }
      });
      this.addEventListener('animationend', this._onAnimationEnd);
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
        this._defineFocusTarget();
        this.onComponentRendered();

      } else {
        this._defineFocusTarget();
        this.onComponentRendered();
      }
    } else {
      if (renderedComponent) {
        this.appendChild(renderedComponent);
        this._defineFocusTarget();
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

  /* Setting the `focus-target` attribute to the first focusable descendant
  starting from the firstChild necessary for the focus to be delegated
  within the flow-component-renderer when used inside a vaadin-grid cell  */
  _defineFocusTarget(){
    var focusable = this._getFirstFocusableDescendant(this.firstChild);
    if(focusable !== null) {
      focusable.setAttribute('focus-target', 'true');
    }
  }

  _getFirstFocusableDescendant(node){
    if(this._isFocusable(node)) {
      return node;
    }
    if ( !node.children ){
        return null;
    }
    for (var i = 0; i < node.children.length; i++) {
      var focusable = this._getFirstFocusableDescendant(node.children[i]);
      if(focusable !== null) {
        return focusable;
      }
    }
    return null;
  }

  _isFocusable(node){
    if (node.hasAttribute && typeof node.hasAttribute === 'function' &&
            (node.hasAttribute("disabled") || node.hasAttribute("hidden"))) {
      return false;
    }

    return node.tabIndex === 0;
  }

  _onAnimationEnd(e) {
    // ShadyCSS applies scoping suffixes to animation names
    // To ensure that child is attached once element is unhidden
    // for when it was filtered out from, eg, ComboBox
    // https://github.com/vaadin/vaadin-flow-components/issues/437
    if (e.animationName.indexOf('flow-component-renderer-appear') === 0) {
      this._attachRenderedComponentIfAble();
    }
  }
}
window.customElements.define(FlowComponentRenderer.is, FlowComponentRenderer);
