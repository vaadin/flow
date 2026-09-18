var os = require('os');
var fs = require('fs');
var path = require('path');
//Windows 64bits "Program Files"
//Windows 32bits "Program Files x86"
var programFiles = os.arch() === "x64" ? process.env.ProgramFiles : process.env["ProgramFiles(x86)"];
var cwd = path.normalize(programFiles);
var appData = appData || process.env.APPDATA;

module.exports = {
  chrome: {
    defaultLocation: altPaths('Google', 'Chrome', 'Application', 'chrome.exe') ,
    pathQuery: 'dir /s /b chrome.exe',
    cwd: cwd,
    opensTab: true
  },
  canary: {
    defaultLocation: altPaths('Google', 'Chrome SxS', 'Application', 'chrome.exe')
  },
  firefox: {
    defaultLocation: path.join(programFiles, 'Mozilla Firefox', 'firefox.exe'),
    pathQuery: 'dir /s /b firefox.exe',
    cwd: cwd,
    opensTab: true
  },
  aurora: {
    defaultLocation: path.join(programFiles, 'firefox.exe'),
    opensTab: true
  },
  opera: {
    defaultLocation: path.join(programFiles, 'Opera', 'launcher.exe'),
    pathQuery: 'dir /s /b opera.exe',
    cwd: cwd,
    imageName: 'opera.exe',
    opensTab: true
  },
  ie: {
    defaultLocation: path.join(programFiles, 'Internet Explorer', 'iexplore.exe'),
    pathQuery: 'dir /s /b iexplore.exe',
    cwd: cwd
  },
  edge: {
    defaultLocation: path.join(getEdgeDirectory() || programFiles, 'MicrosoftEdge.exe'),
    getCommand: function(browser, url) {
      return 'start /wait microsoft-edge:' + url;
    },
    opensTab: true,
    cwd: cwd
  },
  electron: {
    defaultLocation: path.join(process.cwd(), 'node_modules', '.bin', 'electron.cmd'),
    pathQuery: 'dir /s /b electron.exe',
    args: [path.join(__dirname, '..', '..', '..', 'resources', 'electron.js')],
    multi: true,
    cwd: cwd
  },
  phantom: {
    defaultLocation: [
      path.join(process.cwd(), 'node_modules', '.bin', 'phantomjs'),
      path.join(programFiles, 'phantomjs', 'phantomjs.exe')
    ],
    pathQuery: 'dir /s /b phantomjs.exe',
    args: [path.join(__dirname, '..', '..', '..', 'resources', 'phantom.js')],
    multi: true,
    cwd: cwd
  },
  nodeWebkit: {
    pathQuery: 'dir /s /b nw.exe',
    multi: true,
    cwd: cwd,
    imageName: 'nw.exe',
    getCommand: function(browser, url) {
      var app = process.cwd();
      return '"' + browser.command + '" ' + app + ' --url="' + url + '"';
    }
  }
};

function altPaths() {
  var args = Array.prototype.slice.call(arguments);
  var paths = [
    path.join.apply(path, [programFiles].concat(args)),
    path.join.apply(path, [appData].concat(args))
  ];
  //chrome installed under "Program Files x86"
  //even for x64 architecture at least for Win10
  //the installer could be x32?
  if (os.arch() === "x64") {
    paths.push(path.join.apply(path, [process.env["ProgramFiles(x86)"]].concat(args)));
  }
  return paths;
}

function getEdgeDirectory() {
  var windowsDirectory = process.env.winDir;
  var systemApps = path.join(windowsDirectory, 'SystemApps');

  try {
    var edgeFolders = fs.readdirSync(systemApps).filter(function(folder) {
      // Windows 10 has Microsoft.MicrosoftEdge_*** and Microsoft.MicrosoftEdgeDevToolsClient_***
      // see: https://docs.microsoft.com/en-us/windows/application-management/apps-in-windows-10
      if (folder.indexOf("Microsoft.MicrosoftEdge") === 0) {
        var edgePath = path.join(systemApps, folder, "MicrosoftEdge.exe");
        if (fs.existsSync(edgePath)) {
          return folder;
        }
      }
    }).map(function(folder) {
      return path.join(systemApps, folder);
    });

    return edgeFolders[0] || systemApps;
  } catch(ex) {
    return systemApps;
  }
}
