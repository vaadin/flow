export interface FlowConfig {
  imports ?: () => void;
}

interface AppConfig {
  productionMode: boolean;
  appId: string;
  uidl: object;
  webComponentMode: boolean;
}

interface AppInitResponse {
  appConfig: AppConfig;
  pushScript?: string;
}

interface HTMLRouterContainer extends HTMLElement {
  onBeforeEnter ?: (ctx: NavigationParameters, cmd: NavigationCommands) => Promise<any>;
  onBeforeLeave ?: (ctx: NavigationParameters, cmd: NavigationCommands) => Promise<any>;
  serverConnected ?: (cancel: boolean) => void;
}

interface FlowRoute {
  action : (params: NavigationParameters) => Promise<HTMLRouterContainer>;
  path: string;
}

interface FlowRoot {
  $: any ;
  $server: any;
}

export interface NavigationParameters {
  pathname: string;
  search: string;
}

export interface NavigationCommands {
  prevent: () => any;
}

const $wnd = window as any;
let isActive = false;

/**
 * Client API for flow UI operations.
 */
export class Flow {
  config: FlowConfig;
  response ?: AppInitResponse;
  pathname = '';
  // flow uses body for keeping references
  flowRoot : FlowRoot = document.body as any;
  // @ts-ignore
  container : HTMLRouterContainer;

  private baseRegex = /^\//;


  constructor(config?: FlowConfig) {
    this.flowRoot.$ = this.flowRoot.$ || {};
    this.config = config || {};

    // TB checks for the existence of window.Vaadin.Flow in order
    // to consider that TB needs to wait for `initFlow()`.
    $wnd.Vaadin = $wnd.Vaadin || {};
    if (!$wnd.Vaadin.Flow) {
      $wnd.Vaadin.Flow = {
        clients: {
          TypeScript: {
            isActive: () => isActive
          }
        }
      };
    }

    // Regular expression used to remove the app-context
    const elm = document.head.querySelector('base');
    this.baseRegex = new RegExp('^' +
      // IE11 does not support document.baseURI
      (document.baseURI || elm && elm.href || '/')
        .replace(/^https?:\/\/[^\/]+/i, ''));
  }

  /**
   * Return a `route` object for vaadin-router in an one-element array.
   *
   * The `FlowRoute` object `path` property handles any route,
   * and the `action` returns the flow container without updating the content,
   * delaying the actual Flow server call to the `onBeforeEnter` phase.
   *
   * This is a specific API for its use with `vaadin-router`.
   */
  get serverSideRoutes(): [FlowRoute] {
    return [{
      path: '(.*)',
      action: this.action
    }];
  }

  private get action(): (params: NavigationParameters) => Promise<HTMLRouterContainer> {
    // Return a function which is bound to the flow instance, thus we can use
    // the syntax `...serverSideRoutes` in vaadin-router.
    // @ts-ignore
    return async (params: NavigationParameters) => {
      // flag used to inform Testbench whether a server route is in progress
      isActive = true;

      // Store last action pathname so as we can check it in events
      this.pathname = params.pathname;

      await this.flowInit();
      // When an action happens, navigation will be resolved `onBeforeEnter`
      this.container.onBeforeEnter = (ctx, cmd) => this.flowNavigate(ctx, cmd);
      // For covering the 'server -> client' use case
      this.container.onBeforeLeave = (ctx, cmd) => this.flowLeave(ctx, cmd);
      return this.container;
    }
  }

  // Send a remote call to `JavaScriptBootstrapUI` to check
  // whether navigation has to be cancelled.
  private async flowLeave(
    // @ts-ignore
    ctx: NavigationParameters,
    cmd?: NavigationCommands): Promise<any> {

    // server -> server
    if (this.pathname === ctx.pathname) {
      return Promise.resolve({});
    }
    // 'server -> client'
    return new Promise(resolve => {
      // The callback to run from server side to cancel navigation
      this.container.serverConnected = cancel => {
        resolve(cmd && cancel ? cmd.prevent() : {});
        // Make Testbench know that server request has finished
        isActive = false;
      }

      // Call server side to check whether we can leave the view
      this.flowRoot.$server.leaveNavigation(this.getFlowRoute(ctx));
    });
  }

