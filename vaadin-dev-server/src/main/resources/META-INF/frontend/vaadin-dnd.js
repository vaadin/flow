/**
 @license
 Copyright (c) 2017 Vaadin Ltd.
 This program is available under Commercial Vaadin Add-On License version 3
 available at https://vaadin.com/license/cval-3
 */
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

import { afterNextRender } from '@polymer/polymer/lib/utils/render-status.js';
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { getComponents, isComponent } from './shim.ts';
import './dnd-palette.ts';


const isAddedComponent = (element) => {
  return element.added;
};
const isValidTarget = (element) => {
  return isComponent(element) || isAddedComponent(element);
};
const getComponentOrAdded = (element) => {
  if (isComponent(element)) {
    return element;
  }
  if (element.added) {
    return element;
  }
  if (element.parentElement) {
    return getComponentOrAdded(element.parentElement);
  }
  return undefined;
  // const components = getComponents(element);
  // console.log('components', components);
  // return components.pop().element;
};

const emptyLayoutsStyles = new CSSStyleSheet();
emptyLayoutsStyles.replaceSync(`
vaadin-horizontal-layout:empty, 
vaadin-vertical-layout:empty {
  min-width: 30px;min-height: 30px;
}
`);

const hackEmptyLayouts = (on) => {
  if (on) {
    document.adoptedStyleSheets.push(emptyLayoutsStyles);
  } else {
    document.adoptedStyleSheets = [...document.adoptedStyleSheets];
  }
};
/**
 * Vaadin.Dnd provides a way to drag and drop elements for your Polymer element.
 *
 * @memberof Vaadin
 * @demo demo/index.html
 */
class VaadinDnd extends PolymerElement {
  static get template() {
    return html`
      <style>
        :host {
          display: inline-block;
        }

        #parent,
        #indicator,
        #source {
          z-index: 99999;
          display: none;
          position: fixed;
          pointer-events: none;
        }

        #parent {
          outline: 1px dashed rgba(0, 0, 0, 0.5);
        }

        #parent i {
          background-color: rgba(0, 0, 0, 0.5);
          color: #fff;
          font-size: 12px;
          padding: 2px;
          position: relative;
          top: -20px;
          white-space: nowrap;
        }

        #indicator {
          min-width: 1px;
          min-height: 1px;
          background-color: rgba(0, 191, 255, 1);
          outline: 1px solid #000;
          box-shadow: 0 0 5px #000;
        }

        #source {
          background-color: rgba(255, 0, 0, 0.3);
        }

        #source i {
          background-color: rgba(255, 0, 0, 0.3);
          color: #fff;
          font-size: 8px;
          padding: 2px;
          position: relative;
          top: -20px;
        }

        .placeholder {
          background-image: var(--placeholder-url);
          background-repeat: no-repeat;
          background-position: center;
        }

        #content {
          width: 100%;
          height: 100%;
        }

        ::slotted(*) {
          cursor: default;
        }
      </style>
      <div id="parent"><i></i></div>
      <div id="indicator"></div>
      <div id="source"><i></i></div>
      <div id="content">
        <slot id="slot"></slot>
      </div>
    `;
  }

  static get properties() {
    return {
      /**
       * Broadcaster function to broadcast information when drop element
       * The function should be like broadcast(topic, data);
       */
      broadcaster: Object,

      /**
       * Attribute name of the id should be broadcasted when dropping an element
       */
      internalIdName: String,

      /**
       * Query selector for the nested element which is allowed to edit
       */
      editableElement: String,

      /**
       * Enable/disable the drag and drop function. Due to this issue
       * https://github.com/Polymer/polymer/issues/1812,
       * Type should be object to be able to set to false initially
       */
      enableDnd: {
        type: Object,
        value: JSON.parse('true')
      }
    };
  }

  static get is() {
    return 'vaadin-dev-tools-dnd';
  }

  static get importMeta() {
    return import.meta;
  }

