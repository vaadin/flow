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
/// <reference types="node" />
import * as express from 'express';
import * as http from 'spdy';
import * as url from 'url';
export interface ServerOptions {
    /** The root directory to serve **/
    root?: string;
    /**
     * The path on disk of the entry point HTML file that will be served for
     * app-shell style projects. Must be contained by `root`. Defaults to
     * `index.html`.
     */
    entrypoint?: string;
    /** Whether or not to compile JavaScript **/
    compile?: 'always' | 'never' | 'auto';
    /** Resolution algorithm to use for rewriting module specifiers */
    moduleResolution?: 'none' | 'node';
    /** The port to serve from */
    port?: number;
    /** The hostname to serve from */
    hostname?: string;
    /** Headers to send with every response */
    headers?: {
        [name: string]: string;
    };
    /** Whether to open the browser when run **/
    open?: boolean;
    /** The browser(s) to open when run with open argument **/
    browser?: string[];
    /** The URL path to open in each browser **/
    openPath?: string;
    /** The component directory to use **/
    componentDir?: string;
    /** The component url to serve **/
    componentUrl?: string;
    /** The package name to use for the root directory **/
    packageName?: string;
    /**
     * Sets npm mode: component directory is 'node_modules' and the package name
     * is read from package.json.
     */
    npm?: boolean;
    /** The HTTP protocol to use */
    protocol?: string;
    /** Path to TLS service key for HTTPS */
    keyPath?: string;
    /** Path to TLS certificate for HTTPS */
    certPath?: string;
    /** Path to H2 push-manifest file */
    pushManifestPath?: string;
    /** Proxy to redirect for all matching `path` to `target` */
    proxy?: {
        path: string;
        target: string;
    };
    /** Sets the value of the Access-Control-Allow-Origin header */
    allowOrigin?: string;
    /**
     * An optional list of routes & route handlers to attach to the polyserve
     * app, to be handled before all others
     */
    additionalRoutes?: Map<string, express.RequestHandler>;
}
export declare type ExpressAppMapper = (app: express.Express, options: ServerOptions) => Promise<express.Express>;
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
export declare function startServer(options: ServerOptions, appMapper?: ExpressAppMapper): Promise<http.Server>;
export declare type ServerInfo = MainlineServer | VariantServer | ControlServer;
export interface PolyserveServer {
    kind: 'control' | 'mainline' | 'variant';
    server: http.Server;
    app: express.Application;
    options: ServerOptions;
}
/**
 * The `default` or `primary` server. If only one ServerInfo is returned from
 * startServers it must be a MainlineServer. This is the server that's running
 * with the default configuration and not running a variant configuration.
 */
export interface MainlineServer extends PolyserveServer {
    kind: 'mainline';
}
/**
 * These are servers which are running some named variant configuration. For
 * multiple variant dependency directories are detected/configured, there will
 * be one MainlineServer that serves out the default dependency directory, and
 * one VariantServer for each other dependency directory.
 */
export interface VariantServer extends PolyserveServer {
    kind: 'variant';
    variantName: string;
    dependencyDir: string;
}
/**
 * If more than one server is started by startServers, the main port will serve
 * out a control server. This server serves out an HTML interface that
 * describes the other servers which have been started, and provides convenience
 * links to them.
 */
export interface ControlServer extends PolyserveServer {
    kind: 'control';
}
export interface MultipleServersInfo {
    kind: 'MultipleServers';
    mainline: MainlineServer;
    variants: VariantServer[];
    control: ControlServer;
    servers: PolyserveServer[];
}
export declare type StartServerResult = MainlineServer | MultipleServersInfo;
/**
 * Starts one or more web servers, based on the given options and
 * variant bower_components directories that are found in the root dir.
 */
export declare function startServers(options: ServerOptions, appMapper?: ExpressAppMapper): Promise<StartServerResult>;
export declare function startControlServer(options: ServerOptions, mainlineInfo: MainlineServer, variantInfos: VariantServer[]): Promise<ControlServer>;
export declare function getApp(options: ServerOptions): express.Express;
/**
 * Gets the URLs for the main and component pages
 * @param {ServerOptions} options
 * @returns {{serverUrl: {protocol: string, hostname: string, port: string},
 * componentUrl: url.Url}}
 */
export declare function getServerUrls(options: ServerOptions, server: http.Server): {
    serverUrl: url.Url;
    componentUrl: url.Url;
};
/**
 * Starts an HTTP(S) server serving the given app.
 */
export declare function startWithApp(options: ServerOptions, app: express.Application): Promise<http.Server>;
export declare function assertNotString<T>(value: string | T): T;
