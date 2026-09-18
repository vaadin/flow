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
const events = require("events");
const _ = require("lodash");
const config = require("./config");
const plugin_1 = require("./plugin");
const JSON_MATCHER = 'wct.conf.json';
const CONFIG_MATCHER = 'wct.conf.*';
/**
 * Exposes the current state of a WCT run, and emits events/hooks for anyone
 * downstream to listen to.
 *
 * TODO(rictic): break back-compat with plugins by moving hooks entirely away
 *     from callbacks to promises. Easiest way to do this would be to rename
 *     the hook-related methods on this object, so that downstream callers would
 *     break in obvious ways.
 *
 * @param {Object} options Any initially specified options.
 */
class Context extends events.EventEmitter {
    constructor(options) {
        super();
        this._hookHandlers = {};
        options = options || {};
        let matcher;
        if (options.configFile) {
            matcher = options.configFile;
        }
        else if (options.enforceJsonConf) {
            matcher = JSON_MATCHER;
        }
        else {
            matcher = CONFIG_MATCHER;
        }
        /**
         * The configuration for the current WCT run.
         *
         * We guarantee that this object is never replaced (e.g. you are free to
         * hold a reference to it, and make changes to it).
         */
        this.options = config.merge(config.defaults(), config.fromDisk(matcher, options.root), options);
    }
    // Hooks
    //
    // In addition to emitting events, a context also exposes "hooks" that
    // interested parties can use to inject behavior.
    /**
     * Registers a handler for a particular hook. Hooks are typically configured
     * to run _before_ a particular behavior.
     */
    hook(name, handler) {
        this._hookHandlers[name] = this._hookHandlers[name] || [];
        this._hookHandlers[name].unshift(handler);
    }
    /**
     * Registers a handler that will run after any handlers registered so far.
     *
     * @param {string} name
     * @param {function(!Object, function(*))} handler
     */
    hookLate(name, handler) {
        this._hookHandlers[name] = this._hookHandlers[name] || [];
        this._hookHandlers[name].push(handler);
    }
    emitHook(name, ...args) {
        return __awaiter(this, void 0, void 0, function* () {
            this.emit('log:debug', 'hook:', name);
            const hooks = (this._hookHandlers[name] || []);
            let boundHooks;
            let done = (_err) => { };
            let argsEnd = args.length - 1;
            if (args[argsEnd] instanceof Function) {
                done = args[argsEnd];
                argsEnd = argsEnd--;
            }
            const hookArgs = args.slice(0, argsEnd + 1);
            // Not really sure what's going on with typings here.
            boundHooks = hooks.map((hook) => hook.bind.apply(hook, [null].concat(hookArgs)));
            if (!boundHooks) {
                boundHooks = hooks;
            }
            // A hook may return a promise or it may call a callback. We want to
            // treat hooks as though they always return promises, so this converts.
            const hookToPromise = (hook) => {
                return new Promise((resolve, reject) => {
                    const maybePromise = hook((err) => {
                        if (err) {
                            reject(err);
                        }
                        else {
                            resolve();
                        }
                    });
                    if (maybePromise) {
                        maybePromise.then(resolve, reject);
                    }
                });
            };
            // We execute the handlers _sequentially_. This may be slower, but it gives
            // us a lighter cognitive load and more obvious logs.
            try {
                for (const hook of boundHooks) {
                    yield hookToPromise(hook);
                }
            }
            catch (err) {
                // TODO(rictic): stop silently swallowing the error here and just below.
                //     Looks like we'll need to track down some error being thrown from
                //     deep inside the express router.
                try {
                    done(err);
                }
                catch (_) {
                }
                throw err;
            }
            try {
                done();
            }
            catch (_) {
            }
        });
    }
    /**
     * @param {function(*, Array<!Plugin>)} done Asynchronously loads the plugins
     *     requested by `options.plugins`.
     */
    plugins() {
        return __awaiter(this, void 0, void 0, function* () {
            const plugins = [];
            for (const name of this.enabledPlugins()) {
                plugins.push(yield plugin_1.Plugin.get(name));
            }
            return plugins;
        });
    }
    /**
     * @return {!Array<string>} The names of enabled plugins.
     */
    enabledPlugins() {
        // Plugins with falsy configuration or disabled: true are _not_ loaded.
        const pairs = _.reject(_.pairs(this.options.plugins), (p) => !p[1] || p[1].disabled);
        return _.map(pairs, (p) => p[0]);
    }
    /**
     * @param {string} name
     * @return {!Object}
     */
    pluginOptions(name) {
        return this.options.plugins[plugin_1.Plugin.shortName(name)];
    }
}
Context.Context = Context;
exports.Context = Context;
module.exports = Context;
