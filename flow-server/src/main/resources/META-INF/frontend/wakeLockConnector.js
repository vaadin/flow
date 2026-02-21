(function () {
  window.Vaadin = window.Vaadin || {};
  window.Vaadin.Flow = window.Vaadin.Flow || {};

  let sentinel = null;

  async function reacquire(uiElement) {
    if (sentinel !== null && document.visibilityState === 'visible') {
      try {
        sentinel = await navigator.wakeLock.request('screen');
        sentinel.addEventListener('release', () => {
          sentinel = null;
          uiElement.dispatchEvent(new Event('vaadin-wakelock-release'));
        });
      } catch (e) {
        sentinel = null;
        uiElement.dispatchEvent(new Event('vaadin-wakelock-release'));
      }
    }
  }

  window.Vaadin.Flow.wakeLock = {
    request: async function (uiElement) {
      if (sentinel !== null) {
        return;
      }
      sentinel = await navigator.wakeLock.request('screen');
      sentinel.addEventListener('release', () => {
        sentinel = null;
        uiElement.dispatchEvent(new Event('vaadin-wakelock-release'));
      });
      document.addEventListener('visibilitychange', () => reacquire(uiElement));
    },
    release: async function () {
      if (sentinel !== null) {
        await sentinel.release();
        sentinel = null;
      }
    },
    isActive: function () {
      return sentinel !== null;
    }
  };
})();