  connectedCallback() {
    super.connectedCallback();

    this.dragOverHandler = this._handleDragOver.bind(this);
    this.dropHandler = this._handleDrop.bind(this);
    this.dragStartHandler = this._handleDragStart.bind(this);
    this.dragEndHandler = this._handleDragEnd.bind(this);
    this.dragLeaveHandler = this._handleDragLeave.bind(this);
    this.style.setProperty('--placeholder-url', 'url(' + this.importPath + 'images/placeholder.svg)');
    this._scheduleEnableDnd();
  }

  _scheduleEnableDnd() {
    // bypass gulp lint error which doens't allow to use this inside afterNextRender
    const enableDndFunction = () => {
      if (this.enableDnd) {
        this._setEnableDnd();
      } else {
        if (this.onDndReady) {
          this.onDndReady();
        }
      }
      this.updatePlaceholderText();
    };
    if (this._isSlottedIframe()) {
      this._getSlottedIframe().addEventListener('load', enableDndFunction);
    } else {
      // Need to enable Dnd after next render because when vaadin-dnd ready, other nested element is not ready yet.
      afterNextRender(this, enableDndFunction);
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this.enableDnd) {
      this._setDisableDnd();
    }
  }

  /**
   * Public method to show/hide the placeholder if the editableElement is empty/not empty
   */
  updatePlaceholderText() {
    if (this.editableElement) {
      const hostElement = this._isSlottedIframe() ? this._getIframeDocument() : this;
      const myEditableElement = hostElement.getElementsByTagName(this.editableElement)[0];
      if (!myEditableElement) {
        return;
      }
      if (!this.enableDnd) {
        this._getPlaceholderElement().setAttribute('class', '');
        return;
      }
      if (this._isEmpty(myEditableElement) && this._isEmpty(myEditableElement.shadowRoot)) {
        this._getPlaceholderElement().setAttribute('class', 'placeholder');
      } else if (!this._isEmpty(myEditableElement) || !this._isEmpty(myEditableElement.shadowRoot)) {
        this._getPlaceholderElement().setAttribute('class', '');
      }
    }
  }

  _getPlaceholderElement() {
    return this._isSlottedIframe() ? this._getIframeDocument().body : this.$.content;
  }

  _getSlottedIframe() {
    return this.querySelector('iframe');
  }

  _isSlottedIframe() {
    return this._getSlottedIframe() !== null;
  }

  _getIframeDocument() {
    return this._getSlottedIframe().contentDocument;
  }

  _getDocument() {
    return this._isSlottedIframe() ? this._getIframeDocument() : document;
  }

  _handleDragStart(event) {
    this._currentDrag = this._getTarget(event);
    this._outline(this.$.source, this._currentDrag);
    this._isLocalDrag = true;
    event.dataTransfer.setData('text/html', this._currentDrag.outerHTML);
    event.dataTransfer.setDragImage(this._currentDrag, 0, 0);

    hackEmptyLayouts(true);
  }

  _handleDragEnd(event) {
    this._setDragHoverTarget(undefined);
    this._hide(this.$.source);
    this._isLocalDrag = false;
    this._currentDrag = undefined;
    hackEmptyLayouts(false);
  }

  _handleDragLeave(event) {
    this._setDragHoverTarget(undefined);
  }

  _handleDragOver(event) {
    console.log('dragOver');
    if (this._dataTransferHasHtmlText(event.dataTransfer)) {
      event.preventDefault();
      this._setDragHoverTarget(event);
    }
  }

  _handleDrop(event) {
    event.preventDefault();
    this._setDragHoverTarget(undefined);
    const dragEventData = this._getElementFromDragEvent(event);
    const element = dragEventData.element;
    if (!element) {
      return;
    }
    const target = this._getTarget(event);

    if (element.contains(target)) {
      return;
    }

    // Make sure newly added elements are draggable
    this._setElementDraggable(element, true);

    const ip = this._getNearestInsertPoint(this._getEventLocation(event), target);
    if (ip) {
      this.dispatchEvent(
        new CustomEvent('vaadin-dnd-drop', { detail: { element, parent: ip.parent, insertBefore: ip.insertBefore } })
      );
      const template = element.querySelector('template');
      if (template) {
        const e = template.content.firstElementChild.cloneNode(true);
        e.added = true;
        this._insertElement(e, ip.parent, ip.insertBefore);
      } else {
        this._insertElement(element, ip.parent, ip.insertBefore);
      }
      if (element.tagName === undefined) {
        this._broadcastOutsideDnd(dragEventData.snippet, ip.parent, ip.insertBefore, dragEventData.elementName);
      } else {
        this._broadcastInsideDnd(element, ip.parent, ip.insertBefore);
      }
    }
    this.updatePlaceholderText();
  }

