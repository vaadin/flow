

declare module 'wd' {
  interface NodeCB<T> {
    (err: any, value: T): void;
  }
  export interface Browser {
    configureHttp(options: {
      retries: number
    }): void;
    attach(sessionId: string, callback: NodeCB<Capabilities>): void;
    init(capabilities: Capabilities, callback: NodeCB<string>): void;

    get(url: string, callback: NodeCB<void>): void;
    quit(callback: NodeCB<void>): void;

    on(eventName: string, handler: Function): void;
  }
  export interface Capabilities {
    /** The name of the browser being used */
    browserName: 'android'|'chrome'|'firefox'|'htmlunit'|'internet explorer'|'iPhone'|'iPad'|'opera'|'safari'|'phantomjs';
    /** The browser version, or the empty string if unknown. */
    version: string;
    /** A key specifying which platform the browser should be running on. */
    platform?: 'WINDOWS'|'XP'|'VISTA'|'MAC'|'LINUX'|'UNIX'|'ANDROID'|'ANY';

    /** Whether the session can interact with modal popups,
     *  such as window.alert and window.confirm. */
    handlesAlerts?: boolean;
    /** Whether the session supports CSS selectors when searching for elements. */
    cssSelectorsEnabled?: boolean;

    webdriver?: {
      remote: {
        quietExceptions: boolean;
      }
    };

    selenium?: {
      server: {
        url: string;
      }
    };

    chromeOptions?: {
      binary?: string;
      args?: string[];
    };
    firefox_binary?: string;
    marionette?: boolean;
    'moz:firefoxOptions'?: {
      args?: string[];
    },
    'safari.options'?: {
      skipExtensionInstallation?: boolean;
    };
    'phantomjs.binary.path'?: string;
  }

  export type ValidHost =
      string |
      {hostname: string, port?: number,
       auth?: string, path?: string, } |
      {host: string, port?: number,
       username?: string, accesskey?: string, path?: string, };

  export function remote(hostnameOrUrl: ValidHost): Browser;
}
