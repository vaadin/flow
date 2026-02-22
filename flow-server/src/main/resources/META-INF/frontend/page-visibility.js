window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.pageVisibility = window.Vaadin.Flow.pageVisibility || {};

window.Vaadin.Flow.pageVisibility.init = function (el) {
  document.addEventListener('visibilitychange', function () {
    if (document.hidden) {
      el.dispatchEvent(new CustomEvent('vaadin-page-visibility-change', { detail: 'HIDDEN' }));
    } else {
      el.dispatchEvent(new CustomEvent('vaadin-page-visibility-change', { detail: 'VISIBLE' }));
    }
    if (window.Vaadin.Flow.pageVisibility._blurTimer) {
      clearTimeout(window.Vaadin.Flow.pageVisibility._blurTimer);
      delete window.Vaadin.Flow.pageVisibility._blurTimer;
    }
  });
  window.addEventListener('blur', function () {
    var timeout = 10;
    // Firefox defers visibilitychange events when the page is blurred,
    // so use a longer timeout to let visibilitychange fire first
    var isFirefox = navigator.userAgent.indexOf('Firefox') > -1;
    if (isFirefox) {
      timeout = 500;
    }
    window.Vaadin.Flow.pageVisibility._blurTimer = setTimeout(function () {
      if (!document.hidden) {
        el.dispatchEvent(new CustomEvent('vaadin-page-visibility-change', { detail: 'VISIBLE_NOT_FOCUSED' }));
      }
      delete window.Vaadin.Flow.pageVisibility._blurTimer;
    }, timeout);
  });
  window.addEventListener('focus', function () {
    if (!document.hidden) {
      el.dispatchEvent(new CustomEvent('vaadin-page-visibility-change', { detail: 'VISIBLE' }));
    }
  });
};
