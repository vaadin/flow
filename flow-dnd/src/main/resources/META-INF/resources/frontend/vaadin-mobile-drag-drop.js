import MobileDragDrop from 'mobile-drag-drop/index.js';

(function () {
  window.Vaadin = window.Vaadin || {};

  // Inspired by https://github.com/timruffles/mobile-drag-drop/issues/115#issuecomment-375469457
  function tryFindDraggableTarget(event) {
    if (window.Vaadin.__mobilePolyfillTouchStartPath) {
      const cp = window.Vaadin.__mobilePolyfillTouchStartPath;
      for (const o of cp) {
        let el = o;
        do {
          if (el.getAttribute && el.getAttribute('draggable') === 'true') {
            return el;
          }
        } while ((el = el.parentNode) && el !== document.body);
      }
    }
  }

  function elementFromPoint(x, y) {
    for (const o of this._path) {
      if (o.elementFromPoint) {
        let el = o.elementFromPoint(x, y);
        if (el) {
          while (el.shadowRoot) {
            const fromPoint = el.shadowRoot.elementFromPoint(x, y);
            if (el === fromPoint) {
              return el;
            } else {
              el = fromPoint;
            }
          }
          return el;
        }
      }
    }
  }

  function dragStartConditionOverride(event) {
    this._path = event.composedPath();
    return true;
  }

  const config = {
    tryFindDraggableTarget,
    dragStartConditionOverride,
    holdToDrag: 300,
    forceApply: window.Vaadin.__forceApplyMobileDragDrop
  };
  if (!window.ShadyDOM) {
    config.elementFromPoint = elementFromPoint;
  }

  if (MobileDragDrop.polyfill(config)) {
    document.addEventListener('touchstart', (e) => {
      window.Vaadin.__mobilePolyfillTouchStartPath = e.composedPath();
    });
    document.addEventListener('touchmove', (e) => {
      delete window.Vaadin.__mobilePolyfillTouchStartPath;
    });
  }
})();
