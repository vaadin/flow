var path = require('path');
var spawn = require("child_process").spawn;
var exec = require("child_process").exec;
var execFile = require("child_process").execFile;
var EventEmitter = require('events').EventEmitter;
var debug = require('debug')('launchpad:local:instance');
var rimraf = require('rimraf');

var safe = function (str) {
   // Avoid quotes makes impossible escape the `multi command` scenario
   return str.replace(/['"]+/g, '');
}

var getProcessId = function (name, callback) {

  name = safe(name);

  var commands = {
    darwin: "ps -clx | grep '" + name + "$' | awk '{print $2}' | head -1",
    linux: "ps -ax | grep '" + name + "$' | awk '{print $2}' | head -1",
    freebsd: "ps -clx | grep '" + name + "$' | awk '{print $2}' | head -1",
    sunos: "ps -ax | grep '" + name + "$' | awk '{print $2}' | head -1" // Don't actually know about this
  };

  // Get the process with the given name if it is running
  exec(commands[process.platform], function (err, stdout) {
    var pid = stdout.trim();

    debug('Got process ID', name, pid);
    if (!pid) {
      return callback(new Error('There does not seem to be a ' + name + ' process running'));
    }
    callback(null, pid);
  });
};

var Instance = function (cmd, args, settings, options) {
  this.options = options || {};
  var self = this;

  var childProcess = args === null ? exec(cmd, settings || {}) : spawn(cmd, args, settings || {});

  debug( (args === null ? 'exec' : 'spawn') + ' child process with process id',
    childProcess.pid, cmd, args);

  childProcess.on('exit', function (code, signal) {
    self.emit('stop', {
      code: code,
      signal: signal
    });
  });

  if (settings && settings.timeout) {
    var timeout = settings.timeout * 1000;
    setTimeout(function () {
      debug('Intance timed out', self.id, timeout);
      self.stop();
    }, timeout);
  }

  this.stdout = childProcess.stdout;
  this.stderr = childProcess.stderr;
  this.id = childProcess.pid;
  this.process = childProcess;
  this.cmd = cmd;
  this.args = args;
};

Instance.prototype = new EventEmitter();

Instance.prototype.getPid = function (callback) {
  if (this.options.process) {
    getProcessId(this.options.process, callback);
  } else {
    callback(null, this.process.pid);
  }
};

Instance.prototype.stop = function (callback) {
  var self = this;
  var command;

  if(self.running) {
    debug('Instance was open already, not stopping', this.id);
    return callback(null, {});
  }

  if (callback) {
    this.once('stop', function (data) {
      debug('Instance stopped');
      callback(null, data);
    });
  }
  if (this.options.clean) {
    try {
      debug('Killing process', this.id);
      process.kill(-this.id);
    } catch (error) {}
  } else {
    if (this.options.command.indexOf('open') === 0) {
      command = 'osascript -e \'tell application "' + safe(self.options.process) + '" to quit\'';
      debug('Executing shutdown AppleScript', command);
      command = command.split(' ');
      execFile(command[0], command.slice(1));
    } else if (process.platform === 'win32') {
      //Adding `"` wasn't safe/functional on Win systems
      command = 'taskkill /IM ' + (this.options.imageName || path.basename(this.cmd)); 
      debug('Executing shutdown taskkil', command);
      command = command.split(' ');
      execFile(command[0], command.slice(1)).once('exit', function(data) {
        self.emit('stop', data);
      });
    } else {
      debug('Killing process', this.id);
      this.process.kill();
    }
  }

  if (this.options.tmpdir) {
    debug('Removing tmpdir', this.options.tmpdir);
    rimraf(this.options.tmpdir, function() {});
  }
};

exports.Instance = Instance;

/**
 * Starts a new process.
 *
 * @param cmd The process command line
 * @param args The process arugments
 * @param settings The process environment settings
 * @param callback function(error, instance) Callback after the instance is started
 * @see http://nodejs.org/api/child_process.html#child_process_child_process
 */
exports.start = function (cmd, args, settings, options, callback) {
  var getInstance = function () {
    debug('Starting instance', cmd, args);
    return new Instance(cmd, args, settings, options);
  };

  // Check if the process is already running but only if it's a browser we
  // can only launch once
  if (options.process && !options.multi) {
    getProcessId(options.process, function (err, pid) {
      if (!err && !options.opensTab) {
        return callback(new Error(options.process + ' seems already running with process id ' + pid));
      }

      var instance = getInstance();

      // Add a `running` flag if the browser is already running but can open a new tab
      if(!err && options.opensTab && !options.clean) {
        debug('Marking instance as already running (but is able to open a tab)');
        instance.running = true;
      }

      return callback(null, instance);
    });
  } else {
    callback(null, getInstance());
  }
};
