// An Electron script that sets up communication with the main Electron process
(function(){
  'use strict';
  const {ipcRenderer} = require('electron');
  const {format} = require('util');

  const console_log = console.log;

  console.log = function () {
    const data = `${format.apply(null, arguments)}\n`;
    ipcRenderer.send('stdout', data);
    console_log.apply(console, arguments);
  };
 
  const console_error = console.error;

  console.error = function () {
    const data = `${format.apply(null, arguments)}\n`;
    ipcRenderer.send('stderr', data);
    console_error.apply(console, arguments);
  };

  window.onerror = function (messageOrEvent, source, lineno, colno, error) {
    console.error(error.stack);
  };
})();
