window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.componentSizeObserver = {
  /**
   * Creates a shared ResizeObserver on the given UI element.
   * Size changes are dispatched as "vaadin-component-resize"
   * custom events on the UI element.
   */
  init: function (uiElement) {
    uiElement._componentSizeObserver = new ResizeObserver(function (entries) {
      var sizes = {};
      for (var i = 0; i < entries.length; i++) {
        var entry = entries[i];
        if (entry.target.isConnected && entry.contentBoxSize) {
          var id = entry.target._componentSizeId;
          sizes[id] = {
            w: Math.round(entry.contentRect.width),
            h: Math.round(entry.contentRect.height)
          };
        }
      }
      if (Object.keys(sizes).length > 0) {
        var event = new Event('vaadin-component-resize');
        event.sizes = sizes;
        uiElement.dispatchEvent(event);
      }
    });
  },

  /**
   * Starts observing the given element with the given numeric ID.
   */
  observe: function (uiElement, element, id) {
    element._componentSizeId = id;
    uiElement._componentSizeObserver.observe(element);
  }
};