  _setDragHoverTarget(event) {
    if (event === undefined) {
      this._hide(this.$.indicator, this.$.parent, this.$.source);
      return;
    }

    const target = this._getTarget(event);
    console.log('target', target);
    if (target === undefined) {
      this._hide(this.$.indicator, this.$.parent, this.$.source);
      return;
    }

    if (this._currentDrag && this._currentDrag.contains && this._currentDrag.contains(target)) {
      // hovering drag source
      this._hide(this.$.indicator, this.$.parent);
      return;
    }

    const ip = this._getNearestInsertPoint(this._getEventLocation(event), target);
    if (ip) {
      this._outline(this.$.parent, ip.parent);
      this._show(this.$.indicator, ip.rect.x, ip.rect.y, ip.rect.width, ip.rect.height);
    } else {
      this._hide(this.$.parent, this.$.indicator);
    }
  }

  /**
   * Navigate to the bottom of event target to find out element which is actually targeted for the DnD
   */
  _getTarget(event) {
    const elementsFromPoint = this._getElementsFromPoint(this._getEventLocation(event));
    if (elementsFromPoint === undefined) {
      return undefined;
    } else if (this._isEmpty(this._rootElement.shadowRoot)) {
      return this._rootElement.shadowRoot;
    }

    if (!this.editableElement) {
      const cursorElement = elementsFromPoint[0];
      console.log('cursorElement', cursorElement);
      if (cursorElement.hasAttribute('palette')) {
        return cursorElement;
      }
      // const e = parentWithProperty(customElement, "added");
      // if (e) {
      //   return e;
      // }
      const c = getComponentOrAdded(cursorElement);
      if (c === document.body) {
        return undefined;
      }
      console.log('component', c);
      return c;
    }
    let isInsideANestedElement = false;
    let nestedElement = null;
    for (let i = 0; i < elementsFromPoint.length; i++) {
      if (elementsFromPoint[i].nodeType === Node.ELEMENT_NODE && elementsFromPoint[i].getAttribute('vaadin-dnd-item')) {
        return elementsFromPoint[i];
      }
      // If we reach the editable nested element and nestedElement is null,
      // it means we should return the deepest element in the composedPath of editable element
      if (elementsFromPoint[i].tagName === this.editableElement.toUpperCase()) {
        return nestedElement ? nestedElement : elementsFromPoint[0];
      }
      if (elementsFromPoint[i] === this.$.slot) {
        return elementsFromPoint[0];
      }
      if (isInsideANestedElement) {
        // the target comes from another nested element, not the editableElement, then save the current checking element
        nestedElement = elementsFromPoint[i];
        isInsideANestedElement = false;
      }
      // If tagname == undefined, it means we are in some nested element and about to get out of it
      if (elementsFromPoint[i].tagName === undefined) {
        isInsideANestedElement = true;
      }
    }
    return elementsFromPoint[0];
  }

  _getElementsFromPoint(p) {
    const hostElement = this._isSlottedIframe() ? this._getIframeDocument() : this.shadowRoot;
    const slottedElement = hostElement.elementFromPoint(p.x, p.y);
    if (!this.editableElement) {
      // if there is no editableElement, return right away the element under the mouse
      return [slottedElement];
    }
    // The slotted element (that the dnd operations are targeted to) is
    // defined in the document (light DOM) whereas all the draggable elements are
    // located under the shadow root of the slotted element.
    if (slottedElement.shadowRoot) {
      return slottedElement.shadowRoot.elementsFromPoint(p.x, p.y);
    } else {
      return [this._rootElement.shadowRoot];
    }
  }

