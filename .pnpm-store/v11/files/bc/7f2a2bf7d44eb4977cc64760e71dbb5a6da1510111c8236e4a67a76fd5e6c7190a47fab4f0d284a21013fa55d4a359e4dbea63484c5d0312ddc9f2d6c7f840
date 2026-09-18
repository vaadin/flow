"use strict";

var
  fs = require("fs"),
  path = require("path"),
  rimraf = require("rimraf"),
  os = require("os"),
  _ = require("lodash"),
  async = require("async"),
  https = require("https"),
  HttpsProxyAgent = require("https-proxy-agent"),
  AdmZip = require("adm-zip"),
  spawn = require("child_process").spawn,
  exec = require("child_process").exec,
  crypto = require("crypto"),
  processOptions = require("./process_options"),
  scDir = path.normalize(__dirname + "/../sc"),
  exists = fs.existsSync || path.existsSync,
  currentTunnel,
  logger = console.log,
  cleanup_registered = false,
  sc_version = process.env.SAUCE_CONNECT_VERSION ||
    require("../package.json").sauceConnectLauncher.scVersion,
  tunnelIdRegExp = /(?:Tunnel ID|Provisioned tunnel):\s*([a-z0-9]+)/i,
  portRegExp = /port\s*([0-9]+)/i,
  tryRun = require("./try_run"),
  defaultConnectRetryTimeout = 2000,
  defaultDownloadRetryTimeout = 1000,
  SAUCE_API_HOST = process.env["SAUCE_API_HOST"] || "saucelabs.com";

function setWorkDir(workDir) {
  scDir = workDir;
}

function killProcesses(callback) {
  callback = callback || function () {};

  if (!currentTunnel) {
    return callback();
  }

  currentTunnel.on("close", function () {
    currentTunnel = null;
    callback();
  });
  currentTunnel.kill("SIGTERM");
}

function clean(callback) {
  async.series([
    killProcesses,
    function (next) {
      rimraf(scDir, next);
    }
  ], callback);
}

function getScFolderName(version) {
  return {
    darwin: "sc-" + version + "-osx",
    win32: "sc-" + version + "-win32"
  }[process.platform] || "sc-" + version + "-linux";
}

function getArchiveName(version) {
  return getScFolderName(version) + ({
    darwin: ".zip",
    win32: ".zip"
  }[process.platform] || ".tar.gz");
}

function getScBin(version) {
  var exe = process.platform === "win32" ? ".exe" : "";
  return path.normalize(scDir + "/" + getScFolderName(version) + "/bin/sc" + exe);
}

// Make sure all processes have been closed
// when the script goes down
function closeOnProcessTermination() {
  if (cleanup_registered) {
    return;
  }
  cleanup_registered = true;
  process.on("exit", function () {
    logger("Shutting down");
    killProcesses();
  });
}

function unpackArchive(archivefile, callback) {
  logger("Unzipping " + archivefile);

  function done(err) {
    if (err) { return callback(new Error("Couldn't unpack archive: " + err.message)); }
    // write queued data before closing the stream
    logger("Removing " + archivefile);
    fs.unlinkSync(archivefile);
    logger("Sauce Connect downloaded correctly");

    callback();
  }

  setTimeout(function () {
    if (archivefile.match(/\.tar\.gz$/)) {
      exec("tar -xzf '" + archivefile + "'", {cwd: scDir}, done);
    } else {
      try {
        var zip = new AdmZip(archivefile);
        zip.extractAllTo(scDir, true);
      } catch (e) {
        return done(new Error("ERROR Unzipping file: " + e.message));
      }

      done();
    }
  }, 1000);
}

function setExecutePermissions(bin, callback) {
  if (os.type() === "Windows_NT") {
    // No need to set permission for the executable on Windows
    callback(null, bin);
  } else {
    // check current permissions
    fs.stat(bin, function (err, stat) {
      if (err) { return callback(new Error("Couldn't read sc permissions: " + err.message)); }

      if (stat.mode.toString(8) !== "100755") {
        fs.chmod(bin, 0o755, function (err) {
          if (err) { return callback(new Error("Couldn't set permissions: " + err.message)); }
          callback(null, bin);
        });
      } else {
        callback(null, bin);
      }
    });
  }
}

