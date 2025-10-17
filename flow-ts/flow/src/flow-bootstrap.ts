// This is a copy of the regular `BootstrapHandler.js` in the flow-server
// module, but with the following modifications:
// - The main function is exported as an ES module for lazy initialization.
// - Application configuration is passed as a parameter instead of using
//   replacement placeholders as in the regular bootstrapping.
// - It reuses `Vaadin.Flow.clients` if exists.
// - Fixed lint errors.

// Global type declarations
declare global {
  interface FlowClient {
    isActive(): boolean;
    initializing: boolean;
    productionMode: boolean;
  }

  interface App {
    getConfig(name: keyof AppConfig): unknown;
  }

  interface Flow {
    clients: Readonly<Record<string, FlowClient | undefined>>;
    initApplication(appId: string, config: AppConfig): App;
    getAppIds(): string[];
    getApp(appId: string): App | undefined;
    registerWidgetset(widgetset: string, callback: (appId: string) => void): void;
    getBrowserDetailsParameters(): Record<string, string>;
    tryCatchWrapper?<T extends (this: object, ...args: readonly unknown[]) => unknown>(
      originalFunction: T,
      component: string
    ): (this: object, ...args: Parameters<T>) => ReturnType<T> | undefined;
    gwtStatsEvents?: unknown[];
  }

  interface Vaadin {
    Flow: Flow;
  }

  interface Window {
    __gwtStatsEvent?(event: unknown): boolean;
  }
}

export interface AppVersionInfo {
  atmosphereVersion: string;
  vaadinVersion: string;
}

export interface SessionExpirationMessage {
  caption: string | null;
  message: string | null;
  url: string | null;
}

export interface LazyConfig {
  mode: string;
  type: string;
  url: string;
}

// TODO: Finalize the UIDL interface
// export interface UIDL {
//   clientId: number;
//   LAZY: readonly LazyConfig[];
//   constants;
//   changes;
//   timings;
//   syncId;
//   'Vaadin-Security-Key';
//   'Vaadin-Push-ID';
// }

export interface AppConfig {
  versionInfo: AppVersionInfo;
  sessExpMsg: SessionExpirationMessage;
  contextRootUrl: string;
  debug: boolean;
  requestTiming: boolean;
  heartbeatInterval: number;
  maxMessageSuspendTimeout: number;
  'v-uiId': number;
  requestURL: string;
  productionMode: boolean;
  appId: string;
  uidl: Readonly<Record<string, unknown>>;
}

export interface Stat {
  is: string;
  version: string;
}

export interface AppInitResponse {
  appConfig: AppConfig;
  errors: readonly string[] | null;
  stats: Readonly<Record<string, Stat>>;
}

type WidgetsetConfig = {
  callback?(appId: string): void;
  pendingApps: string[] | null;
};

type BrowserParams = Record<string, string | number | boolean>;