  // Send the remote call to `JavaScriptBootstrapUI` to render the flow
  // route specified by the context
  private async flowNavigate(ctx: NavigationParameters, cmd?: NavigationCommands): Promise<HTMLElement> {
    return new Promise(resolve => {
      // The callback to run from server side once the view is ready
      this.container.serverConnected = cancel => {
        if (cmd && cancel) {
          resolve(cmd.prevent());
        } else {
          this.container.style.display = '';
          resolve(this.container);
        }
        // Make Testbench know that navigation finished
        isActive = false;
      };

      // Call server side to navigate to the given route
      this.flowRoot.$server
        .connectClient(this.container.localName, this.container.id, this.getFlowRoute(ctx));
    });
  }

  private getFlowRoute(context: NavigationParameters | Location): string {
    return (context.pathname + (context.search || '')).replace(this.baseRegex, '');
  }

  // import flow client modules and initialize UI in server side.
  private async flowInit(serverSideRouting = false): Promise<AppInitResponse> {
    // Do not start flow twice
    if (!this.response) {
      // Initialize server side UI
      this.response = await this.flowInitUi(serverSideRouting);

      // Enable or disable server side routing
      this.response.appConfig.webComponentMode = !serverSideRouting;

      const {pushScript, appConfig} = this.response;

      if (typeof pushScript === 'string') {
        await this.loadScript(pushScript);
      }
      const {appId} = appConfig;

      // Load bootstrap script with server side parameters
      const bootstrapMod = await import('./FlowBootstrap');
      await bootstrapMod.init(this.response);

      // Load custom modules defined by user
      if (typeof this.config.imports === 'function') {
        this.injectAppIdScript(appId);
        await this.config.imports();
      }

      // Load flow-client module
      const clientMod = await import('./FlowClient');
      await this.flowInitClient(clientMod);

      // When client-side router, create a container for server views
      if (!serverSideRouting) {
        // we use a custom tag for the flow app container
        const tag = `flow-container-${appId.toLowerCase()}`;
        this.container = this.flowRoot.$[appId] = document.createElement(tag);
        this.container.id = appId;

        // It might be that components created from server expect that their content has been rendered.
        // Appending eagerly the container we avoid these kind of errors.
        // Note that the client router will move this container to the outlet if the navigation succeed
        this.container.style.display = 'none';
        document.body.appendChild(this.container);
      }
    }
    return this.response;
  }

  private async loadScript(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.onload = () => resolve();
      script.onerror = reject;
      script.src = url;
      document.body.appendChild(script);
    });
  }

  private injectAppIdScript(appId: string) {
    const appIdWithoutHashCode = appId.substring(0, appId.lastIndexOf('-'));
    const scriptAppId = document.createElement('script');
    scriptAppId.type = 'module';
    scriptAppId.setAttribute('data-app-id', appIdWithoutHashCode);
    document.body.append(scriptAppId);
  }

  // After the flow-client javascript module has been loaded, this initializes flow UI
  // in the browser.
  private async flowInitClient(clientMod: any): Promise<void> {
    clientMod.init();
    // client init is async, we need to loop until initialized
    return new Promise(resolve => {
      const intervalId = setInterval(() => {
        // client `isActive() == true` while initializing or processing
        const initializing = Object.keys($wnd.Vaadin.Flow.clients)
          .filter(key => key !== 'TypeScript')
          .reduce((prev, id) => prev || $wnd.Vaadin.Flow.clients[id].isActive(), false);
        if (!initializing) {
          clearInterval(intervalId);
          resolve();
        }
      }, 5);
    });
  }

  // Returns the `appConfig` object
  private async flowInitUi(serverSideRouting: boolean): Promise<AppInitResponse> {
    // appConfig was sent in the index.html request
    const initial = $wnd.Vaadin && $wnd.Vaadin.TypeScript && $wnd.Vaadin.TypeScript.initial;
    if (initial) {
      $wnd.Vaadin.TypeScript.initial = undefined;
      return Promise.resolve(initial);
    }

    // send a request to the `JavaScriptBootstrapHandler`
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const httpRequest = xhr as any;
      const currentPath = location.pathname || '/';
      const requestPath = `${currentPath}?v-r=init` +
            (serverSideRouting ? `&location=${encodeURI(this.getFlowRoute(location))}` : '');

      httpRequest.open('GET', requestPath);

      httpRequest.onerror = () => reject(new Error(
        `Invalid server response when initializing Flow UI.
        ${httpRequest.status}
        ${httpRequest.responseText}`));

      httpRequest.onload = () => {
        if (httpRequest.getResponseHeader('content-type') === 'application/json') {
          resolve(JSON.parse(httpRequest.responseText));
        } else {
          httpRequest.onerror();
        }
      };
      httpRequest.send();
    });
  }
}
