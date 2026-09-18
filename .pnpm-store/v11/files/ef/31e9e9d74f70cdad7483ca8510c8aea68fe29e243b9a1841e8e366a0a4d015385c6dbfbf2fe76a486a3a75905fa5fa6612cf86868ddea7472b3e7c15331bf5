// An Electron script that simply opens the web page passed as the command line parameter
'use strict';
const {app, BrowserWindow, ipcMain} = require('electron');

app
  .on('ready', function () {
    new BrowserWindow({
      height: 1280,
      width: 800,
      show: false,
      webPreferences: {
        nodeIntegration: false,
        preload: `${__dirname}/electron-preload.js`
      }
    })
    .loadURL(process.argv[2]);
  })
  .on('window-all-closed', function (event) {
    app.quit();
  });

// Events emitted from preload script
ipcMain
  .on('stdout', function (event, data) {
    console.log(`CONSOLE: ${data}`);
  })
  .on('stderr', function (event, stack) {
    console.error(`ERROR: ${stack}`);
  });
