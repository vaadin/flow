window.Vaadin.Flow.whenReady = function (callback) {
  function check() {
    if (document.readyState !== 'complete') {
      setTimeout(check, 50);
      return;
    }
    if (window.Vaadin.Flow.devServerIsNotLoaded) {
      setTimeout(check, 50);
      return;
    }
    var clients = window.Vaadin.Flow.clients;
    if (clients) {
      for (var key in clients) {
        if (clients.hasOwnProperty(key) && typeof clients[key].isActive === 'function' && clients[key].isActive()) {
          setTimeout(check, 50);
          return;
        }
      }
    }
    callback();
  }
  check();
};
