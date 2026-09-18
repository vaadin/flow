"use strict";

module.exports = function tryRun(count, retryOptions, fn, callback) {
  fn(function (err, result) {
    if (err) {
      if (count < retryOptions.retries) {
        return setTimeout(function () {
          tryRun(count + 1, retryOptions, fn, callback);
        }, retryOptions.timeout);
      }

      return callback(err);
    }

    callback(null, result);
  });
};
