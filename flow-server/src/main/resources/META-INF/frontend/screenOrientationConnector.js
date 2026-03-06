(function () {
  window.Vaadin = window.Vaadin || {};
  window.Vaadin.Flow = window.Vaadin.Flow || {};

  window.Vaadin.Flow.screenOrientation = {
    init: function (uiElement) {
      if (screen.orientation) {
        screen.orientation.addEventListener('change', () => {
          const event = new Event('vaadin-orientation-change');
          event.orientationType = screen.orientation.type;
          event.orientationAngle = screen.orientation.angle;
          uiElement.dispatchEvent(event);
        });
      }
    },
    lock: function (type) {
      return screen.orientation.lock(type);
    },
    unlock: function () {
      screen.orientation.unlock();
    },
    getCurrent: function () {
      if (screen.orientation) {
        return {
          type: screen.orientation.type,
          angle: screen.orientation.angle
        };
      }
      return null;
    }
  };
})();