  _insertElement(element, parent, insertBefore) {
    if (element.tagName === undefined && this.broadcaster) {
      return;
    }
    parent.insertBefore(element, insertBefore);
  }

  /**
   * Broadcast the internal DnD event
   */
  _broadcastInsideDnd(source, target, before) {
    if (!this.broadcaster) {
      return;
    }
    const sourceId = source.getAttribute(this.internalIdName);
    // target.getAttribute is false when target is the shadowRoot
    const targetId = target.getAttribute ? target.getAttribute(this.internalIdName) : null;
    const beforeId = before ? before.getAttribute(this.internalIdName) : null;
    this._broadcastDropEvent(sourceId, targetId, beforeId, '');
  }

  /**
   * Broadcast event when dropping a element from outside vaadin-dnd
   */
  _broadcastOutsideDnd(snippet, target, before, elementName) {
    if (!this.broadcaster) {
      return;
    }
    const targetId = target.getAttribute ? target.getAttribute(this.internalIdName) : null;
    const beforeId = before ? before.getAttribute(this.internalIdName) : null;
    this._broadcastDropEvent(snippet, targetId, beforeId, elementName);
  }

  _broadcastDropEvent(sourceId, targetId, beforeId, elementName) {
    const dropEvent = { sourceId: sourceId, targetId: targetId, beforeId: beforeId, elementName: elementName };
    this.broadcaster('drop', JSON.stringify(dropEvent));
  }

  /**
   * Use the template element to transform a HTML snippet to actual element.
   * Returns a Document Fragment with template's contents.
   */
  _snippetToElement(htmlSnippet) {
    const template = document.createElement('template');
    template.innerHTML = htmlSnippet;
    return template.content;
  }

  /**
   * Check if data transfer object has HTML text data
   */
  _dataTransferHasHtmlText(dataTransferObject) {
    for (const item of dataTransferObject.items) {
      console.log('item', item);
      if (item.kind === 'string' && item.type === 'text/html') {
        return true;
      }
    }
    return false;
  }

