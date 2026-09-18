/**
 * @license
 * Copyright (c) 2014 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
import * as cleankill from 'cleankill';

import {CliReporter} from './clireporter';
import {Config} from './config';
import {Context} from './context';
import * as steps from './steps';

/**
 * Runs a suite of web component tests.
 *
 * The returned Context (a kind of EventEmitter) fires various events to allow
 * you to track the progress of the tests:
 *
 * Lifecycle Events:
 *
 * `run-start`
 *   WCT is ready to begin spinning up browsers.
 *
 * `browser-init` {browser} {stats}
 *   WCT is ready to begin spinning up browsers.
 *
 * `browser-start` {browser} {metadata} {stats}
 *   The browser has begun running tests. May fire multiple times (i.e. when
 *   manually refreshing the tests).
 *
 * `sub-suite-start` {browser} {sharedState} {stats}
 *   A suite file has begun running.
 *
 * `test-start` {browser} {test} {stats}
 *   A test has begun.
 *
 * `test-end` {browser} {test} {stats}
 *  A test has ended.
 *
 * `sub-suite-end` {browser} {sharedState} {stats}
 *   A suite file has finished running all of its tests.
 *
 * `browser-end` {browser} {error} {stats}
 *   The browser has completed, and it shutting down.
 *
 * `run-end` {error}
 *   WCT has run all browsers, and is shutting down.
 *
 * Generic Events:
 *
 *  * log:debug
 *  * log:info
 *  * log:warn
 *  * log:error
 *
 * @param {!Config|!Context} options The configuration or an already formed
 *     `Context` object.
 */
export async function test(options: Config|Context): Promise<void> {
  const context = (options instanceof Context) ? options : new Context(options);

  // We assume that any options related to logging are passed in via the initial
  // `options`.
  if (context.options.output) {
    new CliReporter(context, context.options.output, context.options);
  }

  try {
    await steps.setupOverrides(context);
    await steps.loadPlugins(context);
    await steps.configure(context);
    await steps.prepare(context);
    await steps.runTests(context);
  } finally {
    if (!context.options.skipCleanup) {
      await cleankill.close();
    }
  }
}

// HACK
test['test'] = test;

module.exports = test;
