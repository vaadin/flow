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

import * as chalk from 'chalk';
import * as events from 'events';
import * as _ from 'lodash';

import {CliReporter} from './clireporter';
import * as config from './config';
import {Context} from './context';
import {Plugin} from './plugin';
import {test} from './test';

const PACKAGE_INFO = require('../package.json');
const noopNotifier = {
  notify: () => {}
};
let updateNotifier = noopNotifier;

(function() {
try {
  updateNotifier = require('update-notifier')({pkg: PACKAGE_INFO});
} catch (error) {
  // S'ok if we don't have update-notifier. It's optional.
}
})();

export async function run(
    _env: any, args: string[], output: NodeJS.WritableStream): Promise<void> {
  await wrapResult(output, _run(args, output));
}

async function _run(args: string[], output: NodeJS.WritableStream) {
  // If the "--version" or "-V" flag is ever present, just print
  // the current version. Useful for globally installed CLIs.
  if (args.includes('--version') || args.includes('-V')) {
    output.write(`${PACKAGE_INFO.version}\n`);
    return Promise.resolve();
  }

  // Options parsing is a two phase affair. First, we need an initial set of
  // configuration so that we know which plugins to load, etc:
  let options = config.preparseArgs(args) as config.Config;
  // Depends on values from the initial merge:
  options = config.merge(options, <config.Config>{
    output: output,
    ttyOutput: !process.env.CI && output['isTTY'] && !options.simpleOutput,
  });
  const context = new Context(options);

  if (options.skipUpdateCheck) {
    updateNotifier = noopNotifier;
  }

  // `parseArgs` merges any new configuration into `context.options`.
  await config.parseArgs(context, args);
  await test(context);
}

// Note that we're cheating horribly here. Ideally all of this logic is within
// wct-sauce. The trouble is that we also want WCT's configuration lookup logic,
// and that's not (yet) cleanly exposed.
export async function runSauceTunnel(
    _env: any, args: string[], output: NodeJS.WritableStream): Promise<void> {
  await wrapResult(output, _runSauceTunnel(args, output));
}

async function _runSauceTunnel(args: string[], output: NodeJS.WritableStream) {
  const cmdOptions = config.preparseArgs(args) as config.Config;
  const context = new Context(cmdOptions);

  const diskOptions = context.options;
  const baseOptions: config.Config =
      (diskOptions.plugins && diskOptions.plugins['sauce']) ||
      diskOptions.sauce || {};

  const plugin = await Plugin.get('sauce');
  const parser = require('nomnom');
  parser.script('wct-st');
  parser.options(_.omit(plugin.cliConfig, 'browsers', 'tunnelId'));
  const options = _.merge(baseOptions, parser.parse(args));

  const wctSauce = require('wct-sauce');
  wctSauce.expandOptions(options);

  const emitter = new events.EventEmitter();
  new CliReporter(emitter, output, <config.Config>{});
  const tunnelId = await new Promise<string>((resolve, reject) => {
    wctSauce.startTunnel(
        options,
        emitter,
        (error: any, tunnelId: string) =>
            error ? reject(error) : resolve(tunnelId));
  });

  output.write('\n');
  output.write(
      'The tunnel will remain active while this process is running.\n');
  output.write(
      'To use this tunnel for other WCT runs, export the following:\n');
  output.write('\n');
  output.write(chalk.cyan('export SAUCE_TUNNEL_ID=' + tunnelId) + '\n');
  output.write('Press CTRL+C to close the sauce tunnel\n');
}

async function wrapResult(
    output: NodeJS.WritableStream, promise: Promise<void>) {
  let error: any;
  try {
    await promise;
  } catch (e) {
    error = e;
  }

  if (!process.env.CI) {
    updateNotifier.notify();
  }

  if (error) {
    output.write('\n');
    output.write(chalk.red(error) + '\n');
    output.write('\n');
    throw error;
  }
}
