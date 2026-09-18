"use strict";
/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
const assert = require("assert");
const escapeHtml = require("escape-html");
const express = require("express");
const fs = require("mz/fs");
const path = require("path");
const path_transformers_1 = require("polymer-build/lib/path-transformers");
const send = require("send");
// TODO: Switch to node-http2 when compatible with express
// https://github.com/molnarg/node-http2/issues/100
const http = require("spdy");
const compile_middleware_1 = require("./compile-middleware");
const config_1 = require("./config");
const custom_elements_es5_adapter_middleware_1 = require("./custom-elements-es5-adapter-middleware");
const make_app_1 = require("./make_app");
const open_browser_1 = require("./util/open_browser");
const push_1 = require("./util/push");
const tls_1 = require("./util/tls");
const compression = require("compression");
const cors = require("cors");
const httpProxy = require('http-proxy-middleware');
function applyDefaultServerOptions(options) {
    const withDefaults = Object.assign({}, options);
    Object.assign(withDefaults, {
        port: options.port || 0,
        hostname: options.hostname || 'localhost',
        root: path.resolve(options.root || '.'),
        compile: options.compile || 'auto',
        certPath: options.certPath || 'cert.pem',
        keyPath: options.keyPath || 'key.pem',
        componentDir: config_1.getComponentDir(options),
        componentUrl: options.componentUrl || 'components'
    });
    withDefaults.packageName = config_1.getPackageName(withDefaults);
    return withDefaults;
}
/**
 * @param {ServerOptions} options used to configure the generated polyserve app
 *     and server.
 * @param {ExpressAppMapper} appMapper optional mapper function which is called
 *     with the generated polyserve app and the options used to generate
 *     it and returns an optional substitution Express app.  This is usually one
 *     that mounts the original app, to add routes or middleware in advance of
 *     polyserve's catch-all routes.
 * @return {Promise} A Promise that completes when the server has started.
 * @deprecated Please use `startServers` instead. This function will be removed
 *     in a future release.
 */
function startServer(options, appMapper) {
    return __awaiter(this, void 0, void 0, function* () {
        return (yield _startServer(options, appMapper)).server;
    });
}
exports.startServer = startServer;
function _startServer(options, appMapper) {
    return __awaiter(this, void 0, void 0, function* () {
        options = options || {};
        assertNodeVersion(options);
        try {
            let app = getApp(options);
            if (appMapper) {
                // If the map function doesn't return an app, we should fallback to the
                // original app, hence the `appMapper(app) || app`.
                app = (yield appMapper(app, options)) || app;
            }
            const server = yield startWithApp(options, app);
            return { app, server };
        }
        catch (e) {
            console.error('ERROR: Server failed to start:', e);
            throw new Error(e);
        }
    });
}
/**
 * Starts one or more web servers, based on the given options and
 * variant bower_components directories that are found in the root dir.
 */
