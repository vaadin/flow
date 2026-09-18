/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
import { BrowserCapability } from 'browser-capabilities';
export declare type JsCompileTarget = 'es5' | 'es2015' | 'es2016' | 'es2017' | 'es2018';
export interface ProjectBuildOptions {
    /**
     * The name of this build, used to determine the output directory name.
     */
    name?: string;
    /**
     * A build preset for this build. A build can inherit some base configuration
     * from a named preset.
     */
    preset?: string;
    /**
     * Generate a service worker for your application to cache all files and
     * assets on the client.
     *
     * Polymer CLI will generate a service worker for your build using the
     * [sw-precache library](https://github.com/GoogleChrome/sw-precache). To
     * customize your service worker, create a sw-precache-config.js file in your
     * project directory that exports your configuration. See the [sw-precache
     * README](https://github.com/GoogleChrome/sw-precache) for a list of all
     * supported options.
     *
     * Note that the sw-precache library uses a cache-first strategy for maximum
     * speed and makes some other assumptions about how your service worker should
     * behave. Read the "Considerations" section of the sw-precache README to make
     * sure that this is suitable for your application.
     */
    addServiceWorker?: boolean;
    /**
     * If `true`, generate an [HTTP/2 Push
     * Manifest](https://github.com/GoogleChrome/http2-push-manifest) for your
     * application.
     */
    addPushManifest?: boolean;
    /**
     * A config file that's passed to the [sw-precache
     * library](https://github.com/GoogleChrome/sw-precache). See [its
     * README](https://github.com/GoogleChrome/sw-precache) for details of the
     * format of this file.
     *
     * Ignored if `addServiceWorker` is not `true`.
     *
     * Defaults to `"sw-precache-config.js`.
     */
    swPrecacheConfig?: string;
    /**
     * Insert prefetch link elements into your fragments so that all dependencies
     * are prefetched immediately. Add dependency prefetching by inserting `<link
     * rel="prefetch">` tags into entrypoint and `<link rel="import">` tags into
     * fragments and shell for all dependencies.
     *
     * Note this option may trigger duplicate requests. See
     * https://github.com/Polymer/polymer-build/issues/239 for details.
     */
    insertPrefetchLinks?: boolean;
    /**
     * By default, fragments are unbundled. This is optimal for HTTP/2-compatible
     * servers and clients.
     *
     * If the --bundle flag is supplied, all fragments are bundled together to
     * reduce the number of file requests. This is optimal for sending to clients
     * or serving from servers that are not HTTP/2 compatible.
     */
    bundle?: boolean | {
        /** URLs of files and/or folders that should not be inlined. */
        excludes?: string[];
        /** Inline external CSS file contents into <style> tags. */
        inlineCss?: boolean;
        /** Inline external Javascript file contents into <script> tags. */
        inlineScripts?: boolean;
        /** Rewrite element attributes inside of templates when inlining html. */
        rewriteUrlsInTemplates?: boolean;
        /** Create identity source maps for inline scripts. */
        sourcemaps?: boolean;
        /**
         * Remove all comments except those tagged '@license', or starting with
         * `<!--!` or `<!--#`, when true.
         */
        stripComments?: boolean;
        /** Remove unreachable/unused code when bundling. */
        treeshake?: boolean;
    };
    /** Options for processing HTML. */
    html?: {
        /** Minify HTMl by removing comments and whitespace. */
        minify?: boolean | {
            /** HTML files listed here will not be minified. */
            exclude?: string[];
        };
    };
    /** Options for processing CSS. */
    css?: {
        /** Minify inlined and external CSS. */
        minify?: boolean | {
            /** CSS files listed here will not be minified. */
            exclude?: string[];
        };
    };
    /** Options for processing JavaScript. */
    js?: {
        /** Minify inlined and external JavaScript. */
        minify?: boolean | {
            /** JavaScript files listed here will not be minified. */
            exclude?: string[];
        };
        /** Use babel to compile all ES6 JS down to ES5 for older browsers. */
        compile?: boolean | JsCompileTarget | {
            target?: JsCompileTarget;
            /** JavaScript files listed here will not be compiled. */
            exclude?: string[];
        };
        /** Transform ES modules to AMD modules. */
        transformModulesToAmd?: boolean;
    };
    /**
     * Capabilities required for a browser to consume this build. Values include
     * `es2015` and `push`. See canonical list at:
     * https://github.com/Polymer/prpl-server-node/blob/master/src/capabilities.ts
     *
     * This field is purely a hint to servers reading this configuration, and
     * does not affect the build process. A server supporting differential
     * serving (e.g. prpl-server) can use this field to help decide which build
     * to serve to a given user agent.
     */
    browserCapabilities?: BrowserCapability[];
    /**
     * Update the entrypoint's `<base>` tag, to support serving this build from a
     * non-root path, such as when doing differential serving based on user
     * agent. Requires that a `<base>` tag already exists. This works well in
     * conjunction with the convention of using relative URLs for static
     * resources and absolute URLs for application routes.
     *
     * If `true`, use the build `name`. If a `string`, use that value.
     * Leading/trailing slashes are optional.
     */
    basePath?: boolean | string;
}
export declare const buildPresets: Map<string, ProjectBuildOptions>;
export declare function isValidPreset(presetName: string): boolean;
/**
 * Apply a build preset (if a valid one exists on the config object) by
 * deep merging the given config with the preset values.
 */
export declare function applyBuildPreset(config: ProjectBuildOptions): ProjectBuildOptions;
