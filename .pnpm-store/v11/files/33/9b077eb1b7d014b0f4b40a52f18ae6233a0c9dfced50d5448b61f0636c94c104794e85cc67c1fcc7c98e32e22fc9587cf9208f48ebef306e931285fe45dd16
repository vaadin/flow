/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
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

/// <reference path="../custom_typings/command-line-args.d.ts" />

import {ArgDescriptor} from 'command-line-args';
import * as fs from 'mz/fs';
import * as path from 'path';
import * as url from 'url';

import {args} from './args';
import {babelCompileCache} from './compile-middleware';
import {getServerUrls, ServerOptions, StartServerResult, startServers} from './start_server';

import commandLineArgs = require('command-line-args');
import commandLineUsage = require('command-line-usage');


export async function run(): Promise<StartServerResult> {
  const argsWithHelp: ArgDescriptor[] = args.concat({
    name: 'help',
    description: 'Shows this help message',
    type: Boolean,
  });
  // tslint:disable-next-line: no-any We should declare/infer a type for this.
  let cliOptions: any;

  try {
    cliOptions = commandLineArgs(argsWithHelp);
  } catch (e) {
    printUsage(argsWithHelp);
    return;
  }

  const proxyArgs = {
    path: cliOptions['proxy-path'],
    target: cliOptions['proxy-target']
  };

  const options: ServerOptions = {
    root: cliOptions.root,
    compile: cliOptions.compile,
    moduleResolution: cliOptions['module-resolution'],
    port: cliOptions.port,
    hostname: cliOptions.hostname,
    open: cliOptions.open,
    browser: cliOptions['browser'],
    openPath: cliOptions['open-path'],
    componentDir: cliOptions['component-dir'],
    componentUrl: cliOptions['component-url'],
    packageName: cliOptions['package-name'],
    npm: cliOptions['npm'],
    protocol: cliOptions['protocol'],
    keyPath: cliOptions['key'],
    certPath: cliOptions['cert'],
    pushManifestPath: cliOptions['manifest'],
    proxy: proxyArgs.path && proxyArgs.target && proxyArgs,
    allowOrigin: cliOptions['allow-origin'],
  };

  if (cliOptions.help) {
    printUsage(argsWithHelp);
    return;
  }
  if (cliOptions.version) {
    console.log(getVersion());
    return;
  }

  if (typeof cliOptions['compile-cache'] === 'number') {
    console.log(`compile cache set to ${cliOptions['compile-cache']}`);
    babelCompileCache['max'] = cliOptions['compile-cache'];
  }

  const serverInfos = await startServers(options);

  if (serverInfos.kind === 'mainline') {
    const mainlineServer = serverInfos;
    const urls = getServerUrls(options, mainlineServer.server);
    console.log(`Files in this directory are available under the following URLs
    applications: ${url.format(urls.serverUrl)}
    reusable components: ${url.format(urls.componentUrl)}
  `);
  } else {
    // We started multiple servers, just tell the user about the control server,
    // it serves out human-readable info on how to access the others.
    const urls = getServerUrls(options, serverInfos.control.server);
    console.log(`Started multiple servers with different variants:
    More info here: ${url.format(urls.serverUrl)}`);
  }
  return serverInfos;
}

function printUsage(options: commandLineUsage.ArgDescriptor[]): void {
  const usage = commandLineUsage([{
    header: 'A development server for Polymer projects',
    title: 'polyserve',
    optionList: options,
  }]);
  console.log(usage);
}

function getVersion(): string {
  const packageFilePath = path.resolve(__dirname, '../package.json');
  const packageFile = fs.readFileSync(packageFilePath).toString();
  const packageJson = JSON.parse(packageFile);
  const version = packageJson['version'];
  return version;
}