  /**
   * Get (and show) insert point closest to given location
   * return {
   *   parent: parentElement, // needed because insertBefore can be null
   *   insertBefore: insertBeforeElement,
   *   rect: boundingRectangle
   * }
   */
  _getNearestInsertPoint(location, target) {
    target = this._reTargetToEditableElement(target);
    const elements = getComponents(target).map((c) => c.element);
    const visible = [];
    const parents = [];
    const rects = [];
    let closestDist = Number.MAX_VALUE;
    let closestIndex = -1;
    const indicatorSize = 3;
    // 1. Collect insert points around all visible nodes
    for (const element of elements) {
      if (!element || (element.nodeType !== 1 && element.nodeType !== 11)) {
        // can't put stuff inside non-element nodes
        continue;
      }
      const elementRect = this._getRect(element);
      // Collect visible nodes
      const v = []; // visible nodes
      const r = []; // bounding rects
      for (const node of element.childNodes) {
        if (!isValidTarget(node)) {
          continue;
        }
        const rect = this._getRect(node);
        if (rect) {
          // elements and non-empty text nodes
          v.push(node);
          r.push(rect);
        }
      }
      // Now figure out if insert point should be vertical or horizontal
      // and push into global collection of insert points
      if (!v.length) {
        // empty element; probe to see if stuff can be added
        const probe = this._getProbe();
        element.appendChild(probe);
        const prect = this._getRect(probe);
        element.removeChild(probe);
        if (prect) {
          // probe is visible
          // use element dimensions and probe position to create insert point
          const rect = this._getRect(element);
          // Show full-sized indicator when dropping into an empty element
          const r = this._createDOMRect(rect.left, rect.top, rect.width, rect.height);
          visible.push(null);
          parents.push(element);
          rects.push(r);
        }
      } else if (v.length === 1) {
        // only one child - assume horizontal
        const rect = r[0];
        // insert point at right edge
        const right = this._createDOMRect(rect.right, rect.top, 1, rect.height);
        visible.push(null);
        rects.push(right);
        parents.push(element);
        // insert point at left edge
        const left = this._createDOMRect(rect.left, rect.top, 1, rect.height);
        visible.push(v[0]);
        rects.push(left);
        parents.push(element);
      } else {
        // multiple children, use first two to guess vert/horiz
        const vertical = r[0].bottom <= r[1].top;
        // insert right/bottom insert point for last node
        {
          const last = r[r.length - 1];
          let lastDrop;
          if (vertical) {
            lastDrop = this._createDOMRect(last.left, last.bottom, elementRect.width, indicatorSize);
          } else {
            lastDrop = this._createDOMRect(last.right, last.top, indicatorSize, elementRect.height);
          }
          visible.push(null);
          rects.push(lastDrop);
          parents.push(element);
        }

        {
          const first = r[0];
          let firstDrop;
          if (vertical) {
            firstDrop = this._createDOMRect(
              elementRect.left,
              first.top - indicatorSize,
              elementRect.width,
              indicatorSize
            );
          } else {
            firstDrop = this._createDOMRect(
              first.left - indicatorSize,
              elementRect.top,
              indicatorSize,
              elementRect.height
            );
          }
          visible.push(v[0]);
          rects.push(firstDrop);
          parents.push(element);
        }

        // insertpoints for the rest
        for (let i = 0; i < r.length - 1; i++) {
          const thisRect = r[i];
          const nextRect = r[i + 1];
          let newRect;
          if (vertical) {
            const y = thisRect.bottom + (nextRect.top - thisRect.bottom) / 2 - indicatorSize / 2;
            newRect = this._createDOMRect(elementRect.left, thisRect.top, elementRect.width, indicatorSize);
          } else {
            const x = thisRect.right + (nextRect.left - thisRect.right) / 2 - indicatorSize / 2;
            newRect = this._createDOMRect(x, elementRect.top, indicatorSize, elementRect.height);
          }
          visible.push(v[i + 1]);
          rects.push(newRect);
          parents.push(element);
        }
      }
    } // all points collected

    // 2. Find the closest inser point
    for (let i = 0; i < rects.length; i++) {
      const rect = rects[i];
      const dist = this._getSquaredDistance(location, rect.x, rect.y, rect.width, rect.height);
      if (dist < closestDist) {
        closestDist = dist;
        closestIndex = i;
      }
    }

    if (closestIndex < 0) {
      return null;
    } else {
      return {
        parent: this._getParentIfEditableElementEmpty(parents[closestIndex]),
        insertBefore: visible[closestIndex],
        rect: rects[closestIndex]
      };
    }
  }

  _reTargetToEditableElement(target) {
    if (
      this.editableElement &&
      (this._isEmpty(this._rootElement.shadowRoot) ||
        !this._rootElement.shadowRoot.contains(target) ||
        !this._rootElement.shadowRoot === target)
    ) {
      return this._rootElement.shadowRoot;
    } else {
      return target;
    }
  }

  _getParentIfEditableElementEmpty(parent) {
    if (parent === this && this.editableElement) {
      const editableElementShadowRoot = document.getElementsByTagName(this.editableElement)[0].shadowRoot;
      const isEditableElementEmpty = editableElementShadowRoot.children.length === 0;
      return isEditableElementEmpty ? editableElementShadowRoot : parent;
    }
    return parent;
  }

  _createDOMRect(x, y, w, h) {
    return { x: x, y: y, width: w, height: h };
  }

  _getElementFromDragEvent(event) {
    const data = event.dataTransfer.getData('text/html');
    const elementName = event.dataTransfer.getData('text/designerData');
    if (this._isLocalDrag) {
      const element = this._currentDrag;
      this._currentDrag = undefined;
      return { element: element, snippet: '', elementName: elementName };
    } else if (data) {
      const element = this._snippetToElement(data);
      return { element: element, snippet: data, elementName: elementName };
    }
  }

  _getEventLocation(event) {
    return {
      x: event.x,
      y: event.y
    };
  }

