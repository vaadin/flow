window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.fullscreen = {
  /**
   * Requests fullscreen for a specific component by moving it into the
   * wrapper element and hiding the rest of the view. Fullscreens
   * document.documentElement so that theming and overlays work correctly.
   */
  requestComponentFullscreen: function (element, wrapper) {
    this._resetIfActive();
    if (document.fullscreenEnabled !== true) {
      return;
    }
    const placeholder = document.createComment('placeholder');
    const originalParent = element.parentNode;
    element.parentNode.insertBefore(placeholder, element);

    wrapper.appendChild(element);
    wrapper.firstChild.style.display = 'none';
    document.documentElement.requestFullscreen();

    this._reset = () => {
      originalParent.appendChild(element);
      placeholder.remove();
      wrapper.firstChild.style.display = '';
      document.documentElement.removeEventListener('fullscreenchange', this._onChange);
      delete this._onChange;
      delete this._reset;
    };

    this._onChange = () => {
      if (!document.fullscreenElement) {
        this._reset();
      }
    };
    document.documentElement.addEventListener('fullscreenchange', this._onChange);
  },

  /**
   * Requests fullscreen for the entire page
   * (document.documentElement).
   */
  requestPageFullscreen: function () {
    this._resetIfActive();
    document.documentElement.requestFullscreen();
  },

  /**
   * Sets up a listener on the document that re-dispatches
   * fullscreenchange events on the given UI element so the
   * server side can pick them up.
   */
  setupFullscreenChangeListener: function (uiElement) {
    document.addEventListener('fullscreenchange', () => {
      const event = new Event('flow-fullscreenchange');
      event.fullscreen = document.fullscreenElement !== null;
      uiElement.dispatchEvent(event);
    });
  },

  _resetIfActive: function () {
    if (this._reset) {
      this._reset();
    }
  }
};
