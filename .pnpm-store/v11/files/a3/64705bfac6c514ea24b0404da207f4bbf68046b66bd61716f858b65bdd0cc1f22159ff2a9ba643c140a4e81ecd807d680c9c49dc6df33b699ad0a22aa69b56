"use strict";
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("mz/fs");
const path = require("path");
const url = require("url");
const args_1 = require("./args");
const compile_middleware_1 = require("./compile-middleware");
const start_server_1 = require("./start_server");
const commandLineArgs = require("command-line-args");
const commandLineUsage = require("command-line-usage");
function run() {
    return __awaiter(this, void 0, void 0, function* () {
        const argsWithHelp = args_1.args.concat({
            name: 'help',
            description: 'Shows this help message',
            type: Boolean,
        });
        // tslint:disable-next-line: no-any We should declare/infer a type for this.
        let cliOptions;
        try {
            cliOptions = commandLineArgs(argsWithHelp);
        }
        catch (e) {
            printUsage(argsWithHelp);
            return;
        }
        const proxyArgs = {
            path: cliOptions['proxy-path'],
            target: cliOptions['proxy-target']
        };
        const options = {
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
            compile_middleware_1.babelCompileCache['max'] = cliOptions['compile-cache'];
        }
        const serverInfos = yield start_server_1.startServers(options);
        if (serverInfos.kind === 'mainline') {
            const mainlineServer = serverInfos;
            const urls = start_server_1.getServerUrls(options, mainlineServer.server);
            console.log(`Files in this directory are available under the following URLs
    applications: ${url.format(urls.serverUrl)}
    reusable components: ${url.format(urls.componentUrl)}
  `);
        }
        else {
            // We started multiple servers, just tell the user about the control server,
            // it serves out human-readable info on how to access the others.
            const urls = start_server_1.getServerUrls(options, serverInfos.control.server);
            console.log(`Started multiple servers with different variants:
    More info here: ${url.format(urls.serverUrl)}`);
        }
        return serverInfos;
    });
}
exports.run = run;
function printUsage(options) {
    const usage = commandLineUsage([{
            header: 'A development server for Polymer projects',
            title: 'polyserve',
            optionList: options,
        }]);
    console.log(usage);
}
function getVersion() {
    const packageFilePath = path.resolve(__dirname, '../package.json');
    const packageFile = fs.readFileSync(packageFilePath).toString();
    const packageJson = JSON.parse(packageFile);
    const version = packageJson['version'];
    return version;
}
//# sourceMappingURL=cli.js.map