  _setEnableDnd() {
    // DnD will work within the given element, or the whole content
    const hostElement = this._isSlottedIframe() ? this._getIframeDocument() : this;
    this._rootElement = hostElement.querySelector(this.editableElement) || this;
    const nodes = this._rootElement.shadowRoot ? this._rootElement.shadowRoot.childNodes : this._rootElement.childNodes;
    this._setElementDraggableRefCount = 0;
    this._setElementsDraggable(nodes, true);
    this._addEventListeners();
  }

  _addEventListeners() {
    const listenerParent = this._isSlottedIframe() ? this._getDocument() : this;

    listenerParent.addEventListener('dragstart', this.dragStartHandler, true);
    listenerParent.addEventListener('dragend', this.dragEndHandler, true);
    // listen to dragend event dispatched by FlowEditor
    document.addEventListener('dragend', this.dragEndHandler, true);
    listenerParent.addEventListener('drop', this.dropHandler, true);
    listenerParent.addEventListener('dragover', this.dragOverHandler, true);
    listenerParent.addEventListener('dragleave', this.dragLeaveHandler, true);
  }

  _removeEventListeners() {
    const listenerParent = this._isSlottedIframe() ? this._getDocument() : this;

    listenerParent.removeEventListener('dragstart', this.dragStartHandler, true);
    listenerParent.removeEventListener('dragend', this.dragEndHandler, true);
    document.removeEventListener('dragend', this.dragEndHandler, true);
    listenerParent.removeEventListener('drop', this.dropHandler, true);
    listenerParent.removeEventListener('dragover', this.dragOverHandler, true);
    listenerParent.removeEventListener('dragleave', this.dragLeaveHandler, true);
  }

  _setDisableDnd() {
    this._setElementDraggableRefCount = 0;
    this._removeEventListeners();
    this._setElementsDraggable(this.$.slot.assignedNodes(), false);
    this._hide(this.$.indicator, this.$.parent, this.$.source);
  }

  _setElementsDraggable(nodes, value) {
    this._setElementDraggableRefCount++;

    Array.from(nodes)
      .filter((node) => node.nodeType === Node.ELEMENT_NODE)
      .forEach((node) => this._setElementDraggable(node, value));

    this._setElementDraggableRefCount--;
    if (this._setElementDraggableRefCount == 0) {
      if (this.onDndReady) {
        this.onDndReady();
      }
    }
  }

  _setElementDraggable(element, value) {
    // check type because element might be a DocumentFragment
    if (element.nodeType === Node.ELEMENT_NODE) {
      element.setAttribute('draggable', value);
      // 'vaadin-dnd-item' is used to distinguish between draggable element in childNodes of a vaadin-dnd
      // and draggable elements in shadowRoot of unmodifiable nested element.
      // This attribute is set for all indeed-draggable elements in vaadin-dnd.
      // Example:
      // <vaadin-dnd>
      //    <unmodifiable-nested-element>
      //      <some-child-nodes></some-child-nodes>
      //    </unmodifiable-nested-element>
      // </vaadin-dnd>
      // In this case, if in shadowRoot of unmodifiable-nested-element has some elements with draggable attribute true,
      // we can prevent them from dnd inside vaadin-dnd using this attribute
      element.setAttribute('vaadin-dnd-item', value);
    }
    if (element === this._rootElement) {
      if (element.shadowRoot) {
        this.setElementsDraggableCount++;
        this._setElementsDraggable(element.shadowRoot.childNodes, value);
      } else {
        this._setElementDraggableRefCount++;
        const boundSetElementDraggable = (element, value) => {
          this._setElementDraggable(element, value);
          this._setElementDraggableRefCount--;
          if (this._setElementDraggableRefCount == 0) {
            if (this.onDndReady) {
              this.onDndReady();
            }
          }
        };
        afterNextRender(this, function () {
          boundSetElementDraggable(element, value);
        });
      }
    }
    if (element.tagName === 'SLOT') {
      this._setElementsDraggable(element.assignedNodes(), value);
    }
    this._setElementsDraggable(element.childNodes, value);
  }