function startServers(options, appMapper) {
    return __awaiter(this, void 0, void 0, function* () {
        options = applyDefaultServerOptions(options);
        const variants = yield findVariants(options);
        // TODO(rictic): support manually configuring variants? tracking more
        //   metadata about them besides their names?
        if (variants.length > 0) {
            return yield startVariants(options, variants, appMapper);
        }
        const serverAndApp = yield _startServer(options, appMapper);
        return {
            options,
            kind: 'mainline',
            server: serverAndApp.server,
            app: serverAndApp.app,
        };
    });
}
exports.startServers = startServers;
// TODO(usergenic): Variants should support the directory naming convention in
// the .bowerrc instead of hardcoded 'bower_components' form seen here.
function findVariants(options) {
    return __awaiter(this, void 0, void 0, function* () {
        const root = options.root || process.cwd();
        const filesInRoot = yield fs.readdir(root);
        const variants = filesInRoot
            .map((f) => {
            const match = f.match(`^${options.componentDir}-(.*)`);
            return match && { name: match[1], directory: match[0] };
        })
            .filter((f) => f != null && f.name !== '');
        return variants;
    });
}
function startVariants(options, variants, appMapper) {
    return __awaiter(this, void 0, void 0, function* () {
        const mainlineOptions = Object.assign({}, options);
        mainlineOptions.port = 0;
        const mainServer = yield _startServer(mainlineOptions, appMapper);
        const mainServerInfo = {
            kind: 'mainline',
            server: mainServer.server,
            app: mainServer.app,
            options: mainlineOptions,
        };
        const variantServerInfos = [];
        for (const variant of variants) {
            const variantOpts = Object.assign({}, options);
            variantOpts.port = 0;
            variantOpts.componentDir = variant.directory;
            const variantServer = yield _startServer(variantOpts, appMapper);
            variantServerInfos.push({
                kind: 'variant',
                variantName: variant.name,
                dependencyDir: variant.directory,
                server: variantServer.server,
                app: variantServer.app,
                options: variantOpts
            });
        }
        const controlServerInfo = yield startControlServer(options, mainServerInfo, variantServerInfos);
        const servers = [controlServerInfo, mainServerInfo]
            .concat(variantServerInfos);
        const result = {
            kind: 'MultipleServers',
            control: controlServerInfo,
            mainline: mainServerInfo,
            variants: variantServerInfos,
            servers,
        };
        return result;
    });
}
function startControlServer(options, mainlineInfo, variantInfos) {
    return __awaiter(this, void 0, void 0, function* () {
        options = applyDefaultServerOptions(options);
        const app = express();
        app.get('/api/serverInfo', (_req, res) => {
            res.contentType('json');
            res.send(JSON.stringify({
                packageName: options.packageName,
                mainlineServer: {
                    port: assertNotString(mainlineInfo.server.address()).port,
                },
                variants: variantInfos.map((info) => {
                    return {
                        name: info.variantName,
                        port: assertNotString(info.server.address()).port,
                    };
                })
            }));
            res.end();
        });
        const indexPath = path.join(__dirname, '..', 'static', 'index.html');
        app.get('/', (_req, res) => __awaiter(this, void 0, void 0, function* () {
            res.contentType('html');
            const indexContents = yield fs.readFile(indexPath, 'utf-8');
            res.send(indexContents);
            res.end();
        }));
        const controlServer = {
            kind: 'control',
            options: options,
            server: yield startWithApp(options, app),
            app
        };
        return controlServer;
    });
}
exports.startControlServer = startControlServer;
function getApp(options) {
    options = applyDefaultServerOptions(options);
    // Preload the h2-push manifest to avoid the cost on first push
    if (options.pushManifestPath) {
        push_1.getPushManifest(options.root, options.pushManifestPath);
    }
    const root = options.root || '.';
    const app = express();
    app.use(compression());
    if (options.additionalRoutes) {
        options.additionalRoutes.forEach((handler, route) => {
            app.get(route, handler);
        });
    }
    const componentUrl = options.componentUrl;
    const polyserve = make_app_1.makeApp({
        componentDir: options.componentDir,
        packageName: options.packageName,
        root: root,
        headers: options.headers,
    });
    const filePathRegex = /.*\/.+\..{1,}$/;
    if (options.proxy) {
        if (options.proxy.path.startsWith(componentUrl)) {
            console.error(`proxy path can not start with ${componentUrl}.`);
            return;
        }
        let escapedPath = options.proxy.path;
        for (const char of ['*', '?', '+']) {
            if (escapedPath.indexOf(char) > -1) {
                console.warn(`Proxy path includes character "${char}"` +
                    `which can cause problems during route matching.`);
            }
        }
        if (escapedPath.startsWith('/')) {
            escapedPath = escapedPath.substring(1);
        }
        if (escapedPath.endsWith('/')) {
            escapedPath = escapedPath.slice(0, -1);
        }
        const pathRewrite = {};
        pathRewrite[`^/${escapedPath}`] = '';
        const apiProxy = httpProxy(`/${escapedPath}`, {
            target: options.proxy.target,
            changeOrigin: true,
            pathRewrite: pathRewrite,
            logLevel: 'warn',
        });
        app.use(`/${escapedPath}/`, apiProxy);
    }
    app.use('*', custom_elements_es5_adapter_middleware_1.injectCustomElementsEs5Adapter(options.compile));
    app.use('*', compile_middleware_1.babelCompile(options.compile, options.moduleResolution, root, options.packageName, options.componentUrl, options.componentDir));
    if (options.allowOrigin) {
        app.use(cors({ origin: options.allowOrigin }));
    }
    app.use(`/${componentUrl}/`, polyserve);
    // `send` expects files to be specified relative to the given root and as a
    // URL rather than a file system path.
    const entrypoint = options.entrypoint ?
        path_transformers_1.urlFromPath(root, options.entrypoint) :
        'index.html';
    app.get('/*', (req, res) => {
        push_1.pushResources(options, req, res);
        const filePath = req.path;
        send(req, filePath, { root: root, index: entrypoint, etag: false, lastModified: false })
            .on('error', (error) => {
            if (error.status === 404 && !filePathRegex.test(filePath)) {
                // The static file handling middleware failed to find a file on
                // disk. Serve the entry point HTML file instead of a 404.
                send(req, entrypoint, { root: root }).pipe(res);
            }
            else {
                res.status(error.status || 500);
                res.type('html');
                res.end(escapeHtml(error.message));
            }
        })
            .pipe(res);
    });
    return app;
}
exports.getApp = getApp;
/**
 * Determines whether a protocol requires HTTPS
 * @param {string} protocol Protocol to evaluate.
 * @returns {boolean}
 */
