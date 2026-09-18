"use strict";

var _ = require("lodash");
var overrides = {
  username: "u",
  accessKey: "k",
  verboseDebugging: "verbose",
  port: "P",
  vv: "-vv"
};

var booleans = {
  verboseDebugging: true,
  doctor: true
};

module.exports = function processOptions(options) {
  options.username = options.username || process.env.SAUCE_USERNAME;
  options.accessKey = options.accessKey || process.env.SAUCE_ACCESS_KEY;

  return _.reduce(
    _.omit(options, [
      "readyFileId",
      "verbose",
      "logger",
      "log",
      "connectRetries",
      "connectRetryTimeout",
      "downloadRetries",
      "downloadRetryTimeout",
      "detached",
      "connectVersion",
      "exe"
    ]),
    function (argList, value, key) {
      if (typeof value === "undefined" || value === null) {
        return argList;
      }
      var argName = overrides[key] || ( key[0] !== "-" && key.length > 1 && _.kebabCase(key)) || key;

      if (argName.length === 1) {
        argName = "-" + argName;
      } else if(argName[0] !== "-") {
        argName = "--" + argName;
      }

      if (Array.isArray(value)) {
        value = value.join(",");
      }

      if (booleans[key] || value === true) {
        argList.push(argName);
      } else {
        argList.push(argName, value);
      }

      return argList;
    }, []);
};
