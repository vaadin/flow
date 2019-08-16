/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// This script is intended to proxy all logging calls in order to capture them into the global field
// This is the only way to access the browser logs in IT tests
window.allLogMessages = [];

const oldTrace = console.trace;
console.trace = function(message) {
  window.allLogMessages.push(message);
  oldTrace(message);
};

const oldInfo = console.info;
console.info = function(message) {
  window.allLogMessages.push(message);
  oldInfo(message);
};

const oldLog = console.log;
console.log = function(message) {
  window.allLogMessages.push(message);
  oldLog(message);
};

const oldWarn = console.warn;
console.warn = function(message) {
  window.allLogMessages.push(message);
  oldWarn(message);
};

const oldDebug = console.debug;
console.debug = function(message) {
  window.allLogMessages.push(message);
  oldDebug(message);
};

const oldError = console.error;
console.error = function(message) {
  window.allLogMessages.push(message);
  oldError(message);
};
