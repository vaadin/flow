// TODO(rictic): just import these directly rather than doing this

declare module 'wct' {
  import * as events from 'events';
  import * as http from 'http';
  import * as wd from 'wd';
  import * as express from 'express';

  type Handler = (o: {}, callback: (err: any) => void) => void;

  export interface BrowserDef extends wd.Capabilities {
    id: number;
    url: wd.ValidHost;
    sessionId: string;
    deviceName?: string;
  }

  export interface Stats {
    status: string;
    passing?: number;
    pending?: number;
    failing?: number;
  }

  export interface Config {
      suites: string[];
      output: NodeJS.WritableStream;
      ttyOutput: boolean;
      verbose: boolean;
      quiet?: boolean;
      expanded: boolean;
      root: string;
      testTimeout: number;
      persistent: boolean;
      extraScripts: string[];
      clientOptions: {
          root: string;
          verbose?: boolean;
      };
      activeBrowsers: BrowserDef[];
      browserOptions: {
          [name: string]: wd.Capabilities;
      };
      plugins: (string | boolean)[] | {
          [key: string]: ({
              disabled: boolean;
          } | boolean);
      };
      registerHooks: (wct: Context) => void;
      enforceJsonConf: boolean;
      webserver: {
          port: number;
          hostname: string;
          pathMappings: {
              [urlPath: string]: string;
          }[];
          urlPrefix: string;
          webRunnerPath?: string;
          webRunnerContent?: string;
          staticContent?: {
              [file: string]: string;
          };
      };
      skipPlugins?: boolean;
      sauce?: {};
      remote?: {};
      origSuites?: string[];
      skipCleanup?: boolean;
      simpleOutput?: boolean;
      skipUpdateCheck?: boolean;
  }

  /**
   * Exposes the current state of a WCT run, and emits events/hooks for anyone
   * downstream to listen to.
   *
   * @param {Object} options Any initially specified options.
   */
  export class Context extends events.EventEmitter {
      options: Config;
      private _hookHandlers;
      constructor(options: Config);
      /**
       * Registers a handler for a particular hook. Hooks are typically configured to
       * run _before_ a particular behavior.
       */
      hook(name: string, handler: Handler): void;
      /**
       * Registers a handler that will run after any handlers registered so far.
       *
       * @param {string} name
       * @param {function(!Object, function(*))} handler
       */
      hookLate(name: string, handler: Handler): void;
      /**
       * Once all registered handlers have run for the hook, your callback will be
       * triggered. If any of the handlers indicates an error state, any subsequent
       * handlers will be canceled, and the error will be passed to the callback for
       * the hook.
       *
       * Any additional arguments passed between `name` and `done` will be passed to
       * hooks (before the callback).
       *
       * @param {string} name
       * @param {function(*)} done
       * @return {!Context}
       */
      emitHook(name: 'prepare:webserver', app: express.Application, done: (err?: any) => void): Context;
      emitHook(name: 'configure', done: (err?: any) => void): Context;
      emitHook(name: 'prepare', done: (err?: any) => void): Context;
      emitHook(name: 'cleanup', done: (err?: any) => void): Context;
      emitHook(name: string, done: (err?: any) => void): Context;
      emitHook(name: string, ...args: any[]): Context;
      /**
       * @param {function(*, Array<!Plugin>)} done Asynchronously loads the plugins
       *     requested by `options.plugins`.
       */
      plugins(done: (err: any, plugins?: Plugin[]) => void): void;
      private _plugins();
      /**
       * @return {!Array<string>} The names of enabled plugins.
       */
      enabledPlugins(): string[];
      /**
       * @param {string} name
       * @return {!Object}
       */
      pluginOptions(name: string): any;
      static Context: typeof Context;
  }

  export interface Metadata {
    'cli-options': Config;
}
  export interface PluginInterface {
      (context: Context, pluginOptions: any, pluginMeta: Plugin): void;
  }
  /**
   * A WCT plugin. This constructor is private. Plugins can be retrieved via
   * `Plugin.get`.
   */
  export class Plugin {
      name: string;
      cliConfig: Config;
      packageName: string;
      metadata: Metadata;
      constructor(packageName: string, metadata: Metadata);
      /**
       * @param {!Context} context The context that this plugin should be evaluated
       *     within.
       * @param {function(*)} done
       */
      execute(context: Context, done: (message?: string) => void): void;
      /**
       * Retrieves a plugin by shorthand or module name (loading it as necessary).
       *
       * @param {string} name
       * @param {function(*, Plugin)} done
       */
      static get(name: string, done: (err: any, plugin?: Plugin) => void): void;
      /**
       * @param {string} name
       * @return {string} The short form of `name`.
       */
      static shortName(name: string): string;
      static Plugin: typeof Plugin;
  }

}