function httpsRequest(options) {
  // Optional http proxy to route the download through
  // (if agent is undefined, https.request will proceed as normal)
  var proxy = process.env.https_proxy || process.env.http_proxy;
  var agent;
  if (proxy) {
    agent = new HttpsProxyAgent(proxy);
  }

  options = options || {};
  options.agent = agent;
  options.timeout = 30000;

  return https.request(options);
}

function verifyChecksum(archivefile, checksum, cb) {
  if (!checksum) {
    logger("Checksum check for manually overwritten sc version isn't supported.");
    return cb();
  }

  var fd = fs.createReadStream(archivefile);
  var hash = crypto.createHash("sha1");
  hash.setEncoding("hex");

  hash.on("finish", function() {
    var sha1 = hash.read();
    if (sha1 !== checksum) {
      return cb(new Error("Checksum of the downloaded archive (" + sha1 + ") doesn't match (" + checksum + ")."));
    }

    logger("Archive checksum verified.");

    cb();
  });

  hash.on("error", function (err) {
    cb(err);
  });

  fd.pipe(hash);
}

function fetchArchive(archiveName, archivefile, callback) {
  if (!fs.existsSync(scDir)) {
    fs.mkdirSync(scDir);
  }

  var req = httpsRequest({
    host: SAUCE_API_HOST,
    port: 443,
    path: "/downloads/" + archiveName
  });

  req.on("error", function (err) {
    callback(err);
  });

  function removeArchive() {
    try {
      logger("Removing " + archivefile);
      fs.unlinkSync(archivefile);
    } catch (e) {
      logger("Error removing archive: " + e);
    }
    _.defer(process.exit.bind(null, 0));
  }

  // synchronously makes sure the file exists, so that we don't re-enter
  // in this function (which is only called when the file does not exist yet)
  fs.writeFileSync(archivefile, "");

  logger("Missing Sauce Connect local proxy, downloading dependency");
  logger("This will only happen once.");

  req.on("response", function (res) {
    if (res.statusCode !== 200) {
      logger(`Invalid response status: ${res.statusCode}`);

      return callback(new Error("Download failed with status code: " + res.statusCode));
    }

    var len = parseInt(res.headers["content-length"], 10),
      prettyLen = (len / (1024 * 1024) + "").substr(0, 4);

    logger("Downloading " + prettyLen + "MB");

    res.pipe(fs.createWriteStream(archivefile));

    // cleanup if the process gets interrupted.
    var events = ["exit", "SIGHUP", "SIGINT", "SIGTERM"];
    events.forEach(function (event) {
      process.on(event, removeArchive);
    });

    res.on("end", function () {
      events.forEach(function (event) {
        process.removeListener(event, removeArchive);
      });

      callback();
    });

  });

  req.end();
}

function fetchAndCheckArchive(archiveName, archivefile, checksum, callback) {
  return async.waterfall([
    async.apply(fetchArchive, archiveName, archivefile),
    async.apply(verifyChecksum, archivefile, checksum)
  ], callback);
}

function unpackAndFixArchive(archivefile, bin, callback) {
  return async.waterfall([
    async.apply(unpackArchive, archivefile),
    async.apply(setExecutePermissions, bin)
  ], callback);
}

function fetchAndUnpackArchive(versionDetails, options, callback) {
  var bin = getScBin(versionDetails.version);
  if (exists(bin)) {
    return callback(null, bin);
  }

  var archiveName = getArchiveName(versionDetails.version);
  var archivefile = path.normalize(scDir + "/" + archiveName);

  if (exists(archivefile)) {
    // the archive is being downloaded, poll for the binary to be ready
    async.doUntil(function wait(cb) {
      _.delay(cb, 1000);
    }, async.apply(exists, bin), function () {
      callback(null, bin);
    });
  }

  async.waterfall([
    async.apply(fetchAndCheckArchive, archiveName, archivefile, versionDetails.checksum),
    async.apply(unpackAndFixArchive, archivefile, bin)
  ], callback);
}

function scPlatform() {
  return {
    darwin: "osx",
    win32: "win32",
  }[process.platform] || "linux";
}

function readVersionsFile(versionsfile, cb) {
  var versions = require(versionsfile)["Sauce Connect"];

  return cb(null, {
    version: versions["version"],
    checksum: versions[scPlatform()]["sha1"]
  });
}

