"use strict";
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const bowerConfig = require("bower-config");
const cleankill = require("cleankill");
const fs = require("fs");
const _ = require("lodash");
const path = require("path");
const polyserve_1 = require("polyserve");
const resolve = require("resolve");
const semver = require("semver");
const send = require("send");
const serverDestroy = require("server-destroy");
const config_1 = require("./config");
// Template for generated indexes.
const INDEX_TEMPLATE = _.template(fs.readFileSync(path.resolve(__dirname, '../data/index.html'), { encoding: 'utf-8' }));
const DEFAULT_HEADERS = {
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Pragma': 'no-cache',
    'Expires': '0',
};
function relativeFrom(fromPath, toPath) {
    return path.relative(fromPath, toPath).replace(/\\/g, '/');
}
function resolveFrom(fromPath, moduleId) {
    try {
        return resolve.sync(moduleId, { basedir: fromPath, preserveSymlinks: true });
    }
    catch (e) {
        return '';
    }
}
/**
 * The webserver module is a quasi-plugin. This ensures that it is hooked in a
 * sane way (for other plugins), and just follows the same flow.
 *
 * It provides a static HTTP server for serving the desired tests and WCT's
 * `browser.js`/`environment.js`.
 */
function webserver(wct) {
    const options = wct.options;
    wct.hook('configure', function () {
        return __awaiter(this, void 0, void 0, function* () {
            // For now, you should treat all these options as an implementation detail
            // of WCT. They may be opened up for public configuration, but we need to
            // spend some time rationalizing interactions with external webservers.
            options.webserver = _.merge(options.webserver, {});
            if (options.verbose) {
                options.clientOptions.verbose = true;
            }
            // Hacky workaround for Firefox + Windows issue where FF screws up pathing.
            // Bug: https://github.com/Polymer/web-component-tester/issues/194
            options.suites = options.suites.map((cv) => cv.replace(/\\/g, '/'));
            // The generated index needs the correct "browser.js" script. When using
            // npm, the wct-browser-legacy package may be used, so we test for that
            // package and will use its "browser.js" if present.
            let browserScript = 'web-component-tester/browser.js';
            const scripts = [], extraScripts = [];
            const modules = [], extraModules = [];
            if (options.npm) {
                options.clientOptions = options.clientOptions || {};
                options.clientOptions.environmentScripts =
                    options.clientOptions.environmentScripts || [];
                browserScript = '';
                const fromPath = path.resolve(options.root || process.cwd());
                options.wctPackageName = options.wctPackageName ||
                    ['wct-mocha', 'wct-browser-legacy', 'web-component-tester'].find((p) => !!resolveFrom(fromPath, p));
                const npmPackageRootPath = path.dirname(resolveFrom(fromPath, options.wctPackageName + '/package.json'));
                if (npmPackageRootPath) {
                    const wctPackageScriptName = ['web-component-tester', 'wct-browser-legacy'].includes(options.wctPackageName) ?
                        'browser.js' :
                        `${options.wctPackageName}.js`;
                    browserScript = `${npmPackageRootPath}/${wctPackageScriptName}`.slice(npmPackageRootPath.length - options.wctPackageName.length);
                }
                const packageName = config_1.getPackageName(options);
                const isPackageScoped = packageName && packageName[0] === '@';
                const rootNodeModules = path.resolve(path.join(options.root, 'node_modules'));
                // WCT used to try to bundle a lot of packages for end-users, but
                // because of `node_modules` layout, these need to actually be resolved
                // from the package as installed, to ensure the desired version is
                // loaded.  Here we list the legacy packages and attempt to resolve them
                // from the WCT package.
                if (['web-component-tester', 'wct-browser-legacy'].includes(options.wctPackageName)) {
                    const legacyNpmSupportPackageScripts = [
                        'stacky/browser.js',
                        'async/lib/async.js',
                        'lodash/index.js',
                        'mocha/mocha.js',
                        'chai/chai.js',
                        '@polymer/sinonjs/sinon.js',
                        'sinon-chai/lib/sinon-chai.js',
                        'accessibility-developer-tools/dist/js/axs_testing.js',
                        '@polymer/test-fixture/test-fixture.js',
                    ];
                    const resolvedLegacyNpmSupportPackageScripts = legacyNpmSupportPackageScripts
                        .map((script) => resolveFrom(npmPackageRootPath, script))
                        .filter((script) => script !== '');
                    options.clientOptions.environmentScripts.push(...resolvedLegacyNpmSupportPackageScripts.map((script) => relativeFrom(rootNodeModules, script)));
                }
                else {
                    // We need to load Mocha in the generated index.
                    const resolvedMochaScript = resolveFrom(npmPackageRootPath, 'mocha/mocha.js');
                    if (resolvedMochaScript) {
                        options.clientOptions.environmentScripts.push(relativeFrom(rootNodeModules, resolvedMochaScript));
                    }
                }
                if (browserScript && isPackageScoped) {
                    browserScript = `../${browserScript}`;
                }
            }
            if (browserScript) {
                scripts.push(`../${browserScript}`);
            }
            if (!options.npm) {
                scripts.push('web-component-tester/data/a11ysuite.js');
            }
            options.webserver._generatedIndexContent =
                INDEX_TEMPLATE(Object.assign({ scripts, extraScripts: [], modules }, options));
        });
    });
    wct.hook('prepare', function () {
        return __awaiter(this, void 0, void 0, function* () {
            const wsOptions = options.webserver;
            const additionalRoutes = new Map();
            const packageName = config_1.getPackageName(options);
            let componentDir;
            // Check for client-side compatibility.
            // Non-npm case.
            if (!options.npm) {
                componentDir = bowerConfig.read(options.root).directory;
                const pathToLocalWct = path.join(options.root, componentDir, 'web-component-tester');
                let version = undefined;
                const mdFilenames = ['package.json', 'bower.json', '.bower.json'];
                for (const mdFilename of mdFilenames) {
                    const pathToMetadata = path.join(pathToLocalWct, mdFilename);
                    try {
                        if (!version) {
                            version = require(pathToMetadata).version;
                        }
                    }
                    catch (e) {
                        // Handled below, where we check if we found a version.
                    }
                }
                if (!version) {
                    throw new Error(`
The web-component-tester Bower package is not installed as a dependency of this project (${packageName}).

Please run this command to install:
    bower install --save-dev web-component-tester

Web Component Tester >=6.0 requires that support files needed in the browser are installed as part of the project's dependencies or dev-dependencies. This is to give projects greater control over the versions that are served, while also making Web Component Tester's behavior easier to understand.

Expected to find a ${mdFilenames.join(' or ')} at: ${pathToLocalWct}/
`);
                }
                const allowedRange = require(path.join(__dirname, '..', 'package.json'))['--private-wct--']['client-side-version-range'];
                if (!semver.satisfies(version, allowedRange)) {
                    throw new Error(`
    The web-component-tester Bower package installed is incompatible with the
    wct node package you're using.

    The test runner expects a version that satisfies ${allowedRange} but the
    bower package you have installed is ${version}.
`);
                }
                let hasWarnedBrowserJs = false;
                additionalRoutes.set('/browser.js', function (request, response) {
                    if (!hasWarnedBrowserJs) {
                        console.warn(`

          WARNING:
          Loading WCT's browser.js from /browser.js is deprecated.

          Instead load it from ../web-component-tester/browser.js
          (or with the absolute url /components/web-component-tester/browser.js)
        `);
                        hasWarnedBrowserJs = true;
                    }
                    const browserJsPath = path.join(pathToLocalWct, 'browser.js');
                    send(request, browserJsPath).pipe(response);
                });
            }
            const pathToGeneratedIndex = `/components/${packageName}/generated-index.html`;
            additionalRoutes.set(pathToGeneratedIndex, (_request, response) => {
                response.set(DEFAULT_HEADERS);
                response.send(options.webserver._generatedIndexContent);
            });
            const appMapper = (app, options) => __awaiter(this, void 0, void 0, function* () {
                // Using the define:webserver hook to provide a mapper function that
                // allows user to substitute their own app for the generated polyserve
                // app.
                yield wct.emitHook('define:webserver', app, (substitution) => {
                    app = substitution;
                }, options);
                return app;
            });
            // Serve up project & dependencies via polyserve
            const polyserveResult = yield polyserve_1.startServers({
                root: options.root,
                componentDir,
                compile: options.compile,
                hostname: options.webserver.hostname,
                port: options.webserver.port,
                headers: DEFAULT_HEADERS,
                packageName,
                additionalRoutes,
                npm: !!options.npm,
                moduleResolution: options.moduleResolution,
                proxy: options.proxy,
            }, appMapper);
            let servers;
            const onDestroyHandlers = [];
            const registerServerTeardown = (serverInfo) => {
                const destroyableServer = serverInfo.server;
                serverDestroy(destroyableServer);
                onDestroyHandlers.push(() => {
                    destroyableServer.destroy();
                    return new Promise((resolve) => serverInfo.server.on('close', () => resolve()));
                });
            };
            if (polyserveResult.kind === 'mainline') {
                servers = [polyserveResult];
                registerServerTeardown(polyserveResult);
                const address = polyserveResult.server.address();
                if (typeof address !== 'string') {
                    wsOptions.port = address.port;
                }
            }
            else if (polyserveResult.kind === 'MultipleServers') {
                servers = [polyserveResult.mainline];
                servers = servers.concat(polyserveResult.variants);
                const address = polyserveResult.mainline.server.address();
                if (typeof address !== 'string') {
                    wsOptions.port = address.port;
                }
                for (const server of polyserveResult.servers) {
                    registerServerTeardown(server);
                }
            }
            else {
                const never = polyserveResult;
                throw new Error('Internal error: Got unknown response from polyserve.startServers: ' +
                    `${never}`);
            }
            wct._httpServers = servers.map((s) => s.server);
            // At this point, we allow other plugins to hook and configure the
            // webservers as they please.
            for (const server of servers) {
                yield wct.emitHook('prepare:webserver', server.app);
            }
            options.webserver._servers = servers.map((s) => {
                const address = s.server.address();
                const port = typeof address === 'string' ? '' : `:${address.port}`;
                const hostname = s.options.hostname;
                const url = `http://${hostname}${port}${pathToGeneratedIndex}`;
                return { url, variant: s.kind === 'mainline' ? '' : s.variantName };
            });
            // TODO(rictic): re-enable this stuff. need to either move this code
            // into polyserve or let the polyserve API expose this stuff.
            // app.use('/httpbin', httpbin.httpbin);
            // app.get('/favicon.ico', function(request, response) {
            //   response.end();
            // });
            // app.use(function(request, response, next) {
            //   wct.emit('log:warn', '404', chalk.magenta(request.method),
            //   request.url);
            //   next();
            // });
            function interruptHandler() {
                return __awaiter(this, void 0, void 0, function* () {
                    // close the socket IO server directly if it is spun up
                    for (const io of (wct._socketIOServers || [])) {
                        // we will close the underlying server ourselves
                        io.httpServer = null;
                        io.close();
                    }
                    yield Promise.all(onDestroyHandlers.map((f) => f()));
                });
            }
            cleankill.onInterrupt(() => {
                return new Promise((resolve) => {
                    interruptHandler().then(() => resolve(), resolve);
                });
            });
        });
    });
}
exports.webserver = webserver;
function exists(path) {
    try {
        fs.statSync(path);
        return true;
    }
    catch (_err) {
        return false;
    }
}
// HACK(rictic): remove this ES6-compat hack and export webserver itself
webserver['webserver'] = webserver;
module.exports = webserver;