function init(appInitResponse: AppInitResponse): void {
  // Note: This function intentionally uses any for window.Vaadin.Flow to maintain
  // compatibility with the original JavaScript implementation
  const apps: Record<string, App | undefined> = {};
  const widgetsets: Record<string, WidgetsetConfig | undefined> = {};

  let log: (...args: unknown[]) => void;
  // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  if (typeof console === undefined || !/[&?]debug(&|$)/u.exec(location.search)) {
    // If no console.log present, just use a no-op
    log = () => {};
  } else {
    // If it's a function, use it with apply
    log = console.log.bind(console);
  }

  function isInitializedInDom(appId: string) {
    const appDiv = document.getElementById(appId);
    if (!appDiv) {
      return false;
    }

    for (const element of appDiv.children) {
      // If the app div contains a child with the class
      // 'v-app-loading' we have only received the HTML
      // but not yet started the widget set
      // (UIConnector removes the v-app-loading div).
      if (element.classList.contains('v-app-loading')) {
        return false;
      }
    }

    for (const { className } of appDiv.children) {
      // If the app div contains a child with the class
      // 'v-app-loading' we have only received the HTML
      // but not yet started the widget set
      // (UIConnector removes the v-app-loading div).
      if (className.includes('v-app-loading')) {
        return false;
      }
    }
    return true;
  }

  Object.assign(window.Vaadin.Flow, {
    /**
     * Needed for wrapping custom javascript functionality in the components (i.e. connectors)
     */
    tryCatchWrapper<T extends (this: object, ...args: readonly unknown[]) => unknown>(
      originalFunction: T,
      component: string
    ): (this: object, ...args: Parameters<T>) => ReturnType<T> | undefined {
      return function wrapper(this: object, ...args: Parameters<T>): ReturnType<T> | undefined {
        try {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-type-assertion
          return originalFunction.apply(this, args) as ReturnType<T>;
        } catch (error: unknown) {
          console.error(
            `There seems to be an error in ${component}:
${error instanceof Error ? error.message : String(error)}
Please submit an issue to https://github.com/vaadin/flow-components/issues/new/choose`
          );
          return undefined;
        }
      };
    }
  });

  /*
   * Needed for Testbench compatibility, but prevents any Vaadin 7 app from
   * bootstrapping unless the legacy vaadinBootstrap.js file is loaded before
   * this script.
   */
  // window.Vaadin = window.Vaadin || {};
  // window.Vaadin.Flow = window.Vaadin.Flow || {};

  // Initialize Flow if not already present
  if (typeof window.Vaadin.Flow.initApplication === 'undefined') {
    Object.assign(window.Vaadin.Flow, {
      initApplication(appId: string, config: AppConfig): App {
        const testbenchId = appId.replace(/-\d+$/u, '');

        if (apps[appId]) {
          if (window.Vaadin.Flow.clients[testbenchId]?.initializing) {
            throw new Error('Application ' + appId + ' is already being initialized');
          }
          if (isInitializedInDom(appId)) {
            if (appInitResponse.appConfig.productionMode) {
              throw new Error('Application ' + appId + ' already initialized');
            }

            // Remove old contents for Flow
            const appDiv = document.getElementById(appId);
            if (appDiv) {
              Array.from(appDiv.childNodes).forEach((child) => child.remove());
            }

            // For devMode reset app config and restart widgetset as client
            // is up and running after hrm update.
            const getConfig = (name: keyof AppConfig): unknown => config[name];

            // Export public data
            const app = {
              getConfig
            };
            apps[appId] = app;

            widgetsets.client ??= { pendingApps: [] };
            if (widgetsets.client.callback) {
              log('Starting from bootstrap', appId);
              widgetsets.client.callback(appId);
            } else {
              log('Setting pending startup', appId);
              widgetsets.client.pendingApps?.push(appId);
            }
            return apps[appId];
          }
        }

        log('init application', appId, config);

        Object.assign(Vaadin.Flow.clients, {
          [testbenchId]: {
            isActive() {
              return true;
            },
            initializing: true,
            productionMode: mode
          }
        });

        const getConfig = (name: keyof AppConfig): unknown => config[name];

        // Export public data
        const app = { getConfig };
        apps[appId] = app;

        if (!window.name) {
          window.name = appId + '-' + Math.random();
        }

        const widgetset = 'client';
        widgetsets[widgetset] = {
          pendingApps: []
        };
        if (widgetsets[widgetset].callback) {
          log('Starting from bootstrap', appId);
          widgetsets[widgetset].callback(appId);
        } else {
          log('Setting pending startup', appId);
          widgetsets[widgetset].pendingApps?.push(appId);
        }

        return app;
      },
      getAppIds(): readonly string[] {
        return Object.keys(apps);
      },
      getApp(appId: string): App | undefined {
        return apps[appId];
      },
      registerWidgetset(widgetset: string, callback: (appId: string) => void): void {
        log('Widgetset registered', widgetset);
        const ws = widgetsets[widgetset];
        if (ws?.pendingApps) {
          ws.callback = callback;
          for (const appId of ws.pendingApps) {
            log('Starting from register widgetset', appId);
            callback(appId);
          }
          ws.pendingApps = null;
        }
      },
      getBrowserDetailsParameters(): Record<string, string> {
        const params: BrowserParams = {};

        /* Screen height and width */
        params['v-sh'] = window.screen.height;
        params['v-sw'] = window.screen.width;
        /* Browser window dimensions */
        params['v-wh'] = window.innerHeight;
        params['v-ww'] = window.innerWidth;
        /* Body element dimensions */
        params['v-bh'] = document.body.clientHeight;
        params['v-bw'] = document.body.clientWidth;

        /* Current time */
        const date = new Date();
        params['v-curdate'] = date.getTime();

        /* Current timezone offset (including DST shift) */
        const tzo1 = date.getTimezoneOffset();

        /* Compare the current tz offset with the first offset from the end
         of the year that differs --- if less that, we are in DST, otherwise
         we are in normal time */
        let dstDiff = 0;
        let rawTzo = tzo1;
        for (let m = 12; m > 0; m--) {
          date.setUTCMonth(m);
          const tzo2 = date.getTimezoneOffset();
          if (tzo1 !== tzo2) {
            dstDiff = tzo1 > tzo2 ? tzo1 - tzo2 : tzo2 - tzo1;
            rawTzo = tzo1 > tzo2 ? tzo1 : tzo2;
            break;
          }
        }

        /* Time zone offset */
        params['v-tzo'] = tzo1;

        /* DST difference */
        params['v-dstd'] = dstDiff;

        /* Time zone offset without DST */
        params['v-rtzo'] = rawTzo;

        /* DST in effect? */
        params['v-dston'] = tzo1 !== rawTzo;

        /* Time zone id (if available) */
        try {
          params['v-tzid'] = Intl.DateTimeFormat().resolvedOptions().timeZone;
        } catch {
          params['v-tzid'] = '';
        }

        /* Window name */
        if (window.name) {
          params['v-wn'] = window.name;
        }

        /* Detect touch device support */
        let supportsTouch = false;
        try {
          document.createEvent('TouchEvent');
          supportsTouch = true;
        } catch {
          /* Chrome and IE10 touch detection */
          // @ts-expect-error: legacy
          supportsTouch = 'ontouchstart' in window || typeof navigator.msMaxTouchPoints !== 'undefined';
        }
        params['v-td'] = supportsTouch;

        /* Device Pixel Ratio */
        params['v-pr'] = window.devicePixelRatio;

        // eslint-disable-next-line @typescript-eslint/no-deprecated -- Legacy support required for compatibility
        if (navigator.platform) {
          // eslint-disable-next-line @typescript-eslint/no-deprecated -- Legacy support required for compatibility
          params['v-np'] = navigator.platform;
        }

        /* Stringify each value (they are parsed on the server side) */
        return Object.fromEntries(
          Object.entries(params)
            .filter(([, value]) => typeof value !== 'undefined')
            .map(([key, value]) => [key, value.toString()])
        );
      }
    });
  }

  log('Flow bootstrap loaded');

  if (appInitResponse.appConfig.productionMode && typeof window.__gwtStatsEvent !== 'function') {
    window.Vaadin.Flow.gwtStatsEvents = [];
    window.__gwtStatsEvent = (event: unknown): boolean => {
      window.Vaadin.Flow.gwtStatsEvents?.push(event);
      return true;
    };
  }

  const config = appInitResponse.appConfig;
  const mode = appInitResponse.appConfig.productionMode;
  window.Vaadin.Flow.initApplication(config.appId, config);
}

export { init };