function getVersion(options, cb) {
  if (options.connectVersion) {
    return cb(null, {
      version: options.connectVersion
    });
  }
  if (sc_version !== "latest") {
    logger("Checksum check for manually overwritten sc versions isn't supported.");
    return cb(null, {
      version: sc_version
    });
  }

  var versionsfile = path.normalize(scDir + "/versions.json");

  if (exists(versionsfile)) {
    return readVersionsFile(versionsfile, cb);
  }

  if (!fs.existsSync(scDir)) {
    fs.mkdirSync(scDir);
  }

  var req = httpsRequest({
    host: SAUCE_API_HOST,
    port: 443,
    path: "/versions.json"
  });

  req.on("error", function (err) {
    cb(err);
  });

  req.on("response", function (res) {
    if (res.statusCode !== 200) {
      logger(`Invalid response status: ${res.statusCode}`);

      return cb(new Error("Fetching https://" + SAUCE_API_HOST + "/versions.json failed: " + res.statusCode));
    }

    var file = fs.createWriteStream(versionsfile);

    res.pipe(file);

    file.on("error", function (err) {
      cb(err);
    });

    file.on("close", function () {
      readVersionsFile(versionsfile, cb);
    });

  });

  req.end();
}

function download(options, callback) {
  if (arguments.length === 1) {
    callback = options;
    options = {};
  }

  if (options.exe) {
    return callback(null, options.exe);
  }

  async.waterfall([
    async.apply(getVersion, options),
    function (versionDetails, next) {
      return fetchAndUnpackArchive(versionDetails, options, next);
    }
  ], callback);
}

