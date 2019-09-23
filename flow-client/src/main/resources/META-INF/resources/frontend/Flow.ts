export interface FlowConfig {
  imports ?: () => void;
}

interface AppConfig {
  productionMode: boolean,
  appId: string,
  uidl: object,
  webComponentMode: boolean
}

interface AppInitResponse {
  appConfig: AppConfig;
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

  constructor(config?: FlowConfig) {
    this.flowRoot.$ = this.flowRoot.$ || {};
    this.config = config || {};
  }

  /**
   * Initialize flow in full page mode and with server side routing.
   */
  async start(): Promise<void> {
    await this.flowInit(true);
  }

  /**
   * Go to a route defined in server.
   *
   * This is a generic API for non `vaadin-router` applications.
   */
  get navigate(): (params: NavigationParameters) => Promise<HTMLElement> {
    // Return a function which is bound to the flow instance
    return async (params: NavigationParameters) => {
      await this.flowInit();
      delete this.container.onBeforeEnter;
      return this.flowNavigate(params);
    }
  }

  /**
   * Return a `route` object for vaadin-router
   *
   * It returns a `FlowRoute` object whose `path` property handles any route,
   * and the `action` returns the flow container without updating the content,
   * delaying the actual Flow server call to the `onBeforeEnter` phase.
   *
   * This is a `vaadin-router` specific API.
   */
  get route(): FlowRoute {
    return {
      path: '(.*)',
      action: this.action
    }
  }

  private get action(): (params: NavigationParameters) => Promise<HTMLRouterContainer> {
    // Return a function which is bound to the flow instance, thus we can use
    // the syntax `flow.route` in vaadin-router.
    // @ts-ignore
    return async (params: NavigationParameters) => {

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
      this.container.serverConnected = cancel =>
        resolve(cmd && cancel ? cmd.prevent() : this.container);

      // Call server side to navigate to the given route
      this.flowRoot.$server
        .connectClient(this.container.localName, this.container.id, this.getFlowRoute(ctx));
    });
  }

  private getFlowRoute(context: NavigationParameters | Location): string {
    return context.pathname + (context.search || '');
  }

  // import flow client modules and initialize UI in server side.
  private async flowInit(serverSideRouting = false): Promise<AppInitResponse> {
    // Do not start flow twice
    if (!this.response) {
      // Initialize server side UI
      this.response = await this.flowInitUi(serverSideRouting);

      // Enable or disable server side routing
      this.response.appConfig.webComponentMode = !serverSideRouting;

      // Load bootstrap script with server side parameters
      const bootstrapMod = await import('./FlowBootstrap');
      await bootstrapMod.init(this.response);

      // Load custom modules defined by user
      if (typeof this.config.imports === 'function') {
        await this.config.imports();
      }

      // Load flow-client module
      const clientMod = await import('./FlowClient');
      await this.flowInitClient(clientMod);

      // When client-side router, create a container for server views
      if (!serverSideRouting) {
        const id = this.response.appConfig.appId;
        // we use a custom tag for the flow app container
        const tag = `flow-container-${id.toLowerCase()}`;
        this.container = this.flowRoot.$[id] = document.createElement(tag);
        this.container.id = id;
        window.console.log("Created container for the flow UI with " + tag);
      }
    }
    return this.response;
  }

  // After the flow-client javascript module has been loaded, this initializes flow UI
  // in the browser.
  private async flowInitClient(clientMod: any): Promise<void> {
    clientMod.init();
    // client init is async, we need to loop until initialized
    return new Promise(resolve => {
      const $wnd = window as any;
      const intervalId = setInterval(() => {
        // client `isActive() == true` while initializing
        const initializing = Object.keys($wnd.Vaadin.Flow.clients)
          .reduce((prev, id) => prev || $wnd.Vaadin.Flow.clients[id].isActive(), false);
        if (!initializing) {
          clearInterval(intervalId);
          resolve();
        }
      }, 5);
    });
  }

  // Send the remote call to `JavaScriptBootstrapHandler` in order to init
  // server session and UI, and get the `appConfig` and `uidl`
  private async flowInitUi(serverSideRouting: boolean): Promise<AppInitResponse> {
    return new Promise((resolve, reject) => {
      const httpRequest = new (window as any).XMLHttpRequest();
      httpRequest.open('GET', 'VAADIN/?v-r=init' +
        (serverSideRouting ? `&location=${encodeURI(this.getFlowRoute(location))}` : ''));
      httpRequest.onload = () => {
        if (httpRequest.getResponseHeader('content-type') === 'application/json') {
          resolve(JSON.parse(httpRequest.responseText));
        } else {
          reject(new Error(
            `Invalid server response when initializing Flow UI.
            ${httpRequest.status}
            ${httpRequest.responseText}`));
        }
      };
      httpRequest.send();
    });
  }
}