  /**
   * Shows given outline element around given target element,
   * set the innerHTML of the first element child (if any) to indicate
   * target element type.
   */
  _outline(outline, element) {
    if (!element) {
      this._hide(outline);
      return;
    }
    let rect = this._getRect(element);
    if (!rect) {
      if (element.host) {
        // if we can't calculate the rect and element is a shadow root, probably the element is empty, then we will get from its parent
        const hostRect = this._getRect(element.host);
        rect = hostRect ? hostRect : this._getRect(element.host.assignedSlot.parentElement);
      } else {
        this._hide(outline);
        return;
      }
    }
    if (outline.firstElementChild) {
      if (element.host) {
        // shadowRoot
        element = element.host;
      }
      let text = element.tagName || element.nodeName;
      if (element.id) {
        text += '#' + element.id;
      }
      outline.firstChild.innerHTML = text;
    }
    this._show(outline, rect.x ? rect.x : rect.left, rect.y ? rect.y : rect.top, rect.width, rect.height);
  }

  /**
   * Show given outline element at given coordinates.
   */
  _show(outline, x, y, width, height) {
    const thisBoundingRect = this.getBoundingClientRect();
    if (!this._isSlottedIframe()) {
      outline.style.top = y + 'px';
      outline.style.left = x + 'px';
    } else {
      outline.style.top = y + thisBoundingRect.top + 'px';
      outline.style.left = x + thisBoundingRect.left + 'px';
    }
    outline.style.height = height + 'px';
    outline.style.width = width + 'px';
    outline.style.display = 'block';
  }
  /**
   * Hide given elements.
   */
  _hide(...elements) {
    for (const outline of elements) {
      outline.style.display = 'none';
    }
  }

  /**
   * Get singleton "probe" element used to figure out where things will end up.
   */
  _getProbe() {
    if (!this._probe) {
      this._probe = document.createElement('div');
      this._probe.style.width = '3px';
      this._probe.style.height = '3px';
    }
    if (this._currentDrag && this._currentDrag.nodeType === 1) {
      this._probe.style.display = window.getComputedStyle(this._currentDrag, null).display;
    } else {
      this._probe.style.display = 'inline-block';
    }
    return this._probe;
  }

  /**
   * Get bounding rect for element or text node, return null if not possible or bounding rect not visible.
   */
  _getRect(node) {
    if (node === null) {
      return null;
    }
    let rect;
    if (node.getBoundingClientRect) {
      rect = node.getBoundingClientRect();
    } else if (node.host) {
      rect = node.host.getBoundingClientRect();
      // this check is for the case where node is an empty shadowRoot
      if (!rect.width && !rect.height) {
        return this._getRect(this._isSlottedIframe() ? node.host.parentElement : node.host.assignedSlot.parentElement);
      }
    } else {
      // text node
      const range = document.createRange();
      range.selectNode(node);
      rect = range.getBoundingClientRect();
      range.detach();
    }
    return !rect.width && !rect.height ? null : rect;
  }

  /**
   * Get squared distance from given location to given rectangle.
   */
  _getSquaredDistance(location, x, y, width, height) {
    const rectMidX = x + width / 2;
    const rectMidY = y + height / 2;
    const dx = Math.max(Math.abs(location.x - rectMidX) - width / 2, 0);
    const dy = Math.max(Math.abs(location.y - rectMidY) - height / 2, 0);
    return dx * dx + dy * dy;
  }

  /**
   * See if given element is a descendant of the editable element, also looking in shadowRoot.
   */
  _contains(descendant) {
    const parent = this._rootElement;
    return parent.contains(descendant) || (parent.shadowRoot && parent.shadowRoot.contains(descendant));
  }

  _isEmpty(node) {
    if (node.children.length === 0) {
      return true;
    }
    for (const child of node.children) {
      if ((child.nodeType === Node.ELEMENT_NODE && child.tagName !== 'STYLE') || child.nodeType === Node.TEXT_NODE) {
        return false;
      }
    }
    return true;
  }
}

customElements.define(VaadinDnd.is, VaadinDnd);