function connect(bin, options, callback) {
  var child;
  var logger = options.logger || function() {};
  var starting = true;
  var done = function (err, child) {
    if (!starting) {
      return;
    }
    starting = false;
    callback(err, child);
  };

  function ready() {
    logger("Testing tunnel ready");

    if (!options.detached) {
      closeOnProcessTermination();
    }
    done(null, child);
  }

  logger("Opening local tunnel using Sauce Connect");
  var watcher,
    readyfile,
    readyFileName = "sc-launcher-readyfile",
    args = processOptions(options),
    error,
    handleError = function (data) {
      if (data.indexOf("Not authorized") !== -1 && !error) {
        logger("Invalid Sauce Connect Credentials");
        error = new Error("Invalid Sauce Connect Credentials. " + data);
      } else if (data.indexOf("Sauce Connect could not establish a connection") !== -1) {
        logger("Sauce Connect API failure");
        error = new Error(data);
      } else if (data.indexOf("HTTP response code indicated failure") === -1) {
        // sc says the above before it says "Not authorized", but the following
        // Error: message is more useful
        error = new Error(data);
      }
      // error will be handled in the child.on("exit") handler
    },
    dataActions = {
      "Please wait for 'you may start your tests' to start your tests": function connecting() {
        logger("Creating tunnel with Sauce Labs");
      },
      "Tunnel ID:": function (data) {
        var tunnelIdMatch = tunnelIdRegExp.exec(data);
        if (tunnelIdMatch) {
          child.tunnelId = tunnelIdMatch[1];
        }
      },
      "Provisioned tunnel:": function (data) {
        var tunnelIdMatch = tunnelIdRegExp.exec(data);
        if (tunnelIdMatch) {
          child.tunnelId = tunnelIdMatch[1];
        }
      },
      "Selenium listener started on port": function (data) {
        var portMatch = portRegExp.exec(data);
        if (portMatch) {
          child.port = parseInt(portMatch[1], 10);
        }
      },
      //"you may start your tests": ready,
      "This version of Sauce Connect is outdated": function outdated() {

      },
      "Error: ": handleError,
      "ERROR: ": handleError,
      "Error bringing": handleError,
      "Sauce Connect could not establish a connection": handleError,
      "{\"error\":": handleError
    },
    previousData = "",
    killProcessTimeout = null,
    killProcess = function () {
      if (child) {
        child.kill("SIGTERM");
      }
    };

  if (options.readyFileId) {
    readyFileName = readyFileName + "_" + options.readyFileId;
  }

  // Node v0.8 uses os.tmpDir(), v0.10 uses os.tmpdir()
  readyfile = path.normalize((os.tmpdir ? os.tmpdir() : os.tmpDir()) +
    "/" + readyFileName);

  args.push("--readyfile", readyfile);

  // Only add -x when it wasn't specified
  var xArgIdx = args.findIndex((arg) => arg === "-x")
  if (xArgIdx === -1) {
    args.push("-x", `https://${SAUCE_API_HOST}/rest/v1`);
  }

  // Watching file as directory watching does not work on
  // all File Systems http://nodejs.org/api/fs.html#fs_caveats
  watcher = fs.watchFile(readyfile, function () {
    fs.exists(readyfile, function (exists) {
      if (exists) {
        logger("Detected sc ready");
        ready();
      }
    });
  });

  watcher.on("error", done);

  logger("Starting sc with args: " + args
    .join(" ")
    .replace(/-u\ [^\ ]+\ /, "-u XXXXXXXX ")
    .replace(/-k\ [^\ ]+\ /, "-k XXXXXXXX ")
    .replace(/[0-9a-f]{8}\-([0-9a-f]{4}\-){3}[0-9a-f]{12}/i,
      "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXX"));

  var spawnOptions = {};

  if (options.detached) {
    spawnOptions.detached = true;
  }

  child = spawn(bin, args, spawnOptions);

  currentTunnel = child;

  child.stdout.on("data", function (data) {
    previousData += data.toString();
    var lines = previousData.split("\n");
    previousData = lines.pop();
    // only process full lines
    _.each(lines, function (line) {
      line = line.trim();
      if (line === "") {
        return;
      }
      if (options.verbose) {
        logger(line);
      }

      if (!starting) {
        return;
      }
      _.each(dataActions, function (action, key) {
        if (line.indexOf(key) !== -1) {
          action(line);
          return false;
        }
      });
    });
  });

  child.stderr.on("data", function (data) {
    var line = data.toString().trim();
    if (line === "") {
      return;
    }
    if (options.verbose) {
      logger(line);
    }
  });

  child.on("error", function (err) {
    logger("Sauce connect process errored: " + err);

    fs.unwatchFile(readyfile);
    return done(err);
  });

  child.on("exit", function (code, signal) {
    currentTunnel = null;
    child = null;
    if (killProcessTimeout) {
      clearTimeout(killProcessTimeout);
      killProcessTimeout = null;
    }

    fs.unwatchFile(readyfile);

    if (error) { // from handleError() above
      return done(error);
    }

    var message = "Closing Sauce Connect Tunnel";
    if (code > 0) {
      message = "Could not start Sauce Connect. Exit code " + code +
        " signal: " + signal;
      done(new Error(message));
    }
    logger(message);
  });

  child.close = function (closeCallback) {
    if (closeCallback) {
      child.on("exit", function () {
        closeCallback();
      });
    }
    var tunnelId = child.tunnelId;
    if (tunnelId) {
      // rather than killing the process immediately, make a request to close the tunnel,
      // and give some time to the process to shutdown by itself
      httpsRequest({
        method: "DELETE",
        host: SAUCE_API_HOST,
        port: 443,
        auth: options.username + ":" + options.accessKey,
        path: "/rest/v1/" + options.username + "/tunnels/" + tunnelId
      }).on("response", function (res) {
        if (child) {
          // give some time to the process to shut down by itself
          killProcessTimeout = setTimeout(killProcess, 5000);
        }
        res.resume(); // read the full response to free resources
      }).on("error", killProcess).end();
    } else {
      killProcess();
    }
  };
}

function run(version, options, callback) {
  tryRun(0, {
    logger: options.logger,
    retries: options.connectRetries,
    timeout: options.connectRetryTimeout || defaultConnectRetryTimeout
  }, function (tryCallback) {
    return connect(version, options, tryCallback);
  }, callback);
}

function downloadWithRety(options, callback) {
  tryRun(0, {
    logger: options.logger,
    retries: options.downloadRetries,
    timeout: options.downloadRetryTimeout || defaultDownloadRetryTimeout
  }, function (tryCallback) {
    return download(options, tryCallback);
  }, callback);
}

function downloadAndRun(options, callback) {
  if (arguments.length === 1) {
    callback = options;
    options = {};
  }
  logger = options.logger || function () {};

  async.waterfall([
    async.apply(downloadWithRety, options),
    function (bin, next) {
      return run(bin, options, next);
    },
  ], callback);
}

module.exports = downloadAndRun;
module.exports.download = downloadWithRety;
module.exports.kill = killProcesses;
module.exports.clean = clean;
module.exports.setWorkDir = setWorkDir;
