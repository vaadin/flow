window.allLogMessages = [];

const console = (function (oldConsole) {
  return {
    log: function (message) {
      oldConsole.log(message);
      window.allLogMessages.push(message);
    },
    info: function (message) {
      oldConsole.info(message);
      window.allLogMessages.push(message);
    },
    debug: function (message) {
      oldConsole.debug(message);
      window.allLogMessages.push(message);
    },
    trace: function (message) {
      oldConsole.trace(message);
      window.allLogMessages.push(message);
    },
    warn: function (message) {
      oldConsole.warn(message);
      window.allLogMessages.push(message);
    },
    error: function (message) {
      oldConsole.error(message);
      window.allLogMessages.push(message);
    }
  };
})(window.console);

window.console = console;

window.addEventListener('error', function (e) {
  window.console.trace(e.error.message);
  return false;
});
