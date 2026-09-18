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
import * as http from 'http';
import * as _ from 'lodash';
import * as socketIO from 'socket.io';

import {BrowserRunner} from './browserrunner';
import * as config from './config';
import {Context} from './context';
import {Plugin} from './plugin';
import {webserver} from './webserver';

interface ClientMessage<T> {
  browserId: number;
  event: string;
  data: T;
}

// Steps (& Hooks)

export async function setupOverrides(context: Context): Promise<void> {
  if (context.options.registerHooks) {
    context.options.registerHooks(context);
  }
}

export async function loadPlugins(context: Context): Promise<Plugin[]> {
  context.emit('log:debug', 'step: loadPlugins');

  const plugins = await context.plugins();

  // built in quasi-plugin.
  webserver(context);

  // Actual plugins.
  await Promise.all(plugins.map((plugin) => plugin.execute(context)));
  return plugins;
}

export async function configure(context: Context): Promise<void> {
  context.emit('log:debug', 'step: configure');
  const options = context.options;

  await config.expand(context);

  // Note that we trigger the configure hook _after_ filling in the `options`
  // object.
  //
  // If you want to modify options prior to this; do it during plugin init.
  await context.emitHook('configure');

  // Even if the options don't validate; useful debugging info.
  const cleanOptions = _.omit(options, 'output');
  context.emit('log:debug', 'configuration:', cleanOptions);

  await config.validate(options);
}

/**
 * The prepare step is where a lot of the runner's initialization occurs. This
 * is also typically where a plugin will want to spin up any long-running
 * process it requires.
 *
 * Note that some "plugins" are also built directly into WCT (webserver).
 */
export async function prepare(context: Context): Promise<void> {
  await context.emitHook('prepare');
}

export async function runTests(context: Context): Promise<void> {
  context.emit('log:debug', 'step: runTests');

  const result = runBrowsers(context);
  const runners = result.runners;
  context._testRunners = runners;

  context._socketIOServers = context._httpServers.map((httpServer) => {
    const socketIOServer = socketIO(httpServer);
    socketIOServer.on('connection', function(socket) {
      context.emit('log:debug', 'Test client opened sideband socket');
      socket.on('client-event', function(data: ClientMessage<any>) {
        const runner = runners[data.browserId];
        if (!runner) {
          throw new Error(
              `Unable to find browser runner for ` +
              `browser with id: ${data.browserId}`);
        }
        runner.onEvent(data.event, data.data);
      });
    });
    return socketIOServer;
  });

  await result.completionPromise;
}

export function cancelTests(context: Context): void {
  if (!context._testRunners) {
    return;
  }
  context._testRunners.forEach(function(tr) {
    tr.quit();
  });
}

// Helpers

function runBrowsers(context: Context) {
  const options = context.options;
  const numActiveBrowsers = options.activeBrowsers.length;
  if (numActiveBrowsers === 0) {
    throw new Error('No browsers configured to run');
  }

  // TODO(nevir): validate browser definitions.

  // Up the socket limit so that we can maintain more active requests.
  // TODO(nevir): We should be queueing the browsers above some limit too.
  http.globalAgent.maxSockets =
      Math.max(http.globalAgent.maxSockets, numActiveBrowsers * 2);

  context.emit('run-start', options);

  const errors: any[] = [];

  const promises: Promise<void>[] = [];

  const runners: BrowserRunner[] = [];
  let id = 0;
  for (const originalBrowserDef of options.activeBrowsers) {
    let waitFor: undefined|Promise<void> = undefined;
    for (const server of options.webserver._servers) {
      // Needed by both `BrowserRunner` and `CliReporter`.
      const browserDef = _.clone(originalBrowserDef);
      browserDef.id = id++;
      browserDef.variant = server.variant;
      _.defaultsDeep(browserDef, options.browserOptions);

      const runner =
          new BrowserRunner(context, browserDef, options, server.url, waitFor);
      promises.push(runner.donePromise.then(
          () => {
            context.emit('log:debug', browserDef, 'BrowserRunner complete');
          },
          (error) => {
            context.emit('log:debug', browserDef, 'BrowserRunner complete');
            errors.push(error);
          }));
      runners.push(runner);
      if (browserDef.browserName === 'safari') {
        // Control to Safari must be serialized. We can't launch two instances
        // simultaneously, because security lol.
        // https://webkit.org/blog/6900/webdriver-support-in-safari-10/
        waitFor = runner.donePromise.catch(() => {
          // The next runner doesn't care about errors, just wants to know when
          // it can start.
          return undefined;
        });
      }
    }
  }

  return {
    runners,
    completionPromise: (async function() {
      await Promise.all(promises);
      const error = errors.length > 0 ? _.union(errors).join(', ') : null;
      context.emit('run-end', error);
      // TODO(nevir): Better rationalize run-end and hook.
      await context.emitHook('cleanup');

      if (error) {
        throw new Error(error);
      }
    }())
  };
}