function isHttps(protocol) {
    return ['https/1.1', 'https', 'h2'].indexOf(protocol) > -1;
}
/**
 * Gets the URLs for the main and component pages
 * @param {ServerOptions} options
 * @returns {{serverUrl: {protocol: string, hostname: string, port: string},
 * componentUrl: url.Url}}
 */
function getServerUrls(options, server) {
    options = applyDefaultServerOptions(options);
    const address = assertNotString(server.address());
    const serverUrl = {
        protocol: isHttps(options.protocol) ? 'https' : 'http',
        hostname: address.address,
        port: String(address.port),
    };
    const componentUrl = Object.assign({}, serverUrl);
    componentUrl.pathname = `${options.componentUrl}/${options.packageName}/`;
    return { serverUrl, componentUrl };
}
exports.getServerUrls = getServerUrls;
/**
 * Asserts that Node version is valid for h2 protocol
 * @param {ServerOptions} options
 */
function assertNodeVersion(options) {
    if (options.protocol === 'h2') {
        const matches = /(\d+)\./.exec(process.version);
        if (matches) {
            const major = Number(matches[1]);
            assert(major >= 5, 'h2 requires ALPN which is only supported in node.js >= 5.0');
        }
    }
}
/**
 * Creates an HTTP(S) server
 * @param app
 * @param {ServerOptions} options
 * @returns {Promise<http.Server>} Promise of server
 */
function createServer(app, options) {
    return __awaiter(this, void 0, void 0, function* () {
        // tslint:disable-next-line: no-any bad typings
        const opt = { spdy: { protocols: [options.protocol] } };
        if (isHttps(options.protocol)) {
            const keys = yield tls_1.getTLSCertificate(options.keyPath, options.certPath);
            opt.key = keys.key;
            opt.cert = keys.cert;
        }
        else {
            opt.spdy.plain = true;
            opt.spdy.ssl = false;
        }
        return http.createServer(opt, app);
    });
}
// Sauce Labs compatible ports taken from
// https://wiki.saucelabs.com/display/DOCS/Sauce+Connect+Proxy+FAQS#SauceConnectProxyFAQS-CanIAccessApplicationsonlocalhost?
// - 80, 443, 888: these ports must have root access
// - 5555, 8080: not forwarded on Android
const SAUCE_PORTS = [
    8081, 8000, 8001, 8003, 8031,
    2000, 2001, 2020, 2109, 2222, 2310, 3000, 3001, 3030, 3210, 3333,
    4000, 4001, 4040, 4321, 4502, 4503, 4567, 5000, 5001, 5050, 5432,
    6000, 6001, 6060, 6666, 6543, 7000, 7070, 7774, 7777, 8765, 8777,
    8888, 9000, 9001, 9080, 9090, 9876, 9877, 9999, 49221, 55001
];
/**
 * Starts an HTTP(S) server serving the given app.
 */
function startWithApp(options, app) {
    return __awaiter(this, void 0, void 0, function* () {
        options = applyDefaultServerOptions(options);
        const ports = options.port ? [options.port] : SAUCE_PORTS;
        const server = yield startWithFirstAvailablePort(options, app, ports);
        const urls = getServerUrls(options, server);
        open_browser_1.openBrowser(options, urls.serverUrl, urls.componentUrl);
        return server;
    });
}
exports.startWithApp = startWithApp;
function startWithFirstAvailablePort(options, app, ports) {
    return __awaiter(this, void 0, void 0, function* () {
        for (const port of ports) {
            const server = yield tryStartWithPort(options, app, port);
            if (server) {
                return server;
            }
        }
        throw new Error(`No available ports. Ports tried: ${JSON.stringify(ports)}`);
    });
}
function tryStartWithPort(options, app, port) {
    return __awaiter(this, void 0, void 0, function* () {
        const server = yield createServer(app, options);
        return new Promise((resolve, _reject) => {
            server.listen(port, options.hostname, () => {
                resolve(server);
            });
            server.on('error', (_err) => {
                resolve(null);
            });
        });
    });
}
// TODO(usergenic): Something changed in the typings of net.Server.address() in
// that it can now return AddressInfo OR string.  I don't know the circumstances
// where the the address() returns a string or how to handle it, so I made this
// assert function when calling on the address to fix compilation errors and
// have a runtime error as soon as the address is fetched.
function assertNotString(value) {
    assert(typeof value !== 'string');
    return value;
}
exports.assertNotString = assertNotString;
//# sourceMappingURL=start_server.js.